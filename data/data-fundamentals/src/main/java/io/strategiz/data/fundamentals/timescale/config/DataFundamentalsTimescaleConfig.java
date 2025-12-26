package io.strategiz.data.fundamentals.timescale.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for company fundamentals data module with TimescaleDB.
 *
 * This configuration:
 * - Enables JPA repositories for fundamentals data
 * - Reuses the existing TimescaleDB datasource from data-marketdata module
 * - Shares the timescaleEntityManagerFactory and timescaleTransactionManager
 * - Only activates when TimescaleDB is enabled via property
 *
 * The fundamentals data is stored in the same TimescaleDB instance as market data,
 * but in a separate table (company_fundamentals) with its own hypertable partitioning.
 *
 * Configuration properties required:
 * - strategiz.timescale.enabled=true (to activate this config)
 *
 * Dependencies:
 * - data-marketdata module (provides TimescaleDB datasource configuration)
 */
@Configuration
@ConditionalOnProperty(name = "strategiz.timescale.enabled", havingValue = "true")
@EnableJpaRepositories(
    basePackages = "io.strategiz.data.fundamentals.timescale.repository",
    entityManagerFactoryRef = "timescaleEntityManagerFactory",
    transactionManagerRef = "timescaleTransactionManager"
)
@ComponentScan(basePackages = "io.strategiz.data.fundamentals")
public class DataFundamentalsTimescaleConfig {

    /**
     * No additional beans needed - we reuse the TimescaleDB infrastructure
     * from the data-marketdata module:
     *
     * - timescaleDataSource (provided by data-marketdata)
     * - timescaleEntityManagerFactory (provided by data-marketdata)
     * - timescaleTransactionManager (provided by data-marketdata)
     *
     * The entity manager factory scans both:
     * - io.strategiz.data.marketdata.timescale.entity (market data entities)
     * - io.strategiz.data.fundamentals.timescale.entity (fundamentals entities)
     */

    // Additional configuration can be added here if needed in the future
}
