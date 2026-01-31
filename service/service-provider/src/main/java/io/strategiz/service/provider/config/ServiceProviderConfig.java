package io.strategiz.service.provider.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Service Provider Configuration
 *
 * Configures MessageSource to load error messages from provider-errors.properties for use
 * by the ErrorMessageService.
 */
@Configuration
public class ServiceProviderConfig {

	/**
	 * Configure MessageSource to load provider error messages NOTE: Commented out - using
	 * GlobalMessageSourceConfig which includes all module messages
	 */
	// @Bean
	// public MessageSource messageSource() {
	// ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
	// messageSource.setBasenames("messages/service-provider-errors");
	// messageSource.setDefaultEncoding("UTF-8");
	// messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not
	// found
	// return messageSource;
	// }

}