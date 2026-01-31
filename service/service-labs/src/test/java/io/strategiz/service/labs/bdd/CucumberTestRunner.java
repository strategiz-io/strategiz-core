package io.strategiz.service.labs.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.SNIPPET_TYPE_PROPERTY_NAME;

/**
 * Cucumber test runner for AI Strategy BDD tests (JUnit 5 Platform).
 *
 * This runner executes all feature files in the classpath and generates reports in
 * multiple formats (JSON, HTML, JUnit XML).
 *
 * To run these tests: <pre>
 * mvn test -Dtest=CucumberTestRunner
 * </pre>
 *
 * To run specific scenarios by tag: <pre>
 * mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@smoke"
 * </pre>
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.strategiz.service.labs.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
		value = "pretty, html:target/cucumber-reports/cucumber.html, json:target/cucumber-reports/cucumber.json, junit:target/cucumber-reports/cucumber.xml")
@ConfigurationParameter(key = SNIPPET_TYPE_PROPERTY_NAME, value = "camelcase")
public class CucumberTestRunner {

	// This class will be empty - JUnit Platform Suite uses the annotations to run tests

}
