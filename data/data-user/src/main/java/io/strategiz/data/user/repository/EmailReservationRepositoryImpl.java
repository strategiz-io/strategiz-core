package io.strategiz.data.user.repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Transaction;
import com.google.cloud.firestore.WriteBatch;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.transaction.FirestoreTransactionHolder;
import io.strategiz.data.user.entity.EmailReservationEntity;
import io.strategiz.data.user.entity.EmailReservationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of EmailReservationRepository.
 *
 * Uses the userEmails collection where document ID = normalized email. This leverages
 * Firestore's native document ID uniqueness guarantee.
 *
 * Transaction-aware: When a Firestore transaction is active (via
 * FirestoreTransactionHolder), operations participate in that transaction for atomic user
 * creation.
 */
@Repository
public class EmailReservationRepositoryImpl implements EmailReservationRepository {

	private static final Logger log = LoggerFactory.getLogger(EmailReservationRepositoryImpl.class);

	private static final String COLLECTION_NAME = "userEmails";

	private final Firestore firestore;

	@Autowired
	public EmailReservationRepositoryImpl(Firestore firestore) {
		this.firestore = firestore;
	}

	@Override
	public EmailReservationEntity reserve(EmailReservationEntity reservation) {
		String email = normalizeEmail(reservation.getEmail());
		log.info("Attempting to reserve email: {}", email);

		DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(email);

		try {
			// Check if transaction is active
			if (FirestoreTransactionHolder.isTransactionActive()) {
				Transaction transaction = FirestoreTransactionHolder.getTransaction();
				return reserveInTransaction(transaction, docRef, reservation, email);
			}

			// Non-transactional: Use Firestore transaction internally for atomicity
			return firestore.runTransaction(transaction -> {
				return reserveInTransaction(transaction, docRef, reservation, email);
			}).get();

		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof DataRepositoryException) {
				throw (DataRepositoryException) cause;
			}
			log.error("Failed to reserve email {}: {}", email, e.getMessage(), e);
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_SAVE_FAILED, e, "EmailReservation",
					email);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"EmailReservation", email);
		}
	}

	private EmailReservationEntity reserveInTransaction(Transaction transaction, DocumentReference docRef,
			EmailReservationEntity reservation, String email) throws InterruptedException, ExecutionException {
		// Check if document exists
		DocumentSnapshot existingDoc = transaction.get(docRef).get();

		if (existingDoc.exists()) {
			EmailReservationEntity existing = existingDoc.toObject(EmailReservationEntity.class);

			// If existing reservation is CONFIRMED, email is taken
			if (existing != null && existing.getStatus() == EmailReservationStatus.CONFIRMED) {
				log.warn("Email {} already has CONFIRMED reservation", email);
				throw new DataRepositoryException(DataRepositoryErrorDetails.DUPLICATE_ENTITY, "EmailReservation",
						email);
			}

			// If existing reservation is PENDING but not expired, email is taken
			if (existing != null && !existing.isExpired()) {
				log.warn("Email {} has non-expired PENDING reservation", email);
				throw new DataRepositoryException(DataRepositoryErrorDetails.DUPLICATE_ENTITY, "EmailReservation",
						email);
			}

			// Existing reservation is expired - we can overwrite it
			log.info("Overwriting expired PENDING reservation for email: {}", email);
		}

		// Ensure email is normalized
		reservation.setEmail(email);

		// Create the reservation document
		transaction.set(docRef, reservation);
		log.info("Email reservation created for: {}", email);

		return reservation;
	}

	@Override
	public EmailReservationEntity createConfirmed(EmailReservationEntity reservation) {
		String email = normalizeEmail(reservation.getEmail());
		log.info("Creating CONFIRMED email reservation: {}", email);

		reservation.setEmail(email);
		reservation.confirm();

		DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(email);

		try {
			if (FirestoreTransactionHolder.isTransactionActive()) {
				Transaction transaction = FirestoreTransactionHolder.getTransaction();
				transaction.set(docRef, reservation);
			}
			else {
				docRef.set(reservation).get();
			}
			log.info("CONFIRMED email reservation created for: {}", email);
			return reservation;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"EmailReservation", email);
		}
		catch (ExecutionException e) {
			log.error("Failed to create confirmed email reservation {}: {}", email, e.getMessage(), e);
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_SAVE_FAILED, e, "EmailReservation",
					email);
		}
	}

	@Override
	public Optional<EmailReservationEntity> findByEmail(String email) {
		String normalizedEmail = normalizeEmail(email);

		try {
			DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(normalizedEmail);

			// Check if transaction is active
			if (FirestoreTransactionHolder.isTransactionActive()) {
				Transaction transaction = FirestoreTransactionHolder.getTransaction();
				DocumentSnapshot doc = transaction.get(docRef).get();
				if (!doc.exists()) {
					return Optional.empty();
				}
				return Optional.ofNullable(doc.toObject(EmailReservationEntity.class));
			}

			// Non-transactional read
			DocumentSnapshot doc = docRef.get().get();
			if (!doc.exists()) {
				return Optional.empty();
			}
			return Optional.ofNullable(doc.toObject(EmailReservationEntity.class));

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"EmailReservation", normalizedEmail);
		}
		catch (ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "EmailReservation",
					normalizedEmail);
		}
	}

	@Override
	public boolean isEmailAvailable(String email) {
		String normalizedEmail = normalizeEmail(email);

		Optional<EmailReservationEntity> existing = findByEmail(normalizedEmail);
		if (existing.isEmpty()) {
			return true;
		}

		EmailReservationEntity reservation = existing.get();

		// Email is not available if reservation is CONFIRMED
		if (reservation.getStatus() == EmailReservationStatus.CONFIRMED) {
			return false;
		}

		// Email is available if PENDING reservation has expired
		return reservation.isExpired();
	}

	@Override
	public EmailReservationEntity confirm(String email) {
		String normalizedEmail = normalizeEmail(email);
		log.info("Confirming email reservation: {}", normalizedEmail);

		DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(normalizedEmail);

		try {
			// Check if transaction is active
			if (FirestoreTransactionHolder.isTransactionActive()) {
				Transaction transaction = FirestoreTransactionHolder.getTransaction();
				return confirmInTransaction(transaction, docRef, normalizedEmail);
			}

			// Non-transactional: Use Firestore transaction internally
			return firestore.runTransaction(transaction -> {
				return confirmInTransaction(transaction, docRef, normalizedEmail);
			}).get();

		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof DataRepositoryException) {
				throw (DataRepositoryException) cause;
			}
			log.error("Failed to confirm email reservation {}: {}", normalizedEmail, e.getMessage(), e);
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_UPDATE_FAILED, e, "EmailReservation",
					normalizedEmail);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"EmailReservation", normalizedEmail);
		}
	}

	private EmailReservationEntity confirmInTransaction(Transaction transaction, DocumentReference docRef, String email)
			throws InterruptedException, ExecutionException {
		DocumentSnapshot doc = transaction.get(docRef).get();

		if (!doc.exists()) {
			log.error("Cannot confirm - no reservation found for email: {}", email);
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND, "EmailReservation", email);
		}

		EmailReservationEntity reservation = doc.toObject(EmailReservationEntity.class);
		if (reservation == null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_CONVERSION_FAILED, "EmailReservation",
					email);
		}

		// Already confirmed - idempotent operation
		if (reservation.getStatus() == EmailReservationStatus.CONFIRMED) {
			log.info("Email reservation already confirmed: {}", email);
			return reservation;
		}

		// Confirm the reservation
		reservation.confirm();
		transaction.set(docRef, reservation);
		log.info("Email reservation confirmed: {}", email);

		return reservation;
	}

	@Override
	public void delete(String email) {
		String normalizedEmail = normalizeEmail(email);
		log.info("Deleting email reservation: {}", normalizedEmail);

		DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(normalizedEmail);

		try {
			// Check if transaction is active
			if (FirestoreTransactionHolder.isTransactionActive()) {
				Transaction transaction = FirestoreTransactionHolder.getTransaction();
				transaction.delete(docRef);
				log.info("Email reservation deleted (in transaction): {}", normalizedEmail);
				return;
			}

			// Non-transactional delete
			docRef.delete().get();
			log.info("Email reservation deleted: {}", normalizedEmail);

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"EmailReservation", normalizedEmail);
		}
		catch (ExecutionException e) {
			log.error("Failed to delete email reservation {}: {}", normalizedEmail, e.getMessage(), e);
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_DELETE_FAILED, e, "EmailReservation",
					normalizedEmail);
		}
	}

	@Override
	public int deleteExpiredPendingReservations() {
		log.info("Cleaning up expired PENDING email reservations");
		long nowEpochSecond = Instant.now().getEpochSecond();

		try {
			// Query for expired PENDING reservations
			Query query = firestore.collection(COLLECTION_NAME)
				.whereEqualTo("status", EmailReservationStatus.PENDING.name())
				.whereLessThan("expiresAtEpochSecond", nowEpochSecond);

			List<QueryDocumentSnapshot> expiredDocs = query.get().get().getDocuments();

			if (expiredDocs.isEmpty()) {
				log.info("No expired email reservations found");
				return 0;
			}

			// Use batched writes for efficiency (max 500 per batch)
			int totalDeleted = 0;
			WriteBatch batch = firestore.batch();
			int batchCount = 0;

			for (QueryDocumentSnapshot doc : expiredDocs) {
				batch.delete(doc.getReference());
				batchCount++;

				// Firestore batch limit is 500
				if (batchCount >= 500) {
					batch.commit().get();
					totalDeleted += batchCount;
					batch = firestore.batch();
					batchCount = 0;
				}
			}

			// Commit remaining deletes
			if (batchCount > 0) {
				batch.commit().get();
				totalDeleted += batchCount;
			}

			log.info("Deleted {} expired email reservations", totalDeleted);
			return totalDeleted;

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"EmailReservation", "cleanup");
		}
		catch (ExecutionException e) {
			log.error("Failed to clean up expired email reservations: {}", e.getMessage(), e);
			throw new DataRepositoryException(DataRepositoryErrorDetails.BULK_OPERATION_FAILED, e, "EmailReservation",
					"cleanup");
		}
	}

	@Override
	public boolean existsAndValid(String email) {
		String normalizedEmail = normalizeEmail(email);

		Optional<EmailReservationEntity> existing = findByEmail(normalizedEmail);
		if (existing.isEmpty()) {
			return false;
		}

		return existing.get().isValid();
	}

	/**
	 * Normalize email to lowercase for consistent document IDs.
	 */
	private String normalizeEmail(String email) {
		if (email == null) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ARGUMENT, "EmailReservation",
					"Email cannot be null");
		}
		return email.toLowerCase().trim();
	}

}
