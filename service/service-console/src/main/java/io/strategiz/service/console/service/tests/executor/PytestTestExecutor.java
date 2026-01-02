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
 * Test executor for pytest framework (Python).
 */
@Component
public class PytestTestExecutor implements TestExecutor {

    @Value("${test.runner.python-directory:../strategiz-core/application-strategy-execution}")
    private String pythonDirectory;

    @Override
    public Process executeModule(TestModuleEntity module, TestRunRequest request) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("pytest");
        command.add(module.getModulePath());
        command.add("-v");
        command.add("--tb=short");

        return startProcess(command);
    }

    @Override
    public Process executeSuite(TestSuiteEntity suite, TestRunRequest request) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("pytest");
        command.add(suite.getFilePath());
        command.add("-v");
        command.add("--tb=short");

        return startProcess(command);
    }

    @Override
    public Process executeTest(TestCaseEntity testCase, TestRunRequest request) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("pytest");
        command.add(testCase.getMethodName());
        command.add("-v");
        command.add("--tb=short");

        return startProcess(command);
    }

    private Process startProcess(List<String> command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(new File(pythonDirectory));
        builder.redirectErrorStream(true);
        return builder.start();
    }
}
