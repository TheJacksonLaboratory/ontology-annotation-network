package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.core.Annotation;
import org.jax.oan.core.Assay;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.repository.PhenotypeRepository;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

@Singleton
public class PhenotypeService {

	private final PhenotypeRepository phenotypeRepository;

	public PhenotypeService(PhenotypeRepository phenotypeRepository) {
		this.phenotypeRepository = phenotypeRepository;
	}

	public Annotation findAll(TermId termId){
		List<Disease> diseases = findDiseases(termId);
		List<Gene> genes = findGenes(termId);
		List<Assay> assays = findAssays(termId);
		return new Annotation(diseases, genes, assays);
	}

	public List<Disease> findDiseases(TermId termId) {
		return this.phenotypeRepository.findDiseasesByTerm(termId);
	}

	public List<Gene> findGenes(TermId termId) {
		return this.phenotypeRepository.findGenesByTerm(termId);
	}

	public List<Assay> findAssays(TermId termId) {
		return this.phenotypeRepository.findAssaysByTerm(termId);
	}

}
