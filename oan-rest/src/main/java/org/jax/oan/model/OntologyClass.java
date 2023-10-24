package org.jax.oan.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

public abstract class OntologyClass {

	TermId id;

	String name;

	public OntologyClass(TermId id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id.getValue();
	}

	public String getName() {
		return name;
	}
}
