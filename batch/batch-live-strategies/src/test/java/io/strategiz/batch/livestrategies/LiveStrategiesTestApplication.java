package io.strategiz.batch.livestrategies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Test application for live strategies batch integration tests.
 *
 * Bootstraps a Spring context with:
 * - Firestore repositories for AlertDeployment, BotDeployment, Strategy
 * - TimescaleDB repository for market data
 * - gRPC client for strategy execution
 * - Pub/Sub publisher for message dispatch
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
	"io.strategiz.batch.livestrategies",
	"io.strategiz.business.livestrategies",
	"io.strategiz.business.marketdata",
	"io.strategiz.data.strategy",
	"io.strategiz.data.marketdata",
	"io.strategiz.client.execution",
	"io.strategiz.service.base.config"
})
public class LiveStrategiesTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiveStrategiesTestApplication.class, args);
	}

}
