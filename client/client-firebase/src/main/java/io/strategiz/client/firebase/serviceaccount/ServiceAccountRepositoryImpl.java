package io.strategiz.client.firebase.serviceaccount;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.preferences.entity.ServiceAccountEntity;
import io.strategiz.data.preferences.repository.ServiceAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of ServiceAccountRepository.
 *
 * Collection path: service_accounts/{id}
 *
 * Provides CRUD operations for service accounts used in machine-to-machine
 * authentication for CI/CD pipelines and integration testing.
 */
@Repository
public class ServiceAccountRepositoryImpl implements ServiceAccountRepository {

	private static final Logger log = LoggerFactory.getLogger(ServiceAccountRepositoryImpl.class);

	private static final String COLLECTION = "service_accounts";

	private final Firestore firestore;

	public ServiceAccountRepositoryImpl(Firestore firestore) {
		this.firestore = firestore;
	}

	@Override
	public ServiceAccountEntity create(ServiceAccountEntity entity) {
		try {
			log.info("Creating service account: name={}", entity.getName());

			// Generate ID if not present
			if (entity.getId() == null || entity.getId().isEmpty()) {
				entity.setId(firestore.collection(COLLECTION).document().getId());
			}

			// Initialize audit fields using BaseEntity API
			if (!entity._hasAudit()) {
				entity._initAudit(entity.getCreatedBy() != null ? entity.getCreatedBy() : "system");
			}

			// Save to Firestore
			firestore.collection(COLLECTION).document(entity.getId()).set(entity).get();

			log.info("Service account created successfully: id={}, clientId={}", entity.getId(), entity.getClientId());
			return entity;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while creating service account", e);
			throw new RuntimeException("Failed to create service account", e);
		}
		catch (ExecutionException e) {
			log.error("Failed to create service account", e);
			throw new RuntimeException("Failed to create service account", e);
		}
	}

	@Override
	public Optional<ServiceAccountEntity> findById(String id) {
		try {
			log.debug("Finding service account by ID: {}", id);

			DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();

			if (doc.exists()) {
				ServiceAccountEntity entity = doc.toObject(ServiceAccountEntity.class);
				log.debug("Found service account: id={}, name={}", entity.getId(), entity.getName());
				return Optional.of(entity);
			}

			log.debug("No service account found with ID: {}", id);
			return Optional.empty();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while finding service account by ID", e);
			return Optional.empty();
		}
		catch (ExecutionException e) {
			log.error("Failed to find service account by ID", e);
			return Optional.empty();
		}
	}

	@Override
	public Optional<ServiceAccountEntity> findByClientId(String clientId) {
		try {
			log.debug("Finding service account by client ID");

			QuerySnapshot snapshot = firestore.collection(COLLECTION)
				.whereEqualTo("clientId", clientId)
				.limit(1)
				.get()
				.get();

			if (!snapshot.isEmpty()) {
				ServiceAccountEntity entity = snapshot.getDocuments().get(0).toObject(ServiceAccountEntity.class);
				log.debug("Found service account: id={}, name={}", entity.getId(), entity.getName());
				return Optional.of(entity);
			}

			log.debug("No service account found for client ID");
			return Optional.empty();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while finding service account by client ID", e);
			return Optional.empty();
		}
		catch (ExecutionException e) {
			log.error("Failed to find service account by client ID", e);
			return Optional.empty();
		}
	}

	@Override
	public List<ServiceAccountEntity> findAll() {
		try {
			log.debug("Fetching all service accounts");

			QuerySnapshot snapshot = firestore.collection(COLLECTION).get().get();

			List<ServiceAccountEntity> entities = new ArrayList<>();
			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				if (doc.exists()) {
					entities.add(doc.toObject(ServiceAccountEntity.class));
				}
			}

			log.debug("Found {} service accounts", entities.size());
			return entities;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching all service accounts", e);
			return new ArrayList<>();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch all service accounts", e);
			return new ArrayList<>();
		}
	}

	@Override
	public List<ServiceAccountEntity> findAllEnabled() {
		try {
			log.debug("Fetching all enabled service accounts");

			QuerySnapshot snapshot = firestore.collection(COLLECTION).whereEqualTo("enabled", true).get().get();

			List<ServiceAccountEntity> entities = new ArrayList<>();
			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				if (doc.exists()) {
					entities.add(doc.toObject(ServiceAccountEntity.class));
				}
			}

			log.debug("Found {} enabled service accounts", entities.size());
			return entities;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching enabled service accounts", e);
			return new ArrayList<>();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch enabled service accounts", e);
			return new ArrayList<>();
		}
	}

	@Override
	public ServiceAccountEntity update(ServiceAccountEntity entity) {
		try {
			log.info("Updating service account: id={}", entity.getId());

			// Update audit fields
			entity._updateAudit("system");

			// Save to Firestore
			firestore.collection(COLLECTION).document(entity.getId()).set(entity).get();

			log.info("Service account updated successfully: id={}", entity.getId());
			return entity;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while updating service account", e);
			throw new RuntimeException("Failed to update service account", e);
		}
		catch (ExecutionException e) {
			log.error("Failed to update service account", e);
			throw new RuntimeException("Failed to update service account", e);
		}
	}

	@Override
	public boolean delete(String id) {
		try {
			log.info("Deleting service account: id={}", id);

			// Check if exists first
			DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();

			if (!doc.exists()) {
				log.warn("Service account not found for deletion: id={}", id);
				return false;
			}

			firestore.collection(COLLECTION).document(id).delete().get();

			log.info("Service account deleted successfully: id={}", id);
			return true;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while deleting service account", e);
			throw new RuntimeException("Failed to delete service account", e);
		}
		catch (ExecutionException e) {
			log.error("Failed to delete service account", e);
			throw new RuntimeException("Failed to delete service account", e);
		}
	}

	@Override
	public void recordUsage(String id, String ip) {
		try {
			log.debug("Recording usage for service account: id={}, ip={}", id, ip);

			firestore.collection(COLLECTION)
				.document(id)
				.update(Map.of("lastUsedAt", Timestamp.now(), "lastUsedIp", ip, "usageCount", FieldValue.increment(1),
						"modifiedDate", Timestamp.now()))
				.get();

			log.debug("Usage recorded for service account: id={}", id);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while recording service account usage", e);
		}
		catch (ExecutionException e) {
			log.error("Failed to record service account usage", e);
		}
	}

	@Override
	public boolean existsByClientId(String clientId) {
		return findByClientId(clientId).isPresent();
	}

}
