package org.jax.oan.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceTest {
	@Test
	void valueOf() {
		assertEquals(Evidence.TAS, Evidence.valueOf("TAS"));
		assertEquals(Evidence.PCS, Evidence.valueOf("PCS"));
		assertEquals(Evidence.IEA, Evidence.valueOf("IEA"));
	}
}
