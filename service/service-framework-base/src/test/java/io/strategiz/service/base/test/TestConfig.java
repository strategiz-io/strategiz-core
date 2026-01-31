package io.strategiz.service.base.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for integration tests.
 *
 * <p>
 * Provides test-specific bean configurations and overrides for integration testing
 * environment.
 * </p>
 *
 * <h3>Usage:</h3>
 * <p>
 * Import this configuration in your test classes:
 * </p>
 * <pre>{@code
 * &#64;SpringBootTest
 *

&#64;Import(TestConfig.class)
 * public class MyIntegrationTest extends BaseIntegrationTest {
 *     // Tests
 * }
 * }</pre>
 *
 * <h3>Features:</h3>
 * <ul>
 * <li>Test-specific bean overrides using @Primary</li>
 * <li>Mock external service clients</li>
 * <li>Test database configuration</li>
 * <li>Disabled security for easier testing (optional)</li>
 * </ul>
 */
@TestConfiguration
public class TestConfig {

	/**
	 * Configure test-specific beans here. Example: Mock external clients, override
	 * repositories, etc.
	 */

	// Example: Mock Firebase client for tests
	// @Bean
	// @Primary
	// public FirebaseApp testFirebaseApp() {
	// // Return mock or test Firebase app
	// return Mockito.mock(FirebaseApp.class);
	// }

	// Example: Mock Vault client for tests
	// @Bean
	// @Primary
	// public VaultTemplate testVaultTemplate() {
	// return Mockito.mock(VaultTemplate.class);
	// }

}
