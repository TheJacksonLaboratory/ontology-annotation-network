package org.jax.oan.core;

import java.util.List;

public record GeneAnnotationDto(List<Disease> diseases, List<Phenotype> phenotypes) { }
