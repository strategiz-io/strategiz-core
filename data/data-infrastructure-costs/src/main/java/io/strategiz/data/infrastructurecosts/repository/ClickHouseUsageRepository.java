package io.strategiz.data.infrastructurecosts.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.infrastructurecosts.entity.ClickHouseUsageEntity;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Repository for ClickHouseUsageEntity stored at infrastructure/usage/clickhouse/{date}
 */
@Repository
public class ClickHouseUsageRepository {

	private static final Logger logger = LoggerFactory.getLogger(ClickHouseUsageRepository.class);

	private static final String COLLECTION_PATH = "infrastructure";

	private static final String SUBCOLLECTION_USAGE = "usage";

	private static final String SUBCOLLECTION_TIMESCALE = "clickhouse";

	private final Firestore firestore;

	public ClickHouseUsageRepository(Firestore firestore) {
		this.firestore = firestore;
	}

	private CollectionReference getClickHouseCollection() {
		return firestore.collection(COLLECTION_PATH)
			.document(SUBCOLLECTION_USAGE)
			.collection(SUBCOLLECTION_TIMESCALE);
	}

	/**
	 * Save or update a ClickHouseDB usage record
	 */
	public ClickHouseUsageEntity save(ClickHouseUsageEntity entity) {
		try {
			DocumentReference docRef = getClickHouseCollection().document(entity.getDate());
			docRef.set(entity).get();
			logger.debug("Saved ClickHouseDB usage for date: {}", entity.getDate());
			return entity;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error saving ClickHouseDB usage: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_SAVE_FAILED, e,
					"ClickHouseUsageEntity");
		}
	}

	/**
	 * Find ClickHouseDB usage by date
	 */
	public Optional<ClickHouseUsageEntity> findByDate(String date) {
		try {
			DocumentSnapshot doc = getClickHouseCollection().document(date).get().get();
			if (doc.exists()) {
				return Optional.ofNullable(doc.toObject(ClickHouseUsageEntity.class));
			}
			return Optional.empty();
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error finding ClickHouseDB usage by date: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	/**
	 * Find ClickHouseDB usage for a date range
	 */
	public List<ClickHouseUsageEntity> findByDateRange(String startDate, String endDate) {
		try {
			Query query = getClickHouseCollection().whereGreaterThanOrEqualTo("date", startDate)
				.whereLessThanOrEqualTo("date", endDate)
				.orderBy("date", Query.Direction.DESCENDING);

			QuerySnapshot snapshot = query.get().get();
			List<ClickHouseUsageEntity> results = new ArrayList<>();
			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				ClickHouseUsageEntity entity = doc.toObject(ClickHouseUsageEntity.class);
				if (entity != null) {
					results.add(entity);
				}
			}
			return results;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error finding ClickHouseDB usage by range: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			return new ArrayList<>();
		}
	}

	/**
	 * Find the most recent N ClickHouseDB usage records
	 */
	public List<ClickHouseUsageEntity> findRecent(int limit) {
		try {
			Query query = getClickHouseCollection().orderBy("date", Query.Direction.DESCENDING).limit(limit);

			QuerySnapshot snapshot = query.get().get();
			List<ClickHouseUsageEntity> results = new ArrayList<>();
			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				ClickHouseUsageEntity entity = doc.toObject(ClickHouseUsageEntity.class);
				if (entity != null) {
					results.add(entity);
				}
			}
			return results;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error finding recent ClickHouseDB usage: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			return new ArrayList<>();
		}
	}

	/**
	 * Delete a ClickHouseDB usage record
	 */
	public void delete(String date) {
		try {
			getClickHouseCollection().document(date).delete().get();
			logger.debug("Deleted ClickHouseDB usage for date: {}", date);
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error deleting ClickHouseDB usage: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
	}

}
