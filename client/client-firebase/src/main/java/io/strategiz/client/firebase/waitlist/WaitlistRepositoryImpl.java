package io.strategiz.client.firebase.waitlist;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.waitlist.entity.WaitlistEntity;
import io.strategiz.data.waitlist.repository.WaitlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of WaitlistRepository.
 *
 * Collection path: waitlist/{id}
 */
@Repository
public class WaitlistRepositoryImpl implements WaitlistRepository {

	private static final Logger log = LoggerFactory.getLogger(WaitlistRepositoryImpl.class);

	private static final String COLLECTION = "waitlist";

	private final Firestore firestore;

	public WaitlistRepositoryImpl(Firestore firestore) {
		this.firestore = firestore;
	}

	@Override
	public Optional<WaitlistEntity> findByEmailHash(String emailHash) {
		try {
			log.debug("Finding waitlist entry by email hash");

			QuerySnapshot snapshot = firestore.collection(COLLECTION)
				.whereEqualTo("emailHash", emailHash)
				.limit(1)
				.get()
				.get();

			if (!snapshot.isEmpty()) {
				WaitlistEntity entity = snapshot.getDocuments().get(0).toObject(WaitlistEntity.class);
				log.debug("Found waitlist entry: id={}", entity.getId());
				return Optional.of(entity);
			}

			log.debug("No waitlist entry found for email hash");
			return Optional.empty();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while finding waitlist entry by email hash", e);
			return Optional.empty();
		}
		catch (ExecutionException e) {
			log.error("Failed to find waitlist entry by email hash", e);
			return Optional.empty();
		}
	}

	@Override
	public WaitlistEntity save(WaitlistEntity entity) {
		try {
			log.info("Saving waitlist entry");

			// Generate ID if not present
			if (entity.getId() == null || entity.getId().isEmpty()) {
				entity.setId(firestore.collection(COLLECTION).document().getId());
			}

			// Save to Firestore
			firestore.collection(COLLECTION).document(entity.getId()).set(entity).get();

			log.info("Waitlist entry saved successfully: id={}", entity.getId());
			return entity;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while saving waitlist entry", e);
			throw new RuntimeException("Failed to save waitlist entry", e);
		}
		catch (ExecutionException e) {
			log.error("Failed to save waitlist entry", e);
			throw new RuntimeException("Failed to save waitlist entry", e);
		}
	}

	@Override
	public List<WaitlistEntity> findAll() {
		try {
			log.debug("Fetching all waitlist entries");

			QuerySnapshot snapshot = firestore.collection(COLLECTION).get().get();

			List<WaitlistEntity> entities = new ArrayList<>();
			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				if (doc.exists()) {
					entities.add(doc.toObject(WaitlistEntity.class));
				}
			}

			log.debug("Found {} waitlist entries", entities.size());
			return entities;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching all waitlist entries", e);
			return new ArrayList<>();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch all waitlist entries", e);
			return new ArrayList<>();
		}
	}

	@Override
	public long count() {
		try {
			log.debug("Counting waitlist entries");

			QuerySnapshot snapshot = firestore.collection(COLLECTION).get().get();
			long count = snapshot.size();

			log.debug("Total waitlist entries: {}", count);
			return count;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while counting waitlist entries", e);
			return 0;
		}
		catch (ExecutionException e) {
			log.error("Failed to count waitlist entries", e);
			return 0;
		}
	}

	@Override
	public List<WaitlistEntity> findUnconfirmed() {
		try {
			log.debug("Fetching unconfirmed waitlist entries");

			QuerySnapshot snapshot = firestore.collection(COLLECTION).whereEqualTo("confirmed", false).get().get();

			List<WaitlistEntity> entities = new ArrayList<>();
			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				if (doc.exists()) {
					entities.add(doc.toObject(WaitlistEntity.class));
				}
			}

			log.debug("Found {} unconfirmed waitlist entries", entities.size());
			return entities;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching unconfirmed waitlist entries", e);
			return new ArrayList<>();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch unconfirmed waitlist entries", e);
			return new ArrayList<>();
		}
	}

	@Override
	public Optional<WaitlistEntity> findById(String id) {
		try {
			log.debug("Finding waitlist entry by ID: {}", id);

			DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();

			if (doc.exists()) {
				WaitlistEntity entity = doc.toObject(WaitlistEntity.class);
				log.debug("Found waitlist entry: id={}", entity.getId());
				return Optional.of(entity);
			}

			log.debug("No waitlist entry found with ID: {}", id);
			return Optional.empty();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while finding waitlist entry by ID", e);
			return Optional.empty();
		}
		catch (ExecutionException e) {
			log.error("Failed to find waitlist entry by ID", e);
			return Optional.empty();
		}
	}

	@Override
	public void deleteById(String id) {
		try {
			log.info("Deleting waitlist entry: id={}", id);

			firestore.collection(COLLECTION).document(id).delete().get();

			log.info("Waitlist entry deleted successfully: id={}", id);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while deleting waitlist entry", e);
			throw new RuntimeException("Failed to delete waitlist entry", e);
		}
		catch (ExecutionException e) {
			log.error("Failed to delete waitlist entry", e);
			throw new RuntimeException("Failed to delete waitlist entry", e);
		}
	}

}
