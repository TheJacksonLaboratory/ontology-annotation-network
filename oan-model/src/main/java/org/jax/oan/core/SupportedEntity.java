package org.jax.oan.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Arrays;
import java.util.List;

public enum SupportedEntity {
	PHENOTYPE("HP"),
	DISEASE("OMIM", "MONDO", "ORPHA","DECIPHER"),
	GENE("NCBIGENE"),
	ASSAY("LOINC"),

	UNKNOWN;
	private final List<String> prefixes;
	private SupportedEntity(String... prefixes){
		this.prefixes = Arrays.asList(prefixes);
	}

	public List<String> prefixes() {
		return prefixes;
	}

	public static SupportedEntity from(TermId termId) {
		final String prefix = termId.getPrefix().toUpperCase();
		for (SupportedEntity entity : SupportedEntity.values()) {
			if (entity.prefixes().contains(prefix)) {
				return entity;
			}
		}
		return SupportedEntity.UNKNOWN;
	}
}
