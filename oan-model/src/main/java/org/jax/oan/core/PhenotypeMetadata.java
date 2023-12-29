package org.jax.oan.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record PhenotypeMetadata(String sex, String onset, String frequency, List<String> sources) {

	public PhenotypeMetadata(String sex, String onset, String frequency, List<String> sources) {
		this.sex = sex;
		this.onset = onset;
		this.frequency = frequency;
		this.sources = sources == null ? Collections.emptyList() : sources;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PhenotypeMetadata that = (PhenotypeMetadata) o;
		return Objects.equals(sex, that.sex) && Objects.equals(onset, that.onset) && Objects.equals(frequency, that.frequency) && Objects.equals(sources, that.sources);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sex, onset, frequency, sources);
	}
}
