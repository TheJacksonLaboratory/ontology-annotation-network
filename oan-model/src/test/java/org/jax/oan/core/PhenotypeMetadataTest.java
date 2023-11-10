package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PhenotypeMetadataTest {

	@Test
	void create(){
		String sex = "Abnormality";
		String onset = "";
		String frequency = "1/1";
		List<String> sources = List.of("OMIM:0199323", "PMID:0193323");
		PhenotypeMetadata pm = new PhenotypeMetadata(sex, onset, frequency, sources);
		PhenotypeMetadata pmOther = new PhenotypeMetadata(sex, onset, frequency, sources);
		assertEquals(pm.getSex(), sex);
		assertEquals(pm.getOnset(), onset);
		assertEquals(pm.getFrequency(), frequency);
		assertEquals(pm.getSources(), sources);
		assertEquals(pm, pmOther);
	}

}
