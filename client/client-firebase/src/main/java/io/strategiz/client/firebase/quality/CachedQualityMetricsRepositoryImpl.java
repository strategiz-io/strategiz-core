package io.strategiz.client.firebase.quality;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import io.strategiz.data.quality.entity.CachedQualityMetricsEntity;
import io.strategiz.data.quality.repository.CachedQualityMetricsRepository;

/**
 * Firestore implementation of CachedQualityMetricsRepository.
 *
 * Collection path: system/quality_cache Latest document: system/quality_cache/latest
 */
@Repository
public class CachedQualityMetricsRepositoryImpl implements CachedQualityMetricsRepository {

	private static final Logger log = LoggerFactory.getLogger(CachedQualityMetricsRepositoryImpl.class);

	private static final String COLLECTION_PATH = "system";

	private static final String SUBCOLLECTION_PATH = "quality_cache";

	private static final String LATEST_DOC_ID = "latest";

	private final Firestore firestore;

	public CachedQualityMetricsRepositoryImpl(Firestore firestore) {
		this.firestore = firestore;
	}

	@Override
	public void save(CachedQualityMetricsEntity entity) {
		try {
			log.info("Saving cached quality metrics: analysisId={}, source={}", entity.getAnalysisId(),
					entity.getAnalysisSource());

			DocumentReference latestRef = firestore.collection(COLLECTION_PATH)
				.document(SUBCOLLECTION_PATH)
				.collection("cache")
				.document(LATEST_DOC_ID);

			// Save as latest
			latestRef.set(entity).get();

			// Also save with specific analysisId for history
			if (entity.getAnalysisId() != null && !entity.getAnalysisId().equals(LATEST_DOC_ID)) {
				DocumentReference historyRef = firestore.collection(COLLECTION_PATH)
					.document(SUBCOLLECTION_PATH)
					.collection("cache")
					.document(entity.getAnalysisId());

				historyRef.set(entity).get();
			}

			log.info("Cached quality metrics saved successfully");
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while saving cached quality metrics", e);
			throw new RuntimeException("Failed to save cached quality metrics", e);
		}
		catch (ExecutionException e) {
			log.error("Failed to save cached quality metrics", e);
			throw new RuntimeException("Failed to save cached quality metrics", e);
		}
	}

	@Override
	public Optional<CachedQualityMetricsEntity> getLatest() {
		try {
			log.debug("Fetching latest cached quality metrics");

			DocumentSnapshot doc = firestore.collection(COLLECTION_PATH)
				.document(SUBCOLLECTION_PATH)
				.collection("cache")
				.document(LATEST_DOC_ID)
				.get()
				.get();

			if (doc.exists()) {
				CachedQualityMetricsEntity entity = doc.toObject(CachedQualityMetricsEntity.class);
				log.debug("Found cached metrics: analyzedAt={}, source={}", entity.getAnalyzedAt(),
						entity.getAnalysisSource());
				return Optional.of(entity);
			}

			log.debug("No cached quality metrics found");
			return Optional.empty();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching cached quality metrics", e);
			return Optional.empty();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch cached quality metrics", e);
			return Optional.empty();
		}
	}

	@Override
	public Optional<CachedQualityMetricsEntity> findById(String analysisId) {
		try {
			log.debug("Fetching cached quality metrics by ID: {}", analysisId);

			DocumentSnapshot doc = firestore.collection(COLLECTION_PATH)
				.document(SUBCOLLECTION_PATH)
				.collection("cache")
				.document(analysisId)
				.get()
				.get();

			if (doc.exists()) {
				return Optional.of(doc.toObject(CachedQualityMetricsEntity.class));
			}

			return Optional.empty();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching cached quality metrics by ID", e);
			return Optional.empty();
		}
		catch (ExecutionException e) {
			log.error("Failed to fetch cached quality metrics by ID", e);
			return Optional.empty();
		}
	}

	@Override
	public void deleteAll() {
		try {
			log.warn("Deleting all cached quality metrics");

			firestore.collection(COLLECTION_PATH)
				.document(SUBCOLLECTION_PATH)
				.collection("cache")
				.listDocuments()
				.forEach(doc -> {
					try {
						doc.delete().get();
					}
					catch (Exception e) {
						log.error("Failed to delete cached metrics document: {}", doc.getId(), e);
					}
				});

			log.info("All cached quality metrics deleted");
		}
		catch (Exception e) {
			log.error("Failed to delete all cached quality metrics", e);
			throw new RuntimeException("Failed to delete all cached quality metrics", e);
		}
	}

}
