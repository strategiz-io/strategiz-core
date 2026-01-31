package io.strategiz.application.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Global MessageSource configuration that aggregates all module message sources. This
 * ensures that error messages from all modules are properly resolved.
 */
@Configuration
public class GlobalMessageSourceConfig {

	@Bean
	@Primary
	public MessageSource messageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();

		// Add all module message bundles
		messageSource.setBasenames("messages/service-base-errors", "messages/data-base-errors",
				"messages/data-strategy-errors", "messages/service-provider-errors", "messages/service-auth-errors",
				"messages/service-strategy-errors", "messages/service-dashboard-errors",
				"messages/service-portfolio-errors", "messages/service-exchange-errors",
				"messages/service-profile-errors", "messages/service-marketplace-errors",
				"messages/service-monitoring-errors", "messages/service-marketing-errors",
				"messages/service-my-strategies-errors");

		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setUseCodeAsDefaultMessage(false); // Show actual error if key not
															// found
		messageSource.setFallbackToSystemLocale(false); // Don't fall back to system
														// locale

		return messageSource;
	}

}