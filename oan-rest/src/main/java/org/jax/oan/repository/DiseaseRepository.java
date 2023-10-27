package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.jax.oan.core.Gene;
import org.jax.oan.core.Phenotype;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.Driver;
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

	public List<Phenotype> findPhenotypesByDisease(TermId termId){
		List<Phenotype> phenotypes = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease {id: $id})-[:MANIFESTS]-(p: Phenotype) RETURN p", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				Value value = result.next().get("p");
				Phenotype phenotype = new Phenotype(TermId.of(value.get("id").asString()), value.get("name").asString());
				phenotypes.add(phenotype);
			}
		}
		return phenotypes;
	}
}
