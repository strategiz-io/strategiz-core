package io.strategiz.data.marketdata.config;

import io.strategiz.data.marketdata.repository.MarketDataRepository;
import io.strategiz.data.marketdata.repository.NoOpMarketDataRepositoryImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for data-marketdata module.
 * Follows the standard naming convention: Data[Module]Config
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.data.marketdata")
public class DataMarketDataConfig {

	/**
	 * Provide no-op MarketDataRepository when neither ClickHouse nor TimescaleDB is enabled.
	 * This bean will only be created if no other MarketDataRepository bean exists.
	 */
	@Bean
	@ConditionalOnMissingBean(MarketDataRepository.class)
	public MarketDataRepository noOpMarketDataRepository() {
		return new NoOpMarketDataRepositoryImpl();
	}

	/**
	 * Configure MessageSource to load data marketdata error messages NOTE: Commented out - using
	 * GlobalMessageSourceConfig which includes all module messages
	 */
	// @Bean
	// public MessageSource messageSource() {
	// ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
	// messageSource.setBasenames("messages/data-marketdata-errors");
	// messageSource.setDefaultEncoding("UTF-8");
	// messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not found
	// return messageSource;
	// }

}
