package io.strategiz.data.base.audit;

import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Abstract base class for all Firestore entities that require audit fields.
 * This class enforces the inclusion of standardized audit fields across all platform entities.
 * 
 * ALL Firestore entities MUST extend this class to ensure audit consistency.
 * 
 * Features:
 * - Automatic audit field initialization
 * - Built-in validation
 * - Soft delete capabilities  
 * - Version control for optimistic locking
 * - User tracking for all operations
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
public abstract class AuditableEntity {

    @PropertyName("auditFields")
    @JsonProperty("auditFields")
    @Valid
    @NotNull(message = "Audit fields are required for all entities")
    private AuditFields auditFields;

    /**
     * Default constructor - creates empty audit fields
     * NOTE: Audit fields MUST be initialized via initializeAuditFields() before saving
     */
    protected AuditableEntity() {
        // Audit fields will be initialized when entity is created through proper service methods
    }

    /**
     * Constructor that initializes audit fields with creating user
     * @param createdBy User ID of the entity creator
     */
    protected AuditableEntity(String createdBy) {
        this.auditFields = new AuditFields(createdBy);
    }

    /**
     * Initializes audit fields for new entities
     * MUST be called before saving new entities to Firestore
     * @param userId The user ID creating the entity
     * @throws IllegalStateException if audit fields are already initialized
     */
    public void initializeAuditFields(String userId) {
        if (this.auditFields != null) {
            throw new IllegalStateException("Audit fields are already initialized. Use updateAuditFields() for modifications.");
        }
        this.auditFields = new AuditFields(userId);
    }

    /**
     * Updates audit fields for entity modification
     * @param userId The user ID modifying the entity
     * @throws IllegalStateException if audit fields are not initialized
     */
    public void updateAuditFields(String userId) {
        ensureAuditFieldsInitialized();
        this.auditFields.updateForModification(userId);
    }

    /**
     * Performs soft delete on the entity
     * @param userId The user ID performing the deletion
     * @throws IllegalStateException if audit fields are not initialized
     */
    public void softDelete(String userId) {
        ensureAuditFieldsInitialized();
        this.auditFields.softDelete(userId);
    }

    /**
     * Restores a soft-deleted entity
     * @param userId The user ID performing the restoration
     * @throws IllegalStateException if audit fields are not initialized
     */
    public void restore(String userId) {
        ensureAuditFieldsInitialized();
        this.auditFields.restore(userId);
    }

    /**
     * Checks if the entity is currently active (not soft-deleted)
     * @return true if active, false if soft-deleted or audit fields not initialized
     */
    public boolean isActive() {
        return auditFields != null && auditFields.isActive();
    }

    /**
     * Checks if the entity is soft-deleted
     * @return true if soft-deleted, false if active or audit fields not initialized
     */
    public boolean isSoftDeleted() {
        return auditFields != null && auditFields.isSoftDeleted();
    }

    /**
     * Gets the user ID who created this entity
     * @return Creator user ID, or null if audit fields not initialized
     */
    public String getCreatedBy() {
        return auditFields != null ? auditFields.getCreatedBy() : null;
    }

    /**
     * Gets the user ID who last modified this entity
     * @return Modifier user ID, or null if audit fields not initialized
     */
    public String getModifiedBy() {
        return auditFields != null ? auditFields.getModifiedBy() : null;
    }

    /**
     * Gets the current version of the entity for optimistic locking
     * @return Version number, or null if audit fields not initialized
     */
    public Long getVersion() {
        return auditFields != null ? auditFields.getVersion() : null;
    }

    /**
     * Validates the entity including audit fields
     * Subclasses should override and call super.validate() first
     * @throws IllegalStateException if entity is in invalid state
     */
    public void validate() {
        ensureAuditFieldsInitialized();
        auditFields.validate();
    }

    /**
     * Ensures audit fields are initialized before performing operations
     * @throws IllegalStateException if audit fields are not initialized
     */
    private void ensureAuditFieldsInitialized() {
        if (this.auditFields == null) {
            throw new IllegalStateException(
                "Audit fields are not initialized. Call initializeAuditFields(userId) before saving entity to Firestore."
            );
        }
    }

    /**
     * Checks if audit fields are initialized
     * @return true if initialized, false otherwise
     */
    public boolean areAuditFieldsInitialized() {
        return this.auditFields != null;
    }

    // Protected getter for subclasses
    protected AuditFields getAuditFields() {
        return auditFields;
    }

    // Protected setter for Firestore deserialization and testing
    protected void setAuditFields(AuditFields auditFields) {
        this.auditFields = auditFields;
    }

    /**
     * Creates a copy of this entity with the same audit fields
     * Subclasses should override this method to provide proper copying
     * @return A copy of this entity
     */
    protected abstract AuditableEntity copy();

    /**
     * Gets the Firestore collection name for this entity type
     * Subclasses MUST implement this to specify their collection name
     * @return The Firestore collection name
     */
    public abstract String getCollectionName();

    /**
     * Gets the unique identifier for this entity
     * Subclasses MUST implement this to provide entity ID
     * @return The entity's unique identifier
     */
    public abstract String getId();

    /**
     * Sets the unique identifier for this entity
     * Subclasses MUST implement this to support ID assignment
     * @param id The entity's unique identifier
     */
    public abstract void setId(String id);
}