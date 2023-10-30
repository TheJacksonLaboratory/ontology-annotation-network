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
public class PhenotypeRepository {

	private final Driver driver;

	public PhenotypeRepository(Driver driver) {
		this.driver = driver;
	}

	/**
	 * Give me all the diseases that manifest this phenotype and its descendants.
	 * @param termId the termId of the phenotype
	 * @return List of diseases  or empty list
	 */
	public List<Disease> findDiseasesByTerm(TermId termId){
		List<Disease> diseases = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("call {MATCH (k: Phenotype {id: $id})-[:HAS_CHILD *0..]->(q:Phenotype) with collect(distinct k.id) + collect(q.id) as nodes return nodes} with nodes MATCH (d: Disease)-[:MANIFESTS]->(p: Phenotype)\n" +
					"WHERE p.id IN nodes RETURN DISTINCT d", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				Value value = result.next().get("d");
				Disease disease = new Disease(TermId.of(value.get("id").asString()), value.get("name").asString());
				diseases.add(disease);
			}
		}
		return diseases;
	}

	/**
	 * Give me all the genes that are expressed in diseases that manifest this phenotype and its descendants.
	 * @param termId the termId of the phenotype
	 * @return List of genes or empty list
	 */
	public List<Gene> findGenesByTerm(TermId termId) {
		List<Gene> genes = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("call {MATCH (k: Phenotype {id: $id})-[:HAS_CHILD *0..]->(q:Phenotype) with collect(distinct k.id) + collect(q.id) as nodes return nodes} call { with nodes MATCH (d: Disease)-[:MANIFESTS]->(p: Phenotype)" +
					" WHERE p.id IN nodes RETURN d as diseases} with diseases MATCH (diseases)-[:EXPRESSES]-(g: Gene) RETURN DISTINCT g", parameters("id", termId.getValue()));

			while (result.hasNext()) {
				Value value = result.next().get("g");
				Gene gene = new Gene(TermId.of(value.get("id").asString()), value.get("name").asString());
				genes.add(gene);
			}
		}
		return genes;
	}

	/**
	 * Give me all the assays that measure this phenotype.
	 * @param termId the termId of the phenotype
	 * @return List of assays or empty list
	 */
	public List<Assay> findAssaysByTerm(TermId termId){
		List<Assay> assays = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (a: Assay)-[:MEASURES]-(p: Phenotype {id: $id}) RETURN a", parameters("id", termId.getValue()));

			while (result.hasNext()) {
				Record record = result.next();
				Assay assay = new Assay(TermId.of(record.get("id").asString()), record.get("name").asString());
				assays.add(assay);
			}
		}
		return assays;
	}
}
