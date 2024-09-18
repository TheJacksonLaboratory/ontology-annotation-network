package org.jax.oan.controller;

import io.micronaut.http.server.types.files.SystemFile;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.jax.oan.TestData;
import org.jax.oan.core.*;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.jax.oan.service.DiseaseService;
import org.jax.oan.service.DownloadService;
import org.jax.oan.service.GeneService;
import org.jax.oan.service.PhenotypeService;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.hamcrest.CoreMatchers.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.jax.oan.TestData.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class AnnotationControllerTest {

	@Inject
	EmbeddedApplication<?> application;

	@Inject
	private PhenotypeService phenotypeService;

	@Inject
	private GeneService geneService;

	@Inject
	private DiseaseService diseaseService;

	@Inject
	private DownloadService downloadService;

	@Test
	void positive_by_disease(RequestSpecification spec) throws OntologyAnnotationNetworkException {
		when(diseaseService.findAll(TermId.of("OMIM:039293")))
				.thenReturn(new DiseaseAnnotationDto(TestData.diseases().get(0), Map.of("limbs", phenotypesExtended()), genes(), List.of()));
		spec.when().get("/api/network/annotation/OMIM:039293").then()
				.statusCode(200).body("genes.id",
						hasItems("NCBIGene:00093", "NCBIGene:02002"));
	}

	@Test
	void positive_by_phenotype(RequestSpecification spec) {
		when(phenotypeService.findAll(TermId.of("HP:0000001")))
				.thenReturn(new PhenotypeAnnotationDto(diseases(), genes(), assays(), List.of()));
		spec.when().get("/api/network/annotation/HP:0000001").then()
				.statusCode(200)
				.body("diseases.id", hasItems("MONDO:099233", "DECIPHER:434444"))
				.body("genes.id", hasItems("NCBIGene:00093", "NCBIGene:02002"))
				.body("assays.id", hasItem("LOINC:55555"));

	}

	@Test
	void positive_by_gene(RequestSpecification spec) {
		when(geneService.findAll(TermId.of("NCBIGene:093232")))
				.thenReturn(new GeneAnnotationDto(diseases(), phenotypes()));
		spec.when().get("/api/network/annotation/NCBIGene:093232").then()
				.statusCode(200)
				.body("diseases.id", hasItems("MONDO:099233", "DECIPHER:434444"))
				.body("phenotypes.id", hasItems("HP:099233", "HP:434444"));
	}

	@Test
	void negative_by_no_prefix(RequestSpecification spec){
		spec.when().get("/api/network/annotation/FAKE:093232").then().statusCode(400);
	}

	@Test
	void negative_by_incorrect_term(RequestSpecification spec){
		spec.when().get("/api/network/annotation/not-right").then().statusCode(400);
	}

	@Test
	void positive_download_file(RequestSpecification spec) throws IOException {
		SystemFile file = buildSimpleSpreadSheet();
		when(downloadService.associations(TermId.of("OMIM:0392932"), SupportedEntity.DISEASE, SupportedEntity.GENE))
				.thenReturn(file);
		spec.when().get("/api/network/annotation/OMIM:0392932/download/gene").then()
				.statusCode(200).contentType("text/tab-separated-values");
		Files.deleteIfExists(file.getFile().toPath());
	}

	@Test
	void negative_download_file_no_prefix(RequestSpecification spec){
		spec.when().get("/api/network/annotation/FAKE:093232/download/gene").then().statusCode(400);
	}

	@Test
	void negative_download_file_incorrect_term(RequestSpecification spec){
		spec.when().get("/api/network/annotation/not-right/download/disease").then().statusCode(400);
	}

	@Test
	void negative_download_file_bad_type(RequestSpecification spec){
		spec.when().get("/api/network/annotation/OMIM:0392932/download/disease").then().statusCode(400);
	}

	private static SystemFile buildSimpleSpreadSheet() throws IOException {
		File file = File.createTempFile("test", ".tsv");
		try (PrintWriter pw = new PrintWriter(file)) {
			pw.println(String.format("%s \t %s", "id", "name"));
			pw.println(String.format("%s \t %s", "NCBIGene:301", "TX2"));
		}
		return new SystemFile(file).attach(file.getName());
	}

	@MockBean(DiseaseService.class)
	DiseaseService diseaseService() {
		return mock(DiseaseService.class);
	}

	@MockBean(GeneService.class)
	GeneService geneService() {
		return mock(GeneService.class);
	}

	@MockBean(PhenotypeService.class)
	PhenotypeService phenotypeService() {
		return mock(PhenotypeService.class);
	}

	@MockBean(DownloadService.class)
	DownloadService downloadService() {
		return mock(DownloadService.class);
	}
}
