package org.jax.oan.core;

import java.util.List;

public class PhenotypeAnnotationDto {

	private final List<Disease> diseases;
	private final List<Gene> genes;
	private final List<Assay> assays;

	public PhenotypeAnnotationDto(List<Disease> diseases, List<Gene> genes, List<Assay> assays) {
		this.diseases = diseases;
		this.genes = genes;
		this.assays = assays;
	}

	public List<Disease> getDiseases() {
		return diseases;
	}

	public List<Gene> getGenes() {
		return genes;
	}

	public List<Assay> getAssays() {
		return assays;
	}
}
