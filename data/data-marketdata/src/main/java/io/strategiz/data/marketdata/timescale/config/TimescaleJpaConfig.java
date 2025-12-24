package io.strategiz.data.marketdata.timescale.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA configuration for TimescaleDB market data.
 * Creates datasource, entity manager, and transaction manager
 * for market data, isolated from the Firestore configuration.
 */
@Configuration
@ConditionalOnProperty(name = "strategiz.timescale.enabled", havingValue = "true")
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "io.strategiz.data.marketdata.timescale.repository",
    entityManagerFactoryRef = "timescaleEntityManagerFactory",
    transactionManagerRef = "timescaleTransactionManager"
)
public class TimescaleJpaConfig {

    private static final Logger log = LoggerFactory.getLogger(TimescaleJpaConfig.class);

    private final TimescaleProperties properties;
    private final SecretManager secretManager;

    public TimescaleJpaConfig(TimescaleProperties properties, SecretManager secretManager) {
        this.properties = properties;
        this.secretManager = secretManager;
    }

    @Bean(name = "timescaleDataSource")
    public DataSource timescaleDataSource() {
        log.info("Configuring TimescaleDB DataSource");

        HikariConfig config = new HikariConfig();

        // Load credentials from Vault
        String jdbcUrl = loadSecret("timescale.jdbc-url", properties.getJdbcUrl());
        String username = loadSecret("timescale.username", properties.getUsername());
        String password = loadSecret("timescale.password", properties.getPassword());

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // Connection pool settings optimized for Cloud Run
        TimescaleProperties.HikariSettings hikari = properties.getHikari();
        config.setMinimumIdle(hikari.getMinimumIdle());
        config.setMaximumPoolSize(hikari.getMaximumPoolSize());
        config.setConnectionTimeout(hikari.getConnectionTimeoutMs());
        config.setIdleTimeout(hikari.getIdleTimeoutMs());
        config.setMaxLifetime(hikari.getMaxLifetimeMs());
        config.setLeakDetectionThreshold(hikari.getLeakDetectionThresholdMs());
        config.setKeepaliveTime(hikari.getKeepaliveTimeMs());

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // Pool identification
        config.setPoolName("TimescaleHikariPool");
        config.setRegisterMbeans(true);

        // PostgreSQL-specific optimizations
        config.addDataSourceProperty("preparedStatementCacheQueries", "256");
        config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
        config.addDataSourceProperty("reWriteBatchedInserts", "true");

        log.info("TimescaleDB DataSource configured with pool size {}-{}",
            hikari.getMinimumIdle(), hikari.getMaximumPoolSize());

        return new HikariDataSource(config);
    }

    @Bean(name = "timescaleEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean timescaleEntityManagerFactory(
            @Qualifier("timescaleDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("io.strategiz.data.marketdata.timescale.entity");
        em.setPersistenceUnitName("timescale");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(false);
        em.setJpaVendorAdapter(vendorAdapter);

        em.setJpaPropertyMap(hibernateProperties());

        return em;
    }

    @Bean(name = "timescaleTransactionManager")
    public PlatformTransactionManager timescaleTransactionManager(
            @Qualifier("timescaleEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    private Map<String, Object> hibernateProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.hbm2ddl.auto", "validate");
        props.put("hibernate.show_sql", false);
        props.put("hibernate.format_sql", false);
        props.put("hibernate.jdbc.batch_size", 50);
        props.put("hibernate.order_inserts", true);
        props.put("hibernate.order_updates", true);
        return props;
    }

    /**
     * Load secret from Vault with fallback to properties.
     */
    private String loadSecret(String secretKey, String fallback) {
        try {
            String value = secretManager.readSecret(secretKey);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        } catch (Exception e) {
            log.warn("Failed to load secret '{}' from Vault, using fallback", secretKey);
        }
        return fallback;
    }
}
