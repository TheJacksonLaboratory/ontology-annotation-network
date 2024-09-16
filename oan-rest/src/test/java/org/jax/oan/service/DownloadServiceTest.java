package org.jax.oan.service;

import io.micronaut.http.server.types.files.SystemFile;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jax.oan.TestData;
import org.jax.oan.core.SupportedEntity;
import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;
import org.jax.oan.repository.DiseaseRepository;
import org.jax.oan.repository.GeneRepository;
import org.jax.oan.repository.PhenotypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class DownloadServiceTest {

	@Inject
	PhenotypeRepository phenotypeRepository;

	@Inject
	GeneRepository geneRepository;

	@Inject
	DiseaseRepository diseaseRepository;

	@Inject
	DownloadService downloadService;

	@ParameterizedTest
	@MethodSource
	void associations_source(TermId id, SupportedEntity source, SupportedEntity target) {
		when(diseaseRepository.findPhenotypesByDisease(id))
				.thenReturn(TestData.phenotypesExtended());
		when(diseaseRepository.findGenesByDisease(id))
				.thenReturn(TestData.genes());
		when(geneRepository.findPhenotypesByGene(id))
				.thenReturn(TestData.phenotypes());
		when(geneRepository.findDiseasesByGene(id))
				.thenReturn(TestData.diseases());
		when(phenotypeRepository.findGenesByTerm(id))
				.thenReturn(TestData.genes());
		when(phenotypeRepository.findDiseasesByTerm(id))
				.thenReturn(TestData.diseases());
		when(phenotypeRepository.findAssaysByTerm(id))
				.thenReturn(TestData.assays());
		SystemFile systemFile = downloadService.associations(id, source, target);
		assertTrue(systemFile.getFile().getName().contains(String.format("%ss_for_%s", target.toString().toLowerCase(), id.getValue())));
		assertEquals("text/tab-separated-values", systemFile.getMediaType().toString());
		assertTrue(systemFile.getLength() > 0);
		try {
			Files.deleteIfExists(systemFile.getFile().toPath());
		} catch (IOException e){
			fail();
		}

	}

	@Test
	void associations_source_negative(){
		Exception exception = assertThrows(OntologyAnnotationNetworkRuntimeException.class, () -> {
			downloadService.associations(TermId.of("FAKE:000131"), SupportedEntity.UNKNOWN, SupportedEntity.UNKNOWN);
		});

		String expectedMessage = String.format("Downloading %s association for %s failed because source type is not supported.", SupportedEntity.UNKNOWN.toString().toLowerCase(), TermId.of("FAKE:000131").getValue());
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage));
	}

	private static Stream<Arguments> associations_source(){
		return Stream.of(
				Arguments.of(
						TermId.of("OMIM:093032"),
						SupportedEntity.DISEASE,
						SupportedEntity.PHENOTYPE
				),
				Arguments.of(
						TermId.of("MONDO:333330"),
						SupportedEntity.DISEASE,
						SupportedEntity.GENE
				),
				Arguments.of(
						TermId.of("NCBIGene:99302"),
						SupportedEntity.GENE,
						SupportedEntity.PHENOTYPE
				),
				Arguments.of(
						TermId.of("NCBIGene:99302"),
						SupportedEntity.GENE,
						SupportedEntity.DISEASE
				),
				Arguments.of(
						TermId.of("HP:013337"),
						SupportedEntity.PHENOTYPE,
						SupportedEntity.DISEASE
				),
				Arguments.of(
						TermId.of("HP:013337"),
						SupportedEntity.PHENOTYPE,
						SupportedEntity.GENE
				),
				Arguments.of(
						TermId.of("HP:013337"),
						SupportedEntity.PHENOTYPE,
						SupportedEntity.ASSAY
				)
		);
	}

	@Test
	void buildFile() {
	}

	@MockBean(GeneRepository.class)
	GeneRepository geneRepository() {
		return mock(GeneRepository.class);
	}

	@MockBean(DiseaseRepository.class)
	DiseaseRepository diseaseRepository() {
		return mock(DiseaseRepository.class);
	}

	@MockBean(PhenotypeRepository.class)
	PhenotypeRepository phenotypeRepository() {
		return mock(PhenotypeRepository.class);
	}
}
