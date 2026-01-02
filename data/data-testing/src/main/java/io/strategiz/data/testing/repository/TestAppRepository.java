package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestAppEntity;
import io.strategiz.data.testing.entity.TestAppType;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TestAppEntity
 * Handles CRUD operations for test applications in the hierarchy
 */
public interface TestAppRepository {

    // ===============================
    // Basic CRUD Operations
    // ===============================

    /**
     * Save app (create or update)
     *
     * @param app App entity to save
     * @param createdBy User who created/updated the app
     * @return Saved app entity
     */
    TestAppEntity save(TestAppEntity app, String createdBy);

    /**
     * Find app by ID
     *
     * @param id App ID
     * @return App entity if found
     */
    Optional<TestAppEntity> findById(String id);

    /**
     * Find all apps
     *
     * @return List of all apps
     */
    List<TestAppEntity> findAll();

    /**
     * Delete app by ID
     *
     * @param id App ID
     * @param deletedBy User who deleted the app
     * @return true if deleted successfully, false otherwise
     */
    boolean delete(String id, String deletedBy);

    // ===============================
    // Custom Query Operations
    // ===============================

    /**
     * Find all apps of a specific type
     *
     * @param type App type (FRONTEND, BACKEND, MICROSERVICE)
     * @return List of apps matching the type
     */
    List<TestAppEntity> findByType(TestAppType type);

    /**
     * Find app by display name
     *
     * @param displayName Display name to search for
     * @return App entity if found
     */
    TestAppEntity findByDisplayName(String displayName);

    /**
     * Find all active apps ordered by display name
     *
     * @return List of active apps
     */
    List<TestAppEntity> findAllActive();
}
