package io.strategiz.data.auth;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model class representing a user session
 */
public class Session {
    
    private String id;
    private String userId;
    private String token;
    private long createdAt;
    private long expiresAt;
    private long lastAccessedAt;
    
    // Constructors
    public Session() {
    }
    
    public Session(String id, String userId, String token, long createdAt, long expiresAt, long lastAccessedAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastAccessedAt = lastAccessedAt;
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
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public long getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(long lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
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
        return new SessionBuilder()
                .id((String) map.get("id"))
                .userId((String) map.get("userId"))
                .token((String) map.get("token"))
                .createdAt(((Number) map.get("createdAt")).longValue())
                .expiresAt(((Number) map.get("expiresAt")).longValue())
                .lastAccessedAt(((Number) map.get("lastAccessedAt")).longValue())
                .build();
    }
    
    // Builder pattern
    public static SessionBuilder builder() {
        return new SessionBuilder();
    }
    
    public static class SessionBuilder {
        private String id;
        private String userId;
        private String token;
        private long createdAt;
        private long expiresAt;
        private long lastAccessedAt;
        
        public SessionBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public SessionBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public SessionBuilder token(String token) {
            this.token = token;
            return this;
        }
        
        public SessionBuilder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public SessionBuilder expiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public SessionBuilder lastAccessedAt(long lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
            return this;
        }
        
        public Session build() {
            return new Session(id, userId, token, createdAt, expiresAt, lastAccessedAt);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return createdAt == session.createdAt &&
                expiresAt == session.expiresAt &&
                lastAccessedAt == session.lastAccessedAt &&
                Objects.equals(id, session.id) &&
                Objects.equals(userId, session.userId) &&
                Objects.equals(token, session.token);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, userId, token, createdAt, expiresAt, lastAccessedAt);
    }
    
    @Override
    public String toString() {
        return "Session{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", token='" + token + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", lastAccessedAt=" + lastAccessedAt +
                '}';
    }
}
