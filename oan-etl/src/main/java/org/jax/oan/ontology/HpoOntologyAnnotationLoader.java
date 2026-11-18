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
		final HpoAssociationData associations = HpoAssociationData.builder(hpoOntology).orphaToGenePath(dataResolver.orpha2Gene()).mim2GeneMedgen(dataResolver.mim2geneMedgen())
				.hpoDiseases(diseases).hgncCompleteSetArchive(deduplicateHgncEntrezIds(dataResolver.hgncCompleteSet())).build();
		stage = logElapsed("Build gene associations", stage);
		final MondoXrefIndex mondoIndex = MondoXrefIndex.of(mondoOntology.getTerms());
		stage = logElapsed("Index MONDO xrefs", stage);
		Map<TermId, String> categories = phenotypeToCategory(hpoOntology);
		stage = logElapsed("Assign phenotype categories", stage);
		phenotypes(hpoOntology.getTerms(), categories);
		stage = logElapsed("Phenotypes", stage);
		diseases(diseases, mondoIndex);
		stage = logElapsed("Diseases", stage);
		genes(associations);
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
		medicalAction(dataResolver.maxoa(), diseases, mondoIndex);
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

	void genes(HpoAssociationData associations){
		logger.info("Loading Genes...");
		List<Object[]> rows = new ArrayList<>();
		associations.getGeneIdentifiers().forEach(g -> rows.add(new Object[]{g.id().toString(), g.symbol()}));
		sqliteWriter.batchInsert("INSERT OR IGNORE INTO gene (id, name) VALUES (?, ?)", rows);
		logger.info("Done.");
	}

	void diseases(HpoaDiseaseDataContainer diseases, MondoXrefIndex mondoIndex){
		logger.info("Loading Diseases...");
		List<Object[]> rows = new ArrayList<>();
		diseases.diseaseData().stream().distinct().forEach(d -> {
					Optional<Term> equivalent = findMondoEquivalent(d.id(), d.name(), mondoIndex);
					String mondoId = "";
					String description = "No disease description found.";
					if (equivalent.isPresent()){
						mondoId = equivalent.get().id().getValue();
						description = equivalent.get().getDefinition();
					}
					rows.add(new Object[]{d.id().toString(), d.name(), mondoId, description});
				}
		);
		sqliteWriter.batchInsert("INSERT OR IGNORE INTO disease (id, name, mondo_id, description) VALUES (?, ?, ?, ?)", rows);
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

	void medicalAction(Path maxoa, HpoaDiseaseDataContainer diseases, MondoXrefIndex mondoIndex){
		logger.info("Loading Medical Action Relationships...");
		final TermId root = TermId.of("HP:0000118");

		Map<String, String> omimMondoToDiseaseId = new HashMap<>();
		diseases.diseaseData().stream().distinct().forEach(d ->
			findMondoEquivalent(d.id(), d.name(), mondoIndex).ifPresent(term -> {
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
	 * Finds the MONDO term equivalent to a disease by scanning the xrefs of the given terms.
	 *
	 * @param target the disease id, such as OMIM:154700 or ORPHA:558.
	 * @param targetName the disease name, used to choose between several matching terms.
	 * @param diseases the MONDO terms to search.
	 * @return the equivalent MONDO term, or an empty optional if there is none or the match is ambiguous.
	 */
	static Optional<Term> findMondoEquivalent(TermId target, String targetName, Collection<Term> diseases){
		return findMondoEquivalent(target, targetName, MondoXrefIndex.of(diseases));
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
		private final List<Term> terms;
		private final Map<String, List<Integer>> positionsByXref;

		private MondoXrefIndex(List<Term> terms, Map<String, List<Integer>> positionsByXref) {
			this.terms = terms;
			this.positionsByXref = positionsByXref;
		}

		/**
		 * Builds the index. Xrefs that are not valid term ids are skipped and counted in a warning.
		 *
		 * @param mondoTerms the MONDO terms to index.
		 * @return an index over the xrefs of {@code mondoTerms}.
		 */
		static MondoXrefIndex of(Collection<Term> mondoTerms) {
			List<Term> terms = List.copyOf(mondoTerms);
			Map<String, List<Integer>> positionsByXref = new HashMap<>();
			int skipped = 0;
			for (int i = 0; i < terms.size(); i++) {
				for (Dbxref xref : terms.get(i).getXrefs()) {
					String value;
					try {
						value = TermId.of(xref.getName()).getValue();
					} catch (PhenolRuntimeException e) {
						skipped++;
						continue;
					}
					positionsByXref.computeIfAbsent(value, k -> new ArrayList<>()).add(i);
				}
			}
			if (skipped > 0) {
				logger.warn("Skipped {} MONDO xrefs that are not valid term ids", skipped);
			}
			return new MondoXrefIndex(terms, positionsByXref);
		}

		/**
		 * Returns the terms that have an xref equal to any of the given values.
		 *
		 * @param xrefValues the xref values to look up, such as OMIM:154700.
		 * @return the matching terms without duplicates, in the order of the collection the index was built from.
		 */
		List<Term> termsWithXref(String... xrefValues) {
			SortedSet<Integer> positions = new TreeSet<>();
			for (String value : xrefValues) {
				positions.addAll(positionsByXref.getOrDefault(value, List.of()));
			}
			return positions.stream().map(terms::get).toList();
		}
	}
}
