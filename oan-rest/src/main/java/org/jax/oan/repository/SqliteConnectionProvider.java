package org.jax.oan.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides pooled, read-only connections to the SQLite database at {@code sqlite.path}.
 *
 * <p>The pool is created on the first call to {@link #connection()}.
 */
@Singleton
public class SqliteConnectionProvider {
	private final String jdbcUrl;
	private final int poolSize;
	private volatile HikariDataSource dataSource;

	/**
	 * @param path     path to the SQLite database file
	 * @param poolSize maximum number of open connections in the pool
	 */
	public SqliteConnectionProvider(@Value("${sqlite.path}") String path,
									@Value("${sqlite.pool-size:8}") int poolSize) {
		this.jdbcUrl = "jdbc:sqlite:" + Path.of(path).toAbsolutePath();
		this.poolSize = poolSize;
	}

	/**
	 * Borrows a read-only connection from the pool. Callers must close it to return it to the pool.
	 *
	 * @return an open read-only connection
	 * @throws SQLException if the database cannot be opened or no connection becomes available
	 */
	public Connection connection() throws SQLException {
		HikariDataSource ds = dataSource;
		if (ds == null) {
			synchronized (this) {
				ds = dataSource;
				if (ds == null) {
					ds = createDataSource();
					dataSource = ds;
				}
			}
		}
		return ds.getConnection();
	}

	/**
	 * Closes the pool and all of its connections.
	 */
	@PreDestroy
	public void close() {
		HikariDataSource ds = dataSource;
		if (ds != null) {
			ds.close();
		}
	}

	private HikariDataSource createDataSource() {
		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.setReadOnly(true);

		HikariConfig config = new HikariConfig();
		config.setPoolName("sqlite");
		config.setJdbcUrl(jdbcUrl);
		config.setDataSourceProperties(sqliteConfig.toProperties());
		config.setReadOnly(true);
		config.setMaximumPoolSize(poolSize);
		return new HikariDataSource(config);
	}
}
