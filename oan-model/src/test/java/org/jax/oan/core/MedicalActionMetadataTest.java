package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.junit.jupiter.api.Assertions.*;

class MedicalActionMetadataTest {

	@Test
	void construct() {
		MedicalActionMetadata metadata = new MedicalActionMetadata("PMID:1111", Evidence.IEA, new Extension(TermId.of("CHEBI:0000"), "Caffeine"), MedicalActionRelation.NO_OBSERVED_BENEFIT, "ORCID:1110");
		MedicalActionMetadata metadata2 = new MedicalActionMetadata("PMID:1111", Evidence.IEA, new Extension(TermId.of("CHEBI:0000"), "Caffeine"), MedicalActionRelation.TREATS, "ORCID:1110");
		assertEquals("PMID:1111", metadata.sourceId());
		assertEquals(Evidence.IEA, metadata.evidence());
		assertEquals(new Extension(TermId.of("CHEBI:0000"), "Caffeine"), metadata.extension());
		assertEquals(MedicalActionRelation.NO_OBSERVED_BENEFIT, metadata.medicalActionRelation());
		assertEquals("ORCID:1110", metadata.author());
		assertEquals(metadata.hashCode(), metadata.hashCode());
		assertEquals(metadata, metadata);
		assertNotEquals(metadata, metadata2);
	}
}
