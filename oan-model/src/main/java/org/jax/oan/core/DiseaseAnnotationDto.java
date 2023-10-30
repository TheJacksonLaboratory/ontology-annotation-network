package org.jax.oan.core;

import java.util.List;
import java.util.Map;

public class DiseaseAnnotationDto {

	private final Map<String, List<Phenotype>> phenotypeByCategory;
	private final List<Gene> genes;

	public DiseaseAnnotationDto(Map<String, List<Phenotype>> phenotypeByCategory, List<Gene> genes) {
		this.phenotypeByCategory = phenotypeByCategory;
		this.genes = genes;
	}

	public Map<String, List<Phenotype>> getPhenotypes() {
		return phenotypeByCategory;
	}

	public List<Gene> getGenes() {
		return genes;
	}
}
