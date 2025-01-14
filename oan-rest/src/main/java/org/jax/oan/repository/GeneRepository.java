package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.core.Phenotype;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;

import java.util.*;

import static org.neo4j.driver.Values.parameters;

@Singleton
public class GeneRepository {

	private final Driver driver;

	public GeneRepository(Driver driver) {
		this.driver = driver;
	}


	/**
	 * Find me a gene by query.
	 * @param query - the text to search for
	 * @return List of genes matching the query sorted by if the gene starts with query.
	 */
	public Collection<Gene> findGenes(String query){
		Collection<Gene> genes = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
				Result result = tx.run("MATCH (g: Gene) WHERE toLower(g.name) =~ $qe OR g.id CONTAINS $q RETURN g", parameters("q", query, "qe", String.format("%s%s%s",".*", query.toLowerCase().replaceAll("\\s+", " ").replaceAll("[-\\s]", ".*"), ".*")));
				while (result.hasNext()) {
					Value value = result.next().get("g");
					Gene gene = new Gene(TermId.of(value.get("id").asString()), value.get("name").asString());
					genes.add(gene);
				}
		}
		catch (Exception e){
			return Collections.emptyList();
		}
		return genes.stream().sorted(Comparator.comparing((Gene g) -> !g.getName().toLowerCase()
				.startsWith(query.toLowerCase()))).toList();
	}

	/**
	 * Give me all the phenotypes that are determined by this gene.
	 * @param termId the termId of the gene
	 * @return List of phenotypes or empty list
	 */
	public Collection<Phenotype> findPhenotypesByGene(TermId termId){
		Collection<Phenotype> phenotypes = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (g: Gene {id: $id})-[:DETERMINES]-(p:Phenotype) RETURN p", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				Value value = result.next().get("p");
				Phenotype phenotype = new Phenotype(TermId.of(value.get("id").asString()), value.get("name").asString());
				phenotypes.add(phenotype);
			}
		} catch (Exception e){
			return Collections.emptyList();
		}
		return phenotypes;
	}

	/**
	 * Give me all the diseases that are expressed in this gene.
	 * @param termId the termId of the gene
	 * @return List of diseases or empty list
	 */
	public Collection<Disease> findDiseasesByGene(TermId termId) {
		List<Disease> diseases = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease)-[:EXPRESSES]->(g: Gene {id: $id}) RETURN d", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				Value value = result.next().get("d");
				Disease disease = new Disease(TermId.of(value.get("id").asString()), value.get("name").asString(),
						value.get("mondoId").asString(), null);
				diseases.add(disease);
			}
		}
		catch (Exception e){
			return Collections.emptyList();
		}
		return diseases;
	}
}
