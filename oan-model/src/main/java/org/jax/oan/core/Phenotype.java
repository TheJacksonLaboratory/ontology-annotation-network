package org.jax.oan.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Optional;

public class Phenotype extends OntologyClass {

	private PhenotypeMetadata metadata;
	private String category;

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
		if (metadata != null){
			return Optional.of(metadata);
		}
		return Optional.empty();
	}

	public String getCategory() {
		return category;
	}
}
