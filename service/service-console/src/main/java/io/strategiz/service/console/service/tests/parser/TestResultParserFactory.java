package io.strategiz.service.console.service.tests.parser;

import io.strategiz.data.testing.entity.TestFramework;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.exception.ServiceBaseErrorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for creating test result parsers based on framework type.
 */
@Component
public class TestResultParserFactory {

    private final PlaywrightResultParser playwrightParser;
    private final MavenResultParser mavenParser;
    private final PytestResultParser pytestParser;

    @Autowired
    public TestResultParserFactory(
            PlaywrightResultParser playwrightParser,
            MavenResultParser mavenParser,
            PytestResultParser pytestParser) {
        this.playwrightParser = playwrightParser;
        this.mavenParser = mavenParser;
        this.pytestParser = pytestParser;
    }

    /**
     * Get result parser for the specified framework.
     */
    public TestResultParser getParser(TestFramework framework) {
        switch (framework) {
            case PLAYWRIGHT:
            case JEST:
                return playwrightParser;
            case JUNIT:
            case CUCUMBER:
                return mavenParser;
            case PYTEST:
                return pytestParser;
            default:
                throw new StrategizException(ServiceBaseErrorDetails.INVALID_ARGUMENT,
                        "Unsupported test framework: " + framework);
        }
    }
}
