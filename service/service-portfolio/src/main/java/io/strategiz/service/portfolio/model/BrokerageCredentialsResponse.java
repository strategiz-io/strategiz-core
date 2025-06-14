package io.strategiz.service.portfolio.model;

import java.util.Map;
import java.util.Objects;

/**
 * Service model for brokerage credentials.
 */
public class BrokerageCredentialsResponse {
    
    private String userId;
    private String provider;
    private Map<String, String> credentials;
    private boolean active;
    private String lastUpdated;
    private boolean success;
    private String errorMessage;

    public BrokerageCredentialsResponse() {
    }

    public BrokerageCredentialsResponse(String userId, String provider, Map<String, String> credentials, 
                                      boolean active, String lastUpdated, boolean success, String errorMessage) {
        this.userId = userId;
        this.provider = provider;
        this.credentials = credentials;
        this.active = active;
        this.lastUpdated = lastUpdated;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokerageCredentialsResponse that = (BrokerageCredentialsResponse) o;
        return active == that.active &&
               success == that.success &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(provider, that.provider) &&
               Objects.equals(credentials, that.credentials) &&
               Objects.equals(lastUpdated, that.lastUpdated) &&
               Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, provider, credentials, active, lastUpdated, success, errorMessage);
    }

    @Override
    public String toString() {
        return "BrokerageCredentialsResponse{" +
               "userId='" + userId + '\'' +
               ", provider='" + provider + '\'' +
               ", credentials=" + credentials +
               ", active=" + active +
               ", lastUpdated='" + lastUpdated + '\'' +
               ", success=" + success +
               ", errorMessage='" + errorMessage + '\'' +
               '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private String provider;
        private Map<String, String> credentials;
        private boolean active;
        private String lastUpdated;
        private boolean success;
        private String errorMessage;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder credentials(Map<String, String> credentials) {
            this.credentials = credentials;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder lastUpdated(String lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public BrokerageCredentialsResponse build() {
            return new BrokerageCredentialsResponse(userId, provider, credentials, active, lastUpdated, success, errorMessage);
        }
    }
}
