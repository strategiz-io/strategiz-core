package io.strategiz.data.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a device identity for web crypto authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
        return DeviceIdentity.builder()
                .id((String) map.get("id"))
                .userId((String) map.get("userId"))
                .deviceId((String) map.get("deviceId"))
                .publicKey((String) map.get("publicKey"))
                .name((String) map.get("name"))
                .deviceInfo((Map<String, Object>) map.get("deviceInfo"))
                .createdAt(((Number) map.get("createdAt")).longValue())
                .lastUsedAt(((Number) map.get("lastUsedAt")).longValue())
                .trusted((Boolean) map.get("trusted"))
                .build();
    }
}
