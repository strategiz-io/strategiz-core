package io.strategiz.application.console;

import io.strategiz.batch.fundamentals.config.BatchFundamentalsConfig;
import io.strategiz.batch.livestrategies.config.BatchLiveStrategiesConfig;
import io.strategiz.batch.marketdata.config.BatchMarketDataConfig;
import io.strategiz.business.livestrategies.config.LiveStrategiesConfig;
import io.strategiz.business.portfolio.config.BusinessPortfolioConfig;
import io.strategiz.data.auth.config.DataAuthConfig;
import io.strategiz.data.device.config.DataDeviceConfig;
import io.strategiz.data.infrastructurecosts.config.DataInfrastructureCostsConfig;
import io.strategiz.data.portfolio.config.DataPortfolioConfig;
import io.strategiz.data.provider.config.DataProviderConfig;
import io.strategiz.data.strategy.config.DataStrategyConfig;
import io.strategiz.service.console.config.ServiceConsoleConfig;
import io.strategiz.service.marketdata.config.ServiceMarketDataConfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
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

		// Framework
		"io.strategiz.framework.authorization", "io.strategiz.framework.token", "io.strategiz.framework.firebase",
		"io.strategiz.framework.secrets", "io.strategiz.framework.exception",

		// Clients
		"io.strategiz.client.firebase", "io.strategiz.client.vault", "io.strategiz.client.alpaca",
		"io.strategiz.client.gcpbilling", "io.strategiz.client.sendgridbilling", "io.strategiz.client.github",
		"io.strategiz.client.execution", "io.strategiz.client.kraken", "io.strategiz.client.binanceus",
		"io.strategiz.client.coinbase", "io.strategiz.client.coingecko",

		// LLM Provider Clients (for AI chat and cost aggregation)
		"io.strategiz.client.gemini", "io.strategiz.client.claude", "io.strategiz.client.openai",
		"io.strategiz.client.llama", "io.strategiz.client.mistral", "io.strategiz.client.cohere",
		"io.strategiz.client.anthropic", "io.strategiz.client.grok",

		// Service Framework Base
		"io.strategiz.service.base",

		// Data Framework Base (FirebaseConfig + FirebaseVaultConfig)
		"io.strategiz.data.base" })
@Import({
		// Service layer configs (pull in their business + data dependencies)
		ServiceConsoleConfig.class, ServiceMarketDataConfig.class,

		// Batch job configs (pull in their business + data dependencies)
		BatchMarketDataConfig.class, BatchFundamentalsConfig.class, BatchLiveStrategiesConfig.class,

		// Additional business configs not pulled in by service/batch configs above
		LiveStrategiesConfig.class, BusinessPortfolioConfig.class,

		// Additional data configs not pulled in by service/batch configs above
		DataAuthConfig.class, DataDeviceConfig.class, DataProviderConfig.class,
		DataStrategyConfig.class, DataInfrastructureCostsConfig.class, DataPortfolioConfig.class })
public class ConsoleApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConsoleApplication.class, args);
	}

}
