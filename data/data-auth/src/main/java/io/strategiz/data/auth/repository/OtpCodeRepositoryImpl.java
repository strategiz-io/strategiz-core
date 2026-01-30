package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.OtpCodeEntity;
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

/**
 * Firebase implementation of OtpCodeRepository.
 * Manages OTP codes in the otp_codes collection.
 */
@Repository
public class OtpCodeRepositoryImpl extends BaseRepository<OtpCodeEntity> implements OtpCodeRepository {

	private static final Logger log = LoggerFactory.getLogger(OtpCodeRepositoryImpl.class);

	@Autowired
	public OtpCodeRepositoryImpl(Firestore firestore) {
		super(firestore, OtpCodeEntity.class);
	}

	@Override
	protected String getModuleName() {
		return "data-auth";
	}

	@Override
	public OtpCodeEntity save(OtpCodeEntity entity, String userId) {
		return super.save(entity, userId);
	}

	@Override
	public Optional<OtpCodeEntity> findById(String id) {
		return super.findById(id);
	}

	@Override
	public Optional<OtpCodeEntity> findByEmailAndPurpose(String email, String purpose) {
		try {
			Query query = getCollection().whereEqualTo("email", email)
				.whereEqualTo("purpose", purpose)
				.whereEqualTo("isActive", true)
				.limit(1);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			if (docs.isEmpty()) {
				return Optional.empty();
			}

			QueryDocumentSnapshot doc = docs.get(0);
			OtpCodeEntity entity = doc.toObject(OtpCodeEntity.class);
			entity.setId(doc.getId());

			// Check if expired
			if (entity.isExpired()) {
				log.debug("Found expired OTP for email: {}, purpose: {}", email, purpose);
				return Optional.empty();
			}

			return Optional.of(entity);

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"OtpCodeEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "OtpCodeEntity",
					email);
		}
	}

	@Override
	public Optional<OtpCodeEntity> findBySessionId(String sessionId) {
		try {
			Query query = getCollection().whereEqualTo("sessionId", sessionId)
				.whereEqualTo("isActive", true)
				.limit(1);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			if (docs.isEmpty()) {
				return Optional.empty();
			}

			QueryDocumentSnapshot doc = docs.get(0);
			OtpCodeEntity entity = doc.toObject(OtpCodeEntity.class);
			entity.setId(doc.getId());

			if (entity.isExpired()) {
				log.debug("Found expired OTP for sessionId: {}", sessionId);
				return Optional.empty();
			}

			return Optional.of(entity);

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"OtpCodeEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "OtpCodeEntity",
					sessionId);
		}
	}

	@Override
	public OtpCodeEntity update(OtpCodeEntity entity, String userId) {
		return super.save(entity, userId);
	}

	@Override
	public void deleteById(String id) {
		try {
			getCollection().document(id).delete().get();
			log.debug("Hard deleted OTP code with ID: {}", id);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"OtpCodeEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_DELETE_FAILED, e, "OtpCodeEntity", id);
		}
	}

	@Override
	public void deleteByEmailAndPurpose(String email, String purpose) {
		try {
			Query query = getCollection().whereEqualTo("email", email).whereEqualTo("purpose", purpose);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			for (QueryDocumentSnapshot doc : docs) {
				doc.getReference().delete().get();
			}

			if (!docs.isEmpty()) {
				log.debug("Deleted {} OTP codes for email: {}, purpose: {}", docs.size(), email, purpose);
			}

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"OtpCodeEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_DELETE_FAILED, e, "OtpCodeEntity", email);
		}
	}

	@Override
	public int deleteExpired() {
		try {
			Instant now = Instant.now();
			Timestamp nowTimestamp = Timestamp.ofTimeSecondsAndNanos(now.getEpochSecond(), now.getNano());

			Query query = getCollection().whereEqualTo("isActive", true).whereLessThan("expiresAt", nowTimestamp);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			int count = 0;
			for (QueryDocumentSnapshot doc : docs) {
				doc.getReference().delete().get();
				count++;
			}

			if (count > 0) {
				log.info("Deleted {} expired OTP codes", count);
			}

			return count;

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"OtpCodeEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_DELETE_FAILED, e, "OtpCodeEntity", "expired");
		}
	}

	@Override
	public long countByEmailInLastHours(String email, int hours) {
		try {
			Instant cutoff = Instant.now().minusSeconds(hours * 3600L);
			Timestamp cutoffTimestamp = Timestamp.ofTimeSecondsAndNanos(cutoff.getEpochSecond(), cutoff.getNano());

			Query query = getCollection().whereEqualTo("email", email).whereGreaterThan("createdDate", cutoffTimestamp);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
			return docs.size();

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"OtpCodeEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "OtpCodeEntity",
					email);
		}
	}

}
