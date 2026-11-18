package org.jax.oan.repository;

/**
 * Normalizes free-text search queries so that spaces and hyphens are interchangeable.
 */
final class SearchQuery {
	private static final String SEPARATORS = "[-\\s]+";

	private SearchQuery() {
	}

	/**
	 * Builds a case-insensitive SQL LIKE pattern in which each run of spaces or hyphens matches any text.
	 *
	 * @param query the text the user searched for.
	 * @return the pattern, such as {@code %loeys%dietz%}.
	 */
	static String likePattern(String query) {
		return "%" + query.strip().toLowerCase().replaceAll(SEPARATORS, "%") + "%";
	}
}
