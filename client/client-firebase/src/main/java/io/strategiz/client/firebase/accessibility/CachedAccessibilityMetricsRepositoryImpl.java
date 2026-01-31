package io.strategiz.client.firebase.accessibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;

import io.strategiz.data.accessibility.entity.CachedAccessibilityMetricsEntity;
import io.strategiz.data.accessibility.repository.CachedAccessibilityMetricsRepository;

/**
 * Firestore implementation of CachedAccessibilityMetricsRepository.
 *
 * Collection path: system/accessibility_cache/cache Latest by app:
 * system/accessibility_cache/cache/latest_{appId} History:
 * system/accessibility_cache/cache/{scanId}
 */
@Repository
public class CachedAccessibilityMetricsRepositoryImpl implements CachedAccessibilityMetricsRepository {

	private static final Logger log = LoggerFactory.getLogger(CachedAccessibilityMetricsRepositoryImpl.class);

	private static final String COLLECTION_PATH = "system";

	private static final String SUBCOLLECTION_PATH = "accessibility_cache";

	private static final String CACHE_COLLECTION = "cache";

	private static final String LATEST_PREFIX = "latest_";

	private final Firestore firestore;

	public CachedAccessibilityMetricsRepositoryImpl(Firestore firestore) {
		this.firestore = firestore;
	}

	@Override
	public void save(CachedAccessibilityMetricsEntity entity) {
		try {
			log.info("Saving cached accessibility metrics: scanId={}, appId={}, source={}", entity.getScanId(),
					entity.getAppId(), entity.getScanSource());

			// Save as latest for this specific app
			if (entity.getAppId() != null) {
				DocumentReference latestRef = getCacheCollection().document(LATEST_PREFIX + entity.getAppId());
				latestRef.set(entity).get();
			}

			// Also save with specific scanId for history
			if (entity.getScanId() != null && !entity.getScanId().startsWith(LATEST_PREFIX)) {
				DocumentReference historyRef = getCacheCollection().document(entity.getScanId());
				historyRef.set(entity).get();
			}

			log.info("Cached accessibility metrics saved successfully");
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while saving cached accessibility metrics", e);
			throw new RuntimeException("Failed to save cached accessibility metrics", e);
		}
		catch (ExecutionException e) {
			log.error("Failed to save cached accessibility metrics", e);
			throw new RuntimeException("Failed to save cached accessibility metrics", e);
		}
	}

	@Override
	public Optional<CachedAccessibilityMetricsEntity> getLatestByApp(String appId) {
		try {
			log.debug("Fetching latest cached accessibility metrics for app: {}", appId);

			DocumentSnapshot doc = getCacheCollection().document(LATEST_PREFIX + appId).get().get();

			if (doc.exists()) {
				CachedAccessibilityMetricsEntity entity = doc.toObject(CachedAccessibilityMetricsEntity.class);
				log.debug("Found cached metrics for app {}: scannedAt={}", appId, entity.getScannedAt());
				return Optional.of(entity);
			}

			log.debug("No cached accessibility metrics found for app: {}", appId);
			return Optional.empty();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching cached accessibility metrics for app", e);
			return Optional.empty();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch cached accessibility metrics for app", e);
			return Optional.empty();
		}
	}

	@Override
	public Optional<CachedAccessibilityMetricsEntity> getLatest() {
		try {
			log.debug("Fetching latest cached accessibility metrics across all apps");

			// Query all documents, order by scannedAt descending, limit 1
			ApiFuture<QuerySnapshot> future = getCacheCollection().orderBy("scannedAt", Query.Direction.DESCENDING)
				.limit(1)
				.get();

			QuerySnapshot snapshot = future.get();
			if (!snapshot.isEmpty()) {
				CachedAccessibilityMetricsEntity entity = snapshot.getDocuments()
					.get(0)
					.toObject(CachedAccessibilityMetricsEntity.class);
				log.debug("Found latest cached metrics: scannedAt={}, appId={}", entity.getScannedAt(),
						entity.getAppId());
				return Optional.of(entity);
			}

			log.debug("No cached accessibility metrics found");
			return Optional.empty();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching latest cached accessibility metrics", e);
			return Optional.empty();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch latest cached accessibility metrics", e);
			return Optional.empty();
		}
	}

	@Override
	public Optional<CachedAccessibilityMetricsEntity> findById(String scanId) {
		try {
			log.debug("Fetching cached accessibility metrics by scan ID: {}", scanId);

			DocumentSnapshot doc = getCacheCollection().document(scanId).get().get();

			if (doc.exists()) {
				return Optional.of(doc.toObject(CachedAccessibilityMetricsEntity.class));
			}

			return Optional.empty();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching cached accessibility metrics by ID", e);
			return Optional.empty();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch cached accessibility metrics by ID", e);
			return Optional.empty();
		}
	}

	@Override
	public List<CachedAccessibilityMetricsEntity> getHistoryByApp(String appId, int limit) {
		try {
			log.debug("Fetching accessibility metrics history for app: {}, limit: {}", appId, limit);

			ApiFuture<QuerySnapshot> future = getCacheCollection().whereEqualTo("appId", appId)
				.orderBy("scannedAt", Query.Direction.DESCENDING)
				.limit(limit)
				.get();

			QuerySnapshot snapshot = future.get();
			List<CachedAccessibilityMetricsEntity> results = new ArrayList<>();

			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				// Skip the "latest_" documents
				if (!doc.getId().startsWith(LATEST_PREFIX)) {
					results.add(doc.toObject(CachedAccessibilityMetricsEntity.class));
				}
			}

			log.debug("Found {} historical records for app {}", results.size(), appId);
			return results;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching accessibility metrics history", e);
			return List.of();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch accessibility metrics history", e);
			return List.of();
		}
	}

	@Override
	public List<CachedAccessibilityMetricsEntity> getHistory(int limit) {
		try {
			log.debug("Fetching accessibility metrics history, limit: {}", limit);

			ApiFuture<QuerySnapshot> future = getCacheCollection().orderBy("scannedAt", Query.Direction.DESCENDING)
				.limit(limit + 10) // Fetch extra to account for filtering "latest_" docs
				.get();

			QuerySnapshot snapshot = future.get();
			List<CachedAccessibilityMetricsEntity> results = new ArrayList<>();

			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				// Skip the "latest_" documents
				if (!doc.getId().startsWith(LATEST_PREFIX)) {
					results.add(doc.toObject(CachedAccessibilityMetricsEntity.class));
					if (results.size() >= limit) {
						break;
					}
				}
			}

			log.debug("Found {} historical records", results.size());
			return results;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching accessibility metrics history", e);
			return List.of();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch accessibility metrics history", e);
			return List.of();
		}
	}

	@Override
	public void deleteAll() {
		try {
			log.warn("Deleting all cached accessibility metrics");

			getCacheCollection().listDocuments().forEach(doc -> {
				try {
					doc.delete().get();
				}
				catch (Exception e) {
					log.error("Failed to delete accessibility metrics document: {}", doc.getId(), e);
				}
			});

			log.info("All cached accessibility metrics deleted");
		}
		catch (Exception e) {
			log.error("Failed to delete all cached accessibility metrics", e);
			throw new RuntimeException("Failed to delete all cached accessibility metrics", e);
		}
	}

	private com.google.cloud.firestore.CollectionReference getCacheCollection() {
		return firestore.collection(COLLECTION_PATH).document(SUBCOLLECTION_PATH).collection(CACHE_COLLECTION);
	}

}
