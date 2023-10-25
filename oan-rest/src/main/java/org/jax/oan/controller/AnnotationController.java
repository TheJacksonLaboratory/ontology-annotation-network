package org.jax.oan.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.serde.annotation.SerdeImport;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jax.oan.core.Disease;
import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;
import org.jax.oan.service.AnnotationService;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

@Controller("${api-prefix}/annotation")
@SerdeImport(Disease.class)
public class AnnotationController {

	private final AnnotationService annotationService;

	public AnnotationController(AnnotationService annotationService) {
		this.annotationService = annotationService;
	}

	@Get(uri="/{id}", produces="application/json")
	public HttpResponse<?> all(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		try {
			TermId termId = TermId.of(id);
			HttpResponse.ok().body(annotationService.findAll(termId));
		} catch(PhenolRuntimeException e){
			throw new OntologyAnnotationNetworkRuntimeException();
		}
		return HttpResponse.ok();
	}

	@Get(uri="/{id}/genes", produces="application/json")
	public HttpResponse<?> genes(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		try {
			TermId termId = TermId.of(id);
			HttpResponse.ok().body(annotationService.findGenes(termId));
		} catch(PhenolRuntimeException e){
			throw new OntologyAnnotationNetworkRuntimeException();
		}
		return HttpResponse.ok();
	}

	@Get(uri="/{id}/diseases", produces="application/json")
	public HttpResponse<?> diseases(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		try {
			TermId termId = TermId.of(id);
			return HttpResponse.ok().body(annotationService.findDiseases(termId));
		} catch(PhenolRuntimeException e){
			throw new OntologyAnnotationNetworkRuntimeException();
		}
	}


	@Get(uri="/{id}/assay", produces="application/json")
	public HttpResponse<?> loinc(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		try {
			TermId termId = TermId.of(id);
			HttpResponse.ok().body(annotationService.findAssays(termId));
		} catch(PhenolRuntimeException e){
			throw new OntologyAnnotationNetworkRuntimeException();
		}
		return HttpResponse.ok();
	}

	@Get(uri="/{id}/actions", produces="application/json")
	public HttpResponse<?> actions(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		try {
			TermId termId = TermId.of(id);
			HttpResponse.ok().body(annotationService.findMedicalActions(termId));
		} catch(PhenolRuntimeException e){
			throw new OntologyAnnotationNetworkRuntimeException();
		}
		return HttpResponse.ok();
	}
}
