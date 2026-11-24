package org.jax.oan.ontology;

import org.jax.oan.core.*;
import org.jax.oan.exception.OntologyAnnotationNetworkDataException;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.annotations.formats.AnnotationReference;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoGeneAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoOnset;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategories;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategoryLookup;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoAnnotationLine;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoaDiseaseDataContainer;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoaDiseaseDataLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class loads all data related to HpoOntology. Connecting phenotypes, diseases and genes with metadata.
 */
public class HpoOntologyAnnotationLoader implements OntologyAnnotationLoader {
	private final SqliteWriter sqliteWriter;
	private static final Logger logger = LoggerFactory.getLogger(HpoOntologyAnnotationLoader.class);

	public HpoOntologyAnnotationLoader(SqliteWriter sqliteWriter) {
		this.sqliteWriter = sqliteWriter;
	}

	/**
	 * Load a SQLite database with hpo data.
	 *
	 * @param hpoDataDirectory the directory for hpo graph.
	 * @throws IOException if a file can't be found
	 * @throws OntologyAnnotationNetworkException if things are not okay
	 */
	@Override
	public void load(Path hpoDataDirectory, Set<DiseaseDatabase> databases) throws IOException, OntologyAnnotationNetworkException {
		final long loadStart = System.nanoTime();
		long stage = System.nanoTime();
		final HpoDataResolver dataResolver = HpoDataResolver.of(hpoDataDirectory);
		SqliteSchema.createTables(sqliteWriter);
		final Ontology hpoOntology = OntologyLoader.loadOntology(dataResolver.hpJson().toFile());
		stage = logElapsed("Load HPO ontology", stage);
		final Ontology mondoOntology = OntologyLoader.loadOntology(dataResolver.mondoJson().toFile(), "MONDO");
		stage = logElapsed("Load MONDO ontology", stage);
		final HpoaDiseaseDataContainer diseases = HpoaDiseaseDataLoader.of(databases).loadDiseaseData(dataResolver.phenotypeAnnotations());
		stage = logElapsed("Load disease annotations", stage);
		final Path hgncCompleteSet = deduplicateHgncEntrezIds(dataResolver.hgncCompleteSet());
		final HpoAssociationData associations = HpoAssociationData.builder(hpoOntology).orphaToGenePath(dataResolver.orpha2Gene()).mim2GeneMedgen(dataResolver.mim2geneMedgen())
				.hpoDiseases(diseases).hgncCompleteSetArchive(hgncCompleteSet).build();
		stage = logElapsed("Build gene associations", stage);
		final MondoXrefIndex mondoIndex = MondoXrefIndex.of(mondoOntology.getTerms());
		stage = logElapsed("Index MONDO xrefs", stage);
		final Map<TermId, Term> mondoEquivalents = mondoEquivalents(diseases, mondoIndex);
		stage = logElapsed("Match diseases to MONDO", stage);
		Map<TermId, String> categories = phenotypeToCategory(hpoOntology);
		stage = logElapsed("Assign phenotype categories", stage);
		phenotypes(hpoOntology.getTerms(), categories);
		stage = logElapsed("Phenotypes", stage);
		diseases(diseases, mondoEquivalents);
		stage = logElapsed("Diseases", stage);
		genes(associations, HgncNames.read(hgncCompleteSet));
		stage = logElapsed("Genes", stage);
		phenotypeToPhenotype(hpoOntology.getTerms(), hpoOntology);
		stage = logElapsed("Phenotype to phenotype", stage);
		diseaseToPhenotype(diseases, hpoOntology);
		stage = logElapsed("Disease to phenotype", stage);
		geneToPhenotype(associations);
		stage = logElapsed("Gene to phenotype", stage);
		diseaseToGene(associations);
		stage = logElapsed("Disease to gene", stage);
		assayToPhenotype(dataResolver.loinc());
		stage = logElapsed("Assay to phenotype", stage);
		medicalAction(dataResolver.maxoa(), diseases, mondoEquivalents);
		stage = logElapsed("Medical actions", stage);
		SqliteSchema.createIndexes(sqliteWriter);
		logElapsed("Create indexes", stage);
		logElapsed("Total load", loadStart);
	}

	/**
	 * Logs how long a load stage took.
	 *
	 * @param stageName the name of the stage, as it appears in the log.
	 * @param startNanos the {@link System#nanoTime()} value when the stage started.
	 * @return the current {@link System#nanoTime()} value, to use as the start of the next stage.
	 */
	private static long logElapsed(String stageName, long startNanos) {
		long now = System.nanoTime();
		logger.info("Timing: {} took {} s", stageName, String.format("%.1f", (now - startNanos) / 1e9));
		return now;
	}

	/**
	 * HGNC's complete set occasionally carries two symbols pointing at the same NCBI Gene
	 * (entrez_id) -- a stale cross-reference on HGNC's side rather than a real ambiguity in
	 * NCBI's own id space. Phenol's association builder has no tolerance for that (a strict,
	 * no-duplicates toMap), so keep the first row seen per entrez_id and drop the rest,
	 * logging what was dropped so a real data problem stays visible.
	 */
	Path deduplicateHgncEntrezIds(Path hgncCompleteSet) throws IOException {
		List<String> lines = Files.readAllLines(hgncCompleteSet);
		if (lines.isEmpty()) {
			return hgncCompleteSet;
		}
		int entrezIdIndex = Arrays.asList(lines.get(0).split("\t", -1)).indexOf("entrez_id");
		if (entrezIdIndex < 0) {
			return hgncCompleteSet;
		}

		Set<String> seenEntrezIds = new HashSet<>();
		List<String> deduplicated = new ArrayList<>(lines.size());
		deduplicated.add(lines.get(0));
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] fields = line.split("\t", -1);
			String entrezId = entrezIdIndex < fields.length ? fields[entrezIdIndex] : "";
			if (!entrezId.isEmpty() && !seenEntrezIds.add(entrezId)) {
				logger.warn("Dropping HGNC row with duplicate entrez_id {}: {}", entrezId, fields.length > 1 ? fields[1] : line);
				continue;
			}
			deduplicated.add(line);
		}
		if (deduplicated.size() == lines.size()) {
			return hgncCompleteSet;
		}

		Path deduplicatedFile = Files.createTempFile("hgnc_complete_set_deduplicated", ".txt");
		Files.write(deduplicatedFile, deduplicated);
		deduplicatedFile.toFile().deleteOnExit();
		return deduplicatedFile;
	}

	void phenotypes(Collection<Term> phenotypes, Map<TermId, String> categories) throws OntologyAnnotationNetworkDataException {
			logger.info("Loading Phenotypes...");
			List<Object[]> rows = new ArrayList<>();
			for (Term term : phenotypes.stream().distinct().filter(t -> t.id().getPrefix().equals("HP")).toList()) {
				String category;
				try {
					category = categories.get(term.id());
				} catch (Exception e) {
					throw new OntologyAnnotationNetworkDataException(
							String.format("TermId %s could not get a category.", term.id().getValue()));
				}
				rows.add(new Object[]{term.id().getValue(), term.getName(), category});
			}
			sqliteWriter.batchInsert("INSERT OR IGNORE INTO phenotype (id, name, category) VALUES (?, ?, ?)", rows);
			logger.info("Done.");
	}

	/**
	 * Loads genes and the names they can be searched by.
	 *
	 * @param associations the gene identifiers to load.
	 * @param hgncNames the alternate names of each gene, keyed by Entrez id digits.
	 */
	void genes(HpoAssociationData associations, Map<String, List<String>> hgncNames){
		logger.info("Loading Genes...");
		List<Object[]> rows = new ArrayList<>();
		List<Object[]> nameRows = new ArrayList<>();
		associations.getGeneIdentifiers().forEach(g -> {
			String id = g.id().toString();
			rows.add(new Object[]{id, g.symbol()});
			nameRows.addAll(nameRows(id, g.symbol(), hgncNames.getOrDefault(g.id().getId(), List.of())));
		});
		sqliteWriter.batchInsert("INSERT OR IGNORE INTO gene (id, name) VALUES (?, ?)", rows);
		sqliteWriter.batchInsert("INSERT INTO gene_fts (gene_id, name, kind) VALUES (?, ?, ?)", nameRows);
		logger.info("Done.");
	}

	/**
	 * Builds the full-text rows for one entity's searchable names.
	 *
	 * @param id the entity id.
	 * @param primary the entity's primary name.
	 * @param synonyms the entity's other names.
	 * @return {@code [id, name, kind]} rows, the primary first with kind {@code primary}, then each synonym with
	 * kind {@code synonym}; blank names and names that repeat an earlier one, ignoring case, are dropped.
	 */
	static List<Object[]> nameRows(String id, String primary, Collection<String> synonyms) {
		List<Object[]> rows = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		if (primary != null && !primary.isBlank() && seen.add(primary.strip().toLowerCase())) {
			rows.add(new Object[]{id, primary.strip(), "primary"});
		}
		for (String synonym : synonyms) {
			if (synonym != null && !synonym.isBlank() && seen.add(synonym.strip().toLowerCase())) {
				rows.add(new Object[]{id, synonym.strip(), "synonym"});
			}
		}
		return rows;
	}

	void diseases(HpoaDiseaseDataContainer diseases, Map<TermId, Term> mondoEquivalents){
		logger.info("Loading Diseases...");
		List<Object[]> rows = new ArrayList<>();
		List<Object[]> nameRows = new ArrayList<>();
		Set<String> loaded = new HashSet<>();
		diseases.diseaseData().stream().distinct().forEach(d -> {
					Optional<Term> equivalent = Optional.ofNullable(mondoEquivalents.get(d.id()));
					String mondoId = "";
					String description = "No disease description found.";
					List<String> synonyms = new ArrayList<>();
					if (equivalent.isPresent()){
						mondoId = equivalent.get().id().getValue();
						description = equivalent.get().getDefinition();
						synonyms.add(equivalent.get().getName());
						equivalent.get().getSynonyms().stream()
								.filter(s -> s.getScope() == TermSynonymScope.EXACT || s.getScope() == TermSynonymScope.RELATED)
								.map(TermSynonym::getValue)
								.forEach(synonyms::add);
					}
					String id = d.id().toString();
					rows.add(new Object[]{id, d.name(), mondoId, description});
					if (loaded.add(id)) {
						nameRows.addAll(nameRows(id, d.name(), synonyms));
					}
				}
		);
		sqliteWriter.batchInsert("INSERT OR IGNORE INTO disease (id, name, mondo_id, description) VALUES (?, ?, ?, ?)", rows);
		sqliteWriter.batchInsert("INSERT INTO disease_fts (disease_id, name, kind) VALUES (?, ?, ?)", nameRows);
		logger.info("Done.");
	}

	void assayToPhenotype(Path loinc){
		logger.info("Loading Assay Relationships...");
		List<Object[]> assayRows = new ArrayList<>();
		List<Object[]> assayPhenotypeRows = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(loinc.toFile()))) {
			String line;
			reader.readLine();
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split("\t");
				String assayId = "LOINC:" + fields[1];
				assayRows.add(new Object[]{assayId, fields[0], fields[2]});
				assayPhenotypeRows.add(new Object[]{assayId, fields[4], fields[3]});
			}
			sqliteWriter.batchInsert("INSERT OR IGNORE INTO assay (id, name, scale) VALUES (?, ?, ?)", assayRows);
			sqliteWriter.batchInsert("INSERT OR IGNORE INTO assay_phenotype (assay_id, phenotype_id, outcome) VALUES (?, ?, ?)", assayPhenotypeRows);
			logger.info("Done.");
		} catch (IOException e) {
			throw new OntologyAnnotationNetworkRuntimeException("There was a problem with the required assay file format.", e);
		}
	}

	void phenotypeToPhenotype(Collection<Term> phenotypes, Ontology ontology){
		logger.info("Connecting Phenotypes...");
		List<Object[]> rows = new ArrayList<>();
			for (Term term : phenotypes.stream().distinct().filter(t -> t.id().getPrefix().equals("HP")).toList()) {
				for (TermId child : ontology.graph().getChildren(term.id())) {
					rows.add(new Object[]{term.id().getValue(), child.getValue()});
				}
			}
			sqliteWriter.batchInsert("INSERT OR IGNORE INTO phenotype_child (parent_id, child_id) VALUES (?, ?)", rows);
		logger.info("Done.");
	}

	void diseaseToGene(HpoAssociationData associations){
			logger.info("Loading Disease to Gene Relationships...");
			List<Object[]> rows = new ArrayList<>();
			associations.associations().diseaseIdToGeneAssociations().forEach((key, value) -> value.forEach(x ->
				rows.add(new Object[]{key.toString(), x.geneIdentifier().id().toString()})
			));
			sqliteWriter.batchInsert("INSERT OR IGNORE INTO disease_gene (disease_id, gene_id) VALUES (?, ?)", rows);
			logger.info("Done.");
	}

	void diseaseToPhenotype(HpoaDiseaseDataContainer diseases, Ontology ontology){
			logger.info("Loading Disease to Phenotype Relationships...");
			List<Object[]> rows = new ArrayList<>();
			Set<HpoAnnotationLine> lines = diseases.diseaseData().stream().flatMap(d -> d.annotationLines().stream()).collect(Collectors.toSet());
			for (HpoAnnotationLine line: lines) {
				String onset = line.onset().map(HpoOnset::id).map(TermId::getValue).orElse("");
				String frequency = formatFrequency(line.frequency(), ontology);
				String sources = formatSources(line.annotationReferences());
				String sex = "";
				if (line.sex() != null) {
					sex = line.sex().toString();
				}
				rows.add(new Object[]{line.diseaseId().toString(), line.phenotypeTermId().getValue(), sex, onset, frequency, sources});
			}
			sqliteWriter.batchInsert(
					"INSERT INTO disease_phenotype (disease_id, phenotype_id, sex, onset, frequency, sources) VALUES (?, ?, ?, ?, ?, ?)",
					rows);
			logger.info("Done.");
	}

	void geneToPhenotype(HpoAssociationData associations){
		logger.info("Loading Gene to Phenotype Relationships...");
		List<Object[]> rows = new ArrayList<>();
		for (HpoGeneAnnotation annotation: associations.hpoToGeneAnnotations()){
			final TermId ncbiGene = TermId.of(String.format("NCBIGene:%s", annotation.getEntrezGeneId()));
			rows.add(new Object[]{ncbiGene.getValue(), annotation.id().getValue()});
		}
		sqliteWriter.batchInsert("INSERT OR IGNORE INTO gene_phenotype (gene_id, phenotype_id) VALUES (?, ?)", rows);
		logger.info("Done");
	}

	void medicalAction(Path maxoa, HpoaDiseaseDataContainer diseases, Map<TermId, Term> mondoEquivalents){
		logger.info("Loading Medical Action Relationships...");
		final TermId root = TermId.of("HP:0000118");

		Map<String, String> omimMondoToDiseaseId = new HashMap<>();
		diseases.diseaseData().stream().distinct().forEach(d ->
			Optional.ofNullable(mondoEquivalents.get(d.id())).ifPresent(term -> {
				if (d.id().toString().contains("OMIM")) {
					omimMondoToDiseaseId.put(term.id().getValue(), d.id().toString());
				}
			}));

		List<Object[]> actionRows = new ArrayList<>();
		List<Object[]> targetRows = new ArrayList<>();
		List<Object[]> annotationRows = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(maxoa.toFile()))) {
			String line;
			reader.readLine();

			while ((line = reader.readLine()) != null) {
				String[] fields = line.split("\t");
				String mondoId = fields[0];
				TermId phenotype = TermId.of(fields[5]);
				String medicalActionId = fields[3];
				actionRows.add(new Object[]{medicalActionId, fields[4]});

				if (phenotype.getPrefix().contains("MONDO")){
					phenotype = root;
				}

				String diseaseId = omimMondoToDiseaseId.get(mondoId);
				if (diseaseId == null) {
					continue;
				}

				MedicalActionMetadata meta = fields[8].isEmpty()
						? new MedicalActionMetadata(fields[2], Evidence.valueOf(fields[7]), null, MedicalActionRelation.valueOf(fields[6]), fields[12])
						: new MedicalActionMetadata(fields[2], Evidence.valueOf(fields[7]), new Extension(TermId.of(fields[8]), fields[9]), MedicalActionRelation.valueOf(fields[6]), fields[12]);

				targetRows.add(new Object[]{medicalActionId, phenotype.getValue(), diseaseId, meta.medicalActionRelation().toString()});
				annotationRows.add(new Object[]{medicalActionId, diseaseId, phenotype.getValue(), meta.evidence().toString(),
						meta.author(), meta.sourceId(),
						meta.extension() != null ? meta.extension().getId() : "",
						meta.extension() != null ? meta.extension().getName() : ""});
			}
			sqliteWriter.batchInsert("INSERT OR IGNORE INTO medical_action (id, name) VALUES (?, ?)", actionRows);
			sqliteWriter.batchInsert("INSERT OR IGNORE INTO medical_action_target (medical_action_id, phenotype_id, disease_id, relation) VALUES (?, ?, ?, ?)", targetRows);
			sqliteWriter.batchInsert("INSERT INTO medical_action_annotation (medical_action_id, disease_id, phenotype_id, evidence, author, source, extension_id, extension_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", annotationRows);
			logger.info("Done.");
		} catch (IOException e) {
			throw new OntologyAnnotationNetworkRuntimeException("There was a problem with the required assay file format.", e);
		}
	}



	Map<TermId, String> phenotypeToCategory(Ontology hpoOntology){
		HpoCategoryLookup hpoCategoryLookup = new HpoCategoryLookup(hpoOntology.graph(), HpoCategories.preset());
		List<TermId> allTerms = hpoOntology.getTerms().stream().map(Term::id)
				.filter(x -> x.getPrefix().equals("HP")).distinct().toList();
		return allTerms.stream().collect(Collectors.toMap(t -> t, t -> {
					Optional<Term> term = hpoCategoryLookup.getPrioritizedCategory(t);
					if (term.isEmpty()) {
						return "Other";
					} else {
						return term.get().getName();
					}
				}));
	}

	static String formatSources(List<AnnotationReference> sources){
		final String joinedSources = sources.stream().map(AnnotationReference::id).map(TermId::getValue).collect(Collectors.joining(";"));
		return  joinedSources.length() > 1 ? joinedSources : "UNKNOWN";
	}

	static String formatFrequency(String frequency, Ontology ontology){
		if(frequency.startsWith("HP:")){
			return ontology.getTermLabel(TermId.of(frequency)).orElse("");
		} else if(frequency.equals("n/a") || frequency.isEmpty()) {
			return "";
		}
		return frequency;
	}

	/**
	 * Finds the MONDO equivalent of each disease.
	 *
	 * @param diseases the diseases to match.
	 * @param mondoIndex the index of MONDO terms by xref.
	 * @return the equivalent MONDO term keyed by disease id, for the diseases that have one.
	 */
	static Map<TermId, Term> mondoEquivalents(HpoaDiseaseDataContainer diseases, MondoXrefIndex mondoIndex) {
		Map<TermId, Term> equivalents = new HashMap<>();
		diseases.diseaseData().stream().distinct().forEach(d ->
				findMondoEquivalent(d.id(), d.name(), mondoIndex).ifPresent(term -> equivalents.putIfAbsent(d.id(), term)));
		return equivalents;
	}

	/**
	 * Finds the MONDO term equivalent to a disease using a prebuilt xref index.
	 *
	 * @param target the disease id, such as OMIM:154700 or ORPHA:558.
	 * @param targetName the disease name, used to choose between several matching terms.
	 * @param mondoIndex the index of MONDO terms by xref.
	 * @return the equivalent MONDO term, or an empty optional if there is none or the match is ambiguous.
	 */
	static Optional<Term> findMondoEquivalent(TermId target, String targetName, MondoXrefIndex mondoIndex){
		 List<Term> equivalence = mondoIndex.termsWithXref(target.toString(),
				 TermId.of(AlternativePrefix.from(target.getPrefix()), target.getId()).getValue());

		 if (equivalence.isEmpty()) {
			 return Optional.empty();
		 } else if (equivalence.size() > 1) {
			  // Sometimes we have errenous equivalence so we want to then see if we can find if the names start with each other
			 // Make sure our target has a name
			 if (targetName.length() > 3){
				 List<Term> equivalenceByName = equivalence.stream().filter(a -> a.getName().toLowerCase().startsWith(targetName.substring(0, 3).toLowerCase())).toList();
				 if (equivalenceByName.isEmpty()){
					 return Optional.empty();
				 } else {
					 return equivalenceByName.stream().findFirst();
				 }
			 } else {
				 return Optional.empty();
			 }
		 } else {
			 return equivalence.stream().findFirst();
		 }
	}

	/**
	 * MONDO terms indexed by the value of each of their xrefs, such as OMIM:154700.
	 */
	static final class MondoXrefIndex {
		private final Map<String, List<Term>> termsByXref;

		private MondoXrefIndex(Map<String, List<Term>> termsByXref) {
			this.termsByXref = termsByXref;
		}

		/**
		 * Builds the index. Xrefs that are not valid term ids are skipped and counted in a warning.
		 *
		 * @param mondoTerms the MONDO terms to index.
		 * @return an index over the xrefs of {@code mondoTerms}.
		 */
		static MondoXrefIndex of(Collection<Term> mondoTerms) {
			Map<String, List<Term>> termsByXref = new HashMap<>();
			int skipped = 0;
			for (Term term : mondoTerms) {
				for (Dbxref xref : term.getXrefs()) {
					String value;
					try {
						value = TermId.of(xref.getName()).getValue();
					} catch (PhenolRuntimeException e) {
						skipped++;
						continue;
					}
					termsByXref.computeIfAbsent(value, k -> new ArrayList<>()).add(term);
				}
			}
			if (skipped > 0) {
				logger.warn("Skipped {} MONDO xrefs that are not valid term ids", skipped);
			}
			return new MondoXrefIndex(termsByXref);
		}

		/**
		 * Returns the terms that have an xref equal to any of the given values.
		 *
		 * @param xrefValues the xref values to look up, such as OMIM:154700.
		 * @return the matching terms without duplicates, ordered by the value they matched and then by source order.
		 */
		List<Term> termsWithXref(String... xrefValues) {
			List<Term> matches = new ArrayList<>();
			for (String value : xrefValues) {
				for (Term term : termsByXref.getOrDefault(value, List.of())) {
					if (matches.stream().noneMatch(m -> m == term)) {
						matches.add(term);
					}
				}
			}
			return matches;
		}
	}
}
