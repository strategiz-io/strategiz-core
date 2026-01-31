package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestFramework;
import io.strategiz.data.testing.entity.TestModuleEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TestModuleEntity Handles modules within test applications
 */
public interface TestModuleRepository {

	// ===============================
	// Basic CRUD Operations
	// ===============================

	/**
	 * Save module (create or update)
	 * @param module Module entity to save
	 * @param createdBy User who created/updated the module
	 * @return Saved module entity
	 */
	TestModuleEntity save(TestModuleEntity module, String createdBy);

	/**
	 * Find module by ID
	 * @param id Module ID
	 * @return Module entity if found
	 */
	Optional<TestModuleEntity> findById(String id);

	/**
	 * Find all modules
	 * @return List of all modules
	 */
	List<TestModuleEntity> findAll();

	/**
	 * Delete module by ID
	 * @param id Module ID
	 * @param deletedBy User who deleted the module
	 * @return true if deleted successfully, false otherwise
	 */
	boolean delete(String id, String deletedBy);

	// ===============================
	// Custom Query Operations
	// ===============================

	/**
	 * Find all modules for a specific app
	 * @param appId Parent app ID
	 * @return List of modules for the app
	 */
	List<TestModuleEntity> findByAppId(String appId);

	/**
	 * Find modules by test framework
	 * @param framework Test framework (PLAYWRIGHT, JUNIT, PYTEST, etc.)
	 * @return List of modules using the framework
	 */
	List<TestModuleEntity> findByFramework(TestFramework framework);

	/**
	 * Find modules for an app with specific framework
	 * @param appId Parent app ID
	 * @param framework Test framework
	 * @return List of matching modules
	 */
	List<TestModuleEntity> findByAppIdAndFramework(String appId, TestFramework framework);

	/**
	 * Find module by app ID and display name
	 * @param appId Parent app ID
	 * @param displayName Module display name
	 * @return Module entity if found
	 */
	TestModuleEntity findByAppIdAndDisplayName(String appId, String displayName);

	/**
	 * Find specific module by app ID and module ID
	 * @param appId Parent app ID
	 * @param moduleId Module ID
	 * @return Module entity if found
	 */
	Optional<TestModuleEntity> findByAppIdAndModuleId(String appId, String moduleId);

}
