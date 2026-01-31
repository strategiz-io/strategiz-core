package io.strategiz.data.base.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;

import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;

/**
 * Base class for ALL platform entities with audit fields at root level.
 *
 * Simple rules: 1. Extend this class 2. Add @Collection("collection_name") annotation to
 * your entity 3. Implement getId(), setId() 4. Use repository.save(entity, userId) -
 * that's it!
 *
 * @author Strategiz Platform
 */
@MappedSuperclass
public abstract class BaseEntity {

	@PropertyName("createdBy")
	@JsonProperty("createdBy")
	@NotNull
	private String createdBy;

	@PropertyName("modifiedBy")
	@JsonProperty("modifiedBy")
	@NotNull
	private String modifiedBy;

	@PropertyName("createdDate")
	@JsonProperty("createdDate")
	@NotNull
	private Timestamp createdDate;

	@PropertyName("modifiedDate")
	@JsonProperty("modifiedDate")
	@NotNull
	private Timestamp modifiedDate;

	@PropertyName("version")
	@JsonProperty("version")
	private Long version = 1L;

	@PropertyName("isActive")
	@JsonProperty("isActive")
	private Boolean isActive = true;

	protected BaseEntity() {
		// Fields initialized by repository layer
	}

	protected BaseEntity(String userId) {
		Timestamp now = Timestamp.now();
		this.createdBy = userId;
		this.modifiedBy = userId;
		this.createdDate = now;
		this.modifiedDate = now;
		this.version = 1L;
		this.isActive = true;
	}

	// === PUBLIC API ===

	// === INTERNAL API (used by repository layer) ===

	public void _initAudit(String userId) {
		if (this.createdBy != null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE,
					getClass().getSimpleName(), "Entity already has audit fields");
		}
		Timestamp now = Timestamp.now();
		this.createdBy = userId;
		this.modifiedBy = userId;
		this.createdDate = now;
		this.modifiedDate = now;
		this.version = 1L;
		this.isActive = true;
	}

	public void _updateAudit(String userId) {
		if (this.createdBy == null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE,
					getClass().getSimpleName(), "Entity audit fields not initialized");
		}
		this.modifiedBy = userId;
		this.modifiedDate = Timestamp.now();
		// Keep version at 1 during pre-launch development
		// TODO: Enable version incrementing after launch
		// this.version = this.version + 1;
	}

	public void _softDelete(String userId) {
		if (this.createdBy == null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE,
					getClass().getSimpleName(), "Entity audit fields not initialized");
		}
		this.isActive = false;
		_updateAudit(userId);
	}

	public void _restore(String userId) {
		if (this.createdBy == null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE,
					getClass().getSimpleName(), "Entity audit fields not initialized");
		}
		this.isActive = true;
		_updateAudit(userId);
	}

	public boolean _hasAudit() {
		return this.createdBy != null;
	}

	public void _validate() {
		if (this.createdBy == null || this.createdBy.trim().isEmpty()) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE,
					getClass().getSimpleName(), "createdBy cannot be null or empty");
		}
		if (this.modifiedBy == null || this.modifiedBy.trim().isEmpty()) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE,
					getClass().getSimpleName(), "modifiedBy cannot be null or empty");
		}
		if (this.createdDate == null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE,
					getClass().getSimpleName(), "createdDate cannot be null");
		}
		if (this.modifiedDate == null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE,
					getClass().getSimpleName(), "modifiedDate cannot be null");
		}
		if (this.version == null || this.version < 1) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE,
					getClass().getSimpleName(), "version must be >= 1");
		}
	}

	// === GETTERS/SETTERS ===

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Timestamp getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Timestamp createdDate) {
		this.createdDate = createdDate;
	}

	public Timestamp getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(Timestamp modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	// === ABSTRACT METHODS (must implement) ===

	public abstract String getId();

	public abstract void setId(String id);

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		BaseEntity that = (BaseEntity) obj;
		return getId() != null && getId().equals(that.getId());
	}

	@Override
	public int hashCode() {
		return getId() != null ? getId().hashCode() : 0;
	}

}