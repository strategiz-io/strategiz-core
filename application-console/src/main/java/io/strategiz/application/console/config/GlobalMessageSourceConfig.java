package io.strategiz.application.console.config;

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
				"messages/service-auth-errors");

		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setUseCodeAsDefaultMessage(false);
		messageSource.setFallbackToSystemLocale(false);

		return messageSource;
	}

}
