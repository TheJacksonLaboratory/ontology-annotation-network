package org.jax.oan.service;

import io.micronaut.http.server.types.files.SystemFile;
import jakarta.inject.Singleton;
import org.jax.oan.core.*;
import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;
import org.jax.oan.repository.DiseaseRepository;
import org.jax.oan.repository.GeneRepository;
import org.jax.oan.repository.PhenotypeRepository;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Singleton
public class DownloadService {

	private final DiseaseRepository diseaseRepository;
	private final PhenotypeRepository phenotypeRepository;
	private final GeneRepository geneRepository;

	public DownloadService(DiseaseRepository diseaseRepository, PhenotypeRepository phenotypeRepository, GeneRepository geneRepository) {
		this.diseaseRepository = diseaseRepository;
		this.phenotypeRepository = phenotypeRepository;
		this.geneRepository = geneRepository;
	}

	public SystemFile associations(TermId termId, SupportedEntity source, SupportedEntity target){
		switch (source){
			case DISEASE -> {
				if (target.equals(SupportedEntity.PHENOTYPE)){
					Collection<PhenotypeExtended> phenotypes = this.diseaseRepository.findPhenotypesByDisease(termId);
					return buildFile(String.format("phenotypes_for_%s", termId.getValue()), phenotypes);

				} else {
					Collection<Gene> genes = this.diseaseRepository.findGenesByDisease(termId);
					return buildFile(String.format("genes_for_%s", termId.getValue()), genes);
				}
			}
			case GENE -> {
				if(target.equals(SupportedEntity.DISEASE)){
					Collection<Disease> diseases = this.geneRepository.findDiseasesByGene(termId);
					return buildFile(String.format("diseases_for_%s", termId.getValue()), diseases);
				} else {
					Collection<Phenotype> phenotypes = this.geneRepository.findPhenotypesByGene(termId);
					return buildFile(String.format("phenotypes_for_%s", termId.getValue()), phenotypes);
				}
			}
			case PHENOTYPE -> {
				if(target.equals(SupportedEntity.DISEASE)){
					Collection<Disease> diseases = this.phenotypeRepository.findDiseasesByTerm(termId);
					return buildFile(String.format("diseases_for_%s", termId.getValue()), diseases);
				} else if(target.equals(SupportedEntity.GENE)) {
					Collection<Gene> genes = this.phenotypeRepository.findGenesByTerm(termId);
					return buildFile(String.format("genes_for_%s", termId.getValue()), genes);
				} else {
					Collection<Assay> assays = this.phenotypeRepository.findAssaysByTerm(termId);
					return buildFile(String.format("assays_for_%s", termId.getValue()), assays);
				}
			}
			default -> throw new OntologyAnnotationNetworkRuntimeException(String.format("Downloading %s association for %s failed because source type is not supported.", target.toString().toLowerCase(), termId.getValue()));
		}
	}

	public SystemFile buildFile(String filename, Collection<? extends OntologyEntity> targetList){
		try {
			File file = File.createTempFile(filename, ".tsv");
			try (PrintWriter pw = new PrintWriter(file)) {
				pw.println(String.format("%s \t %s", "id", "name"));
				for (OntologyEntity target : targetList.stream().sorted(Comparator.comparing(OntologyEntity::getName)).toList()) {
					pw.println(String.format("%s \t %s", target.getId(), target.getName()));
				}
			}
			return new SystemFile(file).attach(filename);
		} catch (IOException e) {
			throw new OntologyAnnotationNetworkRuntimeException("Could not generate association file.", e);
		}
	}
}
