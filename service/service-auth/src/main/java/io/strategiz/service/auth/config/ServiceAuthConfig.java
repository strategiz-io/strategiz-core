package io.strategiz.service.auth.config;

import io.strategiz.data.auth.config.DataAuthConfig;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Configuration for service-auth module. Imports data module configurations to ensure
 * repositories and entities are available.
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.service.auth")
@Import({ DataAuthConfig.class })
public class ServiceAuthConfig {

	/**
	 * Configure MessageSource to load auth error messages NOTE: Commented out - using
	 * GlobalMessageSourceConfig which includes all module messages
	 */
	// @Bean
	// public MessageSource messageSource() {
	// ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
	// messageSource.setBasenames("messages/auth-errors");
	// messageSource.setDefaultEncoding("UTF-8");
	// messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not
	// found
	// return messageSource;
	// }

}
