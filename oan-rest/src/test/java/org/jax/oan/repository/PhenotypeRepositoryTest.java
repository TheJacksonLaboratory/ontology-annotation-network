package org.jax.oan.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.jax.oan.core.Assay;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.core.MedicalActionRelation;
import org.jax.oan.core.MedicalActionSourceExtended;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments = "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhenotypeRepositoryTest implements TestPropertyProvider {

	@TempDir
	static Path tempDir;

	@Inject
	PhenotypeRepository phenotypeRepository;

	@Override
	public Map<String, String> getProperties() {
		return Map.of("sqlite.path", tempDir.resolve("phenotype-repository-test.db").toString());
	}

	@BeforeAll
	void initialize() throws Exception {
		try (Connection connection = DriverManager.getConnection(
				"jdbc:sqlite:" + tempDir.resolve("phenotype-repository-test.db"));
			 Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE disease (id TEXT PRIMARY KEY, name TEXT NOT NULL, mondo_id TEXT NOT NULL DEFAULT '', description TEXT NOT NULL DEFAULT '')");
			statement.execute("CREATE TABLE phenotype (id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL)");
			statement.execute("CREATE TABLE gene (id TEXT PRIMARY KEY, name TEXT NOT NULL)");
			statement.execute("CREATE TABLE assay (id TEXT PRIMARY KEY, name TEXT NOT NULL, scale TEXT)");
			statement.execute("CREATE TABLE medical_action (id TEXT PRIMARY KEY, name TEXT NOT NULL)");
			statement.execute("CREATE TABLE medical_action_target (medical_action_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, disease_id TEXT NOT NULL, relation TEXT NOT NULL, PRIMARY KEY (medical_action_id, phenotype_id, disease_id, relation))");
			statement.execute("CREATE TABLE medical_action_annotation (medical_action_id TEXT NOT NULL, disease_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, evidence TEXT NOT NULL, author TEXT, source TEXT, extension_id TEXT, extension_name TEXT)");
			statement.execute("CREATE TABLE disease_phenotype (disease_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, sex TEXT NOT NULL DEFAULT '', onset TEXT NOT NULL DEFAULT '', frequency TEXT NOT NULL DEFAULT '', sources TEXT NOT NULL)");
			statement.execute("CREATE TABLE disease_gene (disease_id TEXT NOT NULL, gene_id TEXT NOT NULL, PRIMARY KEY (disease_id, gene_id))");
			statement.execute("CREATE TABLE gene_phenotype (gene_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, PRIMARY KEY (gene_id, phenotype_id))");
			statement.execute("CREATE TABLE assay_phenotype (assay_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, outcome TEXT, PRIMARY KEY (assay_id, phenotype_id))");
			statement.execute("CREATE TABLE phenotype_child (parent_id TEXT NOT NULL, child_id TEXT NOT NULL, PRIMARY KEY (parent_id, child_id))");

			// HP:000001 is the parent of HP:000002 -- the disease/gene fixtures attach only to the
			// CHILD, so these tests genuinely exercise the recursive descendant closure rather than
			// a trivial single-node case.
			statement.execute("INSERT INTO phenotype VALUES ('HP:000001', 'short stature', '')");
			statement.execute("INSERT INTO phenotype VALUES ('HP:000002', 'very short stature', '')");
			statement.execute("INSERT INTO phenotype_child VALUES ('HP:000001', 'HP:000002')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:092320', 'Some bad disease', '', '')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:9999', 'TX2')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:7777', 'MNN')");
			statement.execute("INSERT INTO assay VALUES ('LOINC:03923', 'Glucose in blood', NULL)");
			statement.execute("INSERT INTO medical_action VALUES ('MAXO:0001001', 'gene therapy')");
			statement.execute("INSERT INTO disease_phenotype VALUES ('OMIM:092320', 'HP:000002', '', '', '', '')");
			statement.execute("INSERT INTO disease_gene VALUES ('OMIM:092320', 'NCBIGene:7777')");
			statement.execute("INSERT INTO disease_gene VALUES ('OMIM:092320', 'NCBIGene:9999')");
			statement.execute("INSERT INTO assay_phenotype VALUES ('LOINC:03923', 'HP:000001', NULL)");
			statement.execute("INSERT INTO medical_action_target VALUES ('MAXO:0001001', 'HP:000001', 'OMIM:092320', 'TREATS')");
			statement.execute("INSERT INTO medical_action_annotation VALUES ('MAXO:0001001', 'OMIM:092320', 'HP:000001', 'TAS', 'fake author', 'PMID:99999', '', '')");
		}
	}

	@Test
	void findDiseasesByTerm() {
		Collection<Disease> diseases = this.phenotypeRepository.findDiseasesByTerm(TermId.of("HP:000001"));
		Collection<Disease> expected = List.of(
				new Disease(TermId.of("OMIM:092320"), "Some bad disease", "", "")
		);
		assertTrue(diseases.containsAll(expected));
	}

	@Test
	void findGenesByTerm() {
		Collection<Gene> genes = this.phenotypeRepository.findGenesByTerm(TermId.of("HP:000001"));
		Collection<Gene> expected = List.of(
				new Gene(TermId.of("NCBIGene:9999"),"TX2"),
				new Gene(TermId.of("NCBIGene:7777"),"MNN")

		);
		assertTrue(
				expected.containsAll(genes)
		);
	}

	@Test
	void findAssaysByTerm() {
		Collection<Assay> assays = this.phenotypeRepository.findAssaysByTerm(TermId.of("HP:000001"));
		Collection<Assay> expected = List.of(
				new Assay(TermId.of("LOINC:03923"), "Glucose in blood")
		);
		assertTrue(assays.containsAll(expected));
	}

	@Test
	void findMedicalActionsByTerm() {
		Collection<MedicalActionSourceExtended> actions = this.phenotypeRepository.findMedicalActionsByTerm(TermId.of("HP:000001"));
		assertEquals(1, actions.size());
		MedicalActionSourceExtended action = actions.iterator().next();
		assertEquals("MAXO:0001001", action.getId());
		assertEquals(List.of(MedicalActionRelation.TREATS), action.getRelations());
		assertEquals(List.of("PMID:99999"), action.getSources());
	}
}
