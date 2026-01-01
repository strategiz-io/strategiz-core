package io.strategiz.service.marketplace;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test Application for service-marketplace integration tests.
 *
 * <p>This minimal Spring Boot application is used for testing service-marketplace
 * controllers without requiring the full application-api setup.</p>
 *
 * <h3>Exclusions:</h3>
 * <ul>
 *   <li>DataSource: No database required for controller tests</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * @SpringBootTest(classes = TestApplication.class)
 * @ActiveProfiles("test")
 * public class MyIntegrationTest extends BaseIntegrationTest {
 *     // Tests
 * }
 * }</pre>
 */
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    }
)
@ComponentScan(
    basePackages = {
        "io.strategiz.service.marketplace",
        "io.strategiz.service.base",
        "io.strategiz.framework",
        "io.strategiz.data.strategy"
    }
)
@TestConfiguration
public class TestApplication {
    // Test application entry point for integration tests
}
