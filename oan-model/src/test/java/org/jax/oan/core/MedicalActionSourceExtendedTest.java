package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicalActionSourceExtendedTest {

	@Test
	void construct() {
		TermId termId = TermId.of("MAXO:1234");
		String name = "surgery";
		List<MedicalActionRelation> relations = List.of(MedicalActionRelation.TREATS, MedicalActionRelation.PREVENTS);
		List<String> sources = List.of("PMID:0001932");
		MedicalActionSourceExtended mse = new MedicalActionSourceExtended(termId, name,  relations,  sources);
		MedicalActionSourceExtended mse2 = new MedicalActionSourceExtended(termId, "weight loss",  relations,  sources);
		assertEquals(termId.getValue(), mse.getId());
		assertEquals(name, mse.getName());
		assertEquals(List.of(MedicalActionRelation.PREVENTS, MedicalActionRelation.TREATS), mse.getRelations());
		assertEquals(sources, mse.getSources());
		assertEquals(mse.hashCode(), mse.hashCode());
		assertEquals(mse, mse);
		assertNotEquals(mse, mse2);
	}

}
