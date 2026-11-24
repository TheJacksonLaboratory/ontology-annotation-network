package org.jax.oan.ontology;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HgncNamesTest {

	private static final Path FIXTURE = Path.of("src/test/resources/hgnc_complete_set.txt");

	@Test
	void readsAlternateNamesKeyedByEntrezId() throws IOException {
		Map<String, List<String>> names = HgncNames.read(FIXTURE);
		assertEquals(Set.of("10058", "4905"), names.keySet());
		assertEquals(List.of(
				"N-ethylmaleimide sensitive factor, vesicle fusing ATPase",
				"SKD2", "SEC18",
				"N-ethylmaleimide-sensitive factor-like protein",
				"N-ethylmaleimide-sensitive factor", "N-ethylmaleimide sensitive factor"),
			names.get("4905"));
	}

	@Test
	void stripsQuotesAndSplitsOnPipe() throws IOException {
		List<String> abcb6 = HgncNames.read(FIXTURE).get("10058");
		assertTrue(abcb6.containsAll(List.of("EST45597", "umat", "MTABC3")));
		assertTrue(abcb6.stream().noneMatch(n -> n.contains("\"") || n.contains("|") || n.isBlank()));
	}
}
