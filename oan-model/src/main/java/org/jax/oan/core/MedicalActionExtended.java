package org.jax.oan.core;

import java.util.List;
import java.util.Objects;

public class MedicalActionExtended extends BaseOntologyClass {
	private final List<SourceRelation> sourceRelations;

	public MedicalActionExtended(MedicalAction medicalAction, List<SourceRelation> sourceRelations) {
		super(medicalAction.getId(), medicalAction.getName());
		this.sourceRelations = sourceRelations;
	}

	public List<SourceRelation> getSourceRelations() {
		return sourceRelations;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		MedicalActionExtended that = (MedicalActionExtended) o;
		return Objects.equals(sourceRelations, that.sourceRelations);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), sourceRelations);
	}
}
