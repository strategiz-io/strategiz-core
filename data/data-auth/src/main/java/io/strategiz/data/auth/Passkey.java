package io.strategiz.data.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing a passkey for web authentication
 */
public class Passkey {
    private String id;
    private String userId;
    private String credentialId;
    private String publicKey;
    private String name;
    private Map<String, Object> metadata;
    private long createdAt;
    private long lastUsedAt;
    private boolean trusted;

    // Constructors
    public Passkey() {
    }

    public Passkey(String id, String userId, String credentialId, String publicKey, String name, 
                   Map<String, Object> metadata, long createdAt, long lastUsedAt, boolean trusted) {
        this.id = id;
        this.userId = userId;
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.name = name;
        this.metadata = metadata;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
    public void updateLastUsed() {
        this.lastUsedAt = System.currentTimeMillis();
    }

    /**
     * Converts the passkey to a map representation
     * 
     * @return Map representation of the passkey
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("credentialId", credentialId);
        map.put("publicKey", publicKey);
        map.put("name", name);
        map.put("metadata", metadata != null ? metadata : new HashMap<>());
        map.put("createdAt", createdAt);
        map.put("lastUsedAt", lastUsedAt);
        map.put("trusted", trusted);
        return map;
    }

    /**
     * Creates a passkey from a map representation
     * 
     * @param map Map representation of the passkey
     * @return Passkey instance
     */
    @SuppressWarnings("unchecked")
    public static Passkey fromMap(Map<String, Object> map) {
        return new PasskeyBuilder()
                .id((String) map.get("id"))
                .userId((String) map.get("userId"))
                .credentialId((String) map.get("credentialId"))
                .publicKey((String) map.get("publicKey"))
                .name((String) map.get("name"))
                .metadata((Map<String, Object>) map.getOrDefault("metadata", new HashMap<>()))
                .createdAt(map.get("createdAt") instanceof Long ? (Long) map.get("createdAt") : 0L)
                .lastUsedAt(map.get("lastUsedAt") instanceof Long ? (Long) map.get("lastUsedAt") : 0L)
                .trusted(map.get("trusted") instanceof Boolean ? (Boolean) map.get("trusted") : false)
                .build();
    }

    // Builder pattern
    public static PasskeyBuilder builder() {
        return new PasskeyBuilder();
    }

    public static class PasskeyBuilder {
        private String id;
        private String userId;
        private String credentialId;
        private String publicKey;
        private String name;
        private Map<String, Object> metadata;
        private long createdAt;
        private long lastUsedAt;
        private boolean trusted;

        public PasskeyBuilder id(String id) {
            this.id = id;
            return this;
        }

        public PasskeyBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public PasskeyBuilder credentialId(String credentialId) {
            this.credentialId = credentialId;
            return this;
        }

        public PasskeyBuilder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public PasskeyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PasskeyBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public PasskeyBuilder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PasskeyBuilder lastUsedAt(long lastUsedAt) {
            this.lastUsedAt = lastUsedAt;
            return this;
        }

        public PasskeyBuilder trusted(boolean trusted) {
            this.trusted = trusted;
            return this;
        }

        public Passkey build() {
            return new Passkey(id, userId, credentialId, publicKey, name, metadata, createdAt, lastUsedAt, trusted);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passkey passkey = (Passkey) o;
        return createdAt == passkey.createdAt &&
                lastUsedAt == passkey.lastUsedAt &&
                trusted == passkey.trusted &&
                Objects.equals(id, passkey.id) &&
                Objects.equals(userId, passkey.userId) &&
                Objects.equals(credentialId, passkey.credentialId) &&
                Objects.equals(publicKey, passkey.publicKey) &&
                Objects.equals(name, passkey.name) &&
                Objects.equals(metadata, passkey.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, credentialId, publicKey, name, metadata, createdAt, lastUsedAt, trusted);
    }

    @Override
    public String toString() {
        return "Passkey{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", credentialId='" + credentialId + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", name='" + name + '\'' +
                ", metadata=" + metadata +
                ", createdAt=" + createdAt +
                ", lastUsedAt=" + lastUsedAt +
                ", trusted=" + trusted +
                '}';
    }
}
