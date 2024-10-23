package org.jax.oan.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.serde.annotation.SerdeImport;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jax.oan.core.Disease;
import org.jax.oan.core.SearchDto;
import org.jax.oan.core.SupportedEntity;
import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;
import org.jax.oan.service.DiseaseService;
import org.jax.oan.service.SearchService;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import jakarta.validation.constraints.Pattern;

import java.util.Arrays;
import java.util.Collection;

@Controller("/search")
@SerdeImport(SearchDto.class)
@SerdeImport(Disease.class)
public class SearchController {
	private final SearchService searchService;
	private final DiseaseService diseaseService;

	public SearchController(SearchService searchService, DiseaseService diseaseService) {
		this.searchService = searchService;
		this.diseaseService = diseaseService;
	}

	@Get(uri="/{entity}", produces="application/json")
	public HttpResponse<?> searchEntity(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*", format = "string")
											@PathVariable String entity,
										@Schema(minLength = 1, maxLength = 255, type = "string", pattern = "^[a-zA-Z0-9\\s\\-':]+$", format = "string")
										@QueryValue @Pattern(regexp = "^[a-zA-Z0-9\\s\\-':,]+$") String q,
										@Schema(minLength = 0, maxLength = 1000, type = "number", format = "int32")
											@QueryValue(value = "page", defaultValue = "0") int page,
										@Schema(minLength = 0, maxLength = 10000, type = "number", format = "int32")
											@QueryValue(value = "limit", defaultValue = "10") int limit) {

		if (entity.equalsIgnoreCase("GENE")){
			return HttpResponse.ok(this.searchService.searchGene(q.toUpperCase(), page, limit));
		} else if (entity.equalsIgnoreCase("DISEASE")){
			return HttpResponse.ok(this.searchService.searchDisease(q.toUpperCase(), page, limit));
		} else {
			return HttpResponse.badRequest();
		}
	}


	/**
	 * This is our base controller for annotations that deals with different ontology term types
	 * and returns a defined annotation schema.
	 * @param entity the entity you care about with your list of phenotypes
	 * @param p the list of comma-seperated phenotype(hp) term ids
	 * @return an http response with the specific annotation schema based on the type
	 * @throws OntologyAnnotationNetworkRuntimeException which will be a 500
	 */
	@Get(uri="/{entity}/intersect", produces="application/json")
	public HttpResponse<?> intersect(
			@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*", format = "string") @PathVariable String entity,
			@Schema(minLength = 1, maxLength = 20000, type = "string", pattern = ".*", format = "string") @QueryValue String p) {
		try {
			Collection<TermId> terms = Arrays.stream(p.split(",")).map(TermId::of).toList();
			SupportedEntity target = SupportedEntity.valueOf(entity.toUpperCase());
			if (SupportedEntity.isLinkedType(SupportedEntity.PHENOTYPE, target)){
				return HttpResponse.ok(this.diseaseService.findIntersectingByPhenotypes(terms));
			} else {
				throw new HttpStatusException(HttpStatus.BAD_REQUEST, String.format("Intersecting %s associations for your phenotypes is not supported.", entity));
			}
		} catch(PhenolRuntimeException | OntologyAnnotationNetworkRuntimeException e){
			throw new HttpStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

}
