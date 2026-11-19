package org.jax.oan.repository;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final Logger log = LoggerFactory.getLogger(GeneRepository.class);
	private static final RankedSearch SEARCH = new RankedSearch("gene", "gene_fts", "gene_id", false);

	private final SqliteConnectionProvider connectionProvider;

	public GeneRepository(SqliteConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	/**
	 * Finds every gene matching a query, ranked.
	 *
	 * @param query the text to search for.
	 * @return all matching genes, in ranked order.
	 */
	public Collection<Gene> findGenes(String query){
		return searchGenes(query, 0, -1).results();
	}

	/**
	 * Searches genes by id, symbol and HGNC alternate names, and returns one page of ranked results.
	 *
	 * @param query the text to search for.
	 * @param offset the number of ranked results to skip.
	 * @param limit the maximum number of results to return, or {@code -1} for no limit.
	 * @return the page and the number of matching genes across all pages; an empty page with a total of zero if the
	 * query has no words or the search fails.
	 */
	public SearchPage<Gene> searchGenes(String query, int offset, int limit) {
		try (Connection connection = connectionProvider.connection()) {
			return SEARCH.page(connection, "e.id, e.name", "gene", query, offset, limit,
					rs -> new Gene(TermId.of(rs.getString("id")), rs.getString("name")));
		} catch (Exception e) {
			log.error("Gene search failed", e);
			return new SearchPage<>(List.of(), 0);
		}
	}

	/**
	 * Give me all the phenotypes that are determined by this gene.
	 * @param termId the termId of the gene
	 * @return List of phenotypes or empty list
	 */
	public Collection<Phenotype> findPhenotypesByGene(TermId termId){
		List<Phenotype> phenotypes = new ArrayList<>();
		try (Connection connection = connectionProvider.connection();
			 PreparedStatement statement = connection.prepareStatement(
				"SELECT p.id, p.name FROM gene_phenotype gp JOIN phenotype p ON p.id = gp.phenotype_id WHERE gp.gene_id = ?")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					phenotypes.add(new Phenotype(TermId.of(rs.getString("id")), rs.getString("name")));
				}
			}
		} catch (Exception e) {
			log.warn("Repository query failed", e);
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
		try (Connection connection = connectionProvider.connection();
			 PreparedStatement statement = connection.prepareStatement(
				"SELECT d.id, d.name, d.mondo_id FROM disease_gene dg JOIN disease d ON d.id = dg.disease_id WHERE dg.gene_id = ?")) {
			statement.setString(1, termId.getValue());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					diseases.add(new Disease(TermId.of(rs.getString("id")), rs.getString("name"),
							rs.getString("mondo_id"), null));
				}
			}
		} catch (Exception e) {
			log.warn("Repository query failed", e);
			return Collections.emptyList();
		}
		return diseases;
	}
}
