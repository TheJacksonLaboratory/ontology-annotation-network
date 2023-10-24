package org.jax.oan.graph;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.jax.oan.core.OntologyModule;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class OperationsTest {

	Driver driver;
	Operations operations;
	public OperationsTest(Driver driver, Operations operations) {
		this.driver = driver;
		this.operations = operations;
	}

	@Test
	void truncate() {
		// Create fake nodes
		// expect nodes to be 0.
		driver.session().executeWriteWithoutResult(tx -> tx.run("CREATE (sample1),(sample2),(sample3),(sample4),(sample5)"));
		int current = driver.session().executeWrite(tx -> tx.run("MATCH (x) RETURN COUNT(x)").single().values().get(0).asInt());
		assertEquals(5, current);
		operations.truncate();
		current = driver.session().executeWrite(tx -> tx.run("MATCH (x) RETURN COUNT(x)").single().values().get(0).asInt());
		assertEquals(0, current);
	}

	@Test
	void createAndDropIndexes() {
		driver.session().executeWriteWithoutResult(tx ->
				tx.run("CREATE (p: Phenotype {id: 'HP:000001'}),(d: Disease {id: 'OMIM:1000'}),(g: Gene {id: 'NCBIGene:30'}),(a: Assay {id: 'LOINC:09103-3'})"));

		operations.createIndexes(OntologyModule.HPO);
		List<Record> indexes = driver.session().executeWrite(tx -> tx.run("SHOW INDEXES YIELD name, labelsOrTypes, properties, type").list()).stream().filter(r -> r.get("type").asString().equals("RANGE")).collect(Collectors.toList());
		assertEquals(4, indexes.size());
		operations.dropIndexes(OntologyModule.HPO);
		indexes = driver.session().executeWrite(tx -> tx.run("SHOW INDEXES YIELD name, labelsOrTypes, properties, type").list()).stream().filter(r -> r.get("type").asString().equals("RANGE")).collect(Collectors.toList());;
		assertEquals(0, indexes.size());
		operations.truncate();
	}
}
