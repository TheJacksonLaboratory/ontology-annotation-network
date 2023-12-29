package org.jax.oan.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record DiseaseAnnotationDto(Map<String, List<Phenotype>> categories, Collection<Gene> genes) { }
