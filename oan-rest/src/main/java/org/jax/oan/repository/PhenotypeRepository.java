package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.jax.oan.core.*;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Singleton
public class PhenotypeRepository {

	private final Connection connection;

	public PhenotypeRepository(SqliteConnectionProvider connectionProvider) {
		this.connection = connectionProvider.connection();
	}

	private static final String DESCENDANTS_CTE =
			"WITH RECURSIVE descendants(id) AS (" +
					"SELECT ? UNION SELECT pc.child_id FROM phenotype_child pc JOIN descendants d ON pc.parent_id = d.id" +
					") ";

	/**
	 * Give me all the diseases that manifest this phenotype and its descendants.
	 * @param termId the termId of the phenotype
	 * @return List of diseases  or empty list
	 */
	public Collection<Disease> findDiseasesByTerm(TermId termId){
		List<Disease> diseases = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(DESCENDANTS_CTE +
				"SELECT DISTINCT d.id, d.name, d.mondo_id " +
				"FROM descendants desc " +
				"CROSS JOIN disease_phenotype dp ON dp.phenotype_id = desc.id " +
				"JOIN disease d ON d.id = dp.disease_id")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					diseases.add(new Disease(TermId.of(rs.getString("id")), rs.getString("name"),
							rs.getString("mondo_id"), null));
				}
			}
		} catch (Exception e){
			return Collections.emptyList();
		}
		return diseases;
	}

	/**
	 * Give me all the genes that are expressed in diseases that manifest this phenotype and its descendants.
	 * @param termId the termId of the phenotype
	 * @return List of genes or empty list
	 */
	public Collection<Gene> findGenesByTerm(TermId termId) {
		List<Gene> genes = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(DESCENDANTS_CTE +
				"SELECT DISTINCT g.id, g.name " +
				"FROM descendants desc " +
				"CROSS JOIN disease_phenotype dp ON dp.phenotype_id = desc.id " +
				"JOIN disease_gene dg ON dg.disease_id = dp.disease_id " +
				"JOIN gene g ON g.id = dg.gene_id")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					genes.add(new Gene(TermId.of(rs.getString("id")), rs.getString("name")));
				}
			}
		} catch (Exception e){
			return Collections.emptyList();
		}
		return genes;
	}

	/**
	 * Give me all the assays that measure this phenotype.
	 * @param termId the termId of the phenotype
	 * @return List of assays or empty list
	 */
	public Collection<Assay> findAssaysByTerm(TermId termId){
		List<Assay> assays = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT a.id, a.name FROM assay_phenotype ap JOIN assay a ON a.id = ap.assay_id WHERE ap.phenotype_id = ?")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					assays.add(new Assay(TermId.of(rs.getString("id")), rs.getString("name")));
				}
			}
		} catch (Exception e){
			return Collections.emptyList();
		}
		return assays;
	}

	/**
	 * Give me all the assays that measure this phenotype.
	 * @param termId the termId of the phenotype
	 * @return List of assays or empty list
	 */
	public Collection<MedicalActionSourceExtended> findMedicalActionsByTerm(TermId termId){
		Map<String, String> actionNames = new LinkedHashMap<>();
		Map<String, Set<MedicalActionRelation>> actionRelations = new LinkedHashMap<>();
		Map<String, Set<String>> actionSources = new LinkedHashMap<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT mat.medical_action_id, ma.name AS action_name, mat.relation, maa.source " +
						"FROM medical_action_target mat " +
						"JOIN medical_action ma ON ma.id = mat.medical_action_id " +
						"JOIN medical_action_annotation maa ON maa.medical_action_id = mat.medical_action_id AND maa.phenotype_id = mat.phenotype_id " +
						"WHERE mat.phenotype_id = ?")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					String actionId = rs.getString("medical_action_id");
					actionNames.putIfAbsent(actionId, rs.getString("action_name"));
					actionRelations.computeIfAbsent(actionId, k -> new LinkedHashSet<>())
							.add(MedicalActionRelation.valueOf(rs.getString("relation")));
					actionSources.computeIfAbsent(actionId, k -> new LinkedHashSet<>())
							.add(rs.getString("source"));
				}
			}
		} catch (Exception e){
			return Collections.emptyList();
		}

		List<MedicalActionSourceExtended> medicalActions = new ArrayList<>();
		for (String actionId : actionNames.keySet()) {
			medicalActions.add(new MedicalActionSourceExtended(TermId.of(actionId), actionNames.get(actionId),
					new ArrayList<>(actionRelations.get(actionId)), new ArrayList<>(actionSources.get(actionId))));
		}
		return medicalActions;
	}
}
