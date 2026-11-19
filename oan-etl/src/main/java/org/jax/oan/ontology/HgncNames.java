package org.jax.oan.ontology;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the alternate names of genes from the HGNC complete set.
 */
final class HgncNames {
	private static final List<String> NAME_FIELDS = List.of("name", "alias_symbol", "alias_name", "prev_symbol", "prev_name");
	private static final String ENTREZ_ID = "entrez_id";

	private HgncNames() {
	}

	/**
	 * Reads the full name, alias symbols and names, and previous symbols and names of every gene.
	 *
	 * <p>Rows without an Entrez id are skipped; when an Entrez id appears more than once, the first row wins.
	 *
	 * @param hgncCompleteSet the tab-separated HGNC complete set, with a header row.
	 * @return the alternate names keyed by Entrez id digits (such as {@code 4905}), in the order
	 * {@code name, alias_symbol, alias_name, prev_symbol, prev_name}, with {@code |}-separated values
	 * split, surrounding quotes removed, and blanks dropped.
	 * @throws IOException if the file cannot be read.
	 * @throws IllegalArgumentException if the header lacks {@code entrez_id} or one of the name columns.
	 */
	static Map<String, List<String>> read(Path hgncCompleteSet) throws IOException {
		List<String> lines = Files.readAllLines(hgncCompleteSet);
		Map<String, List<String>> names = new LinkedHashMap<>();
		if (lines.isEmpty()) {
			return names;
		}
		List<String> header = Arrays.asList(lines.get(0).split("\t", -1));
		int entrezColumn = column(header, ENTREZ_ID);
		int[] nameColumns = NAME_FIELDS.stream().mapToInt(field -> column(header, field)).toArray();
		for (String line : lines.subList(1, lines.size())) {
			String[] fields = line.split("\t", -1);
			String entrezId = fields.length > entrezColumn ? unquote(fields[entrezColumn]) : "";
			if (entrezId.isEmpty() || names.containsKey(entrezId)) {
				continue;
			}
			List<String> geneNames = new ArrayList<>();
			for (int column : nameColumns) {
				if (column < fields.length) {
					for (String value : unquote(fields[column]).split("\\|")) {
						String name = value.strip();
						if (!name.isEmpty()) {
							geneNames.add(name);
						}
					}
				}
			}
			names.put(entrezId, geneNames);
		}
		return names;
	}

	private static int column(List<String> header, String field) {
		int index = header.indexOf(field);
		if (index < 0) {
			throw new IllegalArgumentException("HGNC complete set has no '" + field + "' column");
		}
		return index;
	}

	private static String unquote(String value) {
		String stripped = value.strip();
		if (stripped.length() >= 2 && stripped.startsWith("\"") && stripped.endsWith("\"")) {
			return stripped.substring(1, stripped.length() - 1);
		}
		return stripped;
	}
}
