package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.core.*;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.jax.oan.repository.DiseaseRepository;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class DiseaseService {

	private final DiseaseRepository diseaseRepository;

	public DiseaseService(DiseaseRepository diseaseRepository) {
		this.diseaseRepository = diseaseRepository;
	}

	public DiseaseAnnotationDto findAll(TermId termId) throws OntologyAnnotationNetworkException {
		Optional<Disease> disease = this.diseaseRepository.findDiseaseById(termId);
		Collection<Phenotype> phenotypes = this.diseaseRepository.findPhenotypesByDisease(termId);
		Collection<Gene> genes = this.diseaseRepository.findGenesByDisease(termId);
		if(disease.isPresent()){
			return new DiseaseAnnotationDto(
					disease.get(),
					phenotypes.stream().filter(p -> p.getCategory().isPresent()).collect(Collectors.groupingBy(
							p -> p.getCategory().orElse("UNKNOWN")
					)),
					genes);
		}
		throw new OntologyAnnotationNetworkException(String.format("Could not find disease with id %s", termId.getValue()));
	}
}
