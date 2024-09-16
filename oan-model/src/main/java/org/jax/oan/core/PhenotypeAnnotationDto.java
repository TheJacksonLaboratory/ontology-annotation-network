package org.jax.oan.core;

import java.util.Collection;

public record PhenotypeAnnotationDto(Collection<Disease> diseases, Collection<Gene> genes, Collection<Assay> assays, Collection<MedicalActionSourceExtended> medicalActions) { }
