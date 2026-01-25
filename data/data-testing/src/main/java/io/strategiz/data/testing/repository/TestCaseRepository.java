package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestCaseEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TestCaseEntity
 * Handles individual test cases within suites
 */
public interface TestCaseRepository {

    // ===============================
    // Basic CRUD Operations
    // ===============================

    /**
     * Save test case (create or update)
     *
     * @param testCase Test case entity to save
     * @param createdBy User who created/updated the test case
     * @return Saved test case entity
     */
    TestCaseEntity save(TestCaseEntity testCase, String createdBy);

    /**
     * Find test case by ID
     *
     * @param id Test case ID
     * @return Test case entity if found
     */
    Optional<TestCaseEntity> findById(String id);

    /**
     * Find all test cases
     *
     * @return List of all test cases
     */
    List<TestCaseEntity> findAll();

    /**
     * Delete test case by ID
     *
     * @param id Test case ID
     * @param deletedBy User who deleted the test case
     * @return true if deleted successfully, false otherwise
     */
    boolean delete(String id, String deletedBy);

    // ===============================
    // Custom Query Operations
    // ===============================

    /**
     * Find all test cases for a specific suite
     *
     * @param appId Parent app ID
     * @param moduleId Parent module ID
     * @param suiteId Parent suite ID
     * @return List of test cases for the suite
     */
    List<TestCaseEntity> findByAppIdAndModuleIdAndSuiteId(String appId, String moduleId, String suiteId);

    /**
     * Find test case by method name
     *
     * @param appId Parent app ID
     * @param moduleId Parent module ID
     * @param suiteId Parent suite ID
     * @param methodName Test method name
     * @return Test case entity if found
     */
    TestCaseEntity findByAppIdAndModuleIdAndSuiteIdAndMethodName(
            String appId, String moduleId, String suiteId, String methodName);

    /**
     * Find all test cases for a module (across all suites)
     *
     * @param appId Parent app ID
     * @param moduleId Parent module ID
     * @return List of all test cases in the module
     */
    List<TestCaseEntity> findByAppIdAndModuleId(String appId, String moduleId);

    /**
     * Find all test cases for an app (across all modules and suites)
     *
     * @param appId Parent app ID
     * @return List of all test cases in the app
     */
    List<TestCaseEntity> findByAppId(String appId);

    /**
     * Find all test cases for a suite
     *
     * @param suiteId Suite ID
     * @return List of all test cases in the suite
     */
    List<TestCaseEntity> findBySuiteId(String suiteId);
}
