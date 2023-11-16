package org.jax.oan;

import io.micronaut.configuration.picocli.PicocliRunner;
import jakarta.inject.Inject;
import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;
import org.jax.oan.graph.Operations;
import org.jax.oan.ontology.HpoGraphLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Command(name = "graph",
		description = "This is the default command and loads the graph based on selected modules.",
		mixinStandardHelpOptions = true)
public class GraphCommand implements Runnable {

	@Inject
	HpoGraphLoader hpoGraphLoader;

	@Inject
	Operations operations;

	@Option(names = {"-d", "--data"}, description = "The directory with the data.")
	Path path;

	@Option(names = {"-m", "--modules"}, description = "The list of modules to load into the graph.")
	List<String> modules;

	public static void main(String[] args) {
		PicocliRunner.run(GraphCommand.class, args);
	}

	public void run() {
		try {
			operations.truncate();
			hpoGraphLoader.load(path, Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.ORPHANET));
		} catch (Exception e) {
			throw new OntologyAnnotationNetworkRuntimeException(e);
		}
	}
}
