package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.junit.jupiter.api.Assertions.*;

class MedicalActionTest {

	@Test
	void create(){
		TermId id = TermId.of("MAXO:0000001");
		String name = "Surgical Procedure";
		OntologyClass ontologyClass = new MedicalAction(id, name);
		OntologyClass ontologyClassOther = new MedicalAction(id, name);
		assertEquals(ontologyClass.getId(), id.getValue());
		assertEquals(ontologyClass.getName(), name);
		assertEquals(ontologyClass, ontologyClassOther);
	}

}
