package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.SmsOtpSessionEntity;
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
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firebase implementation of SmsOtpSessionRepository.
 * Manages SMS OTP sessions in the sms_otp_sessions collection.
 *
 * <p>This implementation provides production-ready SMS OTP session storage
 * with support for distributed deployments (unlike in-memory storage).</p>
 */
@Repository
public class SmsOtpSessionRepositoryImpl extends BaseRepository<SmsOtpSessionEntity>
		implements SmsOtpSessionRepository {

	private static final Logger log = LoggerFactory.getLogger(SmsOtpSessionRepositoryImpl.class);

	@Autowired
	public SmsOtpSessionRepositoryImpl(Firestore firestore) {
		super(firestore, SmsOtpSessionEntity.class);
	}

	@Override
	protected String getModuleName() {
		return "data-auth";
	}

	@Override
	public SmsOtpSessionEntity save(SmsOtpSessionEntity entity, String systemUserId) {
		return super.save(entity, systemUserId);
	}

	@Override
	public Optional<SmsOtpSessionEntity> findById(String id) {
		return super.findById(id);
	}

	@Override
	public Optional<SmsOtpSessionEntity> findActiveByPhoneNumber(String phoneNumber) {
		try {
			Instant now = Instant.now();
			Timestamp nowTimestamp = Timestamp.ofTimeSecondsAndNanos(now.getEpochSecond(), now.getNano());

			Query query = getCollection().whereEqualTo("phoneNumber", phoneNumber)
				.whereEqualTo("isActive", true)
				.whereEqualTo("verified", false)
				.whereGreaterThan("expiresAt", nowTimestamp)
				.orderBy("expiresAt", Query.Direction.DESCENDING)
				.limit(1);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			if (docs.isEmpty()) {
				return Optional.empty();
			}

			QueryDocumentSnapshot doc = docs.get(0);
			SmsOtpSessionEntity entity = doc.toObject(SmsOtpSessionEntity.class);
			entity.setId(doc.getId());

			// Double-check expiration (in case of clock skew)
			if (entity.isExpired()) {
				log.debug("Found expired session for phone: {}", entity.getMaskedPhoneNumber());
				return Optional.empty();
			}

			return Optional.of(entity);

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"SmsOtpSessionEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e,
					"SmsOtpSessionEntity", phoneNumber);
		}
	}

	@Override
	public Optional<SmsOtpSessionEntity> findActiveByPhoneNumberAndPurpose(String phoneNumber, String purpose) {
		try {
			Instant now = Instant.now();
			Timestamp nowTimestamp = Timestamp.ofTimeSecondsAndNanos(now.getEpochSecond(), now.getNano());

			Query query = getCollection().whereEqualTo("phoneNumber", phoneNumber)
				.whereEqualTo("purpose", purpose)
				.whereEqualTo("isActive", true)
				.whereEqualTo("verified", false)
				.whereGreaterThan("expiresAt", nowTimestamp)
				.orderBy("expiresAt", Query.Direction.DESCENDING)
				.limit(1);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			if (docs.isEmpty()) {
				return Optional.empty();
			}

			QueryDocumentSnapshot doc = docs.get(0);
			SmsOtpSessionEntity entity = doc.toObject(SmsOtpSessionEntity.class);
			entity.setId(doc.getId());

			if (entity.isExpired()) {
				return Optional.empty();
			}

			return Optional.of(entity);

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"SmsOtpSessionEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e,
					"SmsOtpSessionEntity", phoneNumber);
		}
	}

	@Override
	public Optional<SmsOtpSessionEntity> findMostRecentByPhoneNumber(String phoneNumber) {
		try {
			// Query without expiration filter to find most recent session for rate limiting
			Query query = getCollection().whereEqualTo("phoneNumber", phoneNumber)
				.whereEqualTo("isActive", true)
				.orderBy("createdDate", Query.Direction.DESCENDING)
				.limit(1);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			if (docs.isEmpty()) {
				return Optional.empty();
			}

			QueryDocumentSnapshot doc = docs.get(0);
			SmsOtpSessionEntity entity = doc.toObject(SmsOtpSessionEntity.class);
			entity.setId(doc.getId());

			return Optional.of(entity);

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"SmsOtpSessionEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e,
					"SmsOtpSessionEntity", phoneNumber);
		}
	}

	@Override
	public SmsOtpSessionEntity update(SmsOtpSessionEntity entity, String systemUserId) {
		return super.save(entity, systemUserId);
	}

	@Override
	public void deleteById(String id) {
		try {
			getCollection().document(id).delete().get();
			log.debug("Hard deleted SMS OTP session with ID: {}", id);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"SmsOtpSessionEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_DELETE_FAILED, e,
					"SmsOtpSessionEntity", id);
		}
	}

	@Override
	public void deleteByPhoneNumber(String phoneNumber) {
		try {
			Query query = getCollection().whereEqualTo("phoneNumber", phoneNumber);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			for (QueryDocumentSnapshot doc : docs) {
				doc.getReference().delete().get();
			}

			if (!docs.isEmpty()) {
				log.debug("Deleted {} SMS OTP sessions for phone: {}", docs.size(), maskPhoneNumber(phoneNumber));
			}

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"SmsOtpSessionEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_DELETE_FAILED, e,
					"SmsOtpSessionEntity", phoneNumber);
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
				log.info("Deleted {} expired SMS OTP sessions", count);
			}

			return count;

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"SmsOtpSessionEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_DELETE_FAILED, e,
					"SmsOtpSessionEntity", "expired");
		}
	}

	@Override
	public long countByPhoneNumberInLastMinutes(String phoneNumber, int minutes) {
		try {
			Instant cutoff = Instant.now().minus(minutes, ChronoUnit.MINUTES);
			Timestamp cutoffTimestamp = Timestamp.ofTimeSecondsAndNanos(cutoff.getEpochSecond(), cutoff.getNano());

			Query query = getCollection().whereEqualTo("phoneNumber", phoneNumber)
				.whereGreaterThan("createdDate", cutoffTimestamp);

			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
			return docs.size();

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"SmsOtpSessionEntity");
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e,
					"SmsOtpSessionEntity", phoneNumber);
		}
	}

	@Override
	public boolean canSendOtp(String phoneNumber) {
		Optional<SmsOtpSessionEntity> recentSession = findMostRecentByPhoneNumber(phoneNumber);

		if (recentSession.isEmpty()) {
			return true;
		}

		SmsOtpSessionEntity session = recentSession.get();
		Timestamp createdDate = session.getCreatedDate();

		if (createdDate == null) {
			return true;
		}

		// Convert Timestamp to Instant for comparison
		Instant createdInstant = Instant.ofEpochSecond(createdDate.getSeconds(), createdDate.getNanos());
		long secondsSinceCreated = ChronoUnit.SECONDS.between(createdInstant, Instant.now());

		return secondsSinceCreated >= SmsOtpSessionEntity.RATE_LIMIT_SECONDS;
	}

	/**
	 * Mask phone number for logging.
	 */
	private String maskPhoneNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() < 8) {
			return "***-***-****";
		}
		String countryPrefix = phoneNumber.substring(0, phoneNumber.length() - 7);
		String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
		return countryPrefix + "***" + lastFour;
	}

}
