package org.jax.oan.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Objects;

/**
 * This class represents a line for medical actions in the context of phenotypes
 * the limitation of this structure is not being able to tell which relation is match to which source.
 */
public class MedicalActionTargetExtended extends MedicalAction {
	private final List<MedicalActionRelation> relations;
	private final List<OntologyEntity> targets;

	public MedicalActionTargetExtended(TermId id, String name, List<MedicalActionRelation> relations, List<OntologyEntity> targets) {
		super(id, name);
		this.relations = relations.stream().sorted().toList();
		this.targets = targets;
	}

	public List<MedicalActionRelation> getRelations() {
		return relations;
	}

	public List<OntologyEntity> getTargets() {
		return targets;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		MedicalActionTargetExtended that = (MedicalActionTargetExtended) o;
		return Objects.equals(relations, that.relations) && Objects.equals(targets, that.targets);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), relations, targets);
	}
}
