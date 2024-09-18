package org.jax.oan.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlternativePrefixTest {

	@Test
	void from() {
		assertEquals("Orphanet", AlternativePrefix.from("ORPHA"));
		assertEquals("Orphanet", AlternativePrefix.from("Orphanet"));
	}
}
