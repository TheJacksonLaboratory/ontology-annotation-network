package org.jax.oan.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceRelationTest {

	@Test
	void construct() {
		SourceRelation sr = new SourceRelation(MedicalActionRelation.INVESTIGATES, "PMID:123456");
		assertEquals(sr.relation(), MedicalActionRelation.INVESTIGATES);
		assertEquals(sr.source(), "PMID:123456");
	}

}
