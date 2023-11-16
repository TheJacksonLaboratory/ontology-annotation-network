package org.jax.oan.core;

import java.util.Collection;

public record GeneAnnotationDto(Collection<Disease> diseases, Collection<Phenotype> phenotypes) { }
