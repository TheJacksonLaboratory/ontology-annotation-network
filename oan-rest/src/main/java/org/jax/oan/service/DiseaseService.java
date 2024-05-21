package org.jax.oan.service;

import io.micronaut.serde.annotation.SerdeImport;
import jakarta.inject.Singleton;
import org.jax.oan.core.*;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.jax.oan.repository.DiseaseRepository;
import org.jax.oan.repository.PhenotypeRepository;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@SerdeImport(Disease.class)
public class DiseaseService {

	private final DiseaseRepository diseaseRepository;
	private final PhenotypeRepository phenotypeRepository;

	public DiseaseService(DiseaseRepository diseaseRepository, PhenotypeRepository phenotypeRepository) {
		this.diseaseRepository = diseaseRepository;
		this.phenotypeRepository = phenotypeRepository;
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

	public Collection<Disease> findIntersectingByPhenotypes(Collection<TermId> termIds){
		List<Disease> intersecting = new ArrayList<>();
		for (TermId id: termIds){
			try {
				if (intersecting.isEmpty()){
					intersecting.addAll(phenotypeRepository.findDiseasesByTerm(id));
				} else {
					Collection<Disease> diseases = phenotypeRepository.findDiseasesByTerm(id);
					intersecting = intersecting.stream().distinct()
							.filter(diseases::contains)
							.collect(Collectors.toList());
				}

			} catch (Exception ex) {
				return Collections.emptyList();
			}
		}
		return intersecting.stream().distinct().toList();
	}
}
