package org.jax.oan.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class Disease extends BaseOntologyClass {
	private final String mondoId;

	public Disease(TermId id, String name, String mondoId) {
		super(id, name);
		this.mondoId = mondoId;
	}

	public String getMondoId() {
		return mondoId;
	}
}
