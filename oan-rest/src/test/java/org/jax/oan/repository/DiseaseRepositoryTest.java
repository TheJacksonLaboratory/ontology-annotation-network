package org.jax.oan.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jax.oan.core.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments = "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiseaseRepositoryTest {

	@Inject
	Driver driver;

	@Inject
	DiseaseRepository diseaseRepository;

	@BeforeAll
	void initialize() {
		try(Transaction tx = driver.session().beginTransaction()){
			tx.run("CREATE (d:Disease {id: 'OMIM:092320', name: 'Some bad disease', mondoId: '', description: ''})");
			tx.run("CREATE (d:Disease {id: 'OMIM:555555', name: 'Bad disease', mondoId: 'MONDO:000001', description: 'Rare disease' })");
			tx.run("CREATE (p: Phenotype {id: 'HP:000001', name: 'short stature', category: ''})");
			tx.run("CREATE (g:Gene {id: 'NCBIGene:9999', name: 'TX2'})");
			tx.run("CREATE (g:Gene {id: 'NCBIGene:7777', name: 'MNN'})");
			tx.run("CREATE (m:MedicalAction {id: 'MAXO:0001001', name: 'gene therapy'})");
			tx.run("CREATE (m:MedicalAction {id: 'MAXO:0030018', name: 'uncooked cornstarch supplementation'})");
			tx.run("MATCH (d:Disease {id: 'OMIM:555555'}), (p:Phenotype {id: 'HP:000001'}), (m:MedicalAction {id: 'MAXO:0001001'}) with d, p, m" +
					" MERGE (mm: MedicalActionAnnotation {evidence: 'TAS', author: 'fake author', source: 'PMID:99999', extensionId: '', extensionName: ''})" +
					" with d, p, mm, m MERGE (mm)-[:DESCRIBES {dcontext: d.id, pcontext: p.id }]->(m)" +
					" with d, p, mm, m MERGE (m)-[:CLARIFIES {by: 'TREATS', context: d.id}]->(p)");
			tx.run("MATCH (d:Disease {id: 'OMIM:555555'}), (p:Phenotype {id: 'HP:000001'}), (m:MedicalAction {id: 'MAXO:0030018'}) with d, p, m" +
					" MERGE (mm: MedicalActionAnnotation {evidence: 'TAS', author: 'fake authors', source: 'PMID:99999', extensionId: '', extensionName: ''})" +
					" with d, p, mm, m MERGE (mm)-[:DESCRIBES {dcontext: d.id, pcontext: p.id }]->(m)" +
					" with d, p, mm, m MERGE (m)-[:CLARIFIES {by: 'TREATS', context: d.id}]->(p)");
			tx.run("MATCH (d:Disease {id: 'OMIM:092320'}), (p:Phenotype {id: 'HP:000001'})" +
					"MERGE (d)<-[:MANIFESTS]-(p)<-[:DESCRIBES {context: 'OMIM:092320'}]-(pm: PhenotypeAnnotation {onset: '', frequency: '1/1', sex: 'female'," +
					"sources: '' })");
			tx.run("MATCH (d:Disease {id: 'OMIM:092320'}), (g:Gene {id: 'NCBIGene:7777'}) " +
					"MERGE (d)-[:EXPRESSES]->(g)");
			tx.run("MATCH (d:Disease {id: 'OMIM:092320'}), (g:Gene {id: 'NCBIGene:9999'}) " +
					"MERGE (d)-[:EXPRESSES]->(g)");
			tx.commit();
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
