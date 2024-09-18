package org.jax.oan.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record DiseaseAnnotationDto(Disease disease, Map<String, List<PhenotypeExtended>> categories, Collection<Gene> genes, Collection<MedicalActionTargetExtended> medicalActions) { }
