package io.strategiz.data.marketdata.clickhouse.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.strategiz.framework.secrets.controller.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * ClickHouse database configuration. Creates DataSource and JdbcTemplate beans for
 * ClickHouse Cloud access. Loads credentials from Vault.
 */
@Configuration
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
@EnableConfigurationProperties(ClickHouseProperties.class)
public class ClickHouseConfig {

	private static final Logger log = LoggerFactory.getLogger(ClickHouseConfig.class);

	private final ClickHouseProperties properties;

	private final SecretManager secretManager;

	public ClickHouseConfig(ClickHouseProperties properties, SecretManager secretManager) {
		this.properties = properties;
		this.secretManager = secretManager;
	}

	@Bean(name = "clickHouseDataSource")
	@Primary
	public DataSource clickHouseDataSource() {
		log.info("Configuring ClickHouse DataSource for host: {}", properties.getHost());

		HikariConfig config = new HikariConfig();

		// Load credentials from Vault with fallback to properties
		String host = loadSecret("clickhouse.host", properties.getHost());
		String port = loadSecret("clickhouse.port", String.valueOf(properties.getPort()));
		String database = loadSecret("clickhouse.database", properties.getDatabase());
		String username = loadSecret("clickhouse.username", properties.getUsername());
		String password = loadSecret("clickhouse.password", properties.getPassword());

		// Build JDBC URL with explicit UTC timezone to prevent conversion issues
		String jdbcUrl = String.format("jdbc:clickhouse://%s:%s/%s?ssl=true&use_server_time_zone=false&use_time_zone=UTC", host, port, database);

		config.setJdbcUrl(jdbcUrl);
		config.setUsername(username);
		config.setPassword(password);
		config.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");

		// Connection pool settings
		ClickHouseProperties.ConnectionPoolSettings pool = properties.getPool();
		config.setMinimumIdle(pool.getMinIdle());
		config.setMaximumPoolSize(pool.getMaxPoolSize());
		config.setConnectionTimeout(pool.getConnectionTimeoutMs());
		config.setIdleTimeout(pool.getIdleTimeoutMs());

		// Connection lifecycle settings (prevent thread starvation)
		config.setMaxLifetime(1800000); // 30 minutes - recycle connections
		config.setKeepaliveTime(60000); // 1 minute - send keepalive pings
		config.setLeakDetectionThreshold(180000); // 3 minutes - detect connection leaks

		// Connection validation
		config.setConnectionTestQuery("SELECT 1");
		// Increased to 60s to match connection timeout (was 5s)
		config.setValidationTimeout(60000);

		// Pool identification
		config.setPoolName("ClickHouseHikariPool");
		config.setRegisterMbeans(true);

		log.info("HikariCP configured with keepalive (60s), maxLifetime (30m), leak detection (3m)");

		log.info("ClickHouse DataSource configured: {}", jdbcUrl.replaceAll("password=[^&]*", "password=***"));

		return new HikariDataSource(config);
	}

	@Bean(name = "clickHouseJdbcTemplate")
	@Primary
	public JdbcTemplate clickHouseJdbcTemplate(@Qualifier("clickHouseDataSource") DataSource dataSource) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.setQueryTimeout(60); // 60 second query timeout
		return jdbcTemplate;
	}

	/**
	 * Load secret from Vault with fallback to properties.
	 */
	private String loadSecret(String secretKey, String fallback) {
		try {
			log.debug("Loading secret '{}' from Vault", secretKey);
			String value = secretManager.readSecret(secretKey);
			if (value != null && !value.isEmpty()) {
				log.debug("Secret '{}' loaded successfully from Vault", secretKey);
				return value;
			}
			log.debug("Secret '{}' returned null/empty from Vault, using fallback", secretKey);
		}
		catch (Exception e) {
			log.warn("Failed to load secret '{}' from Vault: {}", secretKey, e.getMessage());
		}
		return fallback;
	}

}
