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
 * Test executor for JUnit and Cucumber (Maven-based) frameworks.
 */
@Component
public class MavenTestExecutor implements TestExecutor {

    @Value("${test.runner.backend-directory:../strategiz-core}")
    private String backendDirectory;

    @Override
    public Process executeModule(TestModuleEntity module, TestRunRequest request) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("test");
        command.add("-pl");
        command.add(module.getModulePath());

        return startProcess(command);
    }

    @Override
    public Process executeSuite(TestSuiteEntity suite, TestRunRequest request) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("test");
        command.add("-Dtest=" + suite.getClassName());

        return startProcess(command);
    }

    @Override
    public Process executeTest(TestCaseEntity testCase, TestRunRequest request) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("test");
        command.add("-Dtest=" + testCase.getMethodName());

        return startProcess(command);
    }

    private Process startProcess(List<String> command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(new File(backendDirectory));
        builder.redirectErrorStream(true);
        return builder.start();
    }
}
