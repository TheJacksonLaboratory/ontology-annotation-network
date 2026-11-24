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
import java.util.Set;

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
			statement.execute("CREATE VIRTUAL TABLE disease_fts USING fts5(disease_id UNINDEXED, name, kind UNINDEXED, tokenize = 'unicode61 remove_diacritics 2', prefix = '2 3')");

			statement.execute("INSERT INTO disease VALUES ('OMIM:092320', 'Some bad disease', '', '')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:555555', 'Bad disease', 'MONDO:000001', 'Rare disease')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:100001', 'Atypical Loeys-Dietz syndrome', '', '')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:609192', 'Loeys-Dietz syndrome 1', '', '')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:301050', 'Alport syndrome 1, X-linked', '', '')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:154700', 'Marfan syndrome', 'MONDO:0007947', '')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:105400', 'Amyotrophic lateral sclerosis 1', '', '')");
			statement.execute("INSERT INTO disease VALUES ('ORPHA:64', 'Alstrom syndrome', '', '')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:115150', 'Cardiofaciocutaneous syndrome', '', '')");
			statement.execute("INSERT INTO disease VALUES ('ORPHA:2322', 'Kabadi syndrome', '', '')");
			statement.execute("INSERT INTO disease VALUES ('ORPHA:558', 'Marfan syndrome type 1', 'MONDO:0007947', '')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:900001', 'Loss or gain of function disorder', '', '')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:900002', 'Hearing loss', '', '')");
			statement.execute("INSERT INTO disease VALUES ('OMIM:900003', 'Not near normal growth', '', '')");
			statement.execute("INSERT INTO disease_fts (disease_id, name, kind) SELECT id, name, 'primary' FROM disease");
			statement.execute("INSERT INTO disease_fts VALUES ('OMIM:154700', 'MFS', 'synonym')");
			statement.execute("INSERT INTO disease_fts VALUES ('OMIM:105400', 'ALS', 'synonym')");
			for (String synonym : List.of("MFS1", "Marfan syndrome classic", "Marfan disease", "Marfan")) {
				statement.execute("INSERT INTO disease_fts VALUES ('ORPHA:558', '" + synonym + "', 'synonym')");
			}
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
		List<Disease> diseases = results("bad");
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

	@Test
	void findDiseasesTreatsSpacesAndHyphensAsSeparators(){
		Disease loeysDietz = new Disease(TermId.of("OMIM:609192"), "Loeys-Dietz syndrome 1", "", "");
		Disease alport = new Disease(TermId.of("OMIM:301050"), "Alport syndrome 1, X-linked", "", "");

		assertTrue(results("loeys dietz").contains(loeysDietz));
		assertTrue(results("loeys-dietz").contains(loeysDietz));
		assertTrue(results("Loeys  Dietz").contains(loeysDietz));
		assertTrue(results("x linked").contains(alport));
		assertTrue(results("alport x").contains(alport));
		assertTrue(results("dietz loeys").contains(loeysDietz));
	}

	@Test
	void findDiseasesRanksPrefixMatchesFirstIgnoringSeparators(){
		List<Disease> diseases = results("loeys dietz");

		assertEquals(2, diseases.size());
		assertEquals("Loeys-Dietz syndrome 1", diseases.get(0).getName());
	}

	@Test
	void findDiseasesMatchesIdsLiterally(){
		assertTrue(results("OMIM:5555").stream().anyMatch(d -> d.getId().equals("OMIM:555555")));
		assertTrue(results("omim 5").isEmpty());
	}

	@Test
	void findDiseasesReturnsNothingForSeparatorOnlyQuery(){
		assertTrue(results("-").isEmpty());
		assertTrue(results("  ").isEmpty());
		assertTrue(results("- -").isEmpty());
	}

	@Test void wordsInAnyOrder()        { assertTrue(ids("SYNDROME MARFAN").contains("OMIM:154700")); }
	@Test void prefixOnLastWord()       { assertEquals("OMIM:154700", ids("MARFAN SYND").get(0)); }
	@Test void exactNameRanksFirst() {
		List<String> r = ids("BAD DISEASE");
		assertEquals(List.of("OMIM:555555", "OMIM:092320"), r.subList(0, 2));
		assertEquals(r.size(), Set.copyOf(r).size());
	}
	@Test void exactSynonymBeatsPrimaryPrefix() { assertEquals(List.of("OMIM:105400", "ORPHA:64"), ids("ALS").subList(0, 2)); }
	@Test void synonymOnlyHit()         { assertEquals("OMIM:154700", ids("MFS").get(0)); }
	@Test void exactIdRanksFirst()      { assertEquals("OMIM:154700", ids("OMIM:154700").get(0)); assertTrue(ids("15470").contains("OMIM:154700")); }
	@Test void mondoIdFindsMappedDiseases() { assertEquals(Set.of("OMIM:154700", "ORPHA:558"), Set.copyOf(ids("MONDO:0007947").subList(0, 2))); }
	@Test void substringHitsRankLast() {
		assertEquals(List.of("OMIM:555555", "OMIM:092320", "ORPHA:2322"), ids("BAD"));
		assertTrue(ids("ARDIO").contains("OMIM:115150"));
	}
	@Test void punctuationIsTokenized() { assertEquals("OMIM:154700", ids("MARFAN-SYNDROME").get(0)); }
	@Test void operatorWordsMatchAsText() {
		assertEquals(List.of("OMIM:900001"), ids("LOSS OR GAIN"));
		assertEquals(List.of("OMIM:900003"), ids("NOT NEAR"));
	}
	@Test void noTokensReturnsEmpty()   { assertEquals(new SearchPage<>(List.of(), 0), diseaseRepository.searchDiseases("-", 0, 10)); }
	@Test void totalCountsEntities() {
		assertEquals(Set.of("OMIM:154700", "ORPHA:558"), Set.copyOf(ids("MARFAN")));
		assertEquals(2, diseaseRepository.searchDiseases("MARFAN", 0, 1).total());
	}
	@Test void pagesAreStableSlices() {
		assertEquals(ids("SYNDROME").subList(1, 3),
			diseaseRepository.searchDiseases("SYNDROME", 1, 2).results().stream().map(Disease::getId).toList());
	}
	@Test void offsetPastEndKeepsTotal() {
		SearchPage<Disease> p = diseaseRepository.searchDiseases("SYNDROME", 1000, 10);
		assertEquals(List.of(), p.results());
		assertEquals(ids("SYNDROME").size(), p.total());
	}
	@Test void limitZeroKeepsTotal()    { assertEquals(ids("SYNDROME").size(), diseaseRepository.searchDiseases("SYNDROME", 0, 0).total()); }

	private List<Disease> results(String query) {
		return diseaseRepository.searchDiseases(query, 0, -1).results();
	}

	private List<String> ids(String query) {
		return results(query).stream().map(Disease::getId).toList();
	}
}
