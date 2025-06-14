package io.strategiz.data.auth;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing a passkey credential
 */
public class PasskeyCredential {
    private String id;
    private String userId;
    private String credentialId;
    private String publicKey;
    private String attestationObject;
    private String clientDataJSON;
    private long createdAt;
    private long lastUsedAt;
    private String userAgent;
    private String deviceName;

    // Constructors
    public PasskeyCredential() {
    }

    public PasskeyCredential(String id, String userId, String credentialId, String publicKey, 
                           String attestationObject, String clientDataJSON, long createdAt, 
                           long lastUsedAt, String userAgent, String deviceName) {
        this.id = id;
        this.userId = userId;
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.attestationObject = attestationObject;
        this.clientDataJSON = clientDataJSON;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.userAgent = userAgent;
        this.deviceName = deviceName;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAttestationObject() {
        return attestationObject;
    }

    public void setAttestationObject(String attestationObject) {
        this.attestationObject = attestationObject;
    }

    public String getClientDataJSON() {
        return clientDataJSON;
    }

    public void setClientDataJSON(String clientDataJSON) {
        this.clientDataJSON = clientDataJSON;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(long lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * Updates the last used timestamp to the current time
     */
    public void updateLastUsedTime() {
        this.lastUsedAt = Instant.now().getEpochSecond();
    }
    
    /**
     * Converts the passkey credential to a map for storage
     * 
     * @return Map representation of the passkey credential
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("credentialId", credentialId);
        map.put("publicKey", publicKey);
        map.put("attestationObject", attestationObject);
        map.put("clientDataJSON", clientDataJSON);
        map.put("createdAt", createdAt);
        map.put("lastUsedAt", lastUsedAt);
        map.put("userAgent", userAgent);
        map.put("deviceName", deviceName);
        return map;
    }
    
    /**
     * Creates a passkey credential from a map (e.g., from Firestore)
     * 
     * @param map Map containing passkey credential data
     * @return PasskeyCredential object
     */
    public static PasskeyCredential fromMap(Map<String, Object> map) {
        return PasskeyCredential.builder()
                .id((String) map.get("id"))
                .userId((String) map.get("userId"))
                .credentialId((String) map.get("credentialId"))
                .publicKey((String) map.get("publicKey"))
                .attestationObject((String) map.get("attestationObject"))
                .clientDataJSON((String) map.get("clientDataJSON"))
                .createdAt(((Number) map.get("createdAt")).longValue())
                .lastUsedAt(((Number) map.get("lastUsedAt")).longValue())
                .userAgent((String) map.get("userAgent"))
                .deviceName((String) map.get("deviceName"))
                .build();
    }

    // Builder pattern
    public static PasskeyCredentialBuilder builder() {
        return new PasskeyCredentialBuilder();
    }

    public static class PasskeyCredentialBuilder {
        private String id;
        private String userId;
        private String credentialId;
        private String publicKey;
        private String attestationObject;
        private String clientDataJSON;
        private long createdAt;
        private long lastUsedAt;
        private String userAgent;
        private String deviceName;

        public PasskeyCredentialBuilder id(String id) {
            this.id = id;
            return this;
        }

        public PasskeyCredentialBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public PasskeyCredentialBuilder credentialId(String credentialId) {
            this.credentialId = credentialId;
            return this;
        }

        public PasskeyCredentialBuilder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public PasskeyCredentialBuilder attestationObject(String attestationObject) {
            this.attestationObject = attestationObject;
            return this;
        }

        public PasskeyCredentialBuilder clientDataJSON(String clientDataJSON) {
            this.clientDataJSON = clientDataJSON;
            return this;
        }

        public PasskeyCredentialBuilder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PasskeyCredentialBuilder lastUsedAt(long lastUsedAt) {
            this.lastUsedAt = lastUsedAt;
            return this;
        }

        public PasskeyCredentialBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public PasskeyCredentialBuilder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public PasskeyCredential build() {
            return new PasskeyCredential(id, userId, credentialId, publicKey, attestationObject, 
                                       clientDataJSON, createdAt, lastUsedAt, userAgent, deviceName);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasskeyCredential that = (PasskeyCredential) o;
        return createdAt == that.createdAt &&
                lastUsedAt == that.lastUsedAt &&
                Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(credentialId, that.credentialId) &&
                Objects.equals(publicKey, that.publicKey) &&
                Objects.equals(attestationObject, that.attestationObject) &&
                Objects.equals(clientDataJSON, that.clientDataJSON) &&
                Objects.equals(userAgent, that.userAgent) &&
                Objects.equals(deviceName, that.deviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, credentialId, publicKey, attestationObject, 
                          clientDataJSON, createdAt, lastUsedAt, userAgent, deviceName);
    }

    @Override
    public String toString() {
        return "PasskeyCredential{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", credentialId='" + credentialId + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", attestationObject='" + attestationObject + '\'' +
                ", clientDataJSON='" + clientDataJSON + '\'' +
                ", createdAt=" + createdAt +
                ", lastUsedAt=" + lastUsedAt +
                ", userAgent='" + userAgent + '\'' +
                ", deviceName='" + deviceName + '\'' +
                '}';
    }
}
