package org.jax.oan.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jax.oan.core.Assay;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@MicronautTest
class PhenotypeRepositoryTest {

	@Inject
	Driver driver;

	@Inject
	PhenotypeRepository phenotypeRepository;

	@BeforeAll
	void initialize() {
		try(Transaction tx = driver.session().beginTransaction()){
			tx.run("CREATE (d:Disease {id: 'OMIM:092320', name: 'Some bad disease'})");
			tx.run("CREATE (p:Phenotype {id: 'HP:000001', name: 'short stature', category: ''})");
			tx.run("CREATE (g:Gene {id: 'NCBIGene:9999', name: 'TX2'})");
			tx.run("CREATE (g:Gene {id: 'NCBIGene:7777', name: 'MNN'})");
			tx.run("CREATE (a:Assay {id: 'LOINC:03923', name: 'Glucose in blood'})");
			tx.run("MATCH (d:Disease {id: 'OMIM:092320'}), (p:Phenotype {id: 'HP:000001'})" +
					"MERGE (d)-[:MANIFESTS]->(p)<-[:WITH_METADATA {context: 'OMIM:092320'}]-(pm: PhenotypeMetadata {onset: '', frequency: '1/1', sex: 'female'," +
					"sources: '' })");
			tx.run("MATCH (d:Disease {id: 'OMIM:092320'}), (g:Gene {id: 'NCBIGene:7777'}) " +
					"MERGE (d)-[:EXPRESSES]->(g)");
			tx.run("MATCH (d:Disease {id: 'OMIM:092320'}), (g:Gene {id: 'NCBIGene:9999'}) " +
					"MERGE (d)-[:EXPRESSES]->(g)");
			tx.run("MATCH (p:Phenotype {id: 'HP:000001'}), (a:Assay {id: 'LOINC:03923'}) " +
					"MERGE (a)-[:MEASURES]->(p)");
			tx.run("MATCH (g: Gene {id: 'NCBIGene:9999'}), (p: Phenotype {id: 'HP:000001'})" +
					" MERGE (g)-[:DETERMINES]-(p)");
			tx.commit();
		}
	}

	@Test
	void findDiseasesByTerm() {
		Collection<Disease> diseases = this.phenotypeRepository.findDiseasesByTerm(TermId.of("HP:000001"));
		Collection<Disease> expected = List.of(
				new Disease(TermId.of("OMIM:092320"), "Some bad disease")
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
		assertTrue(expected.containsAll(assays));
	}
}
