package org.jax.oan.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.core.Phenotype;
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
class GeneRepositoryTest implements TestPropertyProvider {

	@TempDir
	static Path tempDir;

	@Inject
	GeneRepository geneRepository;

	@Override
	public Map<String, String> getProperties() {
		return Map.of("sqlite.path", tempDir.resolve("gene-repository-test.db").toString());
	}

	@BeforeAll
	void initialize() throws Exception {
		try (Connection connection = DriverManager.getConnection(
				"jdbc:sqlite:" + tempDir.resolve("gene-repository-test.db"));
			 Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE disease (id TEXT PRIMARY KEY, name TEXT NOT NULL, mondo_id TEXT NOT NULL DEFAULT '', description TEXT NOT NULL DEFAULT '')");
			statement.execute("CREATE TABLE phenotype (id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL)");
			statement.execute("CREATE TABLE gene (id TEXT PRIMARY KEY, name TEXT NOT NULL)");
			statement.execute("CREATE TABLE disease_gene (disease_id TEXT NOT NULL, gene_id TEXT NOT NULL, PRIMARY KEY (disease_id, gene_id))");
			statement.execute("CREATE TABLE gene_phenotype (gene_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, PRIMARY KEY (gene_id, phenotype_id))");

			statement.execute("INSERT INTO disease VALUES ('OMIM:092320', 'Some bad disease', '', '')");
			statement.execute("INSERT INTO phenotype VALUES ('HP:000001', 'short stature', '')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:9999', 'TX2')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:7777', 'MNN')");
			statement.execute("INSERT INTO disease_gene VALUES ('OMIM:092320', 'NCBIGene:7777')");
			statement.execute("INSERT INTO disease_gene VALUES ('OMIM:092320', 'NCBIGene:9999')");
			statement.execute("INSERT INTO gene_phenotype VALUES ('NCBIGene:9999', 'HP:000001')");
		}
	}


	@Test
	void findPhenotypesByGene() {
		Collection<Phenotype> phenotypes = this.geneRepository.findPhenotypesByGene(TermId.of("NCBIGene:9999"));
		Collection<Phenotype> expected = List.of(
				new Phenotype(TermId.of("HP:000001"), "short stature")
		);
		assertTrue(phenotypes.containsAll(expected));
	}

	@Test
	void findDiseasesByGene() {
		Collection<Disease> diseases = this.geneRepository.findDiseasesByGene(TermId.of("NCBIGene:9999"));
		Collection<Disease> expected = List.of(
				new Disease(TermId.of("OMIM:092320"), "Some bad disease", "", "")
		);
		assertTrue(diseases.containsAll(expected));
	}

	@Test
	void findGene() {
		Collection<Gene> genes = this.geneRepository.findGenes("7777");
		Collection<Gene> expected = List.of(
				new Gene(TermId.of("NCBIGene:7777"), "MNN")
		);
		assertTrue(genes.containsAll(expected));
	}
}
