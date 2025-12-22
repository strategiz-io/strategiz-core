package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioProviderEntity;
import io.strategiz.data.provider.exception.DataProviderErrorDetails;
import io.strategiz.data.provider.exception.ProviderIntegrationException;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Repository for PortfolioProviderEntity.
 * Handles user-scoped collection: users/{userId}/portfolio/{providerId}
 *
 * Provider documents are stored directly in the portfolio collection with providerId as document ID.
 * Example: users/{userId}/portfolio/coinbase, users/{userId}/portfolio/alpaca
 *
 * This repository correctly handles delete operations by using the proper collection path.
 */
@Repository
public class PortfolioProviderRepository {

    private static final Logger log = LoggerFactory.getLogger(PortfolioProviderRepository.class);

    private final Firestore firestore;

    public PortfolioProviderRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Get the collection reference for a user's portfolio.
     * Path: users/{userId}/portfolio
     *
     * Provider documents are stored directly in this collection with providerId as the document ID.
     * Example: users/{userId}/portfolio/coinbase
     */
    private CollectionReference getPortfolioCollection(String userId) {
        return firestore.collection("users")
                .document(userId)
                .collection("portfolio");
    }

    /**
     * Save a provider entity (create or update).
     * Uses providerId as the document ID.
     */
    public PortfolioProviderEntity save(PortfolioProviderEntity entity, String userId) {
        try {
            validateInputs(entity, userId);

            boolean isCreate = !entity._hasAudit();

            if (isCreate) {
                entity._initAudit(userId);
            } else {
                entity._updateAudit(userId);
            }

            // Use providerId as document ID
            String docId = entity.getProviderId();
            entity.setId(docId);

            getPortfolioCollection(userId).document(docId).set(entity).get();

            log.debug("Saved PortfolioProviderEntity for provider {} by user {}", docId, userId);
            return entity;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_SAVE_FAILED, e, "PortfolioProviderEntity");
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_SAVE_FAILED, e, "PortfolioProviderEntity");
        }
    }

    /**
     * Find a provider by userId and providerId.
     * Only returns active (non-deleted) providers.
     */
    public Optional<PortfolioProviderEntity> findByUserIdAndProviderId(String userId, String providerId) {
        try {
            DocumentSnapshot doc = getPortfolioCollection(userId).document(providerId).get().get();

            if (doc.exists()) {
                PortfolioProviderEntity entity = doc.toObject(PortfolioProviderEntity.class);
                if (entity != null && Boolean.TRUE.equals(entity.getIsActive())) {
                    entity.setId(doc.getId());
                    return Optional.of(entity);
                }
            }
            return Optional.empty();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioProviderEntity", providerId);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioProviderEntity", providerId);
        }
    }

    /**
     * Find all providers for a user.
     * Only returns active (non-deleted) providers with status "connected".
     */
    public List<PortfolioProviderEntity> findAllByUserId(String userId) {
        try {
            log.debug("Finding all providers for userId: {}", userId);

            Query query = getPortfolioCollection(userId)
                    .whereEqualTo("status", "connected")
                    .whereEqualTo("isActive", true);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            log.info("Found {} connected providers for userId: {}", docs.size(), userId);

            return docs.stream()
                    .map(doc -> {
                        PortfolioProviderEntity entity = doc.toObject(PortfolioProviderEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioProviderEntity", userId);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioProviderEntity", userId);
        }
    }

    /**
     * Find all providers for a user (including all statuses).
     * Only returns active (non-deleted) providers.
     */
    public List<PortfolioProviderEntity> findAllByUserIdIncludingDisconnected(String userId) {
        try {
            Query query = getPortfolioCollection(userId)
                    .whereEqualTo("isActive", true);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                    .map(doc -> {
                        PortfolioProviderEntity entity = doc.toObject(PortfolioProviderEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioProviderEntity", userId);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioProviderEntity", userId);
        }
    }

    /**
     * Soft delete a provider by marking isActive = false.
     * This is the proper delete that uses the correct collection path.
     */
    public boolean delete(String userId, String providerId) {
        try {
            Optional<PortfolioProviderEntity> optional = findByUserIdAndProviderId(userId, providerId);

            if (optional.isPresent()) {
                PortfolioProviderEntity entity = optional.get();
                entity._softDelete(userId);
                entity.setStatus("disconnected");

                getPortfolioCollection(userId).document(providerId).set(entity).get();

                log.info("Soft deleted provider {} for userId: {}", providerId, userId);
                return true;
            }

            log.warn("Provider {} not found for userId: {} - cannot delete", providerId, userId);
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_DELETE_FAILED, e, "PortfolioProviderEntity", providerId);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_DELETE_FAILED, e, "PortfolioProviderEntity", providerId);
        }
    }

    /**
     * Hard delete a provider (permanently removes the document).
     * Use with caution - this cannot be undone.
     */
    public boolean hardDelete(String userId, String providerId) {
        try {
            getPortfolioCollection(userId).document(providerId).delete().get();
            log.info("Hard deleted provider {} for userId: {}", providerId, userId);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_DELETE_FAILED, e, "PortfolioProviderEntity", providerId);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(
                    DataProviderErrorDetails.REPOSITORY_DELETE_FAILED, e, "PortfolioProviderEntity", providerId);
        }
    }

    /**
     * Check if a provider exists and is active.
     */
    public boolean exists(String userId, String providerId) {
        return findByUserIdAndProviderId(userId, providerId).isPresent();
    }

    /**
     * Update provider status.
     */
    public void updateStatus(String userId, String providerId, String status) {
        Optional<PortfolioProviderEntity> optional = findByUserIdAndProviderId(userId, providerId);

        if (optional.isPresent()) {
            PortfolioProviderEntity entity = optional.get();
            entity.setStatus(status);
            save(entity, userId);
        }
    }

    private void validateInputs(PortfolioProviderEntity entity, String userId) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (entity.getProviderId() == null || entity.getProviderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Provider ID cannot be null or empty");
        }
    }
}
