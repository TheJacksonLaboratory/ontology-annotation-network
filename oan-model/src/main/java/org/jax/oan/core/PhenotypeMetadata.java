package org.jax.oan.core;

import java.util.List;
import java.util.Objects;

public class PhenotypeMetadata {

	private String sex;
	private String onset;
	private String frequency;
	private List<String> sources;

	public PhenotypeMetadata(String sex, String onset, String frequency, List<String> sources) {
		this.sex = sex;
		this.onset = onset;
		this.frequency = frequency;
		this.sources = sources;
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
