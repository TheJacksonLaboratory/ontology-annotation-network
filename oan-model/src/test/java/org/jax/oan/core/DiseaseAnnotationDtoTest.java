package org.jax.oan.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DiseaseAnnotationDtoTest {

	@ParameterizedTest
	@MethodSource
	void create(Map<String, List<Phenotype>> phenotypeByCategory, List<Gene> genes){
		DiseaseAnnotationDto dto = new DiseaseAnnotationDto(phenotypeByCategory, genes);
		assertEquals(dto.categories().keySet(), phenotypeByCategory.keySet());
		assertEquals(dto.categories().values(), phenotypeByCategory.values());
		assertEquals(dto.genes(), genes);
	}

	private static Stream<Arguments> create() {
		return Stream.of(
				Arguments.of(Map.of("inheritance", List.of(new Phenotype(TermId.of("HP:099232"), "Abnormality", "inheritance", null))),
						List.of(new Gene(TermId.of("NCBIGene:02932"), "TX4"), new Gene(TermId.of("NCBIGene:44444"), "GN1")))
		);
	}

}
