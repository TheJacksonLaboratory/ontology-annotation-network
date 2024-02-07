package org.jax.oan.service;

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.core.SearchDto;
import org.jax.oan.repository.DiseaseRepository;
import org.jax.oan.repository.GeneRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class SearchServiceTest {

	@Inject
	DiseaseRepository diseaseRepository;

	@Inject
	GeneRepository geneRepository;

	@Inject
	SearchService searchService;

	@ParameterizedTest
	@MethodSource
	void test_search_gene(String query, Collection<Gene> genes){
		when(geneRepository.findGenes(query))
				.thenReturn(genes);
		SearchDto expected = searchService.searchGene(query, 0, 10);
		assertEquals(expected.results(), genes);
	}


	@ParameterizedTest
	@MethodSource
	void test_search_disease(String query, Collection<Disease> diseases){
		when(diseaseRepository.findDiseases(query))
				.thenReturn(diseases);
		SearchDto expected = searchService.searchDisease(query, 0 ,10);
		assertEquals(expected.results(), diseases);
	}

	private static Stream<Arguments> test_search_disease(){
		return Stream.of(
				Arguments.of("marf", List.of(
						new Disease(TermId.of("OMIM:039203"), "Large Marfanoid Syndrome", "", ""),
						new Disease(TermId.of("MONDO:99999"), "Marfan Syndrome", "MONDO:009121", "Rare disease")
				))
		);
	}

	private static Stream<Arguments> test_search_gene(){
		return Stream.of(
				Arguments.of("marf", List.of(
						new Gene(TermId.of("NCBIGene:77777"), "LTR1")
				))
		);
	}

	@MockBean(DiseaseRepository.class)
	DiseaseRepository diseaseRepository() {
		return mock(DiseaseRepository.class);
	}

	@MockBean(GeneRepository.class)
	GeneRepository geneRepository() {
		return mock(GeneRepository.class);
	}
}
