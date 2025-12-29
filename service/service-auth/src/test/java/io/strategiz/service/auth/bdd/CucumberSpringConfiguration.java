package io.strategiz.service.auth.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import io.strategiz.business.tokenauth.PasetoTokenValidator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring configuration for Cucumber BDD tests.
 *
 * This class integrates Spring Boot with Cucumber, allowing step definitions
 * to use @Autowired and other Spring features.
 *
 * Mocks:
 * - PasetoTokenValidator: Prevents Vault initialization during tests
 *
 * Test Properties:
 * - spring.profiles.active=test: Uses test profile
 * - strategiz.vault.enabled=false: Disables Vault integration
 * - spring.cloud.vault.enabled=false: Disables Spring Cloud Vault
 */
@CucumberContextConfiguration
@SpringBootTest(classes = AuthBDDTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "strategiz.vault.enabled=false",
    "spring.cloud.vault.enabled=false"
})
public class CucumberSpringConfiguration {

    /**
     * Mock PasetoTokenValidator to avoid Vault dependency in tests.
     *
     * PasetoTokenValidator.init() requires Vault keys which aren't available
     * in test context. Mocking prevents bean initialization failure.
     */
    @MockBean
    private PasetoTokenValidator pasetoTokenValidator;
}
