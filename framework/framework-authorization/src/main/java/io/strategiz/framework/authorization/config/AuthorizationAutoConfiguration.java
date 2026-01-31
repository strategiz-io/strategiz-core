package io.strategiz.framework.authorization.config;

import io.strategiz.framework.authorization.filter.PasetoAuthenticationFilter;
import io.strategiz.framework.authorization.resolver.AuthUserArgumentResolver;
import io.strategiz.framework.authorization.validator.PasetoTokenValidator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for the authorization framework.
 *
 * <p>
 * This configuration:
 *
 * <ul>
 * <li>Creates {@link PasetoTokenValidator} for ALL token validation (single source of
 * truth)
 * <li>Registers the {@link PasetoAuthenticationFilter} for token extraction
 * <li>Registers the {@link AuthUserArgumentResolver} for @AuthUser injection
 * <li>Enables component scanning for aspects and FGA client
 * </ul>
 *
 * <p>
 * Token keys are loaded from Vault by {@link PasetoTokenValidator} at startup.
 *
 * <p>
 * To use this framework, add the following to your application.properties:
 *
 * <pre>
 * strategiz.authorization.enabled=true
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthorizationProperties.class)
@ConditionalOnProperty(prefix = "strategiz.authorization", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@ComponentScan(basePackages = { "io.strategiz.framework.authorization.aspect",
		"io.strategiz.framework.authorization.fga", "io.strategiz.framework.authorization.validator" })
public class AuthorizationAutoConfiguration implements WebMvcConfigurer {

	private static final Logger log = LoggerFactory.getLogger(AuthorizationAutoConfiguration.class);

	private final AuthorizationProperties properties;

	/**
	 * Creates a new AuthorizationAutoConfiguration.
	 * @param properties the authorization properties
	 */
	public AuthorizationAutoConfiguration(AuthorizationProperties properties) {
		this.properties = properties;
		log.info("Authorization framework auto-configuration enabled");
	}

	/**
	 * Creates the PasetoTokenValidator bean - the single source of truth for ALL token
	 * validation. Keys are loaded from Vault automatically.
	 * @return the PasetoTokenValidator bean
	 */
	@Bean
	@ConditionalOnMissingBean
	public PasetoTokenValidator pasetoTokenValidator() {
		log.info("Creating PasetoTokenValidator bean - single source of truth for token validation");
		return new PasetoTokenValidator();
	}

	/**
	 * Creates the PasetoAuthenticationFilter bean using the validator for token
	 * validation.
	 * @param tokenValidator the token validator
	 * @return the filter registration bean
	 */
	@Bean
	public FilterRegistrationBean<PasetoAuthenticationFilter> pasetoAuthenticationFilter(
			PasetoTokenValidator tokenValidator) {

		PasetoAuthenticationFilter filter = new PasetoAuthenticationFilter(tokenValidator, properties);

		FilterRegistrationBean<PasetoAuthenticationFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(filter);
		registration.addUrlPatterns("/*");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10); // After CORS
		registration.setName("pasetoAuthenticationFilter");

		log.info("PasetoAuthenticationFilter registered with PasetoTokenValidator, skip-paths: {}",
				properties.getSkipPaths());
		return registration;
	}

	/**
	 * Creates the AuthUserArgumentResolver bean.
	 * @return the AuthUserArgumentResolver bean
	 */
	@Bean
	public AuthUserArgumentResolver authUserArgumentResolver() {
		return new AuthUserArgumentResolver();
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(authUserArgumentResolver());
	}

}
