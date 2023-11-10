package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.junit.jupiter.api.Assertions.*;

class GeneTest {

	@Test
	void create(){
		TermId id = TermId.of("NCBIGene:0000001");
		String name = "MYM";
		OntologyClass ontologyClass = new Gene(id, name);
		OntologyClass ontologyClassOther = new Gene(id, name);
		assertEquals(ontologyClass.getId(), id.getValue());
		assertEquals(ontologyClass.getName(), name);
		assertEquals(ontologyClass, ontologyClassOther);
	}
}
