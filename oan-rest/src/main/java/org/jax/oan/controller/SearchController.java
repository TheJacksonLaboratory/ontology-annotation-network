package org.jax.oan.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.serde.annotation.SerdeImport;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jax.oan.core.OntologyEntity;
import org.jax.oan.service.SearchService;

@Controller("${api-prefix}/network/search")
@SerdeImport(OntologyEntity.class)
public class SearchController {
	private SearchService searchService;

	public SearchController(SearchService searchService) {
		this.searchService = searchService;
	}

	@Get(uri="/{entity}", produces="application/json")
	public HttpResponse<?> searchEntity(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*")
											@PathVariable String entity, @QueryValue String q) {
		if (entity.equalsIgnoreCase("GENE")){
			return HttpResponse.ok(this.searchService.searchGene(q.toUpperCase()));
		} else if (entity.equalsIgnoreCase("DISEASE")){
			return HttpResponse.ok(this.searchService.searchDisease(q.toUpperCase()));
		} else {
			return HttpResponse.noContent();
		}
	}
}
