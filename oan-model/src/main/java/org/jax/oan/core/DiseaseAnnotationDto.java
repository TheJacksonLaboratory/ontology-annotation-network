package org.jax.oan.core;

import java.util.List;

public class DiseaseAnnotationDto {

	private final List<Phenotype> phenotypes;
	private final List<Gene> genes;

	public DiseaseAnnotationDto(List<Phenotype> phenotypes, List<Gene> genes) {
		this.phenotypes = phenotypes;
		this.genes = genes;
	}

	public List<Phenotype> getPhenotypes() {
		return phenotypes;
	}

	public List<Gene> getGenes() {
		return genes;
	}
}
