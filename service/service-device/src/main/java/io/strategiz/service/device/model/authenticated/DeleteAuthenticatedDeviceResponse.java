package io.strategiz.service.device.model.authenticated;

/**
 * Response model for deleting authenticated device
 */
public class DeleteAuthenticatedDeviceResponse {
    private String deviceId;
    private String userId;
    private Boolean deleted;
    private Long deletedAt;
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Boolean getDeleted() {
        return deleted;
    }
    
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
    
    public Long getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(Long deletedAt) {
        this.deletedAt = deletedAt;
    }
}