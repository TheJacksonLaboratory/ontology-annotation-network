package org.jax.oan.repository;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

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
	 * @return the pattern, such as {@code %loeys%dietz%}, or an empty optional if the query has nothing but separators.
	 */
	static Optional<String> likePattern(String query) {
		String terms = query.strip().toLowerCase().replaceAll(SEPARATORS, "%").replaceAll("^%+|%+$", "");
		return terms.isEmpty() ? Optional.empty() : Optional.of("%" + terms + "%");
	}

	/**
	 * Orders results so that exact matches come first, then names that start with the query, then everything else.
	 * Case, spaces and hyphens are ignored when comparing.
	 *
	 * @param query the text the user searched for.
	 * @param name extracts the name to compare from a result.
	 * @param <T> the result type.
	 * @return a comparator that ranks results against {@code query}.
	 */
	static <T> Comparator<T> ranking(String query, Function<T, String> name) {
		String key = key(query);
		return Comparator.comparingInt(result -> {
			String candidate = key(name.apply(result));
			return candidate.equals(key) ? 0 : candidate.startsWith(key) ? 1 : 2;
		});
	}

	private static String key(String text) {
		return text.toLowerCase().replaceAll(SEPARATORS, "");
	}
}
