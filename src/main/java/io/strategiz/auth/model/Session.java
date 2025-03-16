package io.strategiz.auth.model;

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
     * Converts the session to a Firestore document map
     * 
     * @return Map representation of the session
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("token", token);
        map.put("createdAt", createdAt);
        map.put("expiresAt", expiresAt);
        map.put("lastAccessedAt", lastAccessedAt);
        return map;
    }
    
    /**
     * Creates a session from a Firestore document
     * 
     * @param id The session ID
     * @param data The document data
     * @return A new Session instance
     */
    public static Session fromMap(String id, Map<String, Object> data) {
        return Session.builder()
                .id(id)
                .userId((String) data.get("userId"))
                .token((String) data.get("token"))
                .createdAt(((Number) data.get("createdAt")).longValue())
                .expiresAt(((Number) data.get("expiresAt")).longValue())
                .lastAccessedAt(((Number) data.get("lastAccessedAt")).longValue())
                .build();
    }
}
