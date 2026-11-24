package org.jax.oan.repository;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchQueryTest {

	@Test
	void tokensSplitOnPunctuation() {
		assertEquals(List.of("marfan", "s", "syndrome", "type", "1"), SearchQuery.tokens("MARFAN'S SYNDROME, TYPE-1"));
		assertEquals(List.of("omim", "154700"), SearchQuery.tokens("OMIM:154700"));
	}

	@Test
	void tokensOfPunctuationOnlyIsEmpty() {
		assertEquals(List.of(), SearchQuery.tokens(" - : , "));
	}

	@Test
	void ftsMatchPrefixesLastToken() {
		assertEquals("\"marfan\" \"synd\"*", SearchQuery.ftsMatch(List.of("marfan", "synd")));
	}

	@Test
	void operatorWordsAreQuoted() {
		assertEquals("\"loss\" \"or\" \"gain\"*", SearchQuery.ftsMatch(SearchQuery.tokens("LOSS OR GAIN")));
	}

	@Test
	void keyDropsCaseSpacesHyphens() {
		assertEquals("loeysdietz", SearchQuery.key("Loeys- Dietz"));
	}

	@Test
	void searchPageHoldsResultsAndTotal() {
		SearchPage<String> page = new SearchPage<>(List.of("a"), 5);
		assertEquals(List.of("a"), page.results());
		assertEquals(5, page.total());
	}
}
