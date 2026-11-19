package org.jax.oan.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Runs a ranked, paged name search over one entity table and its full-text table.
 *
 * <p>Each matching entity appears once, at its best tier:
 * <ol start="0">
 *   <li>id equals the query, or {@code mondo_id} does when enabled</li>
 *   <li>primary name equals the query</li>
 *   <li>a synonym equals the query</li>
 *   <li>primary name starts with the query</li>
 *   <li>any other full-text hit, best {@code bm25} first</li>
 *   <li>id contains the query</li>
 *   <li>primary name contains the query's words in order</li>
 * </ol>
 * Name comparisons in tiers 1&ndash;3 ignore case, spaces and hyphens, and apply to primary names found by either the
 * full-text index or the in-order substring match, so {@code SCN 1A} still ranks {@code SCN1A} in tier 1. Ties are
 * ordered by name, then id.
 */
final class RankedSearch {
	private static final String KEY = "REPLACE(REPLACE(LOWER(name), ' ', ''), '-', '')";

	private final String cte;
	private final boolean matchMondoId;

	/**
	 * Creates a search over one entity.
	 *
	 * @param table the entity table, with {@code id} and {@code name} columns.
	 * @param ftsTable the FTS5 table with one row per searchable name.
	 * @param ftsIdColumn the column of {@code ftsTable} that holds the entity id.
	 * @param matchMondoId whether an exact match on the entity's {@code mondo_id} also ranks in tier 0.
	 */
	RankedSearch(String table, String ftsTable, String ftsIdColumn, boolean matchMondoId) {
		this.matchMondoId = matchMondoId;
		this.cte = "WITH hits(id, tier, score) AS ("
				+ " SELECT " + ftsIdColumn + ", CASE"
				+ " WHEN kind = 'primary' AND " + KEY + " = ? THEN 1"
				+ " WHEN kind = 'synonym' AND " + KEY + " = ? THEN 2"
				+ " WHEN kind = 'primary' AND " + KEY + " LIKE ? THEN 3"
				+ " ELSE 4 END, bm25(" + ftsTable + ")"
				+ " FROM " + ftsTable + " WHERE " + ftsTable + " MATCH ?"
				+ " UNION ALL SELECT id, 0, NULL FROM " + table + " WHERE LOWER(id) = ?"
				+ (matchMondoId ? " OR LOWER(mondo_id) = ?" : "")
				+ " UNION ALL SELECT id, 5, NULL FROM " + table + " WHERE LOWER(id) LIKE ?"
				+ " UNION ALL SELECT id, CASE WHEN " + KEY + " = ? THEN 1 WHEN " + KEY + " LIKE ? THEN 3 ELSE 6 END, NULL"
				+ " FROM " + table + " WHERE LOWER(name) LIKE ?"
				+ "), ranked AS (SELECT id, MIN(tier) AS tier, MIN(score) AS score FROM hits GROUP BY id) ";
	}

	/**
	 * Reads one row of a page.
	 *
	 * @param <T> the result type.
	 */
	@FunctionalInterface
	interface RowMapper<T> {
		/**
		 * Maps the current row.
		 *
		 * @param rs the result set, positioned on a row.
		 * @return the mapped result.
		 * @throws SQLException if a column cannot be read.
		 */
		T map(ResultSet rs) throws SQLException;
	}

	/**
	 * Runs the search and returns one page of results.
	 *
	 * @param connection the database connection; not closed by this method.
	 * @param columns the entity columns to select, qualified with the alias {@code e}, such as {@code e.id, e.name}.
	 * @param table the entity table, joined as {@code e}.
	 * @param query the text the user searched for.
	 * @param offset the number of ranked results to skip.
	 * @param limit the maximum number of results to return, or {@code -1} for no limit.
	 * @param mapper maps each selected row to a result.
	 * @param <T> the result type.
	 * @return the page, with the number of matching entities across all pages; an empty page with a total of zero
	 * if the query has no words.
	 * @throws SQLException if the query fails.
	 */
	<T> SearchPage<T> page(Connection connection, String columns, String table, String query, int offset, int limit,
						   RowMapper<T> mapper) throws SQLException {
		List<String> tokens = SearchQuery.tokens(query);
		Optional<String> like = SearchQuery.likePattern(query);
		if (tokens.isEmpty() || like.isEmpty()) {
			return new SearchPage<>(List.of(), 0);
		}
		List<T> results = new ArrayList<>();
		int total = 0;
		String sql = cte + "SELECT " + columns + ", COUNT(*) OVER () AS total FROM ranked r JOIN " + table
				+ " e ON e.id = r.id ORDER BY r.tier, r.score IS NULL, r.score, e.name, e.id LIMIT ? OFFSET ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			int next = bind(statement, query, tokens, like.get());
			statement.setInt(next, limit);
			statement.setInt(next + 1, offset);
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					results.add(mapper.map(rs));
					total = rs.getInt("total");
				}
			}
		}
		if (results.isEmpty() && (offset > 0 || limit == 0)) {
			try (PreparedStatement statement = connection.prepareStatement(cte + "SELECT COUNT(*) FROM ranked")) {
				bind(statement, query, tokens, like.get());
				try (ResultSet rs = statement.executeQuery()) {
					total = rs.next() ? rs.getInt(1) : 0;
				}
			}
		}
		return new SearchPage<>(results, total);
	}

	private int bind(PreparedStatement statement, String query, List<String> tokens, String like) throws SQLException {
		String q = query.strip().toLowerCase();
		String key = SearchQuery.key(query.strip());
		int i = 1;
		statement.setString(i++, key);
		statement.setString(i++, key);
		statement.setString(i++, key + "%");
		statement.setString(i++, SearchQuery.ftsMatch(tokens));
		statement.setString(i++, q);
		if (matchMondoId) {
			statement.setString(i++, q);
		}
		statement.setString(i++, "%" + q + "%");
		statement.setString(i++, key);
		statement.setString(i++, key + "%");
		statement.setString(i++, like);
		return i;
	}
}
