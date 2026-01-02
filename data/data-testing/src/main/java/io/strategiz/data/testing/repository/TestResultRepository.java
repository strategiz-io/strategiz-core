package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestResultEntity;
import io.strategiz.data.testing.entity.TestResultStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TestResultEntity
 * Handles individual test results within runs
 * Stored as subcollection: test-runs/{runId}/results/{resultId}
 */
public interface TestResultRepository {

    // ===============================
    // Basic Subcollection CRUD Operations
    // ===============================

    /**
     * Save test result in subcollection (create or update)
     *
     * @param runId Parent test run ID
     * @param result Test result entity to save
     * @param createdBy User who created/updated the result
     * @return Saved test result entity
     */
    TestResultEntity saveInSubcollection(String runId, TestResultEntity result, String createdBy);

    /**
     * Find test result by ID within a run
     *
     * @param runId Parent test run ID
     * @param resultId Result ID
     * @return Test result entity if found
     */
    Optional<TestResultEntity> findByIdInSubcollection(String runId, String resultId);

    /**
     * Find all results in subcollection
     *
     * @param runId Parent test run ID
     * @return List of all results for the run
     */
    List<TestResultEntity> findAllInSubcollection(String runId);

    /**
     * Delete result by ID in subcollection
     *
     * @param runId Parent test run ID
     * @param resultId Result ID
     * @param deletedBy User who deleted the result
     * @return true if deleted successfully, false otherwise
     */
    boolean deleteInSubcollection(String runId, String resultId, String deletedBy);

    // ===============================
    // Custom Query Operations
    // ===============================

    /**
     * Find all results for a specific test run
     *
     * @param runId Parent test run ID
     * @return List of test results for the run
     */
    List<TestResultEntity> findByRunId(String runId);

    /**
     * Find results by status for a specific run
     *
     * @param runId Parent test run ID
     * @param status Result status (PASSED, FAILED, SKIPPED, ERROR)
     * @return List of results with the status
     */
    List<TestResultEntity> findByRunIdAndStatus(String runId, TestResultStatus status);

    /**
     * Find failed results for a run
     *
     * @param runId Parent test run ID
     * @return List of failed test results
     */
    List<TestResultEntity> findFailedByRunId(String runId);

    /**
     * Find results with screenshots (Playwright tests)
     *
     * @param runId Parent test run ID
     * @return List of results that have screenshots
     */
    List<TestResultEntity> findWithScreenshotsByRunId(String runId);

    /**
     * Count results by status for a run
     *
     * @param runId Parent test run ID
     * @param status Result status
     * @return Count of results
     */
    long countByRunIdAndStatus(String runId, TestResultStatus status);

    /**
     * Delete all results for a run (cleanup)
     *
     * @param runId Parent test run ID
     */
    void deleteByRunId(String runId);
}
