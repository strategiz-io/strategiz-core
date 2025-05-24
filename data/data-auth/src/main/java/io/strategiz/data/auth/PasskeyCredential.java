package io.strategiz.data.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a passkey credential
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
