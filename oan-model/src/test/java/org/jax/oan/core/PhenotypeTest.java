package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PhenotypeTest {

	@Test
	void create(){
		TermId id = TermId.of("HP:0000001");
		String name = "Abnormality";
		String category = "Inheritance";
		Phenotype phenotype = new Phenotype(id, name, category);
		assertEquals(phenotype.getId(), id.getValue());
		assertEquals(phenotype.getName(), name);
		assertEquals(phenotype.getCategory(), category);
		assertEquals(phenotype.getMetadata(), Optional.empty());
	}
}
