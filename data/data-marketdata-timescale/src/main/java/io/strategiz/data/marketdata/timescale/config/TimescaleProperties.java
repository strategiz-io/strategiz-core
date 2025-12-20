package io.strategiz.data.marketdata.timescale.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for TimescaleDB connection. Loaded from
 * application.properties with prefix 'strategiz.timescale'
 */
@ConfigurationProperties(prefix = "strategiz.timescale")
public class TimescaleProperties {

	private boolean enabled = false;

	private String jdbcUrl;

	private String username;

	private String password;

	private HikariSettings hikari = new HikariSettings();

	public static class HikariSettings {

		private int minimumIdle = 2;

		private int maximumPoolSize = 10;

		private long connectionTimeoutMs = 10000;

		private long idleTimeoutMs = 300000;

		private long maxLifetimeMs = 1800000;

		private long leakDetectionThresholdMs = 60000;

		private long keepaliveTimeMs = 30000;

		public int getMinimumIdle() {
			return minimumIdle;
		}

		public void setMinimumIdle(int minimumIdle) {
			this.minimumIdle = minimumIdle;
		}

		public int getMaximumPoolSize() {
			return maximumPoolSize;
		}

		public void setMaximumPoolSize(int maximumPoolSize) {
			this.maximumPoolSize = maximumPoolSize;
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

		public long getMaxLifetimeMs() {
			return maxLifetimeMs;
		}

		public void setMaxLifetimeMs(long maxLifetimeMs) {
			this.maxLifetimeMs = maxLifetimeMs;
		}

		public long getLeakDetectionThresholdMs() {
			return leakDetectionThresholdMs;
		}

		public void setLeakDetectionThresholdMs(long leakDetectionThresholdMs) {
			this.leakDetectionThresholdMs = leakDetectionThresholdMs;
		}

		public long getKeepaliveTimeMs() {
			return keepaliveTimeMs;
		}

		public void setKeepaliveTimeMs(long keepaliveTimeMs) {
			this.keepaliveTimeMs = keepaliveTimeMs;
		}

	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
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

	public HikariSettings getHikari() {
		return hikari;
	}

	public void setHikari(HikariSettings hikari) {
		this.hikari = hikari;
	}

}
