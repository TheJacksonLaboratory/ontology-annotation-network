package org.jax.oan.service;

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jax.oan.core.*;
import org.jax.oan.repository.DiseaseRepository;
import org.jax.oan.repository.GeneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class GeneServiceTest {

	@Inject
	GeneRepository geneRepository;

	@Inject
	GeneService geneService;

	@ParameterizedTest
	@MethodSource
	void test_find_all(TermId id, List<Disease> diseases, List<Phenotype> phenotypes){
		when(geneRepository.findDiseasesByGene(id))
				.thenReturn(diseases);
		when(geneRepository.findPhenotypesByGene(id))
				.thenReturn(phenotypes);
		GeneAnnotationDto dto = geneService.findAll(id);
		assertEquals(dto.diseases(), diseases);
		assertEquals(dto.phenotypes(), phenotypes);
	}

	private static Stream<Arguments> test_find_all(){
		return Stream.of(
				Arguments.of(TermId.of("NCBIGene:34"), List.of(
						new Disease(TermId.of("MONDO:099233"),"Really bad one", "", ""),
						new Disease(TermId.of("DECIPHER:434444"),"Kinda bad one", "MODNO:000001", "")
				), List.of(
						new Phenotype(TermId.of("HP:099233"),"Long legs", null, null)
				)),
				Arguments.of(
						TermId.of("NCBIGene:900"),
						Collections.emptyList(),
						Collections.emptyList()
				)
		);
	}




	@MockBean(GeneRepository.class)
	GeneRepository geneRepository() {
		return mock(GeneRepository.class);
	}

}
