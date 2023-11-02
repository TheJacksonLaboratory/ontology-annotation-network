package org.jax.oan.core;

import java.util.List;

public record PhenotypeAnnotationDto(List<Disease> diseases, List<Gene> genes, List<Assay> assays) { }
