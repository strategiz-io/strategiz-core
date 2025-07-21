package io.strategiz.framework.secrets.cache;

import io.strategiz.framework.secrets.controller.SecretCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Default in-memory cache implementation for secrets.
 * Thread-safe implementation using ConcurrentHashMap.
 */
@Service
public class SecretCacheService implements SecretCache {
    
    private static final Logger log = LoggerFactory.getLogger(SecretCacheService.class);
    
    private final Map<String, CachedSecret> cache = new ConcurrentHashMap<>();
    
    @Override
    public Optional<String> get(String key) {
        CachedSecret cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for key: {}", key);
            return Optional.of(cached.getValue());
        }
        
        // Remove expired entry
        if (cached != null) {
            cache.remove(key);
        }
        
        log.debug("Cache miss for key: {}", key);
        return Optional.empty();
    }
    
    @Override
    public void put(String key, String value, Duration ttl) {
        Instant expiryTime = Instant.now().plus(ttl);
        cache.put(key, new CachedSecret(value, expiryTime));
        log.debug("Cached secret for key: {} with TTL: {}", key, ttl);
    }
    
    @Override
    public void evict(String key) {
        cache.remove(key);
        log.debug("Evicted secret for key: {}", key);
    }
    
    @Override
    public void evictAll() {
        cache.clear();
        log.debug("Evicted all cached secrets");
    }
    
    /**
     * Helper class to store cached secrets with expiry
     */
    private static class CachedSecret {
        private final String value;
        private final Instant expiryTime;
        
        public CachedSecret(String value, Instant expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        public String getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }
    }
}