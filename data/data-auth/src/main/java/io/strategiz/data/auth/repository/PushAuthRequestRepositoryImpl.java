package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.PushAuthRequestEntity;
import io.strategiz.data.auth.entity.PushAuthStatus;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.BaseRepository;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firebase implementation of PushAuthRequestRepository.
 */
@Repository
public class PushAuthRequestRepositoryImpl extends BaseRepository<PushAuthRequestEntity>
		implements PushAuthRequestRepository {

	private static final Logger log = LoggerFactory.getLogger(PushAuthRequestRepositoryImpl.class);

	@Autowired
	public PushAuthRequestRepositoryImpl(Firestore firestore) {
		super(firestore, PushAuthRequestEntity.class);
	}

	@Override
	protected String getModuleName() {
		return "data-auth";
	}

	@Override
	public PushAuthRequestEntity save(PushAuthRequestEntity entity, String userId) {
		return super.save(entity, userId);
	}

	@Override
	public Optional<PushAuthRequestEntity> findById(String id) {
		return super.findById(id);
	}

	@Override
	public Optional<PushAuthRequestEntity> findByChallenge(String challenge) {
		List<PushAuthRequestEntity> results = findByField("challenge", challenge);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	@Override
	public List<PushAuthRequestEntity> findPendingByUserId(String userId) {
		try {
			Query query = getCollection().whereEqualTo("userId", userId)
				.whereEqualTo("status", PushAuthStatus.PENDING.name())
				.whereEqualTo("isActive", true);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			return docs.stream().map(doc -> {
				PushAuthRequestEntity entity = doc.toObject(PushAuthRequestEntity.class);
				entity.setId(doc.getId());
				return entity;
			}).filter(entity -> !entity.isExpired()).collect(Collectors.toList());

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"PushAuthRequestEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e,
					"PushAuthRequestEntity", userId);
		}
	}

	@Override
	public List<PushAuthRequestEntity> findByStatus(PushAuthStatus status) {
		return findByField("status", status.name());
	}

	@Override
	public PushAuthRequestEntity update(PushAuthRequestEntity entity, String userId) {
		return super.save(entity, userId);
	}

	@Override
	public int cancelPendingForUser(String userId, String systemUserId) {
		List<PushAuthRequestEntity> pending = findPendingByUserId(userId);
		int count = 0;
		for (PushAuthRequestEntity request : pending) {
			request.setStatus(PushAuthStatus.CANCELLED);
			super.save(request, systemUserId);
			count++;
		}
		if (count > 0) {
			log.info("Cancelled {} pending push auth requests for user {}", count, userId);
		}
		return count;
	}

	@Override
	public int markExpired() {
		try {
			Instant now = Instant.now();
			Timestamp nowTimestamp = Timestamp.ofTimeSecondsAndNanos(now.getEpochSecond(), now.getNano());

			Query query = getCollection().whereEqualTo("status", PushAuthStatus.PENDING.name())
				.whereLessThan("expiresAt", nowTimestamp);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			int count = 0;
			for (QueryDocumentSnapshot doc : docs) {
				PushAuthRequestEntity entity = doc.toObject(PushAuthRequestEntity.class);
				entity.setId(doc.getId());
				entity.markExpired();
				super.save(entity, "system");
				count++;
			}

			if (count > 0) {
				log.info("Marked {} push auth requests as expired", count);
			}

			return count;

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"PushAuthRequestEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e,
					"PushAuthRequestEntity", "expired");
		}
	}

	@Override
	public int deleteOldRequests(int olderThanHours) {
		try {
			Instant cutoff = Instant.now().minusSeconds(olderThanHours * 3600L);
			Timestamp cutoffTimestamp = Timestamp.ofTimeSecondsAndNanos(cutoff.getEpochSecond(), cutoff.getNano());

			Query query = getCollection().whereLessThan("createdDate", cutoffTimestamp);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			int count = 0;
			for (QueryDocumentSnapshot doc : docs) {
				PushAuthRequestEntity entity = doc.toObject(PushAuthRequestEntity.class);
				// Only delete non-pending requests
				if (entity.getStatus() != PushAuthStatus.PENDING) {
					if (super.delete(doc.getId(), "system")) {
						count++;
					}
				}
			}

			if (count > 0) {
				log.info("Deleted {} old push auth requests", count);
			}

			return count;

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"PushAuthRequestEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e,
					"PushAuthRequestEntity", "old");
		}
	}

}
