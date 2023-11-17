package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.junit.jupiter.api.Assertions.*;

class AssayTest {

	@Test
	void create(){
		TermId id = TermId.of("LOINC:1000-1");
		String name = "Glucose Test";
		OntologyEntity ontologyClass = new Assay(id, name);
		OntologyEntity ontologyClassOther= new Assay(id, name);
		assertEquals(ontologyClass.getId(), id.getValue());
		assertEquals(ontologyClass.getName(), name);
		assertEquals(ontologyClass, ontologyClassOther);
	}

}
