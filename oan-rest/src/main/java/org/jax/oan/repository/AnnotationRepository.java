package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.jax.oan.core.Assay;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

@Singleton
public class AnnotationRepository {

	private final Driver driver;

	public AnnotationRepository(Driver driver) {
		this.driver = driver;
	}

	/**
	 * Give me all the diseases that manifest this phenotype.
	 * @param termId the termId of the phenotype
	 * @return List of disease that manifest in the disease.
	 */
	public List<Disease> findDiseasesByTerm(TermId termId){
		List<Disease> diseases = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease)-[:MANIFESTS]-(p: Phenotype {id: $id}) RETURN d", parameters("id", termId.getValue()));

			while (result.hasNext()) {
				Value value = result.next().get("d");
				Disease disease = new Disease(TermId.of(value.get("id").asString()), value.get("name").asString());
				diseases.add(disease);
			}
			return diseases;
		}
	}


	public List<Gene> findGenesByTerm(TermId termId) {
		List<Gene> genes = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease)-[:MANIFEST]-(p: Phenotype {id: $id}) RETURN d", parameters("id", termId.getValue()));

			while (result.hasNext()) {
				Record record = result.next();
				Gene gene = new Gene(TermId.of(record.get("id").asString()), record.get("name").asString());
				genes.add(gene);
			}
			return genes;
		}
	}

	public List<Assay> findAssaysByTerm(TermId termId){
		List<Assay> assays = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (a: Assay)-[:MEASURES]-(p: Phenotype {id: $id}) RETURN a", parameters("id", termId.getValue()));

			while (result.hasNext()) {
				Record record = result.next();
				Assay assay = new Assay(TermId.of(record.get("id").asString()), record.get("name").asString());
				assays.add(assay);
			}
			return assays;
		}
	}
}
