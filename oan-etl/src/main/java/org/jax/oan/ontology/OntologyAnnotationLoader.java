package org.jax.oan.ontology;

import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public interface OntologyAnnotationLoader {
	GraphWriter graphWriter();
	void load(Path directory, Set<DiseaseDatabase> databases) throws IOException, OntologyAnnotationNetworkException;
}
