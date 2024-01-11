package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.junit.jupiter.api.Assertions.*;

class DiseaseTest {

	@Test
	void create(){
		TermId id = TermId.of("OMIM:0000001");
		String name = "Rare Syndrome";
		String mondo = "";
		OntologyEntity ontologyClass = new Disease(id, name, mondo);
		OntologyEntity ontologyClassOther = new Disease(id, name, mondo);
		assertEquals(ontologyClass.getId(), id.getValue());
		assertEquals(ontologyClass.getName(), name);
		assertEquals(ontologyClass, ontologyClassOther);
	}
}
