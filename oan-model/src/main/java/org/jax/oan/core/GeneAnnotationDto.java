package org.jax.oan.core;

import java.util.List;

public class GeneAnnotationDto {

	List<Disease> diseases;
	List<Phenotype> phenotypes;

	public GeneAnnotationDto(List<Disease> diseases, List<Phenotype> phenotypes) {
		this.diseases = diseases;
		this.phenotypes = phenotypes;
	}

	public List<Disease> getDiseases() {
		return diseases;
	}

	public List<Phenotype> getPhenotypes() {
		return phenotypes;
	}
}
