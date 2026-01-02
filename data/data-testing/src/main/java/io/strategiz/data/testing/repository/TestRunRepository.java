package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TestRunEntity
 * Handles test execution runs
 */
public interface TestRunRepository {

    // ===============================
    // Basic CRUD Operations
    // ===============================

    /**
     * Save test run (create or update)
     *
     * @param testRun Test run entity to save
     * @param createdBy User who created/updated the test run
     * @return Saved test run entity
     */
    TestRunEntity save(TestRunEntity testRun, String createdBy);

    /**
     * Find test run by ID
     *
     * @param id Test run ID
     * @return Test run entity if found
     */
    Optional<TestRunEntity> findById(String id);

    /**
     * Find all test runs
     *
     * @return List of all test runs
     */
    List<TestRunEntity> findAll();

    /**
     * Delete test run by ID
     *
     * @param id Test run ID
     * @param deletedBy User who deleted the test run
     * @return true if deleted successfully, false otherwise
     */
    boolean delete(String id, String deletedBy);

    // ===============================
    // Custom Query Operations
    // ===============================

    /**
     * Find all runs for a specific app
     *
     * @param appId App ID
     * @return List of test runs for the app
     */
    List<TestRunEntity> findByAppId(String appId);

    /**
     * Find runs by status
     *
     * @param status Run status (RUNNING, PASSED, FAILED, etc.)
     * @return List of runs with the status
     */
    List<TestRunEntity> findByStatus(TestRunStatus status);

    /**
     * Find runs by trigger type
     *
     * @param trigger Trigger type (MANUAL, CI_CD, SCHEDULED)
     * @return List of runs with the trigger type
     */
    List<TestRunEntity> findByTrigger(TestTrigger trigger);

    /**
     * Find runs for a specific hierarchy level and ID
     *
     * @param level Execution level (APP, MODULE, SUITE, TEST)
     * @param appId App ID
     * @param moduleId Module ID (nullable)
     * @param suiteId Suite ID (nullable)
     * @param testId Test ID (nullable)
     * @return List of matching runs
     */
    List<TestRunEntity> findByHierarchy(TestExecutionLevel level, String appId,
                                       String moduleId, String suiteId, String testId);

    /**
     * Find recent runs (last N runs)
     *
     * @param limit Maximum number of runs to return
     * @return List of recent runs, ordered by start time descending
     */
    List<TestRunEntity> findRecent(int limit);

    /**
     * Find runs within a time range
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of runs within the time range
     */
    List<TestRunEntity> findByTimeRange(Instant startTime, Instant endTime);

    /**
     * Find latest CI/CD run for a specific hierarchy level
     *
     * @param level Execution level
     * @param appId App ID
     * @param moduleId Module ID (nullable)
     * @param suiteId Suite ID (nullable)
     * @return Latest CI/CD run if exists
     */
    TestRunEntity findLatestCiRun(TestExecutionLevel level, String appId,
                                  String moduleId, String suiteId);

    /**
     * Find all currently running tests
     *
     * @return List of tests with RUNNING status
     */
    List<TestRunEntity> findRunning();

    /**
     * Count runs by status for a specific app
     *
     * @param appId App ID
     * @param status Run status
     * @return Count of runs
     */
    long countByAppIdAndStatus(String appId, TestRunStatus status);
}
