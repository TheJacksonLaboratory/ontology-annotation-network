package org.jax.oan.service;

import jakarta.inject.Singleton;
import org.jax.oan.repository.DiseaseRepository;

@Singleton
public class DiseaseService {

	private final DiseaseRepository diseaseRepository;

	public DiseaseService(DiseaseRepository diseaseRepository) {
		this.diseaseRepository = diseaseRepository;
	}
}
