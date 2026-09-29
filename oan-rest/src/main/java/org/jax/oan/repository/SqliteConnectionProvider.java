package org.jax.oan.repository;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Singleton
public class SqliteConnectionProvider {
	private final Connection connection;

	public SqliteConnectionProvider(@Value("${sqlite.path}") String path) {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + Path.of(path).toAbsolutePath());
		} catch (SQLException e) {
			throw new RuntimeException("Could not open SQLite database at " + path, e);
		}
	}

	public Connection connection() {
		return connection;
	}
}
