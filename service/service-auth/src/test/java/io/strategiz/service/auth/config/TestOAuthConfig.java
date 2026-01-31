package io.strategiz.service.auth.config;

import io.strategiz.client.facebook.FacebookClient;
import io.strategiz.client.google.client.GoogleClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for OAuth providers.
 *
 * <p>
 * Provides mock OAuth credentials and clients for testing without connecting to Vault or
 * external OAuth providers. This configuration is loaded in place of OAuthVaultConfig for
 * integration tests.
 * </p>
 */
@TestConfiguration
public class TestOAuthConfig {

	/**
	 * Initialize test OAuth properties.
	 *
	 * <p>
	 * Adds mock OAuth credentials to the Spring environment so that OAuth provider
	 * services can be instantiated in tests.
	 * </p>
	 */
	@Bean
	@Primary
	public MapPropertySource testOAuthProperties(ConfigurableEnvironment environment) {
		Map<String, Object> testProperties = new HashMap<>();

		// Mock Google OAuth credentials
		testProperties.put("oauth.providers.google.client-id", "test-google-client-id");
		testProperties.put("oauth.providers.google.client-secret", "test-google-client-secret");
		testProperties.put("oauth.providers.google.redirect-uri", "http://localhost:8080/oauth/google/callback");

		// Mock Facebook OAuth credentials
		testProperties.put("oauth.providers.facebook.client-id", "test-facebook-client-id");
		testProperties.put("oauth.providers.facebook.client-secret", "test-facebook-client-secret");
		testProperties.put("oauth.providers.facebook.redirect-uri", "http://localhost:8080/oauth/facebook/callback");

		MapPropertySource propertySource = new MapPropertySource("test-oauth", testProperties);
		environment.getPropertySources().addFirst(propertySource);

		return propertySource;
	}

	/**
	 * Mock Google OAuth client for tests.
	 *
	 * <p>
	 * Returns a Mockito mock that can be configured in individual tests as needed.
	 * </p>
	 */
	@Bean
	@Primary
	public GoogleClient googleClient() {
		return mock(GoogleClient.class);
	}

	/**
	 * Mock Facebook OAuth client for tests.
	 *
	 * <p>
	 * Returns a Mockito mock that can be configured in individual tests as needed.
	 * </p>
	 */
	@Bean
	@Primary
	public FacebookClient facebookClient() {
		return mock(FacebookClient.class);
	}

}
