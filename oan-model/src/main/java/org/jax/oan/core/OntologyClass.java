package org.jax.oan.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OntologyClass that = (OntologyClass) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}
}
