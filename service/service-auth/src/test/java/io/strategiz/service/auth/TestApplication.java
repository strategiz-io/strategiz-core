package io.strategiz.service.auth;

import io.strategiz.service.auth.config.OAuthVaultConfig;
import io.strategiz.service.auth.config.TestOAuthConfig;
import io.strategiz.service.auth.controller.AuthTokenController;
import io.strategiz.service.auth.service.AuthTokenService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Test Application for service-auth integration tests.
 *
 * <p>
 * This minimal Spring Boot application is used for testing service-auth controllers
 * without requiring the full application-api setup.
 * </p>
 *
 * <h3>Exclusions:</h3>
 * <ul>
 * <li>DataSource: No database required for controller tests</li>
 * <li>OAuthVaultConfig: Replaced with TestOAuthConfig to avoid Vault dependency</li>
 * <li>AuthTokenController/Service: Requires repository dependencies</li>
 * </ul>
 *
 * <h3>Usage:</h3> <pre>{@code
 * &#64;SpringBootTest(classes = TestApplication.class)
 * &#64;ActiveProfiles("test")
 * public class MyIntegrationTest extends BaseIntegrationTest {
 *     // Tests
 * }
 * }</pre>
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@ComponentScan(
		basePackages = { "io.strategiz.service.auth", "io.strategiz.service.base", "io.strategiz.framework.secrets" },
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
				classes = { OAuthVaultConfig.class, AuthTokenController.class, AuthTokenService.class }))
@Import(TestOAuthConfig.class)
@TestConfiguration
public class TestApplication {

	// Test application entry point for integration tests

}
