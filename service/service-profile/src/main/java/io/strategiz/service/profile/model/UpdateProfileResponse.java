package io.strategiz.service.profile.model;

import io.strategiz.service.profile.constants.ProfileConstants;

/**
 * Response DTO for profile update operations
 */
public class UpdateProfileResponse {
    private String userId;
    private String message;
    private boolean success;
    private long updatedAt;

    // Default constructor
    public UpdateProfileResponse() {
    }

    // Constructor
    public UpdateProfileResponse(String userId, String message, boolean success, long updatedAt) {
        this.userId = userId;
        this.message = message;
        this.success = success;
        this.updatedAt = updatedAt;
    }

    // Static factory methods for common responses
    public static UpdateProfileResponse success(String userId, String message) {
        return new UpdateProfileResponse(userId, message, ProfileConstants.Defaults.IS_ACTIVE, System.currentTimeMillis());
    }

    public static UpdateProfileResponse failure(String userId, String message) {
        return new UpdateProfileResponse(userId, message, !ProfileConstants.Defaults.IS_ACTIVE, System.currentTimeMillis());
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UpdateProfileResponse{" +
                "userId='" + userId + '\'' +
                ", message='" + message + '\'' +
                ", success=" + success +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 