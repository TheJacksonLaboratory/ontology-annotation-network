package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicalActionTargetExtendedTest {

	@Test
	void construct() {
		TermId termId = TermId.of("MAXO:1234");
		String name = "surgery";
		List<MedicalActionRelation> relations = List.of(MedicalActionRelation.PREVENTS, MedicalActionRelation.CONTRAINDICATED);
		List<OntologyEntity> targets = List.of(new Disease(TermId.of("OMIM:00101"), "fake term", "", ""), new Phenotype(TermId.of("HP:111112"), "targeted"));
		MedicalActionTargetExtended mse = new MedicalActionTargetExtended(termId, name,  relations,  targets);
		MedicalActionTargetExtended mse2 = new MedicalActionTargetExtended(termId, "fake term 2",  relations,  targets);
		assertEquals(termId.getValue(), mse.getId());
		assertEquals(name, mse.getName());
		assertEquals(List.of(MedicalActionRelation.CONTRAINDICATED, MedicalActionRelation.PREVENTS), mse.getRelations());
		assertEquals(targets, mse.getTargets());
		assertEquals(mse.hashCode(), mse.hashCode());
		assertEquals(mse, mse);
		assertNotEquals(mse, mse2);
	}
}
