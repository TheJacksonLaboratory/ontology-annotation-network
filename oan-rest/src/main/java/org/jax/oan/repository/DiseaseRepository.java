package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.jax.oan.core.Gene;
import org.jax.oan.core.Phenotype;
import org.jax.oan.core.PhenotypeMetadata;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

@Singleton
public class DiseaseRepository {
	private final Driver driver;

	public DiseaseRepository(Driver driver) {
		this.driver = driver;
	}


	/**
	 * Give me all the genes that are expressed in this disease.
	 * @param termId the termId of the disease
	 * @return List of genes or empty list
	 */
	public List<Gene> findGenesByDisease(TermId termId) {
		List<Gene> genes = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease {id: $id})-[:EXPRESSES]-(g: Gene) RETURN g", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				Value value = result.next().get("g");
				Gene gene = new Gene(TermId.of(value.get("id").asString()), value.get("name").asString());
				genes.add(gene);
			}
		}
		return genes;
	}

	/**
	 * Give me all the phenotypes that manifest in this disease.
	 * @param termId the termId of the disease
	 * @return List of diseases or empty list
	 */
	public List<Phenotype> findPhenotypesByDisease(TermId termId){
		List<Phenotype> phenotypes = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease {id: $id})-[:MANIFESTS]-(p: Phenotype)-[:WITH_METADATA {context: $id }]-(pm: PhenotypeMetadata) RETURN p, pm", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				Record r = result.next();
				Value p = r.get("p");
				Value pm = r.get("pm");
				PhenotypeMetadata phenotypeMetadata = new PhenotypeMetadata(pm.get("sex").asString(), pm.get("onset").asString(), pm.get("frequency").asString(), List.of(pm.get("sources").asString().split(";")));
				Phenotype phenotype = new Phenotype(TermId.of(p.get("id").asString()), p.get("name").asString(), p.get("category").asString(), phenotypeMetadata);
				phenotypes.add(phenotype);
			}
		}
		return phenotypes;
	}
}
