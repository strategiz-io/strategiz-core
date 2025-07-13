package io.strategiz.data.base.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Service that enforces audit field consistency across all Firestore operations.
 * This service provides validation and automatic audit field management to ensure
 * no entity is saved without proper audit information.
 * 
 * Features:
 * - Pre-save validation and audit field initialization
 * - Bulk operation audit enforcement  
 * - Audit trail validation
 * - Integration with Firestore operations
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
@Service
public class AuditEnforcementService {

    private static final Logger log = LoggerFactory.getLogger(AuditEnforcementService.class);

    /**
     * Prepares an entity for creation by ensuring audit fields are properly initialized
     * @param entity The entity to prepare
     * @param userId The user ID creating the entity
     * @param <T> The entity type extending AuditableEntity
     * @return The prepared entity
     * @throws IllegalArgumentException if entity or userId is null
     * @throws IllegalStateException if entity already has audit fields initialized
     */
    public <T extends AuditableEntity> T prepareForCreate(T entity, String userId) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null for entity creation");
        
        if (userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        if (entity.areAuditFieldsInitialized()) {
            log.warn("Entity {} already has audit fields initialized. This may indicate a programming error.", 
                     entity.getClass().getSimpleName());
            throw new IllegalStateException("Entity already has audit fields initialized. Use prepareForUpdate() for modifications.");
        }

        entity.initializeAuditFields(userId);
        entity.validate();

        log.debug("Prepared entity {} for creation by user {}", 
                  entity.getClass().getSimpleName(), userId);
        
        return entity;
    }

    /**
     * Prepares an entity for update by updating audit fields
     * @param entity The entity to prepare
     * @param userId The user ID modifying the entity
     * @param <T> The entity type extending AuditableEntity
     * @return The prepared entity
     * @throws IllegalArgumentException if entity or userId is null
     * @throws IllegalStateException if entity doesn't have audit fields initialized
     */
    public <T extends AuditableEntity> T prepareForUpdate(T entity, String userId) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null for entity update");
        
        if (userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        if (!entity.areAuditFieldsInitialized()) {
            throw new IllegalStateException("Entity does not have audit fields initialized. Use prepareForCreate() for new entities.");
        }

        if (entity.isSoftDeleted()) {
            throw new IllegalStateException("Cannot update a soft-deleted entity. Use restore() first if needed.");
        }

        entity.updateAuditFields(userId);
        entity.validate();

        log.debug("Prepared entity {} for update by user {}", 
                  entity.getClass().getSimpleName(), userId);
        
        return entity;
    }

    /**
     * Prepares an entity for soft deletion
     * @param entity The entity to soft delete
     * @param userId The user ID performing the deletion
     * @param <T> The entity type extending AuditableEntity
     * @return The prepared entity
     * @throws IllegalArgumentException if entity or userId is null
     * @throws IllegalStateException if entity doesn't have audit fields or is already deleted
     */
    public <T extends AuditableEntity> T prepareForSoftDelete(T entity, String userId) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null for entity deletion");
        
        if (userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        if (!entity.areAuditFieldsInitialized()) {
            throw new IllegalStateException("Entity does not have audit fields initialized.");
        }

        if (entity.isSoftDeleted()) {
            log.warn("Entity {} is already soft-deleted", entity.getClass().getSimpleName());
            return entity; // Idempotent operation
        }

        entity.softDelete(userId);
        entity.validate();

        log.debug("Prepared entity {} for soft deletion by user {}", 
                  entity.getClass().getSimpleName(), userId);
        
        return entity;
    }

    /**
     * Prepares an entity for restoration from soft deletion
     * @param entity The entity to restore
     * @param userId The user ID performing the restoration
     * @param <T> The entity type extending AuditableEntity
     * @return The prepared entity
     * @throws IllegalArgumentException if entity or userId is null
     * @throws IllegalStateException if entity doesn't have audit fields or is not deleted
     */
    public <T extends AuditableEntity> T prepareForRestore(T entity, String userId) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null for entity restoration");
        
        if (userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        if (!entity.areAuditFieldsInitialized()) {
            throw new IllegalStateException("Entity does not have audit fields initialized.");
        }

        if (!entity.isSoftDeleted()) {
            log.warn("Entity {} is not soft-deleted, restoration not needed", entity.getClass().getSimpleName());
            return entity; // Idempotent operation
        }

        entity.restore(userId);
        entity.validate();

        log.debug("Prepared entity {} for restoration by user {}", 
                  entity.getClass().getSimpleName(), userId);
        
        return entity;
    }

    /**
     * Prepares multiple entities for creation
     * @param entities The entities to prepare
     * @param userId The user ID creating the entities
     * @param <T> The entity type extending AuditableEntity
     * @return The list of prepared entities
     * @throws IllegalArgumentException if entities list or userId is null
     */
    public <T extends AuditableEntity> List<T> prepareForBulkCreate(List<T> entities, String userId) {
        Objects.requireNonNull(entities, "Entities list cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null for bulk creation");
        
        if (userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        entities.forEach(entity -> prepareForCreate(entity, userId));

        log.debug("Prepared {} entities for bulk creation by user {}", entities.size(), userId);
        
        return entities;
    }

    /**
     * Prepares multiple entities for update
     * @param entities The entities to prepare
     * @param userId The user ID modifying the entities
     * @param <T> The entity type extending AuditableEntity
     * @return The list of prepared entities
     * @throws IllegalArgumentException if entities list or userId is null
     */
    public <T extends AuditableEntity> List<T> prepareForBulkUpdate(List<T> entities, String userId) {
        Objects.requireNonNull(entities, "Entities list cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null for bulk update");
        
        if (userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        entities.forEach(entity -> prepareForUpdate(entity, userId));

        log.debug("Prepared {} entities for bulk update by user {}", entities.size(), userId);
        
        return entities;
    }

    /**
     * Validates that an entity has proper audit fields without modifying them
     * @param entity The entity to validate
     * @throws IllegalStateException if audit fields are missing or invalid
     */
    public void validateAuditFields(AuditableEntity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        
        if (!entity.areAuditFieldsInitialized()) {
            throw new IllegalStateException("Entity does not have audit fields initialized");
        }

        entity.validate();

        log.debug("Validated audit fields for entity {}", entity.getClass().getSimpleName());
    }

    /**
     * Validates multiple entities have proper audit fields
     * @param entities The entities to validate
     * @throws IllegalStateException if any entity has missing or invalid audit fields
     */
    public void validateBulkAuditFields(List<? extends AuditableEntity> entities) {
        Objects.requireNonNull(entities, "Entities list cannot be null");
        
        entities.forEach(this::validateAuditFields);

        log.debug("Validated audit fields for {} entities", entities.size());
    }

    /**
     * Checks if an entity has valid audit fields
     * @param entity The entity to check
     * @return true if audit fields are valid, false otherwise
     */
    public boolean hasValidAuditFields(AuditableEntity entity) {
        if (entity == null || !entity.areAuditFieldsInitialized()) {
            return false;
        }

        try {
            entity.validate();
            return true;
        } catch (Exception e) {
            log.debug("Entity {} failed audit validation: {}", 
                      entity.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
}