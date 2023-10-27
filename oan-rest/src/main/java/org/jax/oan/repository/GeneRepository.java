package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.jax.oan.core.Disease;
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
public class GeneRepository {

	private final Driver driver;

	public GeneRepository(Driver driver) {
		this.driver = driver;
	}

	public List<Phenotype> findPhenotypesByGene(TermId termId){
		List<Phenotype> phenotypes = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (g: Gene {id: $id})-[:DETERMINES]-(p:Phenotype) RETURN p", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				Value value = result.next().get("p");
				Phenotype phenotype = new Phenotype(TermId.of(value.get("id").asString()), value.get("name").asString());
				phenotypes.add(phenotype);
			}
		}
		return phenotypes;
	}

	public List<Disease> findDiseasesByGene(TermId termId) {
		List<Disease> diseases = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease)-[:EXPRESSES]->(g: Gene {id: $id}) RETURN d", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				Value value = result.next().get("d");
				Disease disease = new Disease(TermId.of(value.get("id").asString()), value.get("name").asString());
				diseases.add(disease);
			}
		}
		return diseases;
	}
}
