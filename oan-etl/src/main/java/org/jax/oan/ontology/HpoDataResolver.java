package org.jax.oan.ontology;

import org.jax.oan.exception.OntologyAnnotationNetworkDataException;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class HpoDataResolver {
	private static final Logger LOGGER = LoggerFactory.getLogger(HpoDataResolver.class);

	private final Path dataDirectory;

	public static HpoDataResolver of(Path dataDirectory) throws OntologyAnnotationNetworkException {
		return new HpoDataResolver(dataDirectory);
	}

	private HpoDataResolver(Path dataDirectory) throws OntologyAnnotationNetworkException {
		Objects.requireNonNull(dataDirectory, "Data directory must not be null!");
		this.dataDirectory = dataDirectory;
		validateHpoFiles();
	}

	private void validateHpoFiles() throws OntologyAnnotationNetworkException {
		boolean error = false;
		List<Path> requiredFiles = List.of(hpJson(), hgncCompleteSet(), mim2geneMedgen(), phenotypeAnnotations(),
				orpha2Gene(), loinc(), maxoa());
		for (Path file : requiredFiles) {
			if (!Files.isRegularFile(file)) {
				LOGGER.error("Missing required file `{}` in `{}`.", file.toFile().getName(), dataDirectory.toAbsolutePath());
				error = true;
			}
		}
		if (error) {
			throw new OntologyAnnotationNetworkDataException("Missing one or more required files in OntologyAnnotationNetwork data directory!");
		}
	}


	public Path dataDirectory() {
		return dataDirectory;
	}

	public Path hpJson(){
		return dataDirectory.resolve("hp-simple-non-classified.json");
	}

	public Path hgncCompleteSet() {
		return dataDirectory.resolve("hgnc_complete_set.txt");
	}

	public Path mim2geneMedgen() {
		return dataDirectory.resolve("mim2gene_medgen");
	}

	public Path phenotypeAnnotations() {
		return dataDirectory.resolve("phenotype.hpoa");
	}

	public Path orpha2Gene(){
		return dataDirectory.resolve("en_product6.xml");
	}

	public Path loinc(){
		return dataDirectory.resolve("loinc2hpo-annotations-merged.tsv");
	}

	public Path mondoJson() { return dataDirectory.resolve("mondo-base.json"); }

	public Path maxoa() { return dataDirectory.resolve("maxo-annotations.tsv"); }
}
