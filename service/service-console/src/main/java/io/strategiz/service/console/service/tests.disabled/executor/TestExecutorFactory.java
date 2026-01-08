package io.strategiz.service.console.service.tests.executor;

import io.strategiz.data.testing.entity.TestFramework;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.exception.ServiceBaseErrorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for creating test executors based on framework type.
 */
@Component
public class TestExecutorFactory {

    private final PlaywrightTestExecutor playwrightExecutor;
    private final MavenTestExecutor mavenExecutor;
    private final PytestTestExecutor pytestExecutor;

    @Autowired
    public TestExecutorFactory(
            PlaywrightTestExecutor playwrightExecutor,
            MavenTestExecutor mavenExecutor,
            PytestTestExecutor pytestExecutor) {
        this.playwrightExecutor = playwrightExecutor;
        this.mavenExecutor = mavenExecutor;
        this.pytestExecutor = pytestExecutor;
    }

    /**
     * Get test executor for the specified framework.
     */
    public TestExecutor getExecutor(TestFramework framework) {
        switch (framework) {
            case PLAYWRIGHT:
                return playwrightExecutor;
            case JEST:
                // JEST uses similar commands to Playwright
                return playwrightExecutor;
            case JUNIT:
            case CUCUMBER:
                // Both use Maven
                return mavenExecutor;
            case PYTEST:
                return pytestExecutor;
            default:
                throw new StrategizException(ServiceBaseErrorDetails.INVALID_ARGUMENT,
                        "Unsupported test framework: " + framework);
        }
    }
}
