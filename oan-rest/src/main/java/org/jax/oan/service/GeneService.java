package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.core.Disease;
import org.jax.oan.core.GeneAnnotationDto;
import org.jax.oan.core.Phenotype;
import org.jax.oan.repository.GeneRepository;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;

@Singleton
public class GeneService {

	private final GeneRepository geneRepository;

	public GeneService(GeneRepository geneRepository) {
		this.geneRepository = geneRepository;
	}

	public GeneAnnotationDto findAll(TermId termId){
		Collection<Disease> diseases = this.geneRepository.findDiseasesByGene(termId);
		Collection<Phenotype> phenotypes = this.geneRepository.findPhenotypesByGene(termId);
		return new GeneAnnotationDto(diseases, phenotypes);
	}
}
