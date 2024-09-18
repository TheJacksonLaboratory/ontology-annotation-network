package org.jax.oan.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;


abstract class BaseOntologyClass implements OntologyEntity {
	private final String id;
	private final String name;

	protected BaseOntologyClass(TermId id, String name) {
		this.id = id.getValue();
		this.name = name;
	}

	protected BaseOntologyClass(String id, String name){
		this.id = id;
		this.name = name;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BaseOntologyClass that = (BaseOntologyClass) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}
}
