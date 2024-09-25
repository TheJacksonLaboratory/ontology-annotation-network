package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.jax.oan.core.*;
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
		catch (Exception e){
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
			Result result = tx.run("MATCH (d: Disease) WHERE toLower(d.name) =~ $qe OR toLower(d.id) CONTAINS $q RETURN d", parameters("q", query.toLowerCase(), "qe", String.format("%s%s%s",".*", query.toLowerCase().replaceAll("[-\\s]", ".*"), ".*")));
			while (result.hasNext()) {
				Value value = result.next().get("d");
				Disease disease = new Disease(TermId.of(value.get("id").asString()), value.get("name").asString(),
						value.get("mondoId").asString(), value.get("description").asString());
				diseases.add(disease);
			}
		}
		catch (Exception e){
			return Collections.emptyList();
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
		catch (Exception e){
			return Collections.emptyList();
		}
		return genes;
	}

	/**
	 * Give me all the phenotypes that manifest in this disease.
	 * @param termId the termId of the disease
	 * @return List of diseases or empty list
	 */
	public Collection<PhenotypeExtended> findPhenotypesByDisease(TermId termId){
		Collection<PhenotypeExtended> phenotypes = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (d: Disease {id: $id})<-[:MANIFESTS]-(p: Phenotype)-[:DESCRIBES {context: $id }]-(pm: PhenotypeAnnotation) RETURN p, pm", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				Record r = result.next();
				Value p = r.get("p");
				Value pm = r.get("pm");
				PhenotypeMetadata phenotypeMetadata = new PhenotypeMetadata(pm.get("sex").asString(), pm.get("onset").asString(), pm.get("frequency").asString(), Arrays.stream(pm.get("sources").asString().split(";")).filter(Predicate.not(String::isBlank)).collect(Collectors.toList()));
				PhenotypeExtended phenotype = new PhenotypeExtended(TermId.of(p.get("id").asString()), p.get("name").asString(), p.get("category").asString(), phenotypeMetadata);
				phenotypes.add(phenotype);
			}
		}
		catch (Exception e){
			return Collections.emptyList();
		}
		return phenotypes;
	}

	/**
	 * Give me all the medical actions for a disease with the phenotypes (or diseases) they clarify.
	 * @param termId the termId of the disease
	 * @return List of diseases or empty list
	 */
	public Collection<MedicalActionTargetExtended> findMedicalActionsByDisease(TermId termId){
		Collection<MedicalActionTargetExtended> actions = new ArrayList<>();
		try (Transaction tx = driver.session().beginTransaction()) {
			Result result = tx.run("MATCH (p: Phenotype)-[c:CLARIFIES {context: $id}]-(m: MedicalAction) RETURN m, collect(distinct p) as p, collect(distinct c.by) as c", parameters("id", termId.getValue()));
			while (result.hasNext()) {
				List<OntologyEntity> targets = new ArrayList<>();
				List<MedicalActionRelation> relations = new ArrayList<>();
				Record r = result.next();
				Value m = r.get("m");

				// The root node of hpo serves as a way to annotate to the disease being referenced
				// instead of a phenotype of the disease. We should map back before we return it.
				r.get("p").values().forEach(t -> {
					if (t.get("id").asString().contains("HP:0000118")){
						targets.add(new Phenotype(termId, t.get("name").asString()));
					} else {
						targets.add(new Phenotype(TermId.of(t.get("id").asString()), t.get("name").asString()));
					}
				});
				r.get("c").values().forEach(t -> {
					relations.add(MedicalActionRelation.valueOf(t.asString()));
				});
				actions.add(new MedicalActionTargetExtended(TermId.of(m.get("id").asString()), m.get("name").asString(), relations, targets ));
			}
		}
		catch (Exception e){
			return Collections.emptyList();
		}
		return actions;
	}
}
