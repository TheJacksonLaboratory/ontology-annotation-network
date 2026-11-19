package org.jax.oan.repository;

import java.util.List;

/**
 * One page of search results and the number of results across all pages.
 *
 * @param results the results on this page, in ranked order.
 * @param total the number of matching entities across all pages.
 * @param <T> the result type.
 */
public record SearchPage<T>(List<T> results, int total) {
}
