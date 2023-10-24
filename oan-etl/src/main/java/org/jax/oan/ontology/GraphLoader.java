package org.jax.oan.ontology;

import org.jax.oan.exception.OntologyAnnotationNetworkException;

import java.io.IOException;
import java.nio.file.Path;

public interface GraphLoader {
	void load(Path dataDirectory) throws IOException, OntologyAnnotationNetworkException;
}
