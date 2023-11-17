package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.core.PhenotypeAnnotationDto;
import org.jax.oan.core.Assay;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.repository.PhenotypeRepository;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;

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
		return new PhenotypeAnnotationDto(diseases, genes, assays);
	}
}
