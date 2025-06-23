package io.strategiz.service.auth.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Model representing a user session
 */
public class Session {
    
    private String id;
    private String userId;
    private String token;
    private long createdAt;
    private long lastAccessedAt;
    private long expiresAt;
    
    /**
     * Default constructor
     */
    public Session() {
    }
    
    /**
     * Constructor with all fields
     */
    public Session(String id, String userId, String token, long createdAt, long lastAccessedAt, long expiresAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.createdAt = createdAt;
        this.lastAccessedAt = lastAccessedAt;
        this.expiresAt = expiresAt;
    }
    
    /**
     * Builder pattern implementation for Session
     */
    public static SessionBuilder builder() {
        return new SessionBuilder();
    }
    
    public static class SessionBuilder {
        private String id;
        private String userId;
        private String token;
        private long createdAt;
        private long lastAccessedAt;
        private long expiresAt;
        
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
        
        public SessionBuilder lastAccessedAt(long lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
            return this;
        }
        
        public SessionBuilder expiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public Session build() {
            return new Session(id, userId, token, createdAt, lastAccessedAt, expiresAt);
        }
    }
    
    /**
     * Updates the last accessed time to current time
     */
    public void updateLastAccessedTime() {
        this.lastAccessedAt = Instant.now().getEpochSecond();
    }
    
    /**
     * Extends the session expiry by the specified period
     *
     * @param seconds Number of seconds to extend
     */
    public void extendExpiry(long seconds) {
        this.expiresAt = Instant.now().getEpochSecond() + seconds;
    }
    
    /**
     * Checks if the session is expired
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().getEpochSecond() > this.expiresAt;
    }
    
    // Getters and setters
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
    
    public long getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(long lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return createdAt == session.createdAt &&
               lastAccessedAt == session.lastAccessedAt &&
               expiresAt == session.expiresAt &&
               Objects.equals(id, session.id) &&
               Objects.equals(userId, session.userId) &&
               Objects.equals(token, session.token);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, userId, token, createdAt, lastAccessedAt, expiresAt);
    }
    
    @Override
    public String toString() {
        return "Session{" +
               "id='" + id + '\'' +
               ", userId='" + userId + '\'' +
               ", token='" + token + '\'' +
               ", createdAt=" + createdAt +
               ", lastAccessedAt=" + lastAccessedAt +
               ", expiresAt=" + expiresAt +
               '}';
    }
}
