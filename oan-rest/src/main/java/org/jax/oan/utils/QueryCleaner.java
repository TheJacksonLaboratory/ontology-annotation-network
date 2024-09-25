package org.jax.oan.utils;

public class QueryCleaner {

	/**
	 * This method cleans the query string from any special characters
	 * @param query the query string
	 * @return the cleaned query string
	 */
	public static String clean(String query) {
		return query.replaceAll("[^a-zA-Z0-9\\-:\\s]", "");
	}
}
