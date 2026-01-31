package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.strategy.entity.BotDeployment;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

/**
 * Base repository for BotDeployment entities using Firestore. Used internally by CRUD
 * repository implementations.
 */
@Repository
public class BotDeploymentBaseRepository extends BaseRepository<BotDeployment> {

	public BotDeploymentBaseRepository(Firestore firestore) {
		super(firestore, BotDeployment.class);
	}

	@Override
	protected String getModuleName() {
		return "data-strategy";
	}

	/**
	 * Find strategy bots by userId field
	 */
	public java.util.List<BotDeployment> findAllByUserId(String userId) {
		return findByField("userId", userId);
	}

	/**
	 * Find strategy bots by strategyId field
	 */
	public java.util.List<BotDeployment> findAllByStrategyId(String strategyId) {
		return findByField("strategyId", strategyId);
	}

	/**
	 * Find strategy bots by status
	 */
	public java.util.List<BotDeployment> findAllByStatus(String status) {
		return findByField("status", status);
	}

	/**
	 * Find strategy bots by environment (PAPER or LIVE)
	 */
	public java.util.List<BotDeployment> findAllByEnvironment(String environment) {
		return findByField("environment", environment);
	}

	/**
	 * Find strategy bots by status and subscription tier
	 */
	public java.util.List<BotDeployment> findAllByStatusAndTier(String status, String subscriptionTier) {
		try {
			com.google.cloud.firestore.Query query = getCollection().whereEqualTo("status", status)
				.whereEqualTo("subscriptionTier", subscriptionTier)
				.whereEqualTo("isActive", true);

			java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			return docs.stream().map(doc -> {
				BotDeployment entity = doc.toObject(BotDeployment.class);
				entity.setId(doc.getId());
				return entity;
			}).collect(java.util.stream.Collectors.toList());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"StrategyBot");
		}
		catch (java.util.concurrent.ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "StrategyBot");
		}
	}

	/**
	 * Find active bots by status and environment
	 */
	public java.util.List<BotDeployment> findAllByStatusAndEnvironment(String status, String environment) {
		try {
			com.google.cloud.firestore.Query query = getCollection().whereEqualTo("status", status)
				.whereEqualTo("environment", environment)
				.whereEqualTo("isActive", true);

			java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> docs = query.get().get().getDocuments();

			return docs.stream().map(doc -> {
				BotDeployment entity = doc.toObject(BotDeployment.class);
				entity.setId(doc.getId());
				return entity;
			}).collect(java.util.stream.Collectors.toList());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e,
					"StrategyBot");
		}
		catch (java.util.concurrent.ExecutionException e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "StrategyBot");
		}
	}

}
