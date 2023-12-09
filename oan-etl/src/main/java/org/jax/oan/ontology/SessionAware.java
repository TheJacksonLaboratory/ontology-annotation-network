package org.jax.oan.ontology;

import org.neo4j.driver.Session;

public interface SessionAware {
	Session session();
}
