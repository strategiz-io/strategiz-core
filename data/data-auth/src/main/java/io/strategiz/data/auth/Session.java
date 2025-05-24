package io.strategiz.data.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a user session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    
    private String id;
    private String userId;
    private String token;
    private long createdAt;
    private long expiresAt;
    private long lastAccessedAt;
    
    /**
     * Checks if the session is expired
     * 
     * @return true if the session is expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt < Instant.now().getEpochSecond();
    }
    
    /**
     * Updates the last accessed time to now
     */
    public void updateLastAccessedTime() {
        this.lastAccessedAt = Instant.now().getEpochSecond();
    }
    
    /**
     * Extends the session expiry time
     * 
     * @param seconds The number of seconds to extend the session by
     */
    public void extendExpiry(long seconds) {
        this.expiresAt = Instant.now().getEpochSecond() + seconds;
    }
    
    /**
     * Converts the session to a map for storage
     * 
     * @return Map representation of the session
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("token", token);
        map.put("createdAt", createdAt);
        map.put("expiresAt", expiresAt);
        map.put("lastAccessedAt", lastAccessedAt);
        return map;
    }
    
    /**
     * Creates a session from a map (e.g., from Firestore)
     * 
     * @param map Map containing session data
     * @return Session object
     */
    public static Session fromMap(Map<String, Object> map) {
        return Session.builder()
                .id((String) map.get("id"))
                .userId((String) map.get("userId"))
                .token((String) map.get("token"))
                .createdAt(((Number) map.get("createdAt")).longValue())
                .expiresAt(((Number) map.get("expiresAt")).longValue())
                .lastAccessedAt(((Number) map.get("lastAccessedAt")).longValue())
                .build();
    }
}
