package io.strategiz.data.framework.timescale.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.strategiz.framework.secrets.controller.SecretManager;

@Configuration
@ConditionalOnProperty(name = "strategiz.timescale.enabled", havingValue = "true")
public class TimescaleDataSourceConfig {

	@Autowired
	private SecretManager secretManager;

	@Value("${strategiz.timescale.hikari.minimum-idle:2}")
	private int minimumIdle;

	@Value("${strategiz.timescale.hikari.maximum-pool-size:10}")
	private int maximumPoolSize;

	@Value("${strategiz.timescale.hikari.connection-timeout-ms:10000}")
	private long connectionTimeout;

	@Value("${strategiz.timescale.hikari.idle-timeout-ms:300000}")
	private long idleTimeout;

	@Value("${strategiz.timescale.hikari.max-lifetime-ms:1800000}")
	private long maxLifetime;

	@Value("${strategiz.timescale.hikari.leak-detection-threshold:60000}")
	private long leakDetectionThreshold;

	@Bean(name = "timescaleDataSource")
	@Primary
	public DataSource timescaleDataSource() {
		// Load credentials from Vault
		String jdbcUrl = secretManager.readSecret("timescale/jdbc-url");
		String username = secretManager.readSecret("timescale/username");
		String password = secretManager.readSecret("timescale/password");

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(username);
		config.setPassword(password);
		config.setDriverClassName("org.postgresql.Driver");

		// Connection pool settings
		config.setMinimumIdle(minimumIdle);
		config.setMaximumPoolSize(maximumPoolSize);
		config.setConnectionTimeout(connectionTimeout);
		config.setIdleTimeout(idleTimeout);
		config.setMaxLifetime(maxLifetime);

		// Leak detection
		config.setLeakDetectionThreshold(leakDetectionThreshold);

		// Connection validation
		config.setConnectionTestQuery("SELECT 1");
		config.setValidationTimeout(5000);

		// Pool name for logging
		config.setPoolName("TimescaleHikariPool");

		return new HikariDataSource(config);
	}

}
