package io.strategiz.business.historicalinsights.service;

import io.strategiz.business.historicalinsights.model.SymbolInsights;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caching service for historical insights to avoid recomputing analysis for the same symbol.
 * Uses in-memory cache with 24-hour TTL and scheduled cleanup.
 */
@Service
public class HistoricalInsightsCacheService {

	private static final Logger log = LoggerFactory.getLogger(HistoricalInsightsCacheService.class);

	private static final int TTL_HOURS = 24;

	private static final int MAX_CACHE_SIZE = 1000;

	private final Map<String, CachedInsights> cache = new ConcurrentHashMap<>();

	/**
	 * Get cached insights for a symbol if available and not stale.
	 * @param cacheKey Composite key: symbol:timeframe:fundamentals
	 * @return Optional containing insights if cache hit, empty if miss or stale
	 */
	public Optional<SymbolInsights> getCachedInsights(String cacheKey) {
		CachedInsights cached = cache.get(cacheKey);

		if (cached == null) {
			log.debug("Cache miss for key: {}", cacheKey);
			return Optional.empty();
		}

		if (cached.isStale()) {
			log.debug("Cache hit but stale for key: {}", cacheKey);
			cache.remove(cacheKey);
			return Optional.empty();
		}

		log.info("Cache hit for key: {}", cacheKey);
		return Optional.of(cached.insights);
	}

	/**
	 * Cache insights with automatic TTL expiration.
	 * @param cacheKey Composite key: symbol:timeframe:fundamentals
	 * @param insights SymbolInsights to cache
	 */
	public void cacheInsights(String cacheKey, SymbolInsights insights) {
		// Evict oldest if cache is full
		if (cache.size() >= MAX_CACHE_SIZE) {
			evictOldest();
		}

		CachedInsights cached = new CachedInsights(insights, Instant.now());
		cache.put(cacheKey, cached);

		log.info("Cached insights for key: {} (cache size: {})", cacheKey, cache.size());
	}

	/**
	 * Invalidate (remove) cached insights for a specific key.
	 * @param cacheKey Key to invalidate
	 */
	public void invalidate(String cacheKey) {
		CachedInsights removed = cache.remove(cacheKey);
		if (removed != null) {
			log.info("Invalidated cache for key: {}", cacheKey);
		}
	}

	/**
	 * Clear entire cache.
	 */
	public void clearAll() {
		int size = cache.size();
		cache.clear();
		log.info("Cleared entire cache ({} entries removed)", size);
	}

	/**
	 * Get current cache size.
	 */
	public int getCacheSize() {
		return cache.size();
	}

	/**
	 * Scheduled cleanup job: Remove stale entries every hour.
	 */
	@Scheduled(fixedRate = 3600000) // Every hour
	public void cleanupStaleEntries() {
		int removed = 0;

		for (Map.Entry<String, CachedInsights> entry : cache.entrySet()) {
			if (entry.getValue().isStale()) {
				cache.remove(entry.getKey());
				removed++;
			}
		}

		if (removed > 0) {
			log.info("Cleanup: Removed {} stale cache entries (cache size now: {})", removed, cache.size());
		}
	}

	/**
	 * Evict oldest entry when cache is full.
	 */
	private void evictOldest() {
		String oldestKey = null;
		Instant oldestTime = Instant.now();

		for (Map.Entry<String, CachedInsights> entry : cache.entrySet()) {
			if (entry.getValue().cachedAt.isBefore(oldestTime)) {
				oldestTime = entry.getValue().cachedAt;
				oldestKey = entry.getKey();
			}
		}

		if (oldestKey != null) {
			cache.remove(oldestKey);
			log.debug("Evicted oldest cache entry: {}", oldestKey);
		}
	}

	/**
	 * Internal class to hold cached insights with timestamp.
	 */
	private static class CachedInsights {

		final SymbolInsights insights;

		final Instant cachedAt;

		CachedInsights(SymbolInsights insights, Instant cachedAt) {
			this.insights = insights;
			this.cachedAt = cachedAt;
		}

		boolean isStale() {
			Instant expirationTime = cachedAt.plus(TTL_HOURS, ChronoUnit.HOURS);
			return Instant.now().isAfter(expirationTime);
		}

	}

}
