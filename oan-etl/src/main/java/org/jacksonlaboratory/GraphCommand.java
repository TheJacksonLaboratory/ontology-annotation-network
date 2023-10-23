package org.jacksonlaboratory;

import io.micronaut.configuration.picocli.PicocliRunner;
import jakarta.inject.Inject;
import org.jacksonlaboratory.graph.Operations;
import org.jacksonlaboratory.ontology.HpoGraphLoader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.List;

@Command(name = "graph",
		description = "This is the default command and loads the graph based on selected modules.",
		mixinStandardHelpOptions = true)
public class GraphCommand implements Runnable {

	@Inject
	HpoGraphLoader hpoGraphLoader;

	@Inject
	Operations operations;

	@Option(names = {"-d", "--data"}, description = "The directory with the data.")
	String path;

	@Option(names = {"-m", "--modules"}, description = "The list of modules to load into the graph.")
	List<String> modules;

	public static void main(String[] args) throws Exception {
		PicocliRunner.run(GraphCommand.class, args);
	}

	public void run() {
		try {
			operations.truncate();
			hpoGraphLoader.load(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
