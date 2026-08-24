package org.jax.oan.ontology;

import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SqliteWriter implements AutoCloseable {
	private final Connection connection;

	public SqliteWriter(Path databasePath) {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			throw new OntologyAnnotationNetworkRuntimeException("Could not open SQLite database at " + databasePath, e);
		}
	}

	public void execute(String sql) {
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
			connection.commit();
		} catch (SQLException e) {
			throw new OntologyAnnotationNetworkRuntimeException("Failed executing: " + sql, e);
		}
	}

	public void batchInsert(String sql, List<Object[]> rows) {
		if (rows.isEmpty()) {
			return;
		}
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (Object[] row : rows) {
				for (int i = 0; i < row.length; i++) {
					statement.setObject(i + 1, row[i]);
				}
				statement.addBatch();
			}
			statement.executeBatch();
			connection.commit();
		} catch (SQLException e) {
			throw new OntologyAnnotationNetworkRuntimeException("Failed batch insert: " + sql, e);
		}
	}

	public Connection connection() {
		return connection;
	}

	@Override
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new OntologyAnnotationNetworkRuntimeException("Failed closing SQLite connection", e);
		}
	}
}
