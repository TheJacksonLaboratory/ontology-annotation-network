package org.jax.oan.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;
import java.util.Optional;

public class Phenotype extends OntologyClass {

	private PhenotypeMetadata metadata;
	private String category = "";

	public Phenotype(TermId id, String name) {
		super(id, name);
	}

	public Phenotype(TermId id, String name, String category) {
		super(id, name);
		this.category = category;
	}
	public Phenotype(TermId id, String name, String category, PhenotypeMetadata metadata) {
		super(id, name);
		this.metadata = metadata;
		this.category = category;
	}

	public Optional<PhenotypeMetadata> getMetadata() {
		return Optional.ofNullable(metadata);
	}

	public String getCategory() {
		return category;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Phenotype phenotype = (Phenotype) o;
		return Objects.equals(metadata, phenotype.metadata) && Objects.equals(category, phenotype.category);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), metadata, category);
	}
}
