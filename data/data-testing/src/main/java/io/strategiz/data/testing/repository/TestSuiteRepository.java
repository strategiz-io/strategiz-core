package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestSuiteEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TestSuiteEntity Handles test suites within modules
 */
public interface TestSuiteRepository {

	// ===============================
	// Basic CRUD Operations
	// ===============================

	/**
	 * Save suite (create or update)
	 * @param suite Suite entity to save
	 * @param createdBy User who created/updated the suite
	 * @return Saved suite entity
	 */
	TestSuiteEntity save(TestSuiteEntity suite, String createdBy);

	/**
	 * Find suite by ID
	 * @param id Suite ID
	 * @return Suite entity if found
	 */
	Optional<TestSuiteEntity> findById(String id);

	/**
	 * Find all suites
	 * @return List of all suites
	 */
	List<TestSuiteEntity> findAll();

	/**
	 * Delete suite by ID
	 * @param id Suite ID
	 * @param deletedBy User who deleted the suite
	 * @return true if deleted successfully, false otherwise
	 */
	boolean delete(String id, String deletedBy);

	// ===============================
	// Custom Query Operations
	// ===============================

	/**
	 * Find all suites for a specific module
	 * @param appId Parent app ID
	 * @param moduleId Parent module ID
	 * @return List of suites for the module
	 */
	List<TestSuiteEntity> findByAppIdAndModuleId(String appId, String moduleId);

	/**
	 * Find suite by app, module, and display name
	 * @param appId Parent app ID
	 * @param moduleId Parent module ID
	 * @param displayName Suite display name
	 * @return Suite entity if found
	 */
	TestSuiteEntity findByAppIdAndModuleIdAndDisplayName(String appId, String moduleId, String displayName);

	/**
	 * Find suites by class name (for JUnit/pytest)
	 * @param className Test class name
	 * @return List of suites with matching class name
	 */
	List<TestSuiteEntity> findByClassName(String className);

	/**
	 * Find specific suite by app ID, module ID, and suite ID
	 * @param appId Parent app ID
	 * @param moduleId Parent module ID
	 * @param suiteId Suite ID
	 * @return Suite entity if found
	 */
	Optional<TestSuiteEntity> findByAppIdModuleIdAndSuiteId(String appId, String moduleId, String suiteId);

	/**
	 * Find all suites for a specific module (without requiring appId)
	 * @param moduleId Module ID
	 * @return List of suites for the module
	 */
	List<TestSuiteEntity> findByModuleId(String moduleId);

}
