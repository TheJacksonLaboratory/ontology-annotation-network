package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.core.*;
import org.jax.oan.repository.PhenotypeRepository;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;

@Singleton
public class PhenotypeService {

	private final PhenotypeRepository phenotypeRepository;

	public PhenotypeService(PhenotypeRepository phenotypeRepository) {
		this.phenotypeRepository = phenotypeRepository;
	}

	public PhenotypeAnnotationDto findAll(TermId termId){
		Collection<Disease> diseases = this.phenotypeRepository.findDiseasesByTerm(termId);
		Collection<Gene> genes = this.phenotypeRepository.findGenesByTerm(termId);
		Collection<Assay> assays = this.phenotypeRepository.findAssaysByTerm(termId);
		Collection<MedicalActionSourceExtended> medicalActions = this.phenotypeRepository.findMedicalActionsByTerm(termId);
		return new PhenotypeAnnotationDto(diseases, genes, assays, medicalActions);
	}
}
