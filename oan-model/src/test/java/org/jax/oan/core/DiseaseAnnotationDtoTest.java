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
	void create(Map<String, List<PhenotypeExtended>> phenotypeByCategory, List<Gene> genes, List<MedicalActionTargetExtended> medicalActions){
		Disease disease = new Disease(TermId.of("OMIM:029392"), "Zebra Disease","MONDO:029392", "A very bad one.");
		DiseaseAnnotationDto dto = new DiseaseAnnotationDto(disease, phenotypeByCategory, genes, medicalActions );
		assertEquals(dto.categories().keySet(), phenotypeByCategory.keySet());
		assertEquals(dto.categories().values(), phenotypeByCategory.values());
		assertEquals(dto.genes(), genes);
	}

	private static Stream<Arguments> create() {
		return Stream.of(
				Arguments.of(Map.of("inheritance",
						List.of(new PhenotypeExtended(TermId.of("HP:099232"), "Abnormality", "inheritance", null))),
						List.of(new Gene(TermId.of("NCBIGene:02932"), "TX4"), new Gene(TermId.of("NCBIGene:44444"), "GN1")), List.of())
		);
	}

}
