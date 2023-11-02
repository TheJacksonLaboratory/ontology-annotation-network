package org.jax.oan.core;

import java.util.List;
import java.util.Map;

public record DiseaseAnnotationDto(Map<String, List<Phenotype>> phenotypeByCategory, List<Gene> genes) { }
