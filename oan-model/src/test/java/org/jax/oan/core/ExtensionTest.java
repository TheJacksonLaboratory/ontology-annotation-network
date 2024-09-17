package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.junit.jupiter.api.Assertions.*;

class ExtensionTest {

	@Test
	void construct(){
		Extension ex = new Extension(TermId.of("CHEBI:0000"), "Caffeine");
		assertEquals("CHEBI:0000", ex.getId());
		assertEquals("Caffeine", ex.getName());
	}

}
