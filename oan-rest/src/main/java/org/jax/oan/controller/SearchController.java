package org.jax.oan.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.serde.annotation.SerdeImport;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jax.oan.core.SearchDto;
import org.jax.oan.service.SearchService;

@Controller("/search")
@SerdeImport(SearchDto.class)
public class SearchController {
	private SearchService searchService;

	public SearchController(SearchService searchService) {
		this.searchService = searchService;
	}

	@Get(uri="/{entity}", produces="application/json")
	public HttpResponse<?> searchEntity(@Schema(minLength = 1, maxLength = 20, type = "string", pattern = ".*", format = "string")
											@PathVariable String entity,
										@Schema(minLength = 1, maxLength = 255, type = "string", pattern = ".*", format = "string")
										@QueryValue String q,
										@Schema(minLength = 0, maxLength = 1000, type = "number", format = "int32")
											@QueryValue(value = "page", defaultValue = "0") int page,
										@Schema(minLength = 0, maxLength = 10000, type = "number", format = "int32")
											@QueryValue(value = "limit", defaultValue = "10") int limit) {

		if (entity.equalsIgnoreCase("GENE")){
			return HttpResponse.ok(this.searchService.searchGene(q.toUpperCase(), page, limit));
		} else if (entity.equalsIgnoreCase("DISEASE")){
			return HttpResponse.ok(this.searchService.searchDisease(q.toUpperCase(), page, limit));
		} else {
			return HttpResponse.noContent();
		}
	}
}
