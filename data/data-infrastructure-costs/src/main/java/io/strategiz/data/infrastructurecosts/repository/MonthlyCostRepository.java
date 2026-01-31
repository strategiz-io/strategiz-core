package io.strategiz.data.infrastructurecosts.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.infrastructurecosts.entity.MonthlyCostEntity;
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
 * Repository for MonthlyCostEntity stored at infrastructure/costs/monthly/{month}
 */
@Repository
public class MonthlyCostRepository {

	private static final Logger logger = LoggerFactory.getLogger(MonthlyCostRepository.class);

	private static final String COLLECTION_PATH = "infrastructure";

	private static final String SUBCOLLECTION_COSTS = "costs";

	private static final String SUBCOLLECTION_MONTHLY = "monthly";

	private final Firestore firestore;

	public MonthlyCostRepository(Firestore firestore) {
		this.firestore = firestore;
	}

	private CollectionReference getMonthlyCollection() {
		return firestore.collection(COLLECTION_PATH).document(SUBCOLLECTION_COSTS).collection(SUBCOLLECTION_MONTHLY);
	}

	/**
	 * Save or update a monthly cost record
	 */
	public MonthlyCostEntity save(MonthlyCostEntity entity) {
		try {
			DocumentReference docRef = getMonthlyCollection().document(entity.getMonth());
			docRef.set(entity).get();
			logger.debug("Saved monthly cost for month: {}", entity.getMonth());
			return entity;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error saving monthly cost: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_SAVE_FAILED, e, "MonthlyCostEntity");
		}
	}

	/**
	 * Find monthly cost by month
	 */
	public Optional<MonthlyCostEntity> findByMonth(String month) {
		try {
			DocumentSnapshot doc = getMonthlyCollection().document(month).get().get();
			if (doc.exists()) {
				return Optional.ofNullable(doc.toObject(MonthlyCostEntity.class));
			}
			return Optional.empty();
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error finding monthly cost: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	/**
	 * Find the most recent N monthly cost records
	 */
	public List<MonthlyCostEntity> findRecent(int limit) {
		try {
			Query query = getMonthlyCollection().orderBy("month", Query.Direction.DESCENDING).limit(limit);

			QuerySnapshot snapshot = query.get().get();
			List<MonthlyCostEntity> results = new ArrayList<>();
			for (DocumentSnapshot doc : snapshot.getDocuments()) {
				MonthlyCostEntity entity = doc.toObject(MonthlyCostEntity.class);
				if (entity != null) {
					results.add(entity);
				}
			}
			return results;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Error finding recent monthly costs: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
			return new ArrayList<>();
		}
	}

	/**
	 * Get current month's cost record, creating if needed
	 */
	public MonthlyCostEntity getCurrentMonth(String month) {
		return findByMonth(month).orElseGet(() -> {
			MonthlyCostEntity entity = new MonthlyCostEntity(month);
			return save(entity);
		});
	}

}
