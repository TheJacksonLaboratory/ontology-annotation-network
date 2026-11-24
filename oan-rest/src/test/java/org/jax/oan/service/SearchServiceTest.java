package org.jax.oan.service;

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.core.SearchDto;
import org.jax.oan.repository.DiseaseRepository;
import org.jax.oan.repository.GeneRepository;
import org.jax.oan.repository.SearchPage;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class SearchServiceTest {

	@Inject
	DiseaseRepository diseaseRepository;

	@Inject
	GeneRepository geneRepository;

	@Inject
	SearchService searchService;

	private static final List<Gene> GENES = List.of(new Gene(TermId.of("NCBIGene:77777"), "LTR1"));
	private static final List<Disease> DISEASES = List.of(
			new Disease(TermId.of("OMIM:039203"), "Large Marfanoid Syndrome", "", ""),
			new Disease(TermId.of("MONDO:99999"), "Marfan Syndrome", "MONDO:009121", "Rare disease"));

	@Test
	void pageBecomesOffset() {
		when(geneRepository.searchGenes("fbn", 20, 10)).thenReturn(new SearchPage<>(GENES, 21));
		when(diseaseRepository.searchDiseases("marf", 20, 10)).thenReturn(new SearchPage<>(DISEASES, 22));
		assertEquals(GENES, searchService.searchGene("fbn", 2, 10).results());
		assertEquals(DISEASES, searchService.searchDisease("marf", 2, 10).results());
	}

	@Test
	void unlimitedIgnoresPage() {
		when(geneRepository.searchGenes("ltr", 0, -1)).thenReturn(new SearchPage<>(GENES, 1));
		when(diseaseRepository.searchDiseases("large", 0, -1)).thenReturn(new SearchPage<>(DISEASES, 2));
		assertEquals(GENES, searchService.searchGene("ltr", 3, -1).results());
		assertEquals(DISEASES, searchService.searchDisease("large", 3, -1).results());
	}

	@Test
	void totalComesFromRepository() {
		when(geneRepository.searchGenes("total", 0, 10)).thenReturn(new SearchPage<>(GENES, 57));
		when(diseaseRepository.searchDiseases("total", 0, 10)).thenReturn(new SearchPage<>(DISEASES, 58));
		SearchDto genes = searchService.searchGene("total", 0, 10);
		SearchDto diseases = searchService.searchDisease("total", 0, 10);
		assertEquals(57, genes.totalCount());
		assertEquals(58, diseases.totalCount());
	}

	@MockBean(DiseaseRepository.class)
	DiseaseRepository diseaseRepository() {
		return mock(DiseaseRepository.class);
	}

	@MockBean(GeneRepository.class)
	GeneRepository geneRepository() {
		return mock(GeneRepository.class);
	}
}
