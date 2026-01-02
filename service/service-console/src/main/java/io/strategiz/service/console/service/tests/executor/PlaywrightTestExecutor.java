package io.strategiz.service.console.service.tests.executor;

import io.strategiz.data.testing.entity.TestModuleEntity;
import io.strategiz.data.testing.entity.TestSuiteEntity;
import io.strategiz.data.testing.entity.TestCaseEntity;
import io.strategiz.service.console.service.tests.model.TestRunRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test executor for Playwright and Jest frameworks (npm-based).
 */
@Component
public class PlaywrightTestExecutor implements TestExecutor {

    @Value("${test.runner.ui-directory:../strategiz-ui}")
    private String uiDirectory;

    @Override
    public Process executeModule(TestModuleEntity module, TestRunRequest request) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("npx");
        command.add("playwright");
        command.add("test");
        // Module path is the project name in Playwright config
        if (module.getModulePath() != null) {
            command.add("--project=" + module.getModulePath());
        }

        return startProcess(command);
    }

    @Override
    public Process executeSuite(TestSuiteEntity suite, TestRunRequest request) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("npx");
        command.add("playwright");
        command.add("test");
        // Suite file path
        command.add(suite.getFilePath());

        return startProcess(command);
    }

    @Override
    public Process executeTest(TestCaseEntity testCase, TestRunRequest request) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("npx");
        command.add("playwright");
        command.add("test");
        // Individual test with grep pattern
        command.add("--grep");
        command.add(testCase.getDisplayName());

        return startProcess(command);
    }

    private Process startProcess(List<String> command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(new File(uiDirectory));
        builder.redirectErrorStream(true); // Combine stdout and stderr
        return builder.start();
    }
}
