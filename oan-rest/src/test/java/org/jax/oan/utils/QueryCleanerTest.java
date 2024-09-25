package org.jax.oan.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueryCleanerTest {

	@Test
	void clean() {
		String query = "HP:0000001";
		String badQuery = "HP:0000001!)";
		assertEquals("HP:0000001", QueryCleaner.clean(query));
		assertEquals("HP:0000001", QueryCleaner.clean(badQuery));
	}

}
