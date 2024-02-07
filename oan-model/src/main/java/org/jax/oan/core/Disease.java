package org.jax.oan.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class Disease extends BaseOntologyClass {
	private final String mondoId;
	private final String description;

	public Disease(TermId id, String name, String mondoId, String description) {
		super(id, name);
		this.mondoId = mondoId;
		this.description = description;
	}

	public String getMondoId() {
		return mondoId;
	}

	public String getDescription() {
		return description;
	}
}
