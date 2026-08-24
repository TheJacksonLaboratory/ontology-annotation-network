package org.jax.oan;

import io.micronaut.configuration.picocli.PicocliRunner;
import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;
import org.jax.oan.ontology.HpoOntologyAnnotationLoader;
import org.jax.oan.ontology.SqliteWriter;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Command(name = "graph",
		description = "This is the default command and loads the graph based on selected modules.",
		mixinStandardHelpOptions = true)
public class GraphCommand implements Runnable {
	@Option(names = {"-d", "--data"}, description = "The directory with the data.", required = true)
	Path path;

	@Option(names = {"-o", "--output"}, description = "Path to the SQLite database file to write.", required = true)
	Path output;

	@Option(names = {"-t", "--truncate"}, description = "To truncate the graph or not.", defaultValue = "false")
	boolean truncate;

	public static void main(String[] args) {
		PicocliRunner.run(GraphCommand.class, args);
	}

	public void run() {
		try {
			if (truncate && Files.exists(output)) {
				Files.delete(output);
			}
			try (SqliteWriter sqliteWriter = new SqliteWriter(output)) {
				new HpoOntologyAnnotationLoader(sqliteWriter).load(path, Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.ORPHANET));
			}
		} catch (Exception e) {
			throw new OntologyAnnotationNetworkRuntimeException(e);
		}
	}
}
