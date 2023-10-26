package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.core.Annotation;
import org.jax.oan.core.Assay;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.repository.AnnotationRepository;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

@Singleton
public class AnnotationService {

	private final AnnotationRepository annotationRepository;

	public AnnotationService(AnnotationRepository annotationRepository) {
		this.annotationRepository = annotationRepository;
	}

	public Annotation findAll(TermId termId){
		List<Disease> diseases = findDiseases(termId);
		List<Gene> genes = findGenes(termId);
		List<Assay> assays = findAssays(termId);
		return new Annotation(diseases, genes, assays);
	}

	public List<Disease> findDiseases(TermId termId) {
		return this.annotationRepository.findDiseasesByTerm(termId);
	}

	public List<Gene> findGenes(TermId termId) {
		return this.annotationRepository.findGenesByTerm(termId);
	}

	public List<Assay> findAssays(TermId termId) {
		return this.annotationRepository.findAssaysByTerm(termId);
	}

}
