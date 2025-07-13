package io.strategiz.data.user.model;

import io.strategiz.data.base.entity.BaseEntity;
import java.util.Date;
import java.util.Objects;

/**
 * Represents a financial provider connected to a user's account.
 */
public class ConnectedProvider extends BaseEntity {
    private String id;
    private String providerId;
    private String providerName;
    private String accountId;
    private String accountType; // "paper" or "real"
    private String accountName;
    private String status; // "active", "inactive", "error"
    private Date lastSyncAt;


    // Constructors
    public ConnectedProvider() {
    }

    public ConnectedProvider(String id, String providerId, String providerName, String accountId, String accountType, String accountName, String status, Date lastSyncAt, String createdBy) {
        super(createdBy);
        this.id = id;
        this.providerId = providerId;
        this.providerName = providerName;
        this.accountId = accountId;
        this.accountType = accountType;
        this.accountName = accountName;
        this.status = status;
        this.lastSyncAt = lastSyncAt;
    }

    // Getters and Setters
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getCollectionName() {
        return "connected_providers";
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConnectedProvider that = (ConnectedProvider) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(providerId, that.providerId) &&
               Objects.equals(providerName, that.providerName) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(accountType, that.accountType) &&
               Objects.equals(accountName, that.accountName) &&
               Objects.equals(status, that.status) &&
               Objects.equals(lastSyncAt, that.lastSyncAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, providerId, providerName, accountId, accountType, accountName, status, lastSyncAt);
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
               ", audit=" + getAuditFields() +
               '}';
    }
}
