package io.strategiz.service.provider.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response model for deleting/disconnecting provider connections.
 * Contains results of cleanup operations and disconnection status.
 */
public class DeleteProviderResponse {
    
    // Provider identification
    private String providerId;
    private String providerName;
    
    // Deletion result
    private String status; // "success", "failed", "partial", "not_found"
    private String message;
    private Boolean operationSuccess;
    
    // Cleanup details
    private Boolean tokensRevoked;
    private Boolean dataCleared;
    private Boolean configRemoved;
    private Boolean accountDisconnected;
    
    // Cleanup summary
    private Map<String, Object> cleanupSummary;
    private Integer recordsDeleted;
    private Integer recordsSkipped;
    private Integer recordsError;
    
    // Data that was preserved (if preserveData was true)
    private Map<String, Object> preservedData;
    private List<String> preservedDataTypes;
    
    // Backup information (if backup was created)
    private String backupId;
    private String backupLocation;
    private Long backupSize;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant backupCreatedAt;
    
    // Connection history (summary before deletion)
    private String connectionType; // "oauth", "api_key"
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant originalConnectedAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant lastSyncAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant disconnectedAt;
    
    private Long totalConnectionDays;
    
    // Error information (if status is "failed" or "partial")
    private String errorCode;
    private String errorMessage;
    private String errorDetails;
    private List<String> warnings;
    
    // Additional cleanup operations that may be needed
    private List<String> pendingCleanupTasks;
    private Map<String, Object> recommendedActions;
    
    // Response metadata
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant responseTimestamp;
    
    private Long responseTimeMs;
    private Map<String, Object> metadata;
    
    // Constructors
    public DeleteProviderResponse() {
        this.responseTimestamp = Instant.now();
        this.disconnectedAt = Instant.now();
    }

    public DeleteProviderResponse(String providerId, String providerName) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.responseTimestamp = Instant.now();
        this.disconnectedAt = Instant.now();
    }

    // Getters and Setters
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getOperationSuccess() {
        return operationSuccess;
    }

    public void setOperationSuccess(Boolean operationSuccess) {
        this.operationSuccess = operationSuccess;
    }

    public Boolean getTokensRevoked() {
        return tokensRevoked;
    }

    public void setTokensRevoked(Boolean tokensRevoked) {
        this.tokensRevoked = tokensRevoked;
    }

    public Boolean getDataCleared() {
        return dataCleared;
    }

    public void setDataCleared(Boolean dataCleared) {
        this.dataCleared = dataCleared;
    }

    public Boolean getConfigRemoved() {
        return configRemoved;
    }

    public void setConfigRemoved(Boolean configRemoved) {
        this.configRemoved = configRemoved;
    }

    public Boolean getAccountDisconnected() {
        return accountDisconnected;
    }

    public void setAccountDisconnected(Boolean accountDisconnected) {
        this.accountDisconnected = accountDisconnected;
    }

    public Map<String, Object> getCleanupSummary() {
        return cleanupSummary;
    }

    public void setCleanupSummary(Map<String, Object> cleanupSummary) {
        this.cleanupSummary = cleanupSummary;
    }

    public Integer getRecordsDeleted() {
        return recordsDeleted;
    }

    public void setRecordsDeleted(Integer recordsDeleted) {
        this.recordsDeleted = recordsDeleted;
    }

    public Integer getRecordsSkipped() {
        return recordsSkipped;
    }

    public void setRecordsSkipped(Integer recordsSkipped) {
        this.recordsSkipped = recordsSkipped;
    }

    public Integer getRecordsError() {
        return recordsError;
    }

    public void setRecordsError(Integer recordsError) {
        this.recordsError = recordsError;
    }

    public Map<String, Object> getPreservedData() {
        return preservedData;
    }

    public void setPreservedData(Map<String, Object> preservedData) {
        this.preservedData = preservedData;
    }

    public List<String> getPreservedDataTypes() {
        return preservedDataTypes;
    }

    public void setPreservedDataTypes(List<String> preservedDataTypes) {
        this.preservedDataTypes = preservedDataTypes;
    }

    public String getBackupId() {
        return backupId;
    }

    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public void setBackupLocation(String backupLocation) {
        this.backupLocation = backupLocation;
    }

    public Long getBackupSize() {
        return backupSize;
    }

    public void setBackupSize(Long backupSize) {
        this.backupSize = backupSize;
    }

    public Instant getBackupCreatedAt() {
        return backupCreatedAt;
    }

    public void setBackupCreatedAt(Instant backupCreatedAt) {
        this.backupCreatedAt = backupCreatedAt;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public Instant getOriginalConnectedAt() {
        return originalConnectedAt;
    }

    public void setOriginalConnectedAt(Instant originalConnectedAt) {
        this.originalConnectedAt = originalConnectedAt;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public Instant getDisconnectedAt() {
        return disconnectedAt;
    }

    public void setDisconnectedAt(Instant disconnectedAt) {
        this.disconnectedAt = disconnectedAt;
    }

    public Long getTotalConnectionDays() {
        return totalConnectionDays;
    }

    public void setTotalConnectionDays(Long totalConnectionDays) {
        this.totalConnectionDays = totalConnectionDays;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getPendingCleanupTasks() {
        return pendingCleanupTasks;
    }

    public void setPendingCleanupTasks(List<String> pendingCleanupTasks) {
        this.pendingCleanupTasks = pendingCleanupTasks;
    }

    public Map<String, Object> getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(Map<String, Object> recommendedActions) {
        this.recommendedActions = recommendedActions;
    }

    public Instant getResponseTimestamp() {
        return responseTimestamp;
    }

    public void setResponseTimestamp(Instant responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Helper methods
    public boolean isSuccess() {
        return "success".equals(status);
    }

    public boolean isFailed() {
        return "failed".equals(status);
    }

    public boolean isPartial() {
        return "partial".equals(status);
    }

    public boolean isNotFound() {
        return "not_found".equals(status);
    }

    public boolean isCompleteCleanup() {
        return Boolean.TRUE.equals(tokensRevoked) && 
               Boolean.TRUE.equals(dataCleared) && 
               Boolean.TRUE.equals(configRemoved) && 
               Boolean.TRUE.equals(accountDisconnected);
    }

    public boolean hasBackup() {
        return backupId != null;
    }

    public boolean hasPreservedData() {
        return preservedData != null && !preservedData.isEmpty();
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public boolean hasErrors() {
        return errorCode != null || errorMessage != null;
    }

    public boolean hasPendingTasks() {
        return pendingCleanupTasks != null && !pendingCleanupTasks.isEmpty();
    }

    public boolean hasRecommendedActions() {
        return recommendedActions != null && !recommendedActions.isEmpty();
    }

    // Convenience methods for compatibility
    public void setSuccess(boolean success) {
        this.operationSuccess = success;
        this.status = success ? "success" : "failed";
    }

    public boolean getSuccess() {
        return Boolean.TRUE.equals(operationSuccess);
    }

    public void setData(Map<String, Object> data) {
        this.metadata = data;
    }

    public Map<String, Object> getData() {
        return metadata;
    }

    @Override
    public String toString() {
        return "DeleteProviderResponse{" +
                "providerId='" + providerId + '\'' +
                ", providerName='" + providerName + '\'' +
                ", status='" + status + '\'' +
                ", operationSuccess=" + operationSuccess +
                ", isCompleteCleanup=" + isCompleteCleanup() +
                ", hasBackup=" + hasBackup() +
                ", hasPreservedData=" + hasPreservedData() +
                ", hasWarnings=" + hasWarnings() +
                ", hasErrors=" + hasErrors() +
                ", hasPendingTasks=" + hasPendingTasks() +
                ", responseTimestamp=" + responseTimestamp +
                '}';
    }
} 