package io.strategiz.batch.livestrategies;

import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import com.google.cloud.spring.autoconfigure.firestore.GcpFirestoreAutoConfiguration;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Test application for live strategies batch integration tests.
 *
 * Bootstraps a Spring context with: - Firestore repositories for AlertDeployment,
 * BotDeployment, Strategy - TimescaleDB repository for market data (via
 * TimescaleJpaConfig) - gRPC client for strategy execution - Pub/Sub publisher for
 * message dispatch
 *
 * Prerequisites (run before tests): 1. gcloud auth application-default login 2. vault
 * server -dev && export VAULT_TOKEN=root 3. Load secrets: ./vault/load-secrets.sh
 *
 * GCP auto-configuration is excluded - we use Firestore directly via FirebaseConfig which
 * has its own credential handling.
 *
 * JPA is configured via TimescaleJpaConfig (scanned from data.marketdata.timescale).
 */
@SpringBootApplication(exclude = { GcpContextAutoConfiguration.class, GcpFirestoreAutoConfiguration.class,
		GcpPubSubAutoConfiguration.class })
@EnableScheduling
@ComponentScan(basePackages = { "io.strategiz.batch.livestrategies", "io.strategiz.business.livestrategies",
		"io.strategiz.business.marketdata", "io.strategiz.data.strategy", "io.strategiz.data.marketdata",
		"io.strategiz.data.marketdata.timescale", "io.strategiz.data.symbol", "io.strategiz.data.base.config",
		"io.strategiz.client.execution", "io.strategiz.client.alpaca", "io.strategiz.framework.secrets" })
public class LiveStrategiesTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiveStrategiesTestApplication.class, args);
	}

}
