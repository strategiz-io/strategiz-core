package io.strategiz.data.user.model;

import java.util.Date;
import java.util.Objects;

/**
 * Represents a financial provider connected to a user's account.
 */
public class ConnectedProvider {
    private String id;
    private String providerId;
    private String providerName;
    private String accountId;
    private String accountType; // "paper" or "real"
    private String accountName;
    private String status; // "active", "inactive", "error"
    private Date lastSyncAt;

    // Audit fields
    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
    private Integer version = 1;
    private Boolean isActive = true;

    // Constructors
    public ConnectedProvider() {
    }

    public ConnectedProvider(String id, String providerId, String providerName, String accountId, String accountType, String accountName, String status, Date lastSyncAt, String createdBy, Date createdAt, String modifiedBy, Date modifiedAt, Integer version, Boolean isActive) {
        this.id = id;
        this.providerId = providerId;
        this.providerName = providerName;
        this.accountId = accountId;
        this.accountType = accountType;
        this.accountName = accountName;
        this.status = status;
        this.lastSyncAt = lastSyncAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.modifiedBy = modifiedBy;
        this.modifiedAt = modifiedAt;
        this.version = version;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Date lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsActive() { // Renamed from getActive to getIsActive to match common boolean getter naming
        return isActive;
    }

    public void setIsActive(Boolean active) { // Renamed from setActive to setIsActive
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectedProvider that = (ConnectedProvider) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(providerId, that.providerId) &&
               Objects.equals(providerName, that.providerName) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(accountType, that.accountType) &&
               Objects.equals(accountName, that.accountName) &&
               Objects.equals(status, that.status) &&
               Objects.equals(lastSyncAt, that.lastSyncAt) &&
               Objects.equals(createdBy, that.createdBy) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(modifiedBy, that.modifiedBy) &&
               Objects.equals(modifiedAt, that.modifiedAt) &&
               Objects.equals(version, that.version) &&
               Objects.equals(isActive, that.isActive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, providerId, providerName, accountId, accountType, accountName, status, lastSyncAt, createdBy, createdAt, modifiedBy, modifiedAt, version, isActive);
    }

    @Override
    public String toString() {
        return "ConnectedProvider{" +
               "id='" + id + '\'' +
               ", providerId='" + providerId + '\'' +
               ", providerName='" + providerName + '\'' +
               ", accountId='" + accountId + '\'' +
               ", accountType='" + accountType + '\'' +
               ", accountName='" + accountName + '\'' +
               ", status='" + status + '\'' +
               ", lastSyncAt=" + lastSyncAt +
               ", createdBy='" + createdBy + '\'' +
               ", createdAt=" + createdAt +
               ", modifiedBy='" + modifiedBy + '\'' +
               ", modifiedAt=" + modifiedAt +
               ", version=" + version +
               ", isActive=" + isActive +
               '}';
    }
}
