package io.strategiz.data.auth;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing a device identity for web crypto authentication
 */
public class DeviceIdentity {
    private String id;
    private String userId;
    private String deviceId;
    private String publicKey;
    private String name;
    private Map<String, Object> deviceInfo;
    private long createdAt;
    private long lastUsedAt;
    private boolean trusted;

    // Constructors
    public DeviceIdentity() {
    }

    public DeviceIdentity(String id, String userId, String deviceId, String publicKey, String name, 
                         Map<String, Object> deviceInfo, long createdAt, long lastUsedAt, boolean trusted) {
        this.id = id;
        this.userId = userId;
        this.deviceId = deviceId;
        this.publicKey = publicKey;
        this.name = name;
        this.deviceInfo = deviceInfo;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.trusted = trusted;
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(Map<String, Object> deviceInfo) {
        this.deviceInfo = deviceInfo;
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

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    /**
     * Updates the last used timestamp to the current time
     */
    public void updateLastUsedTime() {
        this.lastUsedAt = Instant.now().getEpochSecond();
    }
    
    /**
     * Converts the device identity to a map for storage
     * 
     * @return Map representation of the device identity
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("deviceId", deviceId);
        map.put("publicKey", publicKey);
        map.put("name", name);
        map.put("deviceInfo", deviceInfo);
        map.put("createdAt", createdAt);
        map.put("lastUsedAt", lastUsedAt);
        map.put("trusted", trusted);
        return map;
    }
    
    /**
     * Creates a device identity from a map (e.g., from Firestore)
     * 
     * @param map Map containing device identity data
     * @return DeviceIdentity object
     */
    public static DeviceIdentity fromMap(Map<String, Object> map) {
        return new DeviceIdentity(
                (String) map.get("id"),
                (String) map.get("userId"),
                (String) map.get("deviceId"),
                (String) map.get("publicKey"),
                (String) map.get("name"),
                (Map<String, Object>) map.get("deviceInfo"),
                ((Number) map.get("createdAt")).longValue(),
                ((Number) map.get("lastUsedAt")).longValue(),
                (Boolean) map.get("trusted")
        );
    }

    // Builder pattern
    public static DeviceIdentityBuilder builder() {
        return new DeviceIdentityBuilder();
    }

    public static class DeviceIdentityBuilder {
        private String id;
        private String userId;
        private String deviceId;
        private String publicKey;
        private String name;
        private Map<String, Object> deviceInfo;
        private long createdAt;
        private long lastUsedAt;
        private boolean trusted;

        public DeviceIdentityBuilder id(String id) {
            this.id = id;
            return this;
        }

        public DeviceIdentityBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public DeviceIdentityBuilder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public DeviceIdentityBuilder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public DeviceIdentityBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DeviceIdentityBuilder deviceInfo(Map<String, Object> deviceInfo) {
            this.deviceInfo = deviceInfo;
            return this;
        }

        public DeviceIdentityBuilder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public DeviceIdentityBuilder lastUsedAt(long lastUsedAt) {
            this.lastUsedAt = lastUsedAt;
            return this;
        }

        public DeviceIdentityBuilder trusted(boolean trusted) {
            this.trusted = trusted;
            return this;
        }

        public DeviceIdentity build() {
            return new DeviceIdentity(id, userId, deviceId, publicKey, name, deviceInfo, 
                                    createdAt, lastUsedAt, trusted);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceIdentity that = (DeviceIdentity) o;
        return createdAt == that.createdAt &&
                lastUsedAt == that.lastUsedAt &&
                trusted == that.trusted &&
                Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(deviceId, that.deviceId) &&
                Objects.equals(publicKey, that.publicKey) &&
                Objects.equals(name, that.name) &&
                Objects.equals(deviceInfo, that.deviceInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, deviceId, publicKey, name, deviceInfo, 
                          createdAt, lastUsedAt, trusted);
    }

    @Override
    public String toString() {
        return "DeviceIdentity{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", name='" + name + '\'' +
                ", deviceInfo=" + deviceInfo +
                ", createdAt=" + createdAt +
                ", lastUsedAt=" + lastUsedAt +
                ", trusted=" + trusted +
                '}';
    }
}
