package org.jax.oan.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MedicalActionRelationTest {

	@Test
	void valueOf() {
		assertEquals(MedicalActionRelation.TREATS, MedicalActionRelation.valueOf("TREATS"));
		assertEquals(MedicalActionRelation.PREVENTS, MedicalActionRelation.valueOf("PREVENTS"));
		assertEquals(MedicalActionRelation.INVESTIGATES, MedicalActionRelation.valueOf("INVESTIGATES"));
		assertEquals(MedicalActionRelation.CONTRAINDICATED, MedicalActionRelation.valueOf("CONTRAINDICATED"));
		assertEquals(MedicalActionRelation.NO_OBSERVED_BENEFIT, MedicalActionRelation.valueOf("NO_OBSERVED_BENEFIT"));
	}
}
