package org.jax.oan.service;

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jax.oan.core.*;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.jax.oan.repository.DiseaseRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class DiseaseServiceTest {

	@Inject
	DiseaseRepository diseaseRepository;

	@Inject
	DiseaseService diseaseService;

	@ParameterizedTest
	@MethodSource
	void test_find_all(TermId id, List<Gene> genes, List<Phenotype> phenotypes) throws OntologyAnnotationNetworkException {
		when(diseaseRepository.findGenesByDisease(id))
				.thenReturn(genes);
		when(diseaseRepository.findPhenotypesByDisease(id))
				.thenReturn(phenotypes);
		when(diseaseRepository.findDiseaseById(id)).thenReturn(Optional.of(new Disease(id, "","", "")));
		DiseaseAnnotationDto dto = diseaseService.findAll(id);
		assertEquals(genes, dto.genes()) ;
		assertEquals(phenotypes, dto.categories().values().stream().flatMap(Collection::stream)
				.collect(Collectors.toList()));
	}

	private static Stream<Arguments> test_find_all(){
		return Stream.of(
				Arguments.of(TermId.of("OMIM:0392932"), List.of(
						new Gene(TermId.of("NCBIGene:00093"),"TP4"),
						new Gene(TermId.of("NCBIGene:02002"),"YZ")
				), List.of(
						new Phenotype(TermId.of("HP:099233"),"Long legs", "Lower Half", null)
				)),
				Arguments.of(
						TermId.of("MONDO:0392932"),
						Collections.emptyList(),
						Collections.emptyList()
				)
		);
	}

	@MockBean(DiseaseRepository.class)
	DiseaseRepository diseaseRepository() {
		return mock(DiseaseRepository.class);
	}

}
