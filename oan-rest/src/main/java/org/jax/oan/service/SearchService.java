package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.core.OntologyEntity;
import org.jax.oan.repository.DiseaseRepository;
import org.jax.oan.repository.GeneRepository;

import java.util.Collection;

@Singleton
public class SearchService {
	private final DiseaseRepository diseaseRepository;

	private final GeneRepository geneRepository;

	public SearchService(DiseaseRepository diseaseRepository, GeneRepository geneRepository) {
		this.diseaseRepository = diseaseRepository;
		this.geneRepository = geneRepository;
	}

	public Collection<? extends OntologyEntity> searchGene(String query){
		return this.geneRepository.findGenes(query);
	}

	public Collection<? extends OntologyEntity> searchDisease(String query){
		return this.diseaseRepository.findDisease(query);
	}
}
