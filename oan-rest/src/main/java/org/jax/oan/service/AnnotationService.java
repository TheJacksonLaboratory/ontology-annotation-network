package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.monarchinitiative.phenol.ontology.data.TermId;

@Singleton
public class AnnotationService {

	public AnnotationService() {
	}

	public boolean findAll(TermId termId){
		return true;
	}

	public boolean findDiseases(TermId termId) {
		return true;
	}

	public boolean findGenes(TermId termId) {
		return true;
	}

	public boolean findAssays(TermId termId) {
		return true;
	}

	public boolean findMedicalActions(TermId termId){
		return true;
	}
}
