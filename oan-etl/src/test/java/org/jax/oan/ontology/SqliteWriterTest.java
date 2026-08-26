package org.jax.oan.ontology;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteWriterTest {

	@Test
	void createsTablesAndIndexesAndBatchInserts(@TempDir Path tempDir) throws Exception {
		try (SqliteWriter writer = new SqliteWriter(tempDir.resolve("test.db"))) {
			SqliteSchema.createTables(writer);
			writer.batchInsert("INSERT INTO phenotype (id, name, category) VALUES (?, ?, ?)",
					List.of(new Object[]{"HP:0000001", "Root", "Other"},
							new Object[]{"HP:0000002", "Term Two", "Other"}));
			SqliteSchema.createIndexes(writer);

			try (Statement statement = writer.connection().createStatement()) {
				ResultSet rows = statement.executeQuery("SELECT COUNT(*) AS c FROM phenotype");
				rows.next();
				assertEquals(2, rows.getInt("c"));

				ResultSet indexes = statement.executeQuery(
						"SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'ix_disease_mondo'");
				assertTrue(indexes.next());
			}
		}
	}

	@Test
	void batchInsertNoOpsOnEmptyRows(@TempDir Path tempDir) {
		try (SqliteWriter writer = new SqliteWriter(tempDir.resolve("empty.db"))) {
			SqliteSchema.createTables(writer);
			writer.batchInsert("INSERT INTO phenotype (id, name, category) VALUES (?, ?, ?)", List.of());
		}
	}
}
