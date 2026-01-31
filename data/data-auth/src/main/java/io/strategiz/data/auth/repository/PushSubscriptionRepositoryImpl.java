package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.PushSubscriptionEntity;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.BaseRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firebase implementation of PushSubscriptionRepository.
 */
@Repository
public class PushSubscriptionRepositoryImpl extends BaseRepository<PushSubscriptionEntity>
		implements PushSubscriptionRepository {

	private static final Logger log = LoggerFactory.getLogger(PushSubscriptionRepositoryImpl.class);

	@Autowired
	public PushSubscriptionRepositoryImpl(Firestore firestore) {
		super(firestore, PushSubscriptionEntity.class);
	}

	@Override
	protected String getModuleName() {
		return "data-auth";
	}

	@Override
	public PushSubscriptionEntity save(PushSubscriptionEntity entity, String userId) {
		return super.save(entity, userId);
	}

	@Override
	public Optional<PushSubscriptionEntity> findById(String id) {
		return super.findById(id);
	}

	@Override
	public List<PushSubscriptionEntity> findByUserId(String userId) {
		return findByField("userId", userId);
	}

	@Override
	public List<PushSubscriptionEntity> findActiveByUserId(String userId) {
		try {
			Query query = getCollection().whereEqualTo("userId", userId)
				.whereEqualTo("isActive", true)
				.whereEqualTo("pushAuthEnabled", true);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			return docs.stream().map(doc -> {
				PushSubscriptionEntity entity = doc.toObject(PushSubscriptionEntity.class);
				entity.setId(doc.getId());
				return entity;
			}).filter(PushSubscriptionEntity::isValid).collect(Collectors.toList());

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"PushSubscriptionEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e,
					"PushSubscriptionEntity", userId);
		}
	}

	@Override
	public Optional<PushSubscriptionEntity> findByEndpoint(String endpoint) {
		List<PushSubscriptionEntity> results = findByField("endpoint", endpoint);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	@Override
	public Optional<PushSubscriptionEntity> findByUserIdAndEndpoint(String userId, String endpoint) {
		try {
			Query query = getCollection().whereEqualTo("userId", userId).whereEqualTo("endpoint", endpoint).limit(1);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			if (docs.isEmpty()) {
				return Optional.empty();
			}

			QueryDocumentSnapshot doc = docs.get(0);
			PushSubscriptionEntity entity = doc.toObject(PushSubscriptionEntity.class);
			entity.setId(doc.getId());
			return Optional.of(entity);

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"PushSubscriptionEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e,
					"PushSubscriptionEntity", userId);
		}
	}

	@Override
	public PushSubscriptionEntity update(PushSubscriptionEntity entity, String userId) {
		return super.save(entity, userId);
	}

	@Override
	public void delete(String id) {
		super.delete(id, "system");
	}

	@Override
	public int deleteAllForUser(String userId) {
		List<PushSubscriptionEntity> subscriptions = findByUserId(userId);
		int count = 0;
		for (PushSubscriptionEntity subscription : subscriptions) {
			if (super.delete(subscription.getId(), userId)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public long countActiveByUserId(String userId) {
		return findActiveByUserId(userId).size();
	}

	@Override
	public boolean hasActiveSubscriptions(String userId) {
		return countActiveByUserId(userId) > 0;
	}

}
