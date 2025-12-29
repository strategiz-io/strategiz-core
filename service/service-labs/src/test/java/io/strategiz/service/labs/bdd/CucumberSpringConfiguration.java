package io.strategiz.service.labs.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import io.strategiz.framework.authorization.validator.PasetoTokenValidator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring configuration for Cucumber BDD tests.
 *
 * This class enables Spring dependency injection in Cucumber step definitions.
 * The @CucumberContextConfiguration annotation tells Cucumber to use this class
 * for Spring context setup.
 *
 * Uses BDDTestConfiguration which provides mocked AI services for testing.
 * Uses @MockBean to mock PasetoTokenValidator and avoid Vault dependency.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = BDDTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "strategiz.vault.enabled=false",
    "spring.cloud.vault.enabled=false"
})
public class CucumberSpringConfiguration {

    /**
     * Mock PasetoTokenValidator to bypass Vault initialization in tests.
     * Using @MockBean automatically replaces the real bean with a Mockito mock.
     */
    @MockBean
    private PasetoTokenValidator pasetoTokenValidator;
}
