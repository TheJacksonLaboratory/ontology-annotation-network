package org.jax.oan.ontology;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.jax.oan.exception.OntologyAnnotationNetworkDataException;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.jax.oan.graph.GraphDatabaseOperations;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.AnnotationReference;
import org.monarchinitiative.phenol.annotations.formats.EvidenceCode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoaDiseaseDataContainer;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoaDiseaseDataLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class HpoOntologyAnnotationLoaderTest {

	final HpoOntologyAnnotationLoader graphLoader;

	final GraphDatabaseOperations graphDatabaseOperations;

	final Driver driver;


	public HpoOntologyAnnotationLoaderTest(Driver driver) throws IOException, OntologyAnnotationNetworkException {
		this.driver = driver;
		final GraphDatabaseWriter graphDatabaseWriter = new GraphDatabaseWriter(driver);
		this.graphLoader = new HpoOntologyAnnotationLoader(graphDatabaseWriter);
		this.graphDatabaseOperations = new GraphDatabaseOperations(graphDatabaseWriter);
		HpoDataResolver hpoDataResolver = HpoDataResolver.of(Path.of("src/test/resources"));
		Ontology hpoOntology = OntologyLoader.loadOntology(hpoDataResolver.hpJson().toFile());
		Ontology mondoOntology = OntologyLoader.loadOntology(hpoDataResolver.mondoJson().toFile());
		final HpoaDiseaseDataContainer container = HpoaDiseaseDataLoader.of(Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.ORPHANET)).loadDiseaseData(hpoDataResolver.phenotypeAnnotations());
		final HpoAssociationData associations = HpoAssociationData.builder(hpoOntology).mim2GeneMedgen(hpoDataResolver.mim2geneMedgen())
				.hpoDiseases(container).hgncCompleteSetArchive(hpoDataResolver.hgncCompleteSet()).build();
		graphDatabaseWriter.truncate();
		configureGraph(associations, container, hpoOntology, mondoOntology, hpoDataResolver.loinc());
	}

	void configureGraph(HpoAssociationData associations,
						HpoaDiseaseDataContainer container, Ontology hpoOntology, Ontology mondoOntology,
						Path loincPath) throws OntologyAnnotationNetworkDataException {
		graphLoader.phenotypes(hpoOntology.getTerms(), Map.of());
		graphLoader.diseases(container, mondoOntology.getTerms());
		graphLoader.diseaseToPhenotype(container, hpoOntology);
		graphLoader.genes(associations);
		graphLoader.diseaseToGene(associations);
		graphLoader.assayToPhenotype(loincPath);
	}

	@Test
	void phenotypes() {
		try(Session session = driver.session()){
			List<Node> nodes = session.run("MATCH (n: Phenotype) RETURN n")
					.list(record -> record.get("n").asNode());
			Node node = session.run("MATCH (n: Phenotype {id: 'HP:0000005'}) RETURN n").single().get("n").asNode();
			assertEquals(5, nodes.size());
			assertEquals("Fake term 5", node.get("name").asString());
		}
	}

	@Test
	void diseases() {
		try(Session session = driver.session()) {
			List<Node> nodes = session.run("MATCH (n: Disease) RETURN n")
					.list(record -> record.get("n").asNode());
			Node node = session.run("MATCH (n: Disease {id: 'OMIM:619340'}) RETURN n").single().get("n").asNode();
			assertEquals(2, nodes.size());
			assertEquals("Developmental and epileptic encephalopathy 96", node.get("name").asString());
			assertEquals("MONDO:0000001", node.get("mondoId").asString());
		}
	}

	@Test
	void genes() {
		try(Session session = driver.session()) {
			List<Node> nodes = session.run("MATCH (n: Gene) RETURN n")
					.list(record -> record.get("n").asNode());
			Node node = session.run("MATCH (n: Gene {name: 'NSF'}) RETURN n").single().get("n").asNode();
			assertEquals(2, nodes.size());
			assertEquals("NCBIGene:4905", node.get("id").asString());
		}
	}

	@Test
	void assayToPhenotype() {
		try(Session session = driver.session()) {
			List<Node> assay = session.run("MATCH (a: Assay) RETURN a")
					.list(record -> record.get("a").asNode());
			List<Node> queryAssayByPhenotype = session.run("MATCH (a: Assay)-[:MEASURES]-(p: Phenotype { id: 'HP:0000004'}) RETURN a").list(record ->
					record.get("a").asNode());
			assertEquals(3, assay.size());
			assertEquals(2, queryAssayByPhenotype.size());
		}

	}

	@Test
	void diseaseToGene() {
		try(Session session = driver.session()) {
			List<Node> allAnnotations = session.run("MATCH (n: Disease)-[:EXPRESSES]-(g: Gene) RETURN DISTINCT g").list(record -> record.get("g").asNode());
			List<Node> filteredAnnotations = session.run("MATCH (n: Disease {id: 'OMIM:619340'})-[:EXPRESSES]-(g: Gene) RETURN DISTINCT g")
					.list(record -> record.get("g").asNode());

			assertEquals(2, allAnnotations.size());
			assertEquals(1, filteredAnnotations.size());
			assertTrue(filteredAnnotations.stream().map(node -> node.get("id").asString()).toList().contains("NCBIGene:4905"));
		}
	}

	@Test
	void diseaseToPhenotype() {
		try(Session session = driver.session()) {
			List<Node> allAnnotations = session.run("MATCH " +
							"(n: Disease)-[:MANIFESTS]-(p: Phenotype)-[:WITH_METADATA]-(pm:PhenotypeMetadata) RETURN DISTINCT pm")
					.list(record -> record.get("pm").asNode());
			List<Node> filteredAnnotations = session.run("MATCH " +
							"(n: Disease {id: 'OMIM:609153'})-[:MANIFESTS]-(p: Phenotype)-[:WITH_METADATA {context: n.id}]-(pm: PhenotypeMetadata) RETURN DISTINCT pm")
					.list(record -> record.get("pm").asNode());

			assertEquals(5, allAnnotations.size());
			assertEquals(3, filteredAnnotations.size());
		}

	}


	@Test
	void formatSources() {
		List<AnnotationReference> singleReference = List.of(AnnotationReference.of(TermId.of("PMID:000913"), EvidenceCode.IEA));
		assertEquals("PMID:000913", HpoOntologyAnnotationLoader.formatSources(singleReference));
		List<AnnotationReference> multipleReferences = List.of(AnnotationReference.of(
				TermId.of("PMID:000913"), EvidenceCode.IEA), AnnotationReference.of(TermId.of("PMID:000924"), EvidenceCode.IEA));
		assertEquals("PMID:000913,PMID:000924", HpoOntologyAnnotationLoader.formatSources(multipleReferences));
	}

	@Test
	void formatFrequency() {
	}

	@Test
	void findMondoEquivalent(){
		TermId targetId = TermId.of("OMIM:619340");
		Term target = Term.builder(targetId).xrefs(
				List.of(
						new Dbxref("OMIM:619340", "", null)
				)
		).name("Bad Disease 1").build();
		Collection<Term> diseases = List.of(target);

		assertEquals(target, HpoOntologyAnnotationLoader.findMondoEquivalent(targetId, diseases).orElse(null));
	}
}
