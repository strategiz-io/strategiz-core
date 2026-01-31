package io.strategiz.service.auth.bdd;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for Authentication BDD tests.
 *
 * This configuration provides a minimal Spring Boot context for Cucumber tests without
 * requiring actual authentication services or external dependencies.
 *
 * All test logic is implemented via internal state in AuthenticationSteps.java using a
 * simple TestUser POJO rather than actual service beans.
 *
 * Excludes: - Firebase/Firestore auto-configuration (no GCP credentials needed) -
 * DataSource/Hibernate auto-configuration (no database needed)
 *
 * This allows BDD tests to: - Run without external dependencies - Validate Gherkin
 * scenarios are correctly written - Document expected authentication behavior - Serve as
 * executable specifications
 *
 * NOTE: These are UNIT tests with mocked state - they verify authentication business
 * logic and flows. For INTEGRATION tests with real services, use a separate test suite
 * with proper credentials and infrastructure.
 */
@SpringBootApplication(exclude = { com.google.cloud.spring.autoconfigure.firestore.GcpFirestoreAutoConfiguration.class,
		com.google.cloud.spring.autoconfigure.firestore.FirestoreTransactionManagerAutoConfiguration.class,
		org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
		org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class })
public class AuthBDDTestConfiguration {

	/**
	 * Dummy bean to satisfy Spring Boot context requirements.
	 *
	 * Spring Boot requires at least one bean in the context. This bean does nothing but
	 * exists to allow the context to start.
	 */
	@Bean
	public String testContextMarker() {
		return "BDD Test Context";
	}

}
