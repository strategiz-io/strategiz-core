package io.strategiz.data.infrastructurecosts.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.infrastructurecosts.entity.ClickHouseCostEntity;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Repository for ClickHouseCostEntity stored at infrastructure/costs/clickhouse/{date}
 */
@Repository
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class ClickHouseCostRepository {

	private static final Logger logger = LoggerFactory.getLogger(ClickHouseCostRepository.class);

	private static final String COLLECTION_PATH = "infrastructure";

	private static final String SUBCOLLECTION_COSTS = "costs";

	private static final String SUBCOLLECTION_TIMESCALE = "clickhouse";

	private final Firestore firestore;

	public ClickHouseCostRepository(Firestore firestore) {
		this.firestore = firestore;
	}

	private CollectionReference getClickHouseCollection() {
		return firestore.collection(COLLECTION_PATH).document(SUBCOLLECTION_COSTS).collection(SUBCOLLECTION_TIMESCALE);
	}

	/**
	 * Save or update a ClickHouseDB cost record
	 */
	public ClickHouseCostEntity save(ClickHouseCostEntity entity) {
		try {
			DocumentReference docRef = getClickHouseCollection().document(entity.getDate());
			docRef.set(entity).get();
			logger.debug("Saved ClickHouseDB cost for date: {}", entity.getDate());
			return entity;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error saving ClickHouseDB cost: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_SAVE_FAILED, e, "ClickHouseCostEntity");
		}
	}

	/**
	 * Find ClickHouseDB cost by date
	 */
	public Optional<ClickHouseCostEntity> findByDate(String date) {
		try {
			DocumentSnapshot doc = getClickHouseCollection().document(date).get().get();
			if (doc.exists()) {
				return Optional.ofNullable(doc.toObject(ClickHouseCostEntity.class));
			}
			return Optional.empty();
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error finding ClickHouseDB cost by date: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	/**
	 * Find ClickHouseDB costs for a date range
	 */
	public List<ClickHouseCostEntity> findByDateRange(String startDate, String endDate) {
		try {
			Query query = getClickHouseCollection().whereGreaterThanOrEqualTo("date", startDate)
				.whereLessThanOrEqualTo("date", endDate)
				.orderBy("date", Query.Direction.DESCENDING);

			QuerySnapshot snapshot = query.get().get();
			List<ClickHouseCostEntity> results = new ArrayList<>();
			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				ClickHouseCostEntity entity = doc.toObject(ClickHouseCostEntity.class);
				if (entity != null) {
					results.add(entity);
				}
			}
			return results;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error finding ClickHouseDB costs by range: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			return new ArrayList<>();
		}
	}

	/**
	 * Find the most recent N ClickHouseDB cost records
	 */
	public List<ClickHouseCostEntity> findRecent(int limit) {
		try {
			Query query = getClickHouseCollection().orderBy("date", Query.Direction.DESCENDING).limit(limit);

			QuerySnapshot snapshot = query.get().get();
			List<ClickHouseCostEntity> results = new ArrayList<>();
			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				ClickHouseCostEntity entity = doc.toObject(ClickHouseCostEntity.class);
				if (entity != null) {
					results.add(entity);
				}
			}
			return results;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error finding recent ClickHouseDB costs: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			return new ArrayList<>();
		}
	}

	/**
	 * Delete a ClickHouseDB cost record
	 */
	public void delete(String date) {
		try {
			getClickHouseCollection().document(date).delete().get();
			logger.debug("Deleted ClickHouseDB cost for date: {}", date);
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error deleting ClickHouseDB cost: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
	}

}
