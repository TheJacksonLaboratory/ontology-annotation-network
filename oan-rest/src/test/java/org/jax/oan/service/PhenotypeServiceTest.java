package org.jax.oan.service;

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jax.oan.core.*;
import org.jax.oan.repository.GeneRepository;
import org.jax.oan.repository.PhenotypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class PhenotypeServiceTest {

	@Inject
	PhenotypeRepository phenotypeRepository;

	@Inject
	PhenotypeService phenotypeService;

	@ParameterizedTest
	@MethodSource
	void test_find_all(TermId id, List<Disease> diseases, List<Gene> genes, List<Assay> assays){
		when(phenotypeRepository.findDiseasesByTerm(id)).thenReturn(diseases);
		when(phenotypeRepository.findGenesByTerm(id)).thenReturn(genes);
		when(phenotypeRepository.findAssaysByTerm(id)).thenReturn(assays);

		PhenotypeAnnotationDto dto = phenotypeService.findAll(id);
		assertEquals(dto.diseases(), diseases);
		assertEquals(dto.genes(), genes);
		assertEquals(dto.assays(), assays);
	}

	private static Stream<Arguments> test_find_all(){
		return Stream.of(
				Arguments.of(TermId.of("HP:099233"), List.of(
						new Disease(TermId.of("MONDO:099233"),"Really bad one", "", ""),
						new Disease(TermId.of("DECIPHER:434444"),"Kinda bad one", "MONDO:000001", "Rare Mondo.")
				), List.of(
						new Gene(TermId.of("NCBIGene:900"),"TCL")
				), List.of(
						new Assay(TermId.of("LOINC:55555"),"Special bicep test")
				)),
				Arguments.of(
						TermId.of("HP:099233"),
						Collections.emptyList(),
						Collections.emptyList(),
						Collections.emptyList()
				)
		);
	}

	@MockBean(PhenotypeRepository.class)
	PhenotypeRepository phenotypeRepository() {
		return mock(PhenotypeRepository.class);
	}
}
