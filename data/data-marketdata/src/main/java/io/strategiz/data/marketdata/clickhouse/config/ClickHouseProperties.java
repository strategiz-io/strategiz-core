package io.strategiz.data.marketdata.clickhouse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for ClickHouse connection. Loaded from
 * application.properties with prefix 'strategiz.clickhouse'
 */
@ConfigurationProperties(prefix = "strategiz.clickhouse")
public class ClickHouseProperties {

	private boolean enabled = false;

	private String host;

	private int port = 8443;

	private String database = "default";

	private String username = "default";

	private String password;

	private boolean ssl = true;

	private ConnectionPoolSettings pool = new ConnectionPoolSettings();

	public static class ConnectionPoolSettings {

		private int maxPoolSize = 10;

		private int minIdle = 2;

		private long connectionTimeoutMs = 10000;

		private long idleTimeoutMs = 300000;

		public int getMaxPoolSize() {
			return maxPoolSize;
		}

		public void setMaxPoolSize(int maxPoolSize) {
			this.maxPoolSize = maxPoolSize;
		}

		public int getMinIdle() {
			return minIdle;
		}

		public void setMinIdle(int minIdle) {
			this.minIdle = minIdle;
		}

		public long getConnectionTimeoutMs() {
			return connectionTimeoutMs;
		}

		public void setConnectionTimeoutMs(long connectionTimeoutMs) {
			this.connectionTimeoutMs = connectionTimeoutMs;
		}

		public long getIdleTimeoutMs() {
			return idleTimeoutMs;
		}

		public void setIdleTimeoutMs(long idleTimeoutMs) {
			this.idleTimeoutMs = idleTimeoutMs;
		}

	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public ConnectionPoolSettings getPool() {
		return pool;
	}

	public void setPool(ConnectionPoolSettings pool) {
		this.pool = pool;
	}

	/**
	 * Build JDBC URL for ClickHouse Cloud.
	 */
	public String getJdbcUrl() {
		String protocol = ssl ? "https" : "http";
		return String.format("jdbc:clickhouse://%s:%d/%s?ssl=%s", host, port, database, ssl);
	}

}
