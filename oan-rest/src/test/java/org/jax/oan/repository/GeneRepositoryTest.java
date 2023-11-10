package org.jax.oan.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Phenotype;
import org.jax.oan.core.PhenotypeMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@MicronautTest
class GeneRepositoryTest {

	@Inject
	Driver driver;

	@Inject
	GeneRepository geneRepository;

	@BeforeAll
	void initialize() {
		try(Transaction tx = driver.session().beginTransaction()){
			tx.run("CREATE (d:Disease {id: 'OMIM:092320', name: 'Some bad disease'})");
			tx.run("CREATE (p: Phenotype {id: 'HP:000001', name: 'short stature', category: ''})");
			tx.run("CREATE (g:Gene {id: 'NCBIGene:9999', name: 'TX2'})");
			tx.run("CREATE (g:Gene {id: 'NCBIGene:7777', name: 'MNN'})");
			tx.run("MATCH (d:Disease {id: 'OMIM:092320'}), (p:Phenotype {id: 'HP:000001'})" +
					"MERGE (d)-[:MANIFESTS]->(p)<-[:WITH_METADATA {context: 'OMIM:092320'}]-(pm: PhenotypeMetadata {onset: '', frequency: '1/1', sex: 'female'," +
					"sources: '' })");
			tx.run("MATCH (d:Disease {id: 'OMIM:092320'}), (g:Gene {id: 'NCBIGene:7777'}) " +
					"MERGE (d)-[:EXPRESSES]->(g)");
			tx.run("MATCH (d:Disease {id: 'OMIM:092320'}), (g:Gene {id: 'NCBIGene:9999'}) " +
					"MERGE (d)-[:EXPRESSES]->(g)");
			tx.run("MATCH (g: Gene {id: 'NCBIGene:9999'}), (p: Phenotype {id: 'HP:000001'})" +
							" MERGE (g)-[:DETERMINES]-(p)");
			tx.commit();
		}
	}


	@Test
	void findPhenotypesByGene() {
		List<Phenotype> phenotypes = this.geneRepository.findPhenotypesByGene(TermId.of("NCBIGene:9999"));
		List<Phenotype> expected = List.of(
				new Phenotype(TermId.of("HP:000001"), "short stature", "")
		);
		assertTrue(phenotypes.containsAll(expected));
	}

	@Test
	void findDiseasesByGene() {
		List<Disease> diseases = this.geneRepository.findDiseasesByGene(TermId.of("NCBIGene:9999"));
		List<Disease> expected = List.of(
				new Disease(TermId.of("OMIM:092320"), "Some bad disease")
		);
		assertTrue(diseases.containsAll(expected));
	}
}
