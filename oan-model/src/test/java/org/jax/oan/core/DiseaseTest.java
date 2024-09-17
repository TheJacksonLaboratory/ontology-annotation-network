package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.junit.jupiter.api.Assertions.*;

class DiseaseTest {

	@Test
	void create(){
		TermId id = TermId.of("OMIM:0000001");
		String name = "Rare Syndrome";
		String mondo = "MONDO:0101";
		String description = "A very rare syndrome.";
		Disease ontologyClass = new Disease(id, name, mondo, description);
		Disease ontologyClassOther = new Disease(id, name, mondo, description);
		assertEquals(id.getValue(), ontologyClass.getId());
		assertEquals(name, ontologyClass.getName());
		assertEquals(mondo, ontologyClass.getMondoId());
		assertEquals(description, ontologyClass.getDescription());
		assertEquals(ontologyClass, ontologyClassOther);
	}
}
