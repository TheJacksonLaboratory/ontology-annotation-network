package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.jax.oan.core.*;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Singleton
public class DiseaseRepository {
	private final Connection connection;

	public DiseaseRepository(SqliteConnectionProvider connectionProvider) {
		this.connection = connectionProvider.connection();
	}

	/**
	 * Find me a disease by query.
	 * @param termId - the termId of the disease
	 * @return List of diseases matching the query sorted by if the disease starts with.
	 */
	public Optional<Disease> findDiseaseById(TermId termId){
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT id, name, mondo_id, description FROM disease WHERE id = ?")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				if (rs.next()) {
					return Optional.of(new Disease(TermId.of(rs.getString("id")), rs.getString("name"),
							rs.getString("mondo_id"), rs.getString("description")));
				}
				return Optional.empty();
			}
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Find me a disease by query.
	 * @param query - the text to search for
	 * @return List of diseases matching the query sorted by if the disease starts with.
	 */
	public Collection<Disease> findDiseases(String query) {
		List<Disease> diseases = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT id, name, mondo_id, description FROM disease WHERE LOWER(name) LIKE ? OR LOWER(id) LIKE ?")) {
			String like = "%" + query.toLowerCase() + "%";
			statement.setString(1, like);
			statement.setString(2, like);
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					diseases.add(new Disease(TermId.of(rs.getString("id")), rs.getString("name"),
							rs.getString("mondo_id"), rs.getString("description")));
				}
			}
		} catch (Exception e) {
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
		List<Gene> genes = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT g.id, g.name FROM disease_gene dg JOIN gene g ON g.id = dg.gene_id WHERE dg.disease_id = ?")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					genes.add(new Gene(TermId.of(rs.getString("id")), rs.getString("name")));
				}
			}
		} catch (Exception e) {
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
		List<PhenotypeExtended> phenotypes = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT p.id, p.name, p.category, dp.sex, dp.onset, dp.frequency, dp.sources " +
						"FROM disease_phenotype dp JOIN phenotype p ON p.id = dp.phenotype_id WHERE dp.disease_id = ?")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					List<String> sources = Arrays.stream(rs.getString("sources").split(";"))
							.filter(s -> !s.isBlank()).toList();
					PhenotypeMetadata metadata = new PhenotypeMetadata(rs.getString("sex"), rs.getString("onset"),
							rs.getString("frequency"), sources);
					phenotypes.add(new PhenotypeExtended(TermId.of(rs.getString("id")), rs.getString("name"),
							rs.getString("category"), metadata));
				}
			}
		} catch (Exception e) {
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
		Map<String, String> actionNames = new LinkedHashMap<>();
		Map<String, List<OntologyEntity>> actionTargets = new LinkedHashMap<>();
		Map<String, Set<MedicalActionRelation>> actionRelations = new LinkedHashMap<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT mat.medical_action_id, ma.name AS action_name, mat.phenotype_id, p.name AS phenotype_name, mat.relation " +
						"FROM medical_action_target mat " +
						"JOIN medical_action ma ON ma.id = mat.medical_action_id " +
						"JOIN phenotype p ON p.id = mat.phenotype_id " +
						"WHERE mat.disease_id = ?")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					String actionId = rs.getString("medical_action_id");
					String phenotypeId = rs.getString("phenotype_id");
					actionNames.putIfAbsent(actionId, rs.getString("action_name"));

					// The root node of hpo serves as a way to annotate to the disease being referenced
					// instead of a phenotype of the disease. We should map back before we return it.
					OntologyEntity target = phenotypeId.equals("HP:0000118")
							? new Phenotype(termId, rs.getString("phenotype_name"))
							: new Phenotype(TermId.of(phenotypeId), rs.getString("phenotype_name"));
					List<OntologyEntity> targets = actionTargets.computeIfAbsent(actionId, k -> new ArrayList<>());
					if (!targets.contains(target)) {
						targets.add(target);
					}
					actionRelations.computeIfAbsent(actionId, k -> new LinkedHashSet<>())
							.add(MedicalActionRelation.valueOf(rs.getString("relation")));
				}
			}
		} catch (Exception e) {
			return Collections.emptyList();
		}

		List<MedicalActionTargetExtended> actions = new ArrayList<>();
		for (String actionId : actionNames.keySet()) {
			actions.add(new MedicalActionTargetExtended(TermId.of(actionId), actionNames.get(actionId),
					new ArrayList<>(actionRelations.get(actionId)), actionTargets.get(actionId)));
		}
		return actions;
	}
}
