package io.strategiz.data.base.entity;

import io.strategiz.data.base.audit.AuditFields;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Base class for ALL platform entities.
 * 
 * Simple rules:
 * 1. Extend this class
 * 2. Implement getId(), setId(), getCollectionName()
 * 3. Use repository.save(entity, userId) - that's it!
 * 
 * The audit system handles everything else automatically.
 * 
 * @author Strategiz Platform
 */
public abstract class BaseEntity {

    @PropertyName("auditFields")
    @JsonProperty("auditFields")
    @Valid
    @NotNull
    private AuditFields auditFields;

    protected BaseEntity() {
        // Audit fields initialized by repository layer
    }

    protected BaseEntity(String userId) {
        this.auditFields = new AuditFields(userId);
    }

    // === SIMPLE PUBLIC API ===

    public boolean isActive() {
        return auditFields != null && auditFields.isActive();
    }

    public boolean isDeleted() {
        return !isActive();
    }

    public String getCreatedBy() {
        return auditFields != null ? auditFields.getCreatedBy() : null;
    }

    public String getModifiedBy() {
        return auditFields != null ? auditFields.getModifiedBy() : null;
    }

    public Long getVersion() {
        return auditFields != null ? auditFields.getVersion() : null;
    }

    // === INTERNAL API (used by repository layer) ===

    public void _initAudit(String userId) {
        if (this.auditFields != null) {
            throw new IllegalStateException("Entity already has audit fields");
        }
        this.auditFields = new AuditFields(userId);
    }

    public void _updateAudit(String userId) {
        if (this.auditFields == null) {
            throw new IllegalStateException("Entity audit fields not initialized");
        }
        this.auditFields.updateForModification(userId);
    }

    public void _softDelete(String userId) {
        if (this.auditFields == null) {
            throw new IllegalStateException("Entity audit fields not initialized");
        }
        this.auditFields.softDelete(userId);
    }

    public void _restore(String userId) {
        if (this.auditFields == null) {
            throw new IllegalStateException("Entity audit fields not initialized");
        }
        this.auditFields.restore(userId);
    }

    public boolean _hasAudit() {
        return this.auditFields != null;
    }

    public void _validate() {
        if (this.auditFields == null) {
            throw new IllegalStateException("Entity missing audit fields");
        }
        this.auditFields.validate();
    }

    // === PROTECTED FOR SUBCLASSES ===

    protected AuditFields getAuditFields() {
        return auditFields;
    }

    protected void setAuditFields(AuditFields auditFields) {
        this.auditFields = auditFields;
    }

    // === ABSTRACT METHODS (must implement) ===

    public abstract String getId();
    public abstract void setId(String id);
    public abstract String getCollectionName();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseEntity that = (BaseEntity) obj;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}