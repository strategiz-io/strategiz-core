package io.strategiz.data.base.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.Exclude;
import com.google.cloud.firestore.annotation.PropertyName;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;

import java.time.Instant;
import java.util.Objects;

/**
 * Centralized audit fields for all Firestore entities. This class ensures consistent
 * audit data across all documents in the platform.
 *
 * Usage: All Firestore entities MUST include this as an embedded field named
 * 'auditFields'.
 *
 * @author Strategiz Platform
 * @since 1.0
 */
public class AuditFields {

	@PropertyName("createdBy")
	@JsonProperty("createdBy")
	private String createdBy;

	@PropertyName("modifiedBy")
	@JsonProperty("modifiedBy")
	private String modifiedBy;

	@PropertyName("createdDate")
	@JsonProperty("createdDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Timestamp createdDate;

	@PropertyName("modifiedDate")
	@JsonProperty("modifiedDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Timestamp modifiedDate;

	@PropertyName("version")
	@JsonProperty("version")
	private Long version;

	@PropertyName("isActive")
	@JsonProperty("isActive")
	private Boolean isActive;

	/**
	 * Default constructor for Firestore deserialization
	 */
	public AuditFields() {
		this.version = 1L;
		this.isActive = true;
	}

	/**
	 * Constructor for creating new audit fields
	 * @param userId The user ID creating the entity
	 */
	public AuditFields(String userId) {
		this();
		Timestamp now = Timestamp.now();
		this.createdBy = userId;
		this.modifiedBy = userId;
		this.createdDate = now;
		this.modifiedDate = now;
	}

	/**
	 * Updates the audit fields for modification
	 * @param userId The user ID modifying the entity
	 */
	public void updateForModification(String userId) {
		Objects.requireNonNull(userId, "User ID cannot be null for audit update");
		this.modifiedBy = userId;
		this.modifiedDate = Timestamp.now();
		this.version = this.version + 1;
	}

	/**
	 * Performs soft delete on the entity
	 * @param userId The user ID performing the deletion
	 */
	public void softDelete(String userId) {
		Objects.requireNonNull(userId, "User ID cannot be null for soft delete");
		this.isActive = false;
		updateForModification(userId);
	}

	/**
	 * Restores a soft-deleted entity
	 * @param userId The user ID performing the restoration
	 */
	public void restore(String userId) {
		Objects.requireNonNull(userId, "User ID cannot be null for restore");
		this.isActive = true;
		updateForModification(userId);
	}

	/**
	 * Checks if the entity is currently active (not soft-deleted)
	 * @return true if active, false if soft-deleted
	 */
	public boolean isActive() {
		return Boolean.TRUE.equals(this.isActive);
	}

	/**
	 * Checks if the entity is soft-deleted
	 * @return true if soft-deleted, false if active
	 */
	public boolean isSoftDeleted() {
		return !isActive();
	}

	/**
	 * Gets the created date as Java Instant (for internal Java use only)
	 * @return Instant representation of creation date
	 */
	@JsonIgnore
	@Exclude
	public Instant getCreatedInstant() {
		return createdDate != null ? createdDate.toDate().toInstant() : null;
	}

	/**
	 * Gets the modified date as Java Instant (for internal Java use only)
	 * @return Instant representation of modification date
	 */
	@JsonIgnore
	@Exclude
	public Instant getModifiedInstant() {
		return modifiedDate != null ? modifiedDate.toDate().toInstant() : null;
	}

	/**
	 * Validates the audit fields integrity
	 * @throws DataRepositoryException if audit fields are in invalid state
	 */
	public void validate() {
		if (createdBy == null || createdBy.trim().isEmpty()) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE, "AuditFields",
					"createdBy cannot be null or empty");
		}
		if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE, "AuditFields",
					"modifiedBy cannot be null or empty");
		}
		if (createdDate == null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE, "AuditFields",
					"createdDate cannot be null");
		}
		if (modifiedDate == null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE, "AuditFields",
					"modifiedDate cannot be null");
		}
		if (version == null || version < 1) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE, "AuditFields",
					"version must be >= 1");
		}
		if (isActive == null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE, "AuditFields",
					"isActive cannot be null");
		}
		if (modifiedDate.compareTo(createdDate) < 0) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ENTITY_STATE, "AuditFields",
					"modifiedDate cannot be before createdDate");
		}
	}

	// Getters and setters
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

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AuditFields that = (AuditFields) o;
		return Objects.equals(createdBy, that.createdBy) && Objects.equals(modifiedBy, that.modifiedBy)
				&& Objects.equals(createdDate, that.createdDate) && Objects.equals(modifiedDate, that.modifiedDate)
				&& Objects.equals(version, that.version) && Objects.equals(isActive, that.isActive);
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, modifiedBy, createdDate, modifiedDate, version, isActive);
	}

	@Override
	public String toString() {
		return "AuditFields{" + "createdBy='" + createdBy + '\'' + ", modifiedBy='" + modifiedBy + '\''
				+ ", createdDate=" + createdDate + ", modifiedDate=" + modifiedDate + ", version=" + version
				+ ", isActive=" + isActive + '}';
	}

}