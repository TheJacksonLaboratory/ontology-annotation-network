package org.jax.oan.core;

import org.junit.jupiter.api.Test;

import java.util.List;

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
		assertEquals(pm.sex(), sex);
		assertEquals(pm.onset(), onset);
		assertEquals(pm.frequency(), frequency);
		assertEquals(pm.sources(), sources);
		assertEquals(pm, pmOther);
	}

}
