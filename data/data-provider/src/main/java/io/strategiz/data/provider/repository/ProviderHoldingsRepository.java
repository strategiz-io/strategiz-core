package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderHoldingsEntity;
import io.strategiz.data.provider.exception.DataProviderErrorDetails;
import io.strategiz.data.provider.exception.ProviderIntegrationException;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Repository for ProviderHoldingsEntity.
 * Handles holdings subcollection: users/{userId}/portfolio/{providerId}/holdings/current
 *
 * Example: users/{userId}/portfolio/coinbase/holdings/current
 *
 * This repository manages the heavy data (positions, balances, transactions) that is
 * stored separately from the lightweight provider status document.
 */
@Repository
public class ProviderHoldingsRepository {

    private static final Logger log = LoggerFactory.getLogger(ProviderHoldingsRepository.class);
    private static final String HOLDINGS_DOC_ID = "current"; // Fixed document ID

    private final Firestore firestore;

    public ProviderHoldingsRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Get the holdings collection reference for a specific provider.
     * Path: users/{userId}/portfolio/{providerId}/holdings
     *
     * Holdings are stored as a subcollection under the provider document.
     * Example: users/{userId}/portfolio/coinbase/holdings/current
     */
    private CollectionReference getHoldingsCollection(String userId, String providerId) {
        return firestore.collection("users")
                .document(userId)
                .collection("portfolio")
                .document(providerId)
                .collection("holdings");
    }

    /**
     * Save holdings for a provider.
     * Uses fixed document ID "current" - there's only one holdings doc per provider.
     */
    public ProviderHoldingsEntity save(String userId, String providerId, ProviderHoldingsEntity entity) {
        try {
            validateInputs(entity, userId, providerId);

            boolean isCreate = !entity._hasAudit();

            if (isCreate) {
                entity._initAudit(userId);
            } else {
                entity._updateAudit(userId);
            }

            entity.setId(HOLDINGS_DOC_ID);
            entity.setProviderId(providerId);

            getHoldingsCollection(userId, providerId).document(HOLDINGS_DOC_ID).set(entity).get();

            log.debug("Saved ProviderHoldingsEntity for provider {} by user {}", providerId, userId);
            return entity;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_SAVE_FAILED, e, "ProviderHoldingsEntity");
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_SAVE_FAILED, e, "ProviderHoldingsEntity");
        }
    }

    /**
     * Find holdings for a provider.
     */
    public Optional<ProviderHoldingsEntity> findByUserIdAndProviderId(String userId, String providerId) {
        try {
            DocumentSnapshot doc = getHoldingsCollection(userId, providerId)
                    .document(HOLDINGS_DOC_ID)
                    .get()
                    .get();

            if (doc.exists()) {
                ProviderHoldingsEntity entity = doc.toObject(ProviderHoldingsEntity.class);
                if (entity != null) {
                    entity.setId(doc.getId());
                    return Optional.of(entity);
                }
            }
            return Optional.empty();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "ProviderHoldingsEntity", providerId);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "ProviderHoldingsEntity", providerId);
        }
    }

    /**
     * Delete holdings for a provider (hard delete).
     * This removes the holdings document permanently.
     */
    public boolean delete(String userId, String providerId) {
        try {
            getHoldingsCollection(userId, providerId).document(HOLDINGS_DOC_ID).delete().get();
            log.info("Deleted holdings for provider {} for userId: {}", providerId, userId);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_DELETE_FAILED, e, "ProviderHoldingsEntity", providerId);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_DELETE_FAILED, e, "ProviderHoldingsEntity", providerId);
        }
    }

    /**
     * Check if holdings exist for a provider.
     */
    public boolean exists(String userId, String providerId) {
        return findByUserIdAndProviderId(userId, providerId).isPresent();
    }

    /**
     * Update sync status for holdings.
     */
    public void updateSyncStatus(String userId, String providerId, String syncStatus, String errorMessage) {
        Optional<ProviderHoldingsEntity> optional = findByUserIdAndProviderId(userId, providerId);

        if (optional.isPresent()) {
            ProviderHoldingsEntity entity = optional.get();
            entity.setSyncStatus(syncStatus);
            entity.setErrorMessage(errorMessage);
            entity.setLastUpdatedAt(java.time.Instant.now());
            save(userId, providerId, entity);
        }
    }

    private void validateInputs(ProviderHoldingsEntity entity, String userId, String providerId) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider ID cannot be null or empty");
        }
    }
}
