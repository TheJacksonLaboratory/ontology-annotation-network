package org.jax.oan.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PhenotypeAnnotationDtoTest {

	@ParameterizedTest
	@MethodSource
	void create(Collection<Disease> diseases, Collection<Gene> genes, Collection<Assay> assays, Collection<MedicalActionSourceExtended> medicalActions){
		PhenotypeAnnotationDto dto = new PhenotypeAnnotationDto(diseases, genes, assays, medicalActions);
		assertEquals(dto.diseases(), diseases);
		assertEquals(dto.genes(), genes);
		assertEquals(dto.assays(), assays);
		assertEquals(dto.medicalActions(), medicalActions);
	}

	private static Stream<Arguments> create() {
		return Stream.of(
				Arguments.of(List.of(new Disease(TermId.of("OMIM:099232"), "Bad Disease", "", "")),
						List.of(new Gene(TermId.of("NCBIGene:02932"), "TX4"), new Gene(TermId.of("NCBIGene:44444"), "GN1")),
						List.of(), List.of()),
				Arguments.of(List.of(),
						List.of(),
						List.of(new Assay(TermId.of("LOINC:98239323"), "Glucose Test")), List.of())
		);
	}
}
