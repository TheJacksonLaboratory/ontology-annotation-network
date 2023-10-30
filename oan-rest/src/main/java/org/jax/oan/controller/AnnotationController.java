package org.jax.oan.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.serde.annotation.SerdeImport;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jax.oan.core.*;
import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;
import org.jax.oan.service.DiseaseService;
import org.jax.oan.service.GeneService;
import org.jax.oan.service.PhenotypeService;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

@Controller("${api-prefix}/annotation")
@SerdeImport(Disease.class)
@SerdeImport(Gene.class)
@SerdeImport(Phenotype.class)
@SerdeImport(PhenotypeMetadata.class)
@SerdeImport(PhenotypeAnnotationDto.class)
@SerdeImport(GeneAnnotationDto.class)
@SerdeImport(DiseaseAnnotationDto.class)
@SerdeImport(Assay.class)
public class AnnotationController {

	private final PhenotypeService phenotypeService;
	private final GeneService geneService;
	private final DiseaseService diseaseService;

	public AnnotationController(PhenotypeService phenotypeService, GeneService geneService, DiseaseService diseaseService) {
		this.phenotypeService = phenotypeService;
		this.geneService = geneService;
		this.diseaseService = diseaseService;
	}

	/**
	 * This is our base controller for annotations that deals with different ontology term types
	 * and returns a defined annotation schema.
	 * @param id the ontology identifier with prefix
	 * @return an http response with the specific annotation schema based on the type
	 * @throws OntologyAnnotationNetworkRuntimeException which will be a 500
	 */
	@Get(uri="/{id}", produces="application/json")
	public HttpResponse<?> all(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		try {
			TermId termId = TermId.of(id);
			switch (SupportedEntity.from(termId)){
				case PHENOTYPE:
					return HttpResponse.ok(phenotypeService.findAll(termId));
				case DISEASE:
					return HttpResponse.ok(diseaseService.findAll(termId));
				case GENE:
					return HttpResponse.ok(geneService.findAll(termId));
				default:
					throw new OntologyAnnotationNetworkRuntimeException("Term Identifier not supported.");
			}
		} catch(PhenolRuntimeException e){
			throw new OntologyAnnotationNetworkRuntimeException();
		}
	}
}
