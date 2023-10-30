package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.core.DiseaseAnnotationDto;
import org.jax.oan.core.Gene;
import org.jax.oan.core.Phenotype;
import org.jax.oan.repository.DiseaseRepository;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class DiseaseService {

	private final DiseaseRepository diseaseRepository;

	public DiseaseService(DiseaseRepository diseaseRepository) {
		this.diseaseRepository = diseaseRepository;
	}

	public DiseaseAnnotationDto findAll(TermId termId){
		List<Phenotype> phenotypes = this.diseaseRepository.findPhenotypesByDisease(termId);
		List<Gene> genes = this.diseaseRepository.findGenesByDisease(termId);
		return new DiseaseAnnotationDto(
				phenotypes.stream().collect(Collectors.groupingBy(Phenotype::getCategory)),
				genes);
	}
}
