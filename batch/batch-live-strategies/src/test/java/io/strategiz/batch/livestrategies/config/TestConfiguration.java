package io.strategiz.batch.livestrategies.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for integration tests.
 *
 * Integration tests run against REAL infrastructure:
 * - Firestore (requires GCP Application Default Credentials)
 * - TimescaleDB (via Vault secrets)
 * - gRPC Execution Service (localhost:50051)
 * - Pub/Sub (requires GCP credentials)
 *
 * Prerequisites:
 * 1. Run: gcloud auth application-default login
 * 2. Run: vault server -dev
 * 3. Export VAULT_TOKEN=root
 *
 * NO MOCKS - these are real integration tests.
 */
@Configuration
@Profile("integration")
public class TestConfiguration {

	private static final Logger log = LoggerFactory.getLogger(TestConfiguration.class);

	public TestConfiguration() {
		log.info("============================================================");
		log.info("INTEGRATION TEST MODE - Using REAL infrastructure");
		log.info("Prerequisites:");
		log.info("  1. gcloud auth application-default login");
		log.info("  2. vault server -dev && export VAULT_TOKEN=root");
		log.info("  3. gRPC execution service on localhost:50051 (optional)");
		log.info("============================================================");
	}

}
