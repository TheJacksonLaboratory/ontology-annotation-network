package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.core.OntologyEntity;
import org.jax.oan.core.SearchDto;
import org.jax.oan.repository.DiseaseRepository;
import org.jax.oan.repository.GeneRepository;
import org.jax.oan.repository.SearchPage;

@Singleton
public class SearchService {
	private final DiseaseRepository diseaseRepository;

	private final GeneRepository geneRepository;

	public SearchService(DiseaseRepository diseaseRepository, GeneRepository geneRepository) {
		this.diseaseRepository = diseaseRepository;
		this.geneRepository = geneRepository;
	}

	/**
	 * Searches genes and returns one page of ranked results.
	 *
	 * @param query the text to search for.
	 * @param page the zero-based page number; ignored when {@code limit} is {@code -1}.
	 * @param limit the page size, or {@code -1} for every result.
	 * @return the page of genes and the number of matching genes across all pages.
	 */
	public SearchDto searchGene(String query, int page, int limit){
		return dto(this.geneRepository.searchGenes(query, offset(page, limit), limit));
	}

	/**
	 * Searches diseases and returns one page of ranked results.
	 *
	 * @param query the text to search for.
	 * @param page the zero-based page number; ignored when {@code limit} is {@code -1}.
	 * @param limit the page size, or {@code -1} for every result.
	 * @return the page of diseases and the number of matching diseases across all pages.
	 */
	public SearchDto searchDisease(String query, int page, int limit){
		return dto(this.diseaseRepository.searchDiseases(query, offset(page, limit), limit));
	}

	private static int offset(int page, int limit) {
		return limit == -1 ? 0 : page * limit;
	}

	private static SearchDto dto(SearchPage<? extends OntologyEntity> page) {
		return new SearchDto(page.results(), page.total());
	}
}
