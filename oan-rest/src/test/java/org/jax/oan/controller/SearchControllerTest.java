package org.jax.oan.controller;

import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.core.SearchDto;
import org.jax.oan.service.DiseaseService;
import org.jax.oan.service.SearchService;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
public class SearchControllerTest {
	@Inject
	EmbeddedApplication<?> application;

	@Inject
	DiseaseService diseaseService;

	@Inject
	SearchService searchService;

	@Test
	void positive_search_by_gene(RequestSpecification spec){
		when(searchService.searchGene("TP53", 0,10))
				.thenReturn(new SearchDto(List.of(new Gene(TermId.of("NCBIGene:093232"), "tp53"),
						new Gene(TermId.of("NCBIGene:093234"), "tp53b")), 2));
		spec.when().get("/api/network/search/gene?q=tp53").then()
				.statusCode(200)
				.body("results.id", hasItems("NCBIGene:093234", "NCBIGene:093232"))
				.body("totalCount", equalTo(2));
	}

    @Test
    void positive_search_by_disease(RequestSpecification spec){
		when(searchService.searchDisease("MARFAN", 0,10))
				.thenReturn(new SearchDto(List.of(
						new Disease(TermId.of("OMIM:333333"), "Bad disease 1", "MONDO:000001", "no description"),
						new Disease(TermId.of("OMIM:444444"), "Bad disease 2", "MONDO:000002", "funky description"),
						new Disease(TermId.of("OMIM:555555"), "Bad disease 3", "MONDO:000003", "description")), 3));
		spec.when().get("/api/network/search/disease?q=marfan").then()
				.statusCode(200)
				.body("results.id", hasItems("OMIM:333333", "OMIM:444444"))
				.body("totalCount", equalTo(3));
	}

	@Test
	void negative_search_by_bad_entity(RequestSpecification spec){
		spec.when().get("/api/network/search/variant?q=chr1:60023-2300").then().statusCode(400);
		spec.when().get("/api/network/search/phenotype?q=bighead").then().statusCode(400);
	}

	@Test
	void negative_search_bad_query(RequestSpecification spec){
		spec.when().get("/api/network/search/disease?q=(    )").then().statusCode(400);
		spec.when().get("/api/network/search/disease?q=%20Cardiac%20tumors%20(").then().statusCode(400);
		spec.when().get("/api/network/search/disease?q=NM_000314.8:c.-511G%3EA").then().statusCode(400);
		spec.when().get("/api/network/search/disease?q=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").then().statusCode(400);
	}

	@Test
	void postive_intersecting(RequestSpecification spec){
		when(diseaseService.findIntersectingByPhenotypes(List.of(TermId.of("HP:333333"), TermId.of("HP:44444"))))
				.thenReturn(List.of(
						new Disease(TermId.of("OMIM:333333"), "Bad disease 1", "MONDO:000001", "no description"),
						new Disease(TermId.of("OMIM:444444"), "Bad disease 2", "MONDO:000002", "funky description"),
						new Disease(TermId.of("OMIM:555555"), "Bad disease 3", "MONDO:000003", "description")));
		spec.when().get("/api/network/search/disease/intersect?p=HP:333333,HP:44444").then()
				.statusCode(200)
				.body("id", hasItems("OMIM:333333", "OMIM:444444"));
	}

	@Test
	void negative_intersecting(RequestSpecification spec){
		spec.when().get("/api/network/search/disease/intersect?q=chr1:60023-2300").then().statusCode(400);
		spec.when().get("/api/network/search/phenotype?q=bighead").then().statusCode(400);
	}

	@MockBean(DiseaseService.class)
	DiseaseService diseaseService() {
		return mock(DiseaseService.class);
	}

	@MockBean(SearchService.class)
	SearchService searchService() {
		return mock(SearchService.class);
	}
}
