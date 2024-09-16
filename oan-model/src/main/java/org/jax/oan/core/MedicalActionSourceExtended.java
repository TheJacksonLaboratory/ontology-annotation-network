package org.jax.oan.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Objects;

/**
 * This class represents a line for medical actions in the context of phenotypes
 * the limitation of this structure is not being able to tell which relation is match to which source.
 */
public class MedicalActionSourceExtended extends MedicalAction {
	private final List<MedicalActionRelation> relations;
	private final List<String> sources;

	public MedicalActionSourceExtended(TermId id, String name, List<MedicalActionRelation> relations, List<String> sources) {
		super(id, name);
		this.relations = relations.stream().sorted().toList();
		this.sources = sources;
	}

	public List<MedicalActionRelation> getRelations() {
		return relations;
	}

	public List<String> getSources() {
		return sources;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		MedicalActionSourceExtended that = (MedicalActionSourceExtended) o;
		return Objects.equals(relations, that.relations) && Objects.equals(sources, that.sources);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), relations, sources);
	}
}
