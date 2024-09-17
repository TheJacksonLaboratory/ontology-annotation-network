package org.jax.oan.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchDtoTest {

	@Test
	void construct(){
		List<OntologyEntity> entities = List.of(new Gene(TermId.of("NcbiGene:11123"), "GENIE"));
		SearchDto dto = new SearchDto(entities, entities.size());
		assertEquals(dto.results().size(), dto.totalCount());
	}

}
