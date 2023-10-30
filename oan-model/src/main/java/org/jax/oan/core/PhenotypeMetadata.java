package org.jax.oan.core;

import java.util.List;

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
}
