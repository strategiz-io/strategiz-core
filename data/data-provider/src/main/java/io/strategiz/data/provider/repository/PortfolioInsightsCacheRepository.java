package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioInsightsCacheEntity;
import io.strategiz.data.provider.exception.DataProviderErrorDetails;
import io.strategiz.data.provider.exception.ProviderIntegrationException;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Repository for caching AI portfolio insights.
 * Path: users/{userId}/portfolio/insights/cached
 *
 * Cache Strategy:
 * - Return cached insights immediately if valid (< 24 hours old, not invalidated)
 * - Invalidate cache when provider data changes (sync, connect, disconnect)
 * - TTL: 24 hours as fallback
 */
@Repository
public class PortfolioInsightsCacheRepository {

    private static final Logger log = LoggerFactory.getLogger(PortfolioInsightsCacheRepository.class);

    /**
     * Cache TTL in hours
     */
    private static final int CACHE_TTL_HOURS = 24;

    /**
     * Document ID for the cached insights
     */
    private static final String CACHE_DOCUMENT_ID = "cached";

    private final Firestore firestore;

    public PortfolioInsightsCacheRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Get cached insights for a user
     * @param userId User ID
     * @return Optional containing cached insights if valid, empty if not found or invalid
     */
    public Optional<PortfolioInsightsCacheEntity> getCachedInsights(String userId) {
        try {
            DocumentSnapshot doc = firestore.collection("users")
                    .document(userId)
                    .collection("portfolio")
                    .document("insights")
                    .collection("data")
                    .document(CACHE_DOCUMENT_ID)
                    .get()
                    .get();

            if (!doc.exists()) {
                log.debug("No cached insights found for user: {}", userId);
                return Optional.empty();
            }

            PortfolioInsightsCacheEntity cache = doc.toObject(PortfolioInsightsCacheEntity.class);
            if (cache == null) {
                log.warn("Failed to deserialize cached insights for user: {}", userId);
                return Optional.empty();
            }

            // Check if cache is valid
            if (!cache.isCacheValid()) {
                log.info("Cached insights expired or invalidated for user: {}", userId);
                return Optional.empty();
            }

            log.info("Returning valid cached insights for user: {}, expires: {}",
                    userId, cache.getExpiresAt());
            return Optional.of(cache);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioInsightsCache", userId);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioInsightsCache", userId);
        }
    }

    /**
     * Save insights to cache
     * @param userId User ID
     * @param cache Cache entity with insights
     */
    public void saveCache(String userId, PortfolioInsightsCacheEntity cache) {
        try {
            // Set timestamps
            Timestamp now = Timestamp.now();
            cache.setGeneratedAt(now);
            cache.setExpiresAt(Timestamp.ofTimeSecondsAndNanos(
                    now.getSeconds() + TimeUnit.HOURS.toSeconds(CACHE_TTL_HOURS), 0));
            cache.setIsValid(true);
            cache._initAudit(userId);

            firestore.collection("users")
                    .document(userId)
                    .collection("portfolio")
                    .document("insights")
                    .collection("data")
                    .document(CACHE_DOCUMENT_ID)
                    .set(cache)
                    .get();

            log.info("Saved insights cache for user: {}, expires: {}", userId, cache.getExpiresAt());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_SAVE_FAILED, e, "PortfolioInsightsCache");
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_SAVE_FAILED, e, "PortfolioInsightsCache");
        }
    }

    /**
     * Invalidate cache for a user (called when provider data changes)
     * @param userId User ID
     */
    public void invalidateCache(String userId) {
        try {
            DocumentSnapshot doc = firestore.collection("users")
                    .document(userId)
                    .collection("portfolio")
                    .document("insights")
                    .collection("data")
                    .document(CACHE_DOCUMENT_ID)
                    .get()
                    .get();

            if (doc.exists()) {
                // Mark as invalid instead of deleting (preserves history)
                firestore.collection("users")
                        .document(userId)
                        .collection("portfolio")
                        .document("insights")
                        .collection("data")
                        .document(CACHE_DOCUMENT_ID)
                        .update("isValid", false)
                        .get();

                log.info("Invalidated insights cache for user: {}", userId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to invalidate cache for user {}: {}", userId, e.getMessage());
        } catch (ExecutionException e) {
            log.error("Failed to invalidate cache for user {}: {}", userId, e.getMessage());
            // Don't throw - cache invalidation failure shouldn't break main flow
        }
    }

    /**
     * Delete cache for a user (hard delete)
     * @param userId User ID
     */
    public void deleteCache(String userId) {
        try {
            firestore.collection("users")
                    .document(userId)
                    .collection("portfolio")
                    .document("insights")
                    .collection("data")
                    .document(CACHE_DOCUMENT_ID)
                    .delete()
                    .get();

            log.info("Deleted insights cache for user: {}", userId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to delete cache for user {}: {}", userId, e.getMessage());
        } catch (ExecutionException e) {
            log.error("Failed to delete cache for user {}: {}", userId, e.getMessage());
        }
    }
}
