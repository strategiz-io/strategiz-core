package io.strategiz.data.marketdata.timescale.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Health indicator for TimescaleDB connection.
 * Reports database connectivity and version.
 */
@Component("timescaleHealth")
@ConditionalOnProperty(name = "strategiz.timescale.enabled", havingValue = "true")
public class TimescaleHealthIndicator implements HealthIndicator {

	private static final Logger log = LoggerFactory.getLogger(TimescaleHealthIndicator.class);

	private final DataSource dataSource;

	public TimescaleHealthIndicator(@Qualifier("timescaleDataSource") DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Health health() {
		Health.Builder builder = new Health.Builder();

		try {
			long startTime = System.currentTimeMillis();
			String timescaleVersion = checkDatabaseConnection();
			long latency = System.currentTimeMillis() - startTime;

			builder.withDetail("latency_ms", latency).withDetail("timescaledb_version", timescaleVersion);

			if (latency > 1000) {
				return builder.status("DEGRADED").withDetail("warning", "High database latency").build();
			}

			return builder.up().build();

		}
		catch (Exception e) {
			log.error("TimescaleDB health check failed", e);
			return builder.down(e).withDetail("error", e.getMessage()).build();
		}
	}

	private String checkDatabaseConnection() throws Exception {
		try (Connection conn = dataSource.getConnection()) {
			try (PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
				ps.setQueryTimeout(5);
				ps.execute();
			}

			try (PreparedStatement ps = conn
				.prepareStatement("SELECT extversion FROM pg_extension WHERE extname = 'timescaledb'")) {
				ps.setQueryTimeout(5);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getString("extversion");
					}
					return "not installed";
				}
			}
		}
	}

}
