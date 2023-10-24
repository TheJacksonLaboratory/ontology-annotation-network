package org.jax.oan.ontology;

import org.jax.oan.exception.OntologyAnnotationNetworkDataException;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HpoDataResolverTest {

	@Test
	void of() throws OntologyAnnotationNetworkException {
		Path dataDirectory = Path.of("src/test/resources");
		HpoDataResolver dataResolver = HpoDataResolver.of(dataDirectory);
		assertEquals(dataResolver.hpJson(), dataDirectory.resolve("hp-simple-non-classified.json"));
		assertEquals(dataResolver.mim2geneMedgen(), dataDirectory.resolve("mim2gene_medgen"));
		assertEquals(dataResolver.hgncCompleteSet(), dataDirectory.resolve("hgnc_complete_set.txt"));
		assertEquals(dataResolver.phenotypeAnnotations(), dataDirectory.resolve("phenotype.hpoa"));
		assertEquals(dataResolver.loinc(), dataDirectory.resolve("loinc2hpo-annotations-merged.tsv"));
		assertEquals(dataDirectory, dataResolver.dataDirectory());
	}

	@Test
	void error() {
		assertThrows(OntologyAnnotationNetworkDataException.class, () -> HpoDataResolver.of(Path.of("src/main/resources")));
	}
}
