package org.jax.oan.ontology;

import org.jax.oan.graph.GraphOperations;
import org.neo4j.driver.Query;

import java.util.Collection;

public interface GraphWriter {
	void write(Collection<Query> queries);
}
