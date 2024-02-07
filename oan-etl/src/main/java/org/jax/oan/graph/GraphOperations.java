package org.jax.oan.graph;

import org.jax.oan.core.OntologyModule;
import org.jax.oan.ontology.GraphWriter;

public interface GraphOperations {
	GraphWriter graphWriter();

	void createIndexes(OntologyModule module);

	void dropIndexes(OntologyModule module);
}
