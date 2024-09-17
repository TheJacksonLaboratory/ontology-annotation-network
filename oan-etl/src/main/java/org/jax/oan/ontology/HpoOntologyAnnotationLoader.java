package org.jax.oan.ontology;

import org.jax.oan.core.*;
import org.jax.oan.exception.OntologyAnnotationNetworkDataException;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;
import org.jax.oan.graph.GraphDatabaseOperations;
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
import org.neo4j.driver.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;

/**
 * This class loads all data related to HpoOntology. Connecting phenotypes, diseases and genes with metadata.
 */
public class HpoOntologyAnnotationLoader implements OntologyAnnotationLoader {
	private final GraphDatabaseOperations graphDatabaseOperations;
	private final GraphWriter graphWriter;
	private static final Logger logger = LoggerFactory.getLogger(HpoOntologyAnnotationLoader.class);

	public HpoOntologyAnnotationLoader(GraphWriter graphWriter) {
		this.graphDatabaseOperations = new GraphDatabaseOperations(graphWriter);
		this.graphWriter = graphWriter;
	}

	@Override
	public GraphWriter graphWriter() {
		return this.graphWriter;
	}

	/**
	 * Load Neo4J Graph with hpo data.
	 *
	 * @param hpoDataDirectory the directory for hpo graph.
	 * @throws IOException if a file can't be found
	 * @throws OntologyAnnotationNetworkException if things are not okay
	 */
	@Override
	public void load(Path hpoDataDirectory, Set<DiseaseDatabase> databases) throws IOException, OntologyAnnotationNetworkException {
		final HpoDataResolver dataResolver = HpoDataResolver.of(hpoDataDirectory);
		final Ontology hpoOntology = OntologyLoader.loadOntology(dataResolver.hpJson().toFile());
		final Ontology mondoOntology = OntologyLoader.loadOntology(dataResolver.mondoJson().toFile(), "MONDO");
		final HpoaDiseaseDataContainer diseases = HpoaDiseaseDataLoader.of(databases).loadDiseaseData(dataResolver.phenotypeAnnotations());
		final HpoAssociationData associations = HpoAssociationData.builder(hpoOntology).orphaToGenePath(dataResolver.orpha2Gene()).mim2GeneMedgen(dataResolver.mim2geneMedgen())
				.hpoDiseases(diseases).hgncCompleteSetArchive(dataResolver.hgncCompleteSet()).build();
		graphDatabaseOperations.dropIndexes(OntologyModule.HPO);
		graphDatabaseOperations.createIndexes(OntologyModule.HPO);
		Map<TermId, String> categories = phenotypeToCategory(hpoOntology);
		phenotypes(hpoOntology.getTerms(), categories);
		diseases(diseases, mondoOntology.getTerms());
		genes(associations);
		phenotypeToPhenotype(hpoOntology.getTerms(), hpoOntology);
		diseaseToPhenotype(diseases, hpoOntology);
		geneToPhenotype(associations);
		diseaseToGene(associations);
		assayToPhenotype(dataResolver.loinc());
		medicalAction(dataResolver.maxoa());
	}
	void phenotypes(Collection<Term> phenotypes, Map<TermId, String> categories) throws OntologyAnnotationNetworkDataException {
			logger.info("Loading Phenotypes...");
			ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
			for (Term term : phenotypes.stream().distinct().toList()) {
				String category;
				try {
					category = categories.get(term.id());
				} catch (Exception e) {
					throw new OntologyAnnotationNetworkDataException(
							String.format("TermId %s could not get a category.", term.id().getValue()));
				}
				Query query = new Query("CREATE (p:Phenotype {id: $id, name: $name, category: $category})",
						parameters("id", term.id().getValue(),
								"name", term.getName(), "category", category));
				queries.add(query);
			}
			graphWriter().write(queries);
			logger.info("Done.");
	}

	void genes(HpoAssociationData associations){
		logger.info("Loading Genes...");
		ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
		associations.getGeneIdentifiers().forEach( g -> {
			Query query = new Query("CREATE (g: Gene {id: $id, name: $name})",
					parameters("id", g.id().toString(), "name", g.symbol()));
			queries.add(query);
			}
		);
		graphWriter().write(queries);
		logger.info("Done.");
	}

	void diseases(HpoaDiseaseDataContainer diseases, Collection<Term> mondoTerms){
		logger.info("Loading Diseases...");
		ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
		diseases.diseaseData().stream().distinct().forEach(d -> {
					Optional<Term> equivalent = findMondoEquivalent(d.id(), d.name(), mondoTerms);
					String mondoId = "";
					String description = "No disease description found.";
					if (equivalent.isPresent()){
						mondoId = equivalent.get().id().getValue();
						description = equivalent.get().getDefinition();
					}
					Query query =  new Query("CREATE (d:Disease {id: $id, name: $name, mondoId: $mondoId, description: $description})",
							parameters("name", d.name(), "id", d.id().toString(), "mondoId", mondoId, "description", description));
					queries.add(query);
				}
		);
		graphWriter().write(queries);
		logger.info("Done.");
	}

	void assayToPhenotype(Path loinc){
		logger.info("Loading Assay Relationships...");
		ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
		try (BufferedReader reader = new BufferedReader(new FileReader(loinc.toFile()))) {
			String line;
			reader.readLine();
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split("\t");
				Query createAssay = new Query("MERGE (a:Assay {id: $id, name: $name, scale: $scale})",
						parameters("id", fields[1], "name",fields[0], "scale", fields[2]));
				Query connectPhenotype = new Query("MATCH (p:Phenotype {id: $phenotypeId}), (a:Assay {id: $assayId}) " +
								"MERGE (a)-[:MEASURES {outcome: $outcome}]->(p)",
						parameters("assayId", fields[1], "outcome", fields[3], "phenotypeId", fields[4]));
				queries.add(createAssay);
				queries.add(connectPhenotype);
			}
			logger.info("Done.");
			graphWriter().write(queries);
		} catch (IOException e) {
			throw new OntologyAnnotationNetworkRuntimeException("There was a problem with the required assay file format.", e);
		}
	}

	void phenotypeToPhenotype(Collection<Term> phenotypes, Ontology ontology){
		logger.info("Connecting Phenotypes...");
		ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
			for (Term term : phenotypes.stream().distinct().toList()) {
				for (TermId child : ontology.graph().getChildren(term.id())) {
					Query query = new Query("MATCH (p: Phenotype {id: $source}), (c: Phenotype {id: $child}) MERGE (p)-[:HAS_CHILD]->(c)",
							parameters("source", term.id().getValue(), "child", child.getValue()));
					queries.add(query);
				}
			}
			graphWriter().write(queries);
		logger.info("Done.");
	}

	void diseaseToGene(HpoAssociationData associations){
			logger.info("Loading Disease to Gene Relationships...");
			ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
			associations.associations().diseaseIdToGeneAssociations().forEach((key, value) -> value.forEach(x -> {
				Query query = new Query("MATCH (d:Disease {id: $diseaseId}), (g:Gene {id: $geneId}) " +
						"MERGE (d)-[:EXPRESSES]->(g)",
						parameters("diseaseId", key.toString(),
								"geneId", x.geneIdentifier().id().toString()));
				queries.add(query);
				}
			));
			graphWriter.write(queries);
			logger.info("Done.");


	}

	void diseaseToPhenotype(HpoaDiseaseDataContainer diseases, Ontology ontology){
			logger.info("Loading Disease to Phenotype Relationships...");
			ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
			Set<HpoAnnotationLine> lines = diseases.diseaseData().stream().flatMap(d -> d.annotationLines().stream()).collect(Collectors.toSet());
			for (HpoAnnotationLine line: lines) {
				String onset = line.onset().map(HpoOnset::id).map(TermId::getValue).orElse("");
				String frequency = formatFrequency(line.frequency(), ontology);
				String sources = formatSources(line.annotationReferences());
				String sex = "";
				if (line.sex() != null) {
					sex = line.sex().toString();
				}
				Query query = new Query("MATCH (d:Disease {id: $diseaseId}), (p:Phenotype {id: $phenotypeId})" +
						"WITH d,p MERGE (pa: PhenotypeAnnotation {onset: $onset, frequency: $frequency, sex: $sex, sources: $source}) " +
						"WITH d,p,pa MERGE (pa)-[:DESCRIBES {context: $diseaseId}]->(p) " +
						"WITH p,d MERGE (p)-[:MANIFESTS]->(d)",
						parameters(
								"diseaseId", line.diseaseId().toString(),
								"phenotypeId", line.phenotypeTermId().getValue(),
								"onset", onset, "frequency", frequency, "sex", sex, "source", sources));
				queries.add(query);
			}
			graphWriter.write(queries);
			logger.info("Done.");
	}

	void geneToPhenotype(HpoAssociationData associations){
		logger.info("Loading Gene to Phenotype Relationships...");
		ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
		for (HpoGeneAnnotation annotation: associations.hpoToGeneAnnotations()){
			final TermId ncbiGene = TermId.of(String.format("NCBIGene:%s", annotation.getEntrezGeneId()));
			Query query = new Query("MATCH (g: Gene {id: $geneId}), (p: Phenotype {id: $phenotypeId})" +
					" MERGE (g)-[:DETERMINES]-(p)", parameters("geneId", ncbiGene.getValue(),
					"phenotypeId", annotation.id().getValue()));
			queries.add(query);
		}
		graphWriter().write(queries);
		logger.info("Done");
	}

	void medicalAction(Path maxoa){
		logger.info("Loading Medical Action Relationships...");
		final TermId root = TermId.of("HP:0000118");
		ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
		try (BufferedReader reader = new BufferedReader(new FileReader(maxoa.toFile()))) {
			queries.clear();
			String line;
			reader.readLine();

			while ((line = reader.readLine()) != null) {
				String[] fields = line.split("\t");
				TermId mondo = TermId.of(fields[0]);
				TermId phenotype = TermId.of(fields[5]);
				MedicalActionMetadata meta;
				Query connectMedicalAction;
				Query createMedicalAction = new Query("MERGE (m:MedicalAction {id: $id}) ON CREATE SET m.name = $name",
						parameters("id", fields[3], "name",fields[4]));

				if (phenotype.getPrefix().contains("MONDO")){
					phenotype = root;
				}

				if (!fields[8].isEmpty()){
					 meta = new MedicalActionMetadata(fields[2], Evidence.valueOf(fields[7]), new Extension(TermId.of(fields[8]), fields[9]), MedicalActionRelation.valueOf(fields[6]), fields[12]);
					connectMedicalAction = new Query("MATCH (d:Disease {mondoId: $diseaseId}), (p:Phenotype {id: $phenotypeId}), (m:MedicalAction {id: $medicalAction}) WHERE d.id IS NOT NULL and d.id contains 'OMIM' " +
							"WITH d,p,m MERGE (mm: MedicalActionAnnotation {evidence: $evidenceCode, author: $author, source: $sourceId, extensionId: $extensionId, extensionName: $extensionName})" +
							"WITH d,p,mm,m MERGE (mm)-[:DESCRIBES {dcontext: d.id, pcontext: p.id }]->(m) " +
							"WITH m,p,d MERGE (m)-[:CLARIFIES {by:$relation, context: d.id}]->(p)",
							parameters("medicalAction", fields[3], "diseaseId", mondo.getValue(), "phenotypeId", phenotype.getValue(),
									"relation", meta.medicalActionRelation().toString(), "sourceId", meta.sourceId(), "evidenceCode", meta.evidence().toString(), "author", meta.author(), "extensionId", meta.extension().getId(), "extensionName", meta.extension().getName()));
				} else {
					 meta = new MedicalActionMetadata(fields[2], Evidence.valueOf(fields[7]), null, MedicalActionRelation.valueOf(fields[6]), fields[12]);
					 connectMedicalAction = new Query("MATCH (d:Disease {mondoId: $diseaseId}), (p:Phenotype {id: $phenotypeId}), (m:MedicalAction {id: $medicalAction}) WHERE d.id IS NOT NULL and d.id contains 'OMIM' " +
							 "WITH d,p,m MERGE (mm: MedicalActionAnnotation {evidence: $evidenceCode, author: $author, source: $sourceId, extensionId: $extensionId, extensionName: $extensionName})" +
							 "WITH d,p,mm,m MERGE (mm)-[:DESCRIBES {dcontext: d.id, pcontext: p.id }]->(m) " +
							 "WITH m,p,d MERGE (m)-[:CLARIFIES {by:$relation, context: d.id}]->(p)",
							 parameters("medicalAction", fields[3], "diseaseId", mondo.getValue(), "phenotypeId", phenotype.getValue(),
									 "relation", meta.medicalActionRelation().toString(), "sourceId", meta.sourceId(), "evidenceCode", meta.evidence().toString(), "author", meta.author(), "extensionId", "", "extensionName", ""));

				}
				queries.add(createMedicalAction);
				queries.add(connectMedicalAction);
			}
			graphWriter().write(queries);
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

	static Optional<Term> findMondoEquivalent(TermId target, String targetName, Collection<Term> diseases){
		 List<Term> equivalence = diseases.stream().filter(term ->
				term.getXrefs().stream().map(Dbxref::getName).map(TermId::of).anyMatch(s ->
						s.getValue().equals(target.toString()) || s.getValue().equals(
								TermId.of(AlternativePrefix.from(target.getPrefix()), target.getId()).getValue())
				)

		).toList();

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
}
