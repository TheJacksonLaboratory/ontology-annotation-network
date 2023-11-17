package org.jax.oan.ontology;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.jax.oan.exception.OntologyAnnotationNetworkDataException;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.jax.oan.graph.Operations;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.AnnotationReference;
import org.monarchinitiative.phenol.annotations.formats.EvidenceCode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoaDiseaseDataContainer;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoaDiseaseDataLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class HpoGraphLoaderTest {

	final HpoGraphLoader graphLoader;

	final Operations operations;

	final Session session;

	public HpoGraphLoaderTest(Driver driver, HpoGraphLoader graphLoader, Operations operations) throws IOException, OntologyAnnotationNetworkException {
		this.graphLoader = graphLoader;
		this.operations = operations;
		HpoDataResolver hpoDataResolver = HpoDataResolver.of(Path.of("src/test/resources"));
		Ontology ontology = OntologyLoader.loadOntology(hpoDataResolver.hpJson().toFile());
		final HpoaDiseaseDataContainer container = HpoaDiseaseDataLoader.of(Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.ORPHANET)).loadDiseaseData(hpoDataResolver.phenotypeAnnotations());
		session = driver.session();
		final HpoAssociationData associations = HpoAssociationData.builder(ontology).mim2GeneMedgen(hpoDataResolver.mim2geneMedgen())
				.hpoDiseases(container).hgncCompleteSetArchive(hpoDataResolver.hgncCompleteSet()).build();
		configureGraph(associations, container, ontology, hpoDataResolver.loinc());
	}

	void configureGraph(HpoAssociationData associations, HpoaDiseaseDataContainer container, Ontology ontology,
						Path loincPath) throws OntologyAnnotationNetworkDataException {
		operations.truncate();
		graphLoader.phenotypes(session, ontology.getTerms(), Map.of());
		graphLoader.diseases(session, container);
		graphLoader.diseaseToPhenotype(session, container, ontology);
		graphLoader.genes(session, associations);
		graphLoader.diseaseToGene(session, associations);
		graphLoader.assayToPhenotype(session, loincPath);
	}

	@Test
	void phenotypes() {
		List<Node> nodes = session.run("MATCH (n: Phenotype) RETURN n")
				.list(record -> record.get("n").asNode());
		Node node = session.run("MATCH (n: Phenotype {id: 'HP:0000005'}) RETURN n").single().get("n").asNode();
		assertEquals(5, nodes.size());
		assertEquals("Fake term 5", node.get("name").asString());
	}

	@Test
	void diseases() {
		List<Node> nodes = session.run("MATCH (n: Disease) RETURN n")
				.list(record -> record.get("n").asNode());
		Node node = session.run("MATCH (n: Disease {id: 'OMIM:619340'}) RETURN n").single().get("n").asNode();
		assertEquals(2, nodes.size());
		assertEquals("Developmental and epileptic encephalopathy 96", node.get("name").asString());
	}

	@Test
	void genes() {
		List<Node> nodes = session.run("MATCH (n: Gene) RETURN n")
				.list(record -> record.get("n").asNode());
		Node node = session.run("MATCH (n: Gene {name: 'NSF'}) RETURN n").single().get("n").asNode();
		assertEquals(2, nodes.size());
		assertEquals("NCBIGene:4905", node.get("id").asString());
	}

	@Test
	void assayToPhenotype() {
		List<Node> assay = session.run("MATCH (a: Assay) RETURN a")
				.list(record -> record.get("a").asNode());
		List<Node> queryAssayByPhenotype = session.run("MATCH (a: Assay)-[:MEASURES]-(p: Phenotype { id: 'HP:0000004'}) RETURN a").list(record ->
				record.get("a").asNode());
		assertEquals(3, assay.size());
		assertEquals(2, queryAssayByPhenotype.size());

	}

	@Test
	void diseaseToGene() {
		List<Node> allAnnotations = session.run("MATCH (n: Disease)-[:EXPRESSES]-(g: Gene) RETURN DISTINCT g").list(record -> record.get("g").asNode());
		List<Node> filteredAnnotations = session.run("MATCH (n: Disease {id: 'OMIM:619340'})-[:EXPRESSES]-(g: Gene) RETURN DISTINCT g")
				.list(record -> record.get("g").asNode());

		assertEquals(2, allAnnotations.size());
		assertEquals(1, filteredAnnotations.size());
		assertTrue(filteredAnnotations.stream().map(node -> node.get("id").asString()).toList().contains("NCBIGene:4905"));
	}

	@Test
	void diseaseToPhenotype() {
		List<Node> allAnnotations = session.run("MATCH " +
						"(n: Disease)-[:MANIFESTS]-(p: Phenotype)-[:WITH_METADATA]-(pm:PhenotypeMetadata) RETURN DISTINCT pm")
				.list(record -> record.get("pm").asNode());
		List<Node> filteredAnnotations = session.run("MATCH " +
						"(n: Disease {id: 'OMIM:609153'})-[:MANIFESTS]-(p: Phenotype)-[:WITH_METADATA {context: n.id}]-(pm: PhenotypeMetadata) RETURN DISTINCT pm")
				.list(record -> record.get("pm").asNode());

		assertEquals(5,  allAnnotations.size());
		assertEquals(3, filteredAnnotations.size());

	}


	@Test
	void formatSources() {
		List<AnnotationReference> singleReference = List.of(AnnotationReference.of(TermId.of("PMID:000913"), EvidenceCode.IEA));
		assertEquals("PMID:000913", graphLoader.formatSources(singleReference));
		List<AnnotationReference> multipleReferences = List.of(AnnotationReference.of(
				TermId.of("PMID:000913"), EvidenceCode.IEA), AnnotationReference.of(TermId.of("PMID:000924"), EvidenceCode.IEA));
		assertEquals("PMID:000913,PMID:000924", graphLoader.formatSources(multipleReferences));
	}

	@Test
	void formatFrequency() {
	}
}
