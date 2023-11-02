package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.phenol.ontology.data.TermId;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SupportedEntityTest {

	@ParameterizedTest
	@MethodSource
	void prefixes(SupportedEntity entity, List<String> prefixes) {
		assertEquals(entity.prefixes(), prefixes);
	}

	private static Stream<Arguments> prefixes() {
		return Stream.of(
				Arguments.of(SupportedEntity.GENE, List.of("NCBIGENE")),
				Arguments.of(SupportedEntity.PHENOTYPE, List.of("HP")),
				Arguments.of(SupportedEntity.DISEASE, List.of("OMIM", "MONDO", "ORPHA","DECIPHER")),
				Arguments.of(SupportedEntity.UNKNOWN, List.of())
		);
	}

	@ParameterizedTest
	@MethodSource
	void from(String id, SupportedEntity entity) {
		assertEquals(SupportedEntity.from(TermId.of(id)), entity);
	}

	private static Stream<Arguments> from() {
		return Stream.of(
				Arguments.of("NCBIGene:9496", SupportedEntity.GENE),
				Arguments.of("HP:0001166", SupportedEntity.PHENOTYPE),
				Arguments.of("ORPHA:3342", SupportedEntity.DISEASE),
				Arguments.of("SMELLY:0932", SupportedEntity.UNKNOWN)
		);
	}

	@ParameterizedTest
	@MethodSource
	void isSupportedDownload(SupportedEntity entity, SupportedEntity association) {
		assertTrue(SupportedEntity.isSupportedDownload(entity, association));
		assertFalse(SupportedEntity.isSupportedDownload(entity, entity));
	}

	private static Stream<Arguments> isSupportedDownload() {
		return Stream.of(
						Arguments.of(SupportedEntity.GENE, SupportedEntity.PHENOTYPE),
						Arguments.of(SupportedEntity.PHENOTYPE, SupportedEntity.DISEASE),
						Arguments.of(SupportedEntity.DISEASE, SupportedEntity.GENE)
				);
	}

	@Test
	void values() {
	}

	@ParameterizedTest
	@MethodSource
	void valueOf(String term, SupportedEntity expected) {
		assertEquals(SupportedEntity.valueOf(term.toUpperCase()), expected);
	}

	private static Stream<Arguments> valueOf() {
		return Stream.of(
				Arguments.of("phenotype", SupportedEntity.PHENOTYPE),
				Arguments.of("disease", SupportedEntity.DISEASE),
				Arguments.of("gene", SupportedEntity.GENE)
		);
	}
}
