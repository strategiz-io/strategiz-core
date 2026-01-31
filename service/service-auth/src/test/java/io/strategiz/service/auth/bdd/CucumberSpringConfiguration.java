package io.strategiz.service.auth.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import io.strategiz.framework.authorization.validator.PasetoTokenValidator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring configuration for Cucumber BDD tests.
 *
 * This class integrates Spring Boot with Cucumber, allowing step definitions to use
 * Spring features if needed.
 *
 * Test Properties: - spring.profiles.active=test: Uses test profile -
 * strategiz.vault.enabled=false: Disables Vault integration -
 * spring.cloud.vault.enabled=false: Disables Spring Cloud Vault
 *
 * Mock Beans: - PasetoTokenValidator: Mocked to prevent Vault initialization errors (the
 * real bean's @PostConstruct init() method requires Vault credentials)
 */
@CucumberContextConfiguration
@SpringBootTest(classes = AuthBDDTestConfiguration.class)
@TestPropertySource(properties = { "spring.profiles.active=test", "strategiz.vault.enabled=false",
		"spring.cloud.vault.enabled=false" })
public class CucumberSpringConfiguration {

	/**
	 * Mock PasetoTokenValidator to prevent bean initialization failure.
	 *
	 * The real PasetoTokenValidator has an @PostConstruct init() method that requires
	 * Vault keys to be loaded. By mocking it with @MockBean, we prevent Spring from
	 * attempting to initialize it and avoid the StrategizException: CONFIGURATION_ERROR
	 * that would occur.
	 */
	@MockBean
	private PasetoTokenValidator pasetoTokenValidator;

}
