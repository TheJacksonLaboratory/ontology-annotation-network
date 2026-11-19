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
import java.util.Set;
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
			statement.execute("CREATE VIRTUAL TABLE gene_fts USING fts5(gene_id UNINDEXED, name, kind UNINDEXED, tokenize = 'unicode61 remove_diacritics 2', prefix = '2 3')");

			statement.execute("INSERT INTO disease VALUES ('OMIM:092320', 'Some bad disease', '', '')");
			statement.execute("INSERT INTO phenotype VALUES ('HP:000001', 'short stature', '')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:9999', 'TX2')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:7777', 'MNN')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:9998', 'XHLA-DRB1')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:3123', 'HLA-DRB1')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:3122', 'HLA-DRA')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:3117', 'HLA-DQA1')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:3105', 'HLA-A')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:2200', 'FBN1')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:2201', 'FBN2')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:6323', 'SCN1A')");
			statement.execute("INSERT INTO gene VALUES ('NCBIGene:11280', 'SCN11A')");
			statement.execute("INSERT INTO gene_fts (gene_id, name, kind) SELECT id, name, 'primary' FROM gene");
			for (String synonym : List.of("fibrillin 1", "MFS1", "FBN")) {
				statement.execute("INSERT INTO gene_fts VALUES ('NCBIGene:2200', '" + synonym + "', 'synonym')");
			}
			statement.execute("INSERT INTO gene_fts VALUES ('NCBIGene:2201', 'fibrillin 2', 'synonym')");
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
		List<Gene> genes = results("7777");
		Collection<Gene> expected = List.of(
				new Gene(TermId.of("NCBIGene:7777"), "MNN")
		);
		assertTrue(genes.containsAll(expected));
	}

	@Test
	void findGenesTreatsSpacesAndHyphensAsSeparators() {
		Gene hla = new Gene(TermId.of("NCBIGene:3123"), "HLA-DRB1");

		assertTrue(results("hla drb1").contains(hla));
		assertTrue(results("HLA-DRB1").contains(hla));
	}

	@Test
	void findGenesRanksExactHyphenatedSymbolFirst() {
		assertEquals("HLA-A", results("HLA-A").get(0).getName());
		assertEquals("HLA-A", results("hla a").get(0).getName());
	}

	@Test
	void findGenesRanksPrefixMatchesFirstIgnoringSeparators() {
		List<Gene> genes = results("hla drb1");

		assertEquals("HLA-DRB1", genes.get(0).getName());
	}

	@Test
	void findGenesReturnsNothingForSeparatorOnlyQuery() {
		assertTrue(results("-").isEmpty());
		assertTrue(results("- -").isEmpty());
	}

	@Test void exactSymbolRanksFirst()    { assertEquals("NCBIGene:2200", ids("FBN1").get(0)); }
	@Test void fullNameHit()              { assertEquals(Set.of("NCBIGene:2200", "NCBIGene:2201"), Set.copyOf(ids("FIBRILLIN"))); }
	@Test void multiWordFullName()        { assertEquals("NCBIGene:2201", ids("FIBRILLIN 2").get(0)); }
	@Test void previousSymbolHit()        { assertEquals("NCBIGene:2200", ids("MFS1").get(0)); }
	@Test void exactSynonymBeatsPrefix()  { assertEquals(List.of("NCBIGene:2200", "NCBIGene:2201"), ids("FBN").subList(0, 2)); }
	@Test void exactIdRanksFirst()        { assertEquals("NCBIGene:2200", ids("NCBIGENE:2200").get(0)); }
	@Test void hyphenatedSymbolExactFirst() { assertEquals("NCBIGene:3123", ids("HLA-DRB1").get(0)); }
	@Test void exactSymbolTypedWithSeparatorRanksFirst() {
		assertEquals("NCBIGene:6323", ids("SCN 1A").get(0));
		assertEquals("NCBIGene:6323", ids("SCN-1A").get(0));
	}
	@Test void limitZeroKeepsTotal()      { assertEquals(ids("HLA").size(), geneRepository.searchGenes("HLA", 0, 0).total()); }

	private List<Gene> results(String query) {
		return geneRepository.searchGenes(query, 0, -1).results();
	}

	private List<String> ids(String query) {
		return results(query).stream().map(Gene::getId).toList();
	}
}
