package io.strategiz.data.social.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.social.entity.UserSubscription;
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
 * Base repository for UserSubscription entities using Firestore.
 * Provides Firestore CRUD operations and specialized queries.
 *
 * Collection path: userSubscriptions/{subscriptionId}
 */
@Repository
public class UserSubscriptionBaseRepository extends BaseRepository<UserSubscription> {

	public UserSubscriptionBaseRepository(Firestore firestore) {
		super(firestore, UserSubscription.class);
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
	public List<UserSubscription> findBySubscriberId(String subscriberId) {
		return findByField("subscriberId", subscriberId);
	}

	/**
	 * Find all active subscriptions for a subscriber.
	 * @param subscriberId The subscriber's user ID
	 * @return List of active subscriptions
	 */
	public List<UserSubscription> findActiveBySubscriberId(String subscriberId) {
		try {
			Query query = getCollection().whereEqualTo("subscriberId", subscriberId)
				.whereEqualTo("status", "ACTIVE")
				.whereEqualTo("isActive", true);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			return docs.stream().map(doc -> {
				UserSubscription entity = doc.toObject(UserSubscription.class);
				entity.setId(doc.getId());
				return entity;
			}).collect(Collectors.toList());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"UserSubscription");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "UserSubscription",
					"subscriberId=" + subscriberId);
		}
	}

	/**
	 * Find all subscriptions for an owner.
	 * @param ownerId The owner's user ID
	 * @return List of subscriptions
	 */
	public List<UserSubscription> findByOwnerId(String ownerId) {
		return findByField("ownerId", ownerId);
	}

	/**
	 * Find all active subscriptions for an owner.
	 * @param ownerId The owner's user ID
	 * @return List of active subscriptions
	 */
	public List<UserSubscription> findActiveByOwnerId(String ownerId) {
		try {
			Query query = getCollection().whereEqualTo("ownerId", ownerId)
				.whereEqualTo("status", "ACTIVE")
				.whereEqualTo("isActive", true);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			return docs.stream().map(doc -> {
				UserSubscription entity = doc.toObject(UserSubscription.class);
				entity.setId(doc.getId());
				return entity;
			}).collect(Collectors.toList());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"UserSubscription");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "UserSubscription",
					"ownerId=" + ownerId);
		}
	}

	/**
	 * Find subscription by subscriber and owner.
	 * @param subscriberId The subscriber's user ID
	 * @param ownerId The owner's user ID
	 * @return Optional containing the subscription if found
	 */
	public Optional<UserSubscription> findBySubscriberIdAndOwnerId(String subscriberId, String ownerId) {
		try {
			Query query = getCollection().whereEqualTo("subscriberId", subscriberId)
				.whereEqualTo("ownerId", ownerId)
				.whereEqualTo("isActive", true)
				.limit(1);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			if (docs.isEmpty()) {
				return Optional.empty();
			}

			QueryDocumentSnapshot doc = docs.get(0);
			UserSubscription entity = doc.toObject(UserSubscription.class);
			entity.setId(doc.getId());
			return Optional.of(entity);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"UserSubscription");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "UserSubscription",
					"subscriberId=" + subscriberId + ",ownerId=" + ownerId);
		}
	}

	/**
	 * Find active subscription by subscriber and owner.
	 * @param subscriberId The subscriber's user ID
	 * @param ownerId The owner's user ID
	 * @return Optional containing the active subscription if found
	 */
	public Optional<UserSubscription> findActiveBySubscriberIdAndOwnerId(String subscriberId, String ownerId) {
		try {
			Query query = getCollection().whereEqualTo("subscriberId", subscriberId)
				.whereEqualTo("ownerId", ownerId)
				.whereEqualTo("status", "ACTIVE")
				.whereEqualTo("isActive", true)
				.limit(1);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			if (docs.isEmpty()) {
				return Optional.empty();
			}

			QueryDocumentSnapshot doc = docs.get(0);
			UserSubscription entity = doc.toObject(UserSubscription.class);
			entity.setId(doc.getId());
			return Optional.of(entity);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"UserSubscription");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "UserSubscription",
					"subscriberId=" + subscriberId + ",ownerId=" + ownerId);
		}
	}

	/**
	 * Find subscription by Stripe subscription ID.
	 * @param stripeSubscriptionId The Stripe subscription ID
	 * @return Optional containing the subscription if found
	 */
	public Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
		try {
			Query query = getCollection().whereEqualTo("stripeSubscriptionId", stripeSubscriptionId)
				.whereEqualTo("isActive", true)
				.limit(1);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			if (docs.isEmpty()) {
				return Optional.empty();
			}

			QueryDocumentSnapshot doc = docs.get(0);
			UserSubscription entity = doc.toObject(UserSubscription.class);
			entity.setId(doc.getId());
			return Optional.of(entity);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"UserSubscription");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "UserSubscription",
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
