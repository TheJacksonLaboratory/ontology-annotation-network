package org.jax.oan.graph;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.jax.oan.core.OntologyModule;
import org.jax.oan.ontology.GraphDatabaseWriter;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments = "test")
class GraphDatabaseOperationsTest {
	GraphDatabaseOperations graphDatabaseOperations;

	GraphDatabaseWriter graphDatabaseWriter;
	public GraphDatabaseOperationsTest(Driver driver) {
		this.graphDatabaseWriter = new GraphDatabaseWriter(driver);
		this.graphDatabaseOperations = new GraphDatabaseOperations(this.graphDatabaseWriter);
	}

	@Test
	void truncate() {
		try(Session session = graphDatabaseWriter.session()){
			session.executeWriteWithoutResult(tx -> tx.run("CREATE (sample1),(sample2),(sample3),(sample4),(sample5)"));
			int current = session.executeWrite(tx -> tx.run("MATCH (x) RETURN COUNT(x)").single().values().get(0).asInt());
			assertEquals(5, current);
			graphDatabaseWriter.truncate();
			current = session.executeWrite(tx -> tx.run("MATCH (x) RETURN COUNT(x)").single().values().get(0).asInt());
			assertEquals(0, current);
		}
	}

	@Test
	void createAndDropIndexes() {
		try (Session session = graphDatabaseWriter.session()){
			session.executeWriteWithoutResult(tx ->
					tx.run("CREATE (p: Phenotype {id: 'HP:000001'}),(d: Disease {id: 'OMIM:1000'}),(g: Gene {id: 'NCBIGene:30'}),(a: Assay {id: 'LOINC:09103-3'})"));
			graphDatabaseOperations.createIndexes(OntologyModule.HPO);
			List<Record> indexes = session.executeWrite(tx -> tx.run("SHOW INDEXES YIELD name, labelsOrTypes, properties, type").list()).stream().filter(r -> r.get("type").asString().equals("RANGE")).collect(Collectors.toList());
			assertEquals(4, indexes.size());
			graphDatabaseOperations.dropIndexes(OntologyModule.HPO);
			indexes = session.executeWrite(tx -> tx.run("SHOW INDEXES YIELD name, labelsOrTypes, properties, type").list()).stream().filter(r -> r.get("type").asString().equals("RANGE")).collect(Collectors.toList());;
			assertEquals(0, indexes.size());
			graphDatabaseWriter.truncate();
		}
	}
}
