package io.strategiz.framework.secrets.controller;

import java.time.Duration;
import java.util.Optional;

/**
 * Interface for caching secret values. Provides abstraction for different caching
 * strategies.
 */
public interface SecretCache {

	/**
	 * Get a cached secret value.
	 * @param key The secret key
	 * @return The cached value if present and not expired
	 */
	Optional<String> get(String key);

	/**
	 * Store a secret value in cache.
	 * @param key The secret key
	 * @param value The secret value
	 * @param ttl Time to live for the cached value
	 */
	void put(String key, String value, Duration ttl);

	/**
	 * Remove a secret from cache.
	 * @param key The secret key
	 */
	void evict(String key);

	/** Clear all cached secrets. */
	void evictAll();

}
