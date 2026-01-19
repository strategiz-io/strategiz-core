package io.strategiz.data.social.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.social.entity.OwnerSubscription;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Base repository for OwnerSubscription entities using Firestore.
 * Provides Firestore CRUD operations and specialized queries.
 *
 * <p>Collection path: ownerSubscriptions/{subscriptionId}</p>
 *
 * @see io.strategiz.data.social.entity.OwnerSubscription
 */
@Repository
public class OwnerSubscriptionBaseRepository extends BaseRepository<OwnerSubscription> {

    public OwnerSubscriptionBaseRepository(Firestore firestore) {
        super(firestore, OwnerSubscription.class);
    }

    @Override
    protected String getModuleName() {
        return "data-social";
    }

    /**
     * Find all subscriptions for a subscriber.
     * @param subscriberId The subscriber's user ID
     * @return List of subscriptions
     */
    public List<OwnerSubscription> findBySubscriberId(String subscriberId) {
        return findByField("subscriberId", subscriberId);
    }

    /**
     * Find all active subscriptions for a subscriber.
     * @param subscriberId The subscriber's user ID
     * @return List of active subscriptions
     */
    public List<OwnerSubscription> findActiveBySubscriberId(String subscriberId) {
        try {
            Query query = getCollection()
                    .whereEqualTo("subscriberId", subscriberId)
                    .whereEqualTo("status", OwnerSubscription.STATUS_ACTIVE)
                    .whereEqualTo("isActive", true);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream().map(doc -> {
                OwnerSubscription entity = doc.toObject(OwnerSubscription.class);
                entity.setId(doc.getId());
                return entity;
            }).collect(Collectors.toList());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
                    "OwnerSubscription");
        }
        catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "OwnerSubscription",
                    "subscriberId=" + subscriberId);
        }
    }

    /**
     * Find all subscriptions for an owner.
     * @param ownerId The owner's user ID
     * @return List of subscriptions
     */
    public List<OwnerSubscription> findByOwnerId(String ownerId) {
        return findByField("ownerId", ownerId);
    }

    /**
     * Find all active subscriptions for an owner.
     * @param ownerId The owner's user ID
     * @return List of active subscriptions
     */
    public List<OwnerSubscription> findActiveByOwnerId(String ownerId) {
        try {
            Query query = getCollection()
                    .whereEqualTo("ownerId", ownerId)
                    .whereEqualTo("status", OwnerSubscription.STATUS_ACTIVE)
                    .whereEqualTo("isActive", true);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream().map(doc -> {
                OwnerSubscription entity = doc.toObject(OwnerSubscription.class);
                entity.setId(doc.getId());
                return entity;
            }).collect(Collectors.toList());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
                    "OwnerSubscription");
        }
        catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "OwnerSubscription",
                    "ownerId=" + ownerId);
        }
    }

    /**
     * Find subscription by subscriber and owner.
     * @param subscriberId The subscriber's user ID
     * @param ownerId The owner's user ID
     * @return Optional containing the subscription if found
     */
    public Optional<OwnerSubscription> findBySubscriberIdAndOwnerId(String subscriberId, String ownerId) {
        try {
            Query query = getCollection()
                    .whereEqualTo("subscriberId", subscriberId)
                    .whereEqualTo("ownerId", ownerId)
                    .whereEqualTo("isActive", true)
                    .limit(1);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            if (docs.isEmpty()) {
                return Optional.empty();
            }

            QueryDocumentSnapshot doc = docs.get(0);
            OwnerSubscription entity = doc.toObject(OwnerSubscription.class);
            entity.setId(doc.getId());
            return Optional.of(entity);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
                    "OwnerSubscription");
        }
        catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "OwnerSubscription",
                    "subscriberId=" + subscriberId + ",ownerId=" + ownerId);
        }
    }

    /**
     * Find active subscription by subscriber and owner.
     * @param subscriberId The subscriber's user ID
     * @param ownerId The owner's user ID
     * @return Optional containing the active subscription if found
     */
    public Optional<OwnerSubscription> findActiveBySubscriberIdAndOwnerId(String subscriberId, String ownerId) {
        try {
            Query query = getCollection()
                    .whereEqualTo("subscriberId", subscriberId)
                    .whereEqualTo("ownerId", ownerId)
                    .whereEqualTo("status", OwnerSubscription.STATUS_ACTIVE)
                    .whereEqualTo("isActive", true)
                    .limit(1);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            if (docs.isEmpty()) {
                return Optional.empty();
            }

            QueryDocumentSnapshot doc = docs.get(0);
            OwnerSubscription entity = doc.toObject(OwnerSubscription.class);
            entity.setId(doc.getId());
            return Optional.of(entity);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
                    "OwnerSubscription");
        }
        catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "OwnerSubscription",
                    "subscriberId=" + subscriberId + ",ownerId=" + ownerId);
        }
    }

    /**
     * Find subscription by Stripe subscription ID.
     * @param stripeSubscriptionId The Stripe subscription ID
     * @return Optional containing the subscription if found
     */
    public Optional<OwnerSubscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
        try {
            Query query = getCollection()
                    .whereEqualTo("stripeSubscriptionId", stripeSubscriptionId)
                    .whereEqualTo("isActive", true)
                    .limit(1);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            if (docs.isEmpty()) {
                return Optional.empty();
            }

            QueryDocumentSnapshot doc = docs.get(0);
            OwnerSubscription entity = doc.toObject(OwnerSubscription.class);
            entity.setId(doc.getId());
            return Optional.of(entity);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
                    "OwnerSubscription");
        }
        catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "OwnerSubscription",
                    "stripeSubscriptionId=" + stripeSubscriptionId);
        }
    }

    /**
     * Count active subscribers for an owner.
     * @param ownerId The owner's user ID
     * @return The number of active subscribers
     */
    public int countActiveByOwnerId(String ownerId) {
        return findActiveByOwnerId(ownerId).size();
    }

    /**
     * Count active subscriptions for a subscriber.
     * @param subscriberId The subscriber's user ID
     * @return The number of active subscriptions
     */
    public int countActiveBySubscriberId(String subscriberId) {
        return findActiveBySubscriberId(subscriberId).size();
    }

}
