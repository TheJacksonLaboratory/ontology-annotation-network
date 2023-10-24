package org.jax.oan.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jax.oan.service.AnnotationService;

@Controller("${api-prefix}/annotation")
public class AnnotationController {

	private final AnnotationService annotationService;

	public AnnotationController(AnnotationService annotationService) {
		this.annotationService = annotationService;
	}

	@Get(uri="/{id}", produces="application/json")
	public HttpResponse<?> all(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		return HttpResponse.ok();
	}

	@Get(uri="/{id}/genes", produces="application/json")
	public HttpResponse<?> genes(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		return HttpResponse.ok();
	}

	@Get(uri="/{id}/diseases", produces="application/json")
	public HttpResponse<?> diseases(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		return HttpResponse.ok();
	}


	@Get(uri="/{id}/assay", produces="application/json")
	public HttpResponse<?> loinc(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		return HttpResponse.ok();
	}

	@Get(uri="/{id}/actions", produces="application/json")
	public HttpResponse<?> actions(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*") @PathVariable String id) {
		return HttpResponse.ok();
	}
}
