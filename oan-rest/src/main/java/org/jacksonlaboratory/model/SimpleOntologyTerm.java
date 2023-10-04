package org.jacksonlaboratory.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

public abstract class SimpleOntologyTerm {

	TermId id;

	String name;

	public SimpleOntologyTerm(TermId id, String name) {
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
