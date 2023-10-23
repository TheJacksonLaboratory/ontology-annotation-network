package org.jacksonlaboratory.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OntologyModuleTest {

	@Test
	void values() {
		assertEquals(OntologyModule.HPO, OntologyModule.HPO);
		assertNotEquals(OntologyModule.HPO, OntologyModule.MAXO);
		assertEquals(OntologyModule.values().length, 2);
	}

	@Test
	void valueOf() {
		assertEquals(OntologyModule.valueOf("HPO"), OntologyModule.HPO);
		assertEquals(OntologyModule.valueOf("MAXO"), OntologyModule.MAXO);
	}
}
