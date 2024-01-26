package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.core.SearchDto;
import org.jax.oan.repository.DiseaseRepository;
import org.jax.oan.repository.GeneRepository;

import java.util.Collection;
import java.util.stream.Collectors;

@Singleton
public class SearchService {
	private final DiseaseRepository diseaseRepository;

	private final GeneRepository geneRepository;

	public SearchService(DiseaseRepository diseaseRepository, GeneRepository geneRepository) {
		this.diseaseRepository = diseaseRepository;
		this.geneRepository = geneRepository;
	}

	public SearchDto searchGene(String query, int page, int limit){
		page = page * limit;
		Collection<Gene> genes = this.geneRepository.findGenes(query);
		if (limit == -1){
			return new SearchDto(genes, genes.size());
		} else {
			return new SearchDto(genes.stream().skip(page).limit(limit).collect(Collectors.toList()),  genes.size());
		}

	}

	public SearchDto searchDisease(String query, int page, int limit){
		page = page * limit;
		Collection<Disease> diseases = this.diseaseRepository.findDiseases(query);
		if (limit == -1){
			return new SearchDto(diseases,  diseases.size());
		} else {
			return new SearchDto(diseases.stream().skip(page).limit(limit).collect(Collectors.toList()),  diseases.size());
		}
	}
}
