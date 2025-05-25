package io.strategiz.data.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a passkey for web authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
        return Passkey.builder()
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
}
