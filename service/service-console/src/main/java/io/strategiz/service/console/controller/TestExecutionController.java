package io.strategiz.service.console.controller;

import io.strategiz.business.tokenauth.AuthenticatedUser;
import io.strategiz.business.tokenauth.RequireAuth;
import io.strategiz.data.testing.entity.TestRunEntity;
import io.strategiz.service.base.BaseController;
import io.strategiz.service.console.service.tests.TestExecutionService;
import io.strategiz.service.console.service.tests.model.TestRunRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST API controller for test execution
 * Supports multi-level test execution: app, module, suite, individual test
 */
@RestController
@RequestMapping("/v1/console/tests")
@Tag(name = "Test Execution", description = "Execute tests at various granularity levels")
public class TestExecutionController extends BaseController {

    private final TestExecutionService testExecutionService;

    @Autowired
    public TestExecutionController(TestExecutionService testExecutionService) {
        this.testExecutionService = testExecutionService;
    }

    protected String getModuleName() {
        return "TEST_EXECUTION_CONTROLLER";
    }

    /**
     * Execute all tests for an application
     *
     * @param appId App ID
     * @param request Test run request
     * @param user Authenticated user
     * @return Test run entity with execution details
     */
    @PostMapping("/apps/{appId}/run")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Execute all tests for an app", description = "Runs all modules and tests within an application")
    public CompletableFuture<ResponseEntity<TestRunEntity>> executeAppTests(
            @PathVariable String appId,
            @RequestBody TestRunRequest request,
            @AuthUser AuthenticatedUser user) {

        return testExecutionService.executeAppTests(appId, request, user.getUserId())
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Execute all tests in a module
     *
     * @param appId App ID
     * @param moduleId Module ID
     * @param request Test run request
     * @param user Authenticated user
     * @return Test run entity with execution details
     */
    @PostMapping("/apps/{appId}/modules/{moduleId}/run")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Execute all tests in a module", description = "Runs all test suites within a module")
    public CompletableFuture<ResponseEntity<TestRunEntity>> executeModuleTests(
            @PathVariable String appId,
            @PathVariable String moduleId,
            @RequestBody TestRunRequest request,
            @AuthUser AuthenticatedUser user) {

        return testExecutionService.executeModuleTests(appId, moduleId, request, user.getUserId())
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Execute all tests in a suite
     *
     * @param appId App ID
     * @param moduleId Module ID
     * @param suiteId Suite ID
     * @param request Test run request
     * @param user Authenticated user
     * @return Test run entity with execution details
     */
    @PostMapping("/apps/{appId}/modules/{moduleId}/suites/{suiteId}/run")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Execute all tests in a suite", description = "Runs all tests within a test suite")
    public CompletableFuture<ResponseEntity<TestRunEntity>> executeSuiteTests(
            @PathVariable String appId,
            @PathVariable String moduleId,
            @PathVariable String suiteId,
            @RequestBody TestRunRequest request,
            @AuthUser AuthenticatedUser user) {

        return testExecutionService.executeSuiteTests(appId, moduleId, suiteId, request, user.getUserId())
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Execute a single test
     *
     * @param appId App ID
     * @param moduleId Module ID
     * @param suiteId Suite ID
     * @param testId Test ID
     * @param request Test run request
     * @param user Authenticated user
     * @return Test run entity with execution details
     */
    @PostMapping("/apps/{appId}/modules/{moduleId}/suites/{suiteId}/tests/{testId}/run")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Execute a single test", description = "Runs an individual test method")
    public CompletableFuture<ResponseEntity<TestRunEntity>> executeIndividualTest(
            @PathVariable String appId,
            @PathVariable String moduleId,
            @PathVariable String suiteId,
            @PathVariable String testId,
            @RequestBody TestRunRequest request,
            @AuthUser AuthenticatedUser user) {

        return testExecutionService.executeIndividualTest(appId, moduleId, suiteId, testId, request, user.getUserId())
                .thenApply(ResponseEntity::ok);
    }
}
