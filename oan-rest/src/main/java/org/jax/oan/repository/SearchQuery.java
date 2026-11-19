package org.jax.oan.repository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Normalizes free-text search queries so that spaces and hyphens are interchangeable.
 */
final class SearchQuery {
	private static final String SEPARATORS = "[-\\s]+";
	private static final String NON_WORD = "[^\\p{L}\\p{N}]+";

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

	/**
	 * Normalizes text for exact and starts-with comparisons by lowercasing it and removing spaces and hyphens.
	 *
	 * @param text the text to normalize.
	 * @return the normalized key, such as {@code loeysdietz} for {@code Loeys- Dietz}.
	 */
	static String key(String text) {
		return text.toLowerCase().replaceAll(SEPARATORS, "");
	}

	/**
	 * Splits a query into lowercase words the way the full-text index tokenizes names.
	 *
	 * @param query the text the user searched for.
	 * @return the words in order, split on every character that is not a letter or digit; empty if there are none.
	 */
	static List<String> tokens(String query) {
		return Arrays.stream(query.toLowerCase().split(NON_WORD))
				.filter(token -> !token.isEmpty())
				.toList();
	}

	/**
	 * Builds an FTS5 match expression that requires every word, treating the last word as a prefix.
	 *
	 * <p>Each word is double-quoted, so words such as {@code or}, {@code not} and {@code near} match as text rather than
	 * as FTS5 operators.
	 *
	 * @param tokens the words from {@link #tokens(String)}; must not be empty.
	 * @return the match expression, such as {@code "marfan" "synd"*}.
	 */
	static String ftsMatch(List<String> tokens) {
		return tokens.stream().map(token -> "\"" + token + "\"").collect(Collectors.joining(" ")) + "*";
	}
}
