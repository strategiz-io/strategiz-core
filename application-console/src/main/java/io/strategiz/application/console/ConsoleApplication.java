package io.strategiz.application.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Console Application - Admin REST API and Batch Jobs service.
 *
 * Serves admin-only endpoints at /v1/console/* and runs batch jobs. Separate deployment
 * from main API for independent scaling and updates. Batch jobs can run without being
 * killed by main API deployments.
 *
 * Endpoints served: - /v1/console/jobs - Job management - /v1/console/costs -
 * Infrastructure costs - /v1/console/users - User management - /v1/console/providers -
 * Provider status - /v1/console/observability - System metrics - /v1/console/quality -
 * Code quality metrics - /v1/marketdata/admin/* - Market data batch operations
 */
@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
		org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class })
@EnableCaching
@EnableScheduling
@ComponentScan(basePackages = {
		// Console Application
		"io.strategiz.application.console",

		// Console Admin Services
		"io.strategiz.service.console",

		// Market Data Service (for batch controller)
		"io.strategiz.service.marketdata",

		// Batch Jobs
		"io.strategiz.batch.marketdata", "io.strategiz.batch.livestrategies", "io.strategiz.batch.fundamentals",

		// Business Layer
		"io.strategiz.business.tokenauth", "io.strategiz.business.marketdata",
		"io.strategiz.business.infrastructurecosts",
		"io.strategiz.business.livestrategies", "io.strategiz.business.fundamentals",
		"io.strategiz.business.cryptotoken", "io.strategiz.business.risk", "io.strategiz.business.preferences",

		// Data Layer
		"io.strategiz.data.base", "io.strategiz.data.auth", "io.strategiz.data.provider", "io.strategiz.data.session",
		"io.strategiz.data.user",
		"io.strategiz.data.marketdata", "io.strategiz.data.symbol", "io.strategiz.data.strategy",
		"io.strategiz.data.fundamentals", "io.strategiz.data.infrastructurecosts", "io.strategiz.data.featureflags",
		"io.strategiz.data.preferences", "io.strategiz.data.cryptotoken", "io.strategiz.data.device",
		"io.strategiz.data.testing", "io.strategiz.data.watchlist",

		// Framework
		"io.strategiz.framework.authorization", "io.strategiz.framework.token", "io.strategiz.framework.firebase",
		"io.strategiz.framework.secrets", "io.strategiz.framework.exception",

		// Clients
		"io.strategiz.client.firebase", "io.strategiz.client.vault", "io.strategiz.client.alpaca",
		"io.strategiz.client.gcpbilling", "io.strategiz.client.sendgridbilling", "io.strategiz.client.github",
		"io.strategiz.client.execution", "io.strategiz.client.kraken", "io.strategiz.client.binanceus",
		"io.strategiz.client.coinbase", "io.strategiz.client.coingecko",

		// Service Framework Base
		"io.strategiz.service.base" })
public class ConsoleApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConsoleApplication.class, args);
	}

}
