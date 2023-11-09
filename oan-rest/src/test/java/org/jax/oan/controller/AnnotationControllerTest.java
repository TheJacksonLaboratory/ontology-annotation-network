package org.jax.oan.controller;

import io.micronaut.http.server.types.files.SystemFile;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.jax.oan.core.*;
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
import java.util.List;
import java.util.Map;

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
	void positive_by_disease(RequestSpecification spec) {
		when(diseaseService.findAll(TermId.of("OMIM:039293")))
				.thenReturn(new DiseaseAnnotationDto(Map.of("limbs", phenotypes()), genes()));
		spec.when().get("/api/annotation/OMIM:039293").then()
				.statusCode(200).body("genes.id",
						hasItems("NCBIGene:00093", "NCBIGene:02002"));
	}

	@Test
	void positive_by_phenotype(RequestSpecification spec) {
		when(phenotypeService.findAll(TermId.of("HP:0000001")))
				.thenReturn(new PhenotypeAnnotationDto(diseases(), genes(), assays()));
		spec.when().get("/api/annotation/HP:0000001").then()
				.statusCode(200)
				.body("diseases.id", hasItems("MONDO:099233", "DECIPHER:434444"))
				.body("genes.id", hasItems("NCBIGene:00093", "NCBIGene:02002"))
				.body("assays.id", hasItem("LOINC:55555"));

	}

	@Test
	void positive_by_gene(RequestSpecification spec) {
		when(geneService.findAll(TermId.of("NCBIGene:093232")))
				.thenReturn(new GeneAnnotationDto(diseases(), phenotypes()));
		spec.when().get("/api/annotation/NCBIGene:093232").then()
				.statusCode(200)
				.body("diseases.id", hasItems("MONDO:099233", "DECIPHER:434444"))
				.body("phenotypes.id", hasItems("HP:099233", "HP:434444"));
	}

	@Test
	void negative_by_no_prefix(RequestSpecification spec){
		spec.when().get("/api/annotation/FAKE:093232").then().statusCode(400);
	}

	@Test
	void negative_by_incorrect_term(RequestSpecification spec){
		spec.when().get("/api/annotation/not-right").then().statusCode(400);
	}

	@Test
	void positive_download_file(RequestSpecification spec) throws IOException {
		when(downloadService.associations(TermId.of("OMIM:0392932"), SupportedEntity.DISEASE, SupportedEntity.GENE))
				.thenReturn(buildSimpleSpreadSheet());
		spec.when().get("/api/annotation/OMIM:0392932/download/gene").then()
				.statusCode(200).contentType("text/tab-separated-values");
	}

	@Test
	void negative_download_file_no_prefix(RequestSpecification spec){
		spec.when().get("/api/annotation/FAKE:093232/download/gene").then().statusCode(400);
	}

	@Test
	void negative_download_file_incorrect_term(RequestSpecification spec){
		spec.when().get("/api/annotation/not-right/download/disease").then().statusCode(400);
	}

	@Test
	void negative_download_file_bad_type(RequestSpecification spec){
		spec.when().get("/api/annotation/OMIM:0392932/download/disease").then().statusCode(400);
	}

	private static List<Gene> genes(){
		return List.of(
				new Gene(TermId.of("NCBIGene:00093"),"TP4"),
				new Gene(TermId.of("NCBIGene:02002"),"YZ")
		);
	}

	private static List<Disease> diseases(){
		return List.of(
				new Disease(TermId.of("MONDO:099233"),"Really bad one"),
				new Disease(TermId.of("DECIPHER:434444"),"Kinda bad one")
		);
	}

	private static List<Phenotype> phenotypes(){
		return List.of(
				new Phenotype(TermId.of("HP:099233"),"Long legs"),
				new Phenotype(TermId.of("HP:434444"),"Big bicep small arm")
		);
	}

	private static List<Assay> assays(){
		return List.of(
				new Assay(TermId.of("LOINC:55555"),"Special bicep test")
		);
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
