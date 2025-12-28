package io.strategiz.service.labs.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Cucumber test runner for AI Strategy BDD tests.
 *
 * This runner executes all feature files in the classpath and generates
 * reports in multiple formats (JSON, HTML, JUnit XML).
 *
 * To run these tests:
 * <pre>
 * mvn test -Dtest=CucumberTestRunner
 * </pre>
 *
 * To run specific scenarios by tag:
 * <pre>
 * mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@smoke"
 * </pre>
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features",
    glue = "io.strategiz.service.labs.bdd",
    plugin = {
        "pretty",
        "html:target/cucumber-reports/cucumber.html",
        "json:target/cucumber-reports/cucumber.json",
        "junit:target/cucumber-reports/cucumber.xml"
    },
    monochrome = true,
    snippets = CucumberOptions.SnippetType.CAMELCASE
)
public class CucumberTestRunner {
    // This class will be empty - Cucumber uses the annotations to run tests
}
