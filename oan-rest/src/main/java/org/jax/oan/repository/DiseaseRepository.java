package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.core.Phenotype;
import org.jax.oan.core.PhenotypeMetadata;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;

@Singleton
public class DiseaseRepository {
	private final Driver driver;

	public DiseaseRepository(Driver driver) {
		this.driver = driver;
	}


	/**
	 * Find me a disease by query.
	 * @param termId - the termId of the disease
	 * @return List of diseases matching the query sorted by if the disease starts with.
	 */
	public Optional<Disease> findDiseaseById(TermId termId){
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease) WHERE d.id = $q RETURN d", parameters("q", termId.getValue()));
			if (result.hasNext()){
				Value value = result.single().get("d");
				return Optional.of(new Disease(TermId.of(value.get("id").asString()), value.get("name").asString(),
						value.get("mondoId").asString(), value.get("description").asString()));
			}
			return Optional.empty();
		}
	}

	/**
	 * Find me a disease by query.
	 * @param query - the text to search for
	 * @return List of diseases matching the query sorted by if the disease starts with.
	 */
	public Collection<Disease> findDiseases(String query) {
		Collection<Disease> diseases = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease) WHERE toLower(d.name) CONTAINS $q OR toLower(d.id) CONTAINS $q RETURN d", parameters("q", query.toLowerCase()));
			while (result.hasNext()) {
				Value value = result.next().get("d");
				Disease disease = new Disease(TermId.of(value.get("id").asString()), value.get("name").asString(),
						value.get("mondoId").asString(), value.get("description").asString());
				diseases.add(disease);
			}
		}
		return diseases.stream().sorted(Comparator.comparing((Disease d) -> !d.getName().toLowerCase()
				.startsWith(query.toLowerCase()))).toList();
	}

	/**
	 * Give me all the genes that are expressed in this disease.
	 * @param termId the termId of the disease
	 * @return List of genes or empty list
	 */
	public Collection<Gene> findGenesByDisease(TermId termId) {
		Collection<Gene> genes = new ArrayList<>();
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
	public Collection<Phenotype> findPhenotypesByDisease(TermId termId){
		Collection<Phenotype> phenotypes = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease {id: $id})-[:MANIFESTS]-(p: Phenotype)-[:WITH_METADATA {context: $id }]-(pm: PhenotypeMetadata) RETURN p, pm", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				Record r = result.next();
				Value p = r.get("p");
				Value pm = r.get("pm");
				PhenotypeMetadata phenotypeMetadata = new PhenotypeMetadata(pm.get("sex").asString(), pm.get("onset").asString(), pm.get("frequency").asString(), Arrays.stream(pm.get("sources").asString().split(";")).filter(Predicate.not(String::isBlank)).collect(Collectors.toList()));
				Phenotype phenotype = new Phenotype(TermId.of(p.get("id").asString()), p.get("name").asString(), p.get("category").asString(), phenotypeMetadata);
				phenotypes.add(phenotype);
			}
		}
		return phenotypes;
	}
}
