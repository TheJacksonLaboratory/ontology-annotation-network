package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.core.Phenotype;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Singleton
public class GeneRepository {

	private final Connection connection;

	public GeneRepository(SqliteConnectionProvider connectionProvider) {
		this.connection = connectionProvider.connection();
	}

	/**
	 * Find me a gene by query.
	 * @param query - the text to search for
	 * @return List of genes matching the query sorted by if the gene starts with query.
	 */
	public Collection<Gene> findGenes(String query){
		List<Gene> genes = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT id, name FROM gene WHERE LOWER(name) LIKE ? OR id LIKE ?")) {
			statement.setString(1, "%" + query.toLowerCase() + "%");
			statement.setString(2, "%" + query + "%");
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					genes.add(new Gene(TermId.of(rs.getString("id")), rs.getString("name")));
				}
			}
		} catch (Exception e){
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
		List<Phenotype> phenotypes = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT p.id, p.name FROM gene_phenotype gp JOIN phenotype p ON p.id = gp.phenotype_id WHERE gp.gene_id = ?")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					phenotypes.add(new Phenotype(TermId.of(rs.getString("id")), rs.getString("name")));
				}
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
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT d.id, d.name, d.mondo_id FROM disease_gene dg JOIN disease d ON d.id = dg.disease_id WHERE dg.gene_id = ?")) {
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
}
