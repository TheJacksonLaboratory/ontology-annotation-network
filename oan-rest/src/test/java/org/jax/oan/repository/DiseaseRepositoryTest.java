package org.jax.oan.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.jax.oan.core.*;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments = "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiseaseRepositoryTest implements TestPropertyProvider {

	@TempDir
	static Path tempDir;

	@Inject
	DiseaseRepository diseaseRepository;

	@Override
	public Map<String, String> getProperties() {
		return Map.of("sqlite.path", tempDir.resolve("disease-repository-test.db").toString());
	}

	@BeforeAll
	void initialize() throws Exception {
		try (Connection connection = DriverManager.getConnection(
				"jdbc:sqlite:" + tempDir.resolve("disease-repository-test.db"));
			 Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE disease (id TEXT PRIMARY KEY, name TEXT NOT NULL, mondo_id TEXT NOT NULL DEFAULT '', description TEXT NOT NULL DEFAULT '')");
			statement.execute("CREATE TABLE phenotype (id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL)");
			statement.execute("CREATE TABLE gene (id TEXT PRIMARY KEY, name TEXT NOT NULL)");
			statement.execute("CREATE TABLE medical_action (id TEXT PRIMARY KEY, name TEXT NOT NULL)");
			statement.execute("CREATE TABLE medical_action_target (medical_action_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, disease_id TEXT NOT NULL, relation TEXT NOT NULL, PRIMARY KEY (medical_action_id, phenotype_id, disease_id, relation))");
			statement.execute("CREATE TABLE medical_action_annotation (medical_action_id TEXT NOT NULL, disease_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, evidence TEXT NOT NULL, author TEXT, source TEXT, extension_id TEXT, extension_name TEXT)");
			statement.execute("CREATE TABLE disease_phenotype (disease_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, sex TEXT NOT NULL DEFAULT '', onset TEXT NOT NULL DEFAULT '', frequency TEXT NOT NULL DEFAULT '', sources TEXT NOT NULL)");
			statement.execute("CREATE TABLE disease_gene (disease_id TEXT NOT NULL, gene_id TEXT NOT NULL, PRIMARY KEY (disease_id, gene_id))");

			statement.execute("INSERT INTO disease VALUES ('OMIM:092320', 'Some bad disease', '', '')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:555555', 'Bad disease', 'MONDO:000001', 'Rare disease')");
			statement.execute("INSERT INTO phenotype VALUES ('HP:000001', 'short stature', '')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:9999', 'TX2')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:7777', 'MNN')");
			statement.execute("INSERT INTO medical_action VALUES ('MAXO:0001001', 'gene therapy')");
			statement.execute("INSERT INTO medical_action VALUES ('MAXO:0030018', 'uncooked cornstarch supplementation')");
			statement.execute("INSERT INTO medical_action_target VALUES ('MAXO:0001001', 'HP:000001', 'OMIM:555555', 'TREATS')");
			statement.execute("INSERT INTO medical_action_target VALUES ('MAXO:0030018', 'HP:000001', 'OMIM:555555', 'TREATS')");
			statement.execute("INSERT INTO medical_action_annotation VALUES ('MAXO:0001001', 'OMIM:555555', 'HP:000001', 'TAS', 'fake author', 'PMID:99999', '', '')");
			statement.execute("INSERT INTO medical_action_annotation VALUES ('MAXO:0030018', 'OMIM:555555', 'HP:000001', 'TAS', 'fake authors', 'PMID:99999', '', '')");
			statement.execute("INSERT INTO disease_phenotype VALUES ('OMIM:092320', 'HP:000001', 'female', '', '1/1', '')");
			statement.execute("INSERT INTO disease_gene VALUES ('OMIM:092320', 'NCBIGene:7777')");
			statement.execute("INSERT INTO disease_gene VALUES ('OMIM:092320', 'NCBIGene:9999')");
		}
	}

	@Test
	void findGenesByDisease() {
		Collection<Gene> genes = diseaseRepository.findGenesByDisease(TermId.of("OMIM:092320"));
		List<Gene> expected = List.of(
				new Gene(TermId.of("NCBIGene:9999"),"TX2"),
				new Gene(TermId.of("NCBIGene:7777"),"MNN")
				);
		assertTrue(
				expected.containsAll(genes)
		);
	}

	@Test
	void findPhenotypesByDisease() {
		Collection<PhenotypeExtended> phenotypes = diseaseRepository.findPhenotypesByDisease(TermId.of("OMIM:092320"));
		List<PhenotypeExtended> expected = List.of(
				new PhenotypeExtended(TermId.of("HP:000001"), "short stature", "", new PhenotypeMetadata("female", "", "1/1", List.of()))
		);

		assertTrue(phenotypes.containsAll(expected));
	}

	@Test
	void findDiseases(){
		Collection<Disease> diseases = diseaseRepository.findDiseases("bad");
		List<Disease> expected = List.of(
				new Disease(TermId.of("OMIM:555555"), "Bad disease", "MONDO:0000001", "Rare disease"),
				new Disease(TermId.of("OMIM:092320"), "Some bad disease", "", "")
		);

		assertTrue(diseases.containsAll(expected));
	}

	@Test
	void findDiseaseById(){
		Optional<Disease> disease = diseaseRepository.findDiseaseById(TermId.of("OMIM:555555"));
		Disease expected = new Disease(TermId.of("OMIM:555555"), "Bad disease", "MONDO:0000001", "Rare disease");

		assertTrue(disease.isPresent());
		assertEquals(disease.get(), expected);
	}

	@Test
	void findMedicalActionsByDisease(){
		Collection<MedicalActionTargetExtended> mse = diseaseRepository.findMedicalActionsByDisease(TermId.of("OMIM:555555"));
		assertEquals(2, mse.size());
	}
}
