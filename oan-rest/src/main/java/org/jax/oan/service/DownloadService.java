package org.jax.oan.service;

import builders.dsl.spreadsheet.builder.poi.PoiSpreadsheetBuilder;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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
					List<Phenotype> phenotypes = this.diseaseRepository.findPhenotypesByDisease(termId);
					return buildFile(String.format("phenotypes_for_%s.xlsx", termId.getValue()), phenotypes);

				} else {
					List<Gene> genes = this.diseaseRepository.findGenesByDisease(termId);
					return buildFile(String.format("genes_for_%s.xlsx", termId.getValue()), genes);
				}
			}
			case GENE -> {
				if(target.equals(SupportedEntity.DISEASE)){
					List<Disease> diseases = this.geneRepository.findDiseasesByGene(termId);
					return buildFile(String.format("dieases_for_%s.xlsx", termId.getValue()), diseases);
				} else {
					List<Phenotype> phenotypes = this.geneRepository.findPhenotypesByGene(termId);
					return buildFile(String.format("phenotypes_for_%s.xlsx", termId.getValue()), phenotypes);
				}
			}
			case PHENOTYPE -> {
				if(target.equals(SupportedEntity.DISEASE)){
					List<Disease> diseases = this.phenotypeRepository.findDiseasesByTerm(termId);
					return buildFile(String.format("dieases_for_%s.xlsx", termId.getValue()), diseases);
				} else {
					List<Gene> genes = this.phenotypeRepository.findGenesByTerm(termId);
					return buildFile(String.format("genes_for_%s.xlsx", termId.getValue()), genes);
				}
			}
			default -> throw new OntologyAnnotationNetworkRuntimeException(String.format("Downloading %s association for %s failed because source type is not supported.", target.toString().toLowerCase(), termId.getValue()));
		}
	}

	public SystemFile buildFile(String filename, List<? extends OntologyClass> targetList){
		try {
			String prefix = filename.split("\\.")[0];
			File file = File.createTempFile(prefix, ".xlsx");
			PoiSpreadsheetBuilder.create(file).build(w -> {
				w.sheet("associations", s -> {
					s.row(r -> Stream.of("id", "name")
							.forEach(header -> r.cell(cd -> {
										cd.value(header);
									})
							));
					for (OntologyClass target : targetList.stream().sorted(Comparator.comparing((OntologyClass o) -> o.getName())).toList()) {
						s.row(r -> {
							r.cell(target.getId());
							r.cell(target.getName());
						});
					}
				});
			});
			return new SystemFile(file).attach(filename);
		} catch (IOException e) {
			e.printStackTrace();
			throw new OntologyAnnotationNetworkRuntimeException("Could not generate association file.");
		}
	}
}
