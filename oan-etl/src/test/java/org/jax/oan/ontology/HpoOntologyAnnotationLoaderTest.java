package org.jax.oan.ontology;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.jax.oan.graph.GraphDatabaseOperations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.monarchinitiative.phenol.annotations.formats.AnnotationReference;
import org.monarchinitiative.phenol.annotations.formats.EvidenceCode;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import org.monarchinitiative.phenol.ontology.data.TermId;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@MicronautTest(environments = "test")
class HpoOntologyAnnotationLoaderTest {

	HpoOntologyAnnotationLoader graphLoader;

	GraphDatabaseOperations graphDatabaseOperations;

	SqliteWriter sqliteWriter;

	@Inject
	Driver driver;

	@TempDir
	static Path tempDir;

	Ontology hpoOntology;

	@BeforeAll
	void setup() throws OntologyAnnotationNetworkException, IOException {
		final GraphDatabaseWriter graphDatabaseWriter = new GraphDatabaseWriter(this.driver);
		this.sqliteWriter = new SqliteWriter(tempDir.resolve("test.db"));
		this.graphLoader = new HpoOntologyAnnotationLoader(graphDatabaseWriter, sqliteWriter);
		this.graphDatabaseOperations = new GraphDatabaseOperations(graphDatabaseWriter);
		graphLoader.load(Path.of("src/test/resources"), Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.ORPHANET));
		this.hpoOntology = OntologyLoader.loadOntology(Path.of("src/test/resources/hp-simple-non-classified.json").toFile());
	}

	@Test
	void sqliteTablesExist() throws Exception {
		try (var statement = sqliteWriter.connection().createStatement()) {
			var rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='phenotype'");
			assertTrue(rs.next());
		}
	}

	@Test
	void phenotypes() throws Exception {
		try (var statement = sqliteWriter.connection().createStatement()) {
			var all = statement.executeQuery("SELECT COUNT(*) AS c FROM phenotype");
			all.next();
			assertEquals(7, all.getInt("c"));

			var one = statement.executeQuery("SELECT name FROM phenotype WHERE id = 'HP:0000005'");
			one.next();
			assertEquals("Fake term 5", one.getString("name"));
		}
	}

	@Test
	void phenotypeToPhenotype() throws Exception {
		try (var statement = sqliteWriter.connection().createStatement()) {
			var rs = statement.executeQuery(
					"SELECT child_id FROM phenotype_child WHERE parent_id = 'HP:0000001' ORDER BY child_id");
			List<String> children = new ArrayList<>();
			while (rs.next()) children.add(rs.getString("child_id"));
			assertEquals(List.of("HP:0000002", "HP:0000004"), children);
		}
	}

	@Test
	void diseases() throws Exception {
		try (var statement = sqliteWriter.connection().createStatement()) {
			var all = statement.executeQuery("SELECT COUNT(*) AS c FROM disease");
			all.next();
			assertEquals(3, all.getInt("c"));

			var one = statement.executeQuery("SELECT name, mondo_id FROM disease WHERE id = 'OMIM:619340'");
			one.next();
			assertEquals("Developmental and epileptic encephalopathy 96", one.getString("name"));
			assertEquals("MONDO:0000001", one.getString("mondo_id"));

			var orpha = statement.executeQuery("SELECT mondo_id FROM disease WHERE id = 'ORPHA:99999'");
			orpha.next();
			assertEquals("MONDO:0008854", orpha.getString("mondo_id"));
		}
	}

	@Test
	void genes() throws Exception {
		try (var statement = sqliteWriter.connection().createStatement()) {
			var all = statement.executeQuery("SELECT COUNT(*) AS c FROM gene");
			all.next();
			assertEquals(2, all.getInt("c"));

			var one = statement.executeQuery("SELECT id FROM gene WHERE name = 'NSF'");
			one.next();
			assertEquals("NCBIGene:4905", one.getString("id"));
		}
	}

	@Test
	void geneToPhenotype() throws Exception {
		try (var statement = sqliteWriter.connection().createStatement()) {
			var rs = statement.executeQuery(
					"SELECT COUNT(*) AS c FROM gene_phenotype WHERE gene_id = 'NCBIGene:4905'");
			rs.next();
			assertTrue(rs.getInt("c") > 0);
		}
	}

	@Test
	void assayToPhenotype() throws Exception {
		try (var statement = sqliteWriter.connection().createStatement()) {
			var all = statement.executeQuery("SELECT COUNT(*) AS c FROM assay");
			all.next();
			assertEquals(3, all.getInt("c"));

			var joined = statement.executeQuery(
					"SELECT COUNT(*) AS c FROM assay_phenotype WHERE phenotype_id = 'HP:0000004'");
			joined.next();
			assertEquals(2, joined.getInt("c"));

			var idFormat = statement.executeQuery("SELECT id FROM assay LIMIT 1");
			idFormat.next();
			assertTrue(idFormat.getString("id").startsWith("LOINC:"));
		}
	}

	@Test
	void diseaseToGene() throws Exception {
		try (var statement = sqliteWriter.connection().createStatement()) {
			var all = statement.executeQuery("SELECT COUNT(DISTINCT gene_id) AS c FROM disease_gene");
			all.next();
			assertEquals(2, all.getInt("c"));

			var filtered = statement.executeQuery(
					"SELECT gene_id FROM disease_gene WHERE disease_id = 'OMIM:619340'");
			List<String> geneIds = new ArrayList<>();
			while (filtered.next()) geneIds.add(filtered.getString("gene_id"));
			assertEquals(1, geneIds.size());
			assertTrue(geneIds.contains("NCBIGene:4905"));
		}
	}

	@Test
	void diseaseToPhenotype() throws Exception {
		try (var statement = sqliteWriter.connection().createStatement()) {
			var all = statement.executeQuery("SELECT COUNT(*) AS c FROM disease_phenotype");
			all.next();
			assertEquals(6, all.getInt("c"));

			var filtered = statement.executeQuery(
					"SELECT COUNT(*) AS c FROM disease_phenotype WHERE disease_id = 'OMIM:609153'");
			filtered.next();
			assertEquals(3, filtered.getInt("c"));

			var orpha = statement.executeQuery(
					"SELECT COUNT(*) AS c FROM disease_phenotype WHERE disease_id = 'ORPHA:99999'");
			orpha.next();
			assertEquals(1, orpha.getInt("c"));
		}
	}

	@Test
	void medicalActions() throws Exception {
		try (var statement = sqliteWriter.connection().createStatement()) {
			var actions = statement.executeQuery("SELECT COUNT(*) AS c FROM medical_action");
			actions.next();
			assertEquals(6, actions.getInt("c"));

			var positiveTargets = statement.executeQuery(
					"SELECT COUNT(*) AS c FROM medical_action_target WHERE disease_id = 'OMIM:609153'");
			positiveTargets.next();
			assertEquals(1, positiveTargets.getInt("c"));

			var positiveAnnotations = statement.executeQuery(
					"SELECT COUNT(*) AS c FROM medical_action_annotation WHERE disease_id = 'OMIM:609153'");
			positiveAnnotations.next();
			assertEquals(1, positiveAnnotations.getInt("c"));

			var bardetBiedlTargets = statement.executeQuery(
					"SELECT COUNT(*) AS c FROM medical_action_target WHERE medical_action_id IN ('MAXO:0000088','MAXO:0000011','MAXO:0000930')");
			bardetBiedlTargets.next();
			assertEquals(0, bardetBiedlTargets.getInt("c"));

			var noMatchTargets = statement.executeQuery(
					"SELECT COUNT(*) AS c FROM medical_action_target WHERE medical_action_id IN ('MAXO:0001110','MAXO:0000885')");
			noMatchTargets.next();
			assertEquals(0, noMatchTargets.getInt("c"));

			var totalAnnotations = statement.executeQuery("SELECT COUNT(*) AS c FROM medical_action_annotation");
			totalAnnotations.next();
			assertEquals(1, totalAnnotations.getInt("c"));
		}
	}


	@Test
	void formatSources() {
		List<AnnotationReference> singleReference = List.of(AnnotationReference.of(TermId.of("PMID:000913"), EvidenceCode.IEA));
		assertEquals("PMID:000913", HpoOntologyAnnotationLoader.formatSources(singleReference));
		List<AnnotationReference> multipleReferences = List.of(AnnotationReference.of(
				TermId.of("PMID:000913"), EvidenceCode.IEA), AnnotationReference.of(TermId.of("PMID:000924"), EvidenceCode.IEA));
		assertEquals("PMID:000913;PMID:000924", HpoOntologyAnnotationLoader.formatSources(multipleReferences));
	}

	@Test
	void formatFrequency() {
	}

	@Test
	void findMondoEquivalentFromSingleMatchingOne(){
		TermId targetId = TermId.of("ORPHA:619340");
		Term target = Term.builder(targetId).xrefs(
				List.of(
						new Dbxref("Orphanet:619340", "", null),
						new Dbxref("OMIM:619340", "", null)
				)
		).name("Bad Disease 1").build();
		Collection<Term> diseases = List.of(target);

		assertEquals(target, HpoOntologyAnnotationLoader.findMondoEquivalent(targetId, "Bad Disease 1", diseases).orElse(null));
	}

	@Test
	void findMondoEquivalentFromMultipleMatchingTwo(){
		TermId targetId = TermId.of("ORPHA:619340");
		Term target = Term.builder(targetId).xrefs(
				List.of(
						new Dbxref("Orphanet:619340", "", null),
						new Dbxref("OMIM:619340", "", null)
				)
		).name("Bad Disease 2").build();
		Term target2 = Term.builder(targetId).xrefs(
				List.of(
						new Dbxref("Orphanet:619340", "", null),
						new Dbxref("OMIM:619340", "", null)
				)
		).name("Other Disease 3").build();
		Collection<Term> diseases = List.of(target, target2);

		assertEquals(target2, HpoOntologyAnnotationLoader.findMondoEquivalent(targetId, "Other Disease 3", diseases).orElse(null));
	}

	@Test
	void findMondoEquivalentFromMultipleMatchingNoneByName(){
		TermId targetId = TermId.of("ORPHA:619340");
		Term target = Term.builder(targetId).xrefs(
				List.of(
						new Dbxref("Orphanet:619340", "", null),
						new Dbxref("OMIM:619340", "", null)
				)
		).name("Bad Disease 2").build();
		Term target2 = Term.builder(targetId).xrefs(
				List.of(
						new Dbxref("Orphanet:619340", "", null),
						new Dbxref("OMIM:619340", "", null)
				)
		).name("Other Disease 3").build();
		Collection<Term> diseases = List.of(target, target2);

		assertEquals(Optional.empty(), HpoOntologyAnnotationLoader.findMondoEquivalent(targetId, "Quadri Disease", diseases));
	}


	@Test
	void phenotypeToCategory(){
		Map<TermId, String> pc = this.graphLoader.phenotypeToCategory(this.hpoOntology);
		assertTrue(pc.containsKey(TermId.of("HP:0100526")));
		assertEquals(pc.get(TermId.of("HP:0100526")), "Respiratory System");
		assertTrue((pc.containsKey(TermId.of("HP:0002086"))));
		assertEquals(pc.get(TermId.of("HP:0002086")), "Respiratory System");
		assertTrue((pc.containsKey(TermId.of("HP:0000001"))));
		assertEquals(pc.get(TermId.of("HP:0000001")), "Other");
	}

}
