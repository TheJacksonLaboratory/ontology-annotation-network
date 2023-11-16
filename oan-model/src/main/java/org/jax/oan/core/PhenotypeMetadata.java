package org.jax.oan.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PhenotypeMetadata {

	private final String sex;
	private final String onset;
	private final String frequency;
	private final List<String> sources;

	public PhenotypeMetadata(String sex, String onset, String frequency, List<String> sources) {
		this.sex = sex == null ? "" : sex;
		this.onset = onset == null ? "" : onset;
		this.frequency =  frequency == null ? "" : frequency;
		this.sources = sources == null ? Collections.emptyList() : sources;
	}

	public String getSex() {
		return sex;
	}

	public String getOnset() {
		return onset;
	}

	public String getFrequency() {
		return frequency;
	}

	public List<String> getSources() {
		return sources;
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
