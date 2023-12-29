package org.jax.oan.service;

import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import org.jax.oan.core.DiseaseAnnotationDto;
import org.jax.oan.core.Gene;
import org.jax.oan.core.OntologyEntity;
import org.jax.oan.core.Phenotype;
import org.jax.oan.repository.DiseaseRepository;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class DiseaseService {

	private final DiseaseRepository diseaseRepository;

	public DiseaseService(DiseaseRepository diseaseRepository) {
		this.diseaseRepository = diseaseRepository;
	}

	public DiseaseAnnotationDto findAll(TermId termId){
		Collection<Phenotype> phenotypes = this.diseaseRepository.findPhenotypesByDisease(termId);
		Collection<Gene> genes = this.diseaseRepository.findGenesByDisease(termId);
		return new DiseaseAnnotationDto(
				phenotypes.stream().filter(p -> p.getCategory().isPresent()).collect(Collectors.groupingBy(
						p -> p.getCategory().orElse("UNKNOWN")
				)),
				genes);
	}
}
