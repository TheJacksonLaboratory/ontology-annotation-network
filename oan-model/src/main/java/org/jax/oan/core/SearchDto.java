package org.jax.oan.core;

import java.util.Collection;

public record SearchDto(Collection<? extends OntologyEntity> results, int totalCount) { }
