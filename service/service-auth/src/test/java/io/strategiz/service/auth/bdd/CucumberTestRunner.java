package io.strategiz.service.auth.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Cucumber BDD Test Runner for Authentication.
 *
 * Executes all .feature files in src/test/resources/features/
 * using JUnit 5 Platform Suite integration.
 *
 * Configuration:
 * - Features: src/test/resources/features/
 * - Glue: io.strategiz.service.auth.bdd (step definitions package)
 * - Plugins: pretty (console output), html (HTML report)
 *
 * To run:
 * - All tests: mvn test
 * - This runner only: mvn test -Dtest=CucumberTestRunner
 * - Single feature: mvn test -Dcucumber.features=src/test/resources/features/passkey-authentication.feature
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.strategiz.service.auth.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-reports/authentication-tests.html")
public class CucumberTestRunner {
    // This class serves as the entry point for running Cucumber tests
    // No implementation needed - JUnit 5 Platform Suite handles execution
}
