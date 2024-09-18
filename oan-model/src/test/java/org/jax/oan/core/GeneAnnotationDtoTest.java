package org.jax.oan.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GeneAnnotationDtoTest {

	@ParameterizedTest
	@MethodSource
	void create(List<Disease> diseases, List<Phenotype> phenotypes){
		GeneAnnotationDto dto = new GeneAnnotationDto(diseases, phenotypes);
		assertEquals(dto.diseases(), diseases);
		assertEquals(dto.phenotypes(), phenotypes);
	}

	private static Stream<Arguments> create() {
		return Stream.of(
				Arguments.of(List.of(new Disease(TermId.of("OMIM:099232"), "Bad Disease", "", "")),
						List.of(new Phenotype(TermId.of("HP:02932"), "abnormal hands"), new Phenotype(TermId.of("HP:099999"), "abnormal eyes"))
				)
		);
	}
}
