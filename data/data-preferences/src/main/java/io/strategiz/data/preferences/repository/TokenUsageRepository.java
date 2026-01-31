package io.strategiz.data.preferences.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.SubcollectionRepository;
import io.strategiz.data.preferences.entity.TokenUsageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for TokenUsageRecord stored at users/{userId}/token_usage/{recordId}. Tracks
 * individual AI API calls for audit trail and analytics.
 */
@Repository
public class TokenUsageRepository extends SubcollectionRepository<TokenUsageRecord> {

	private static final Logger logger = LoggerFactory.getLogger(TokenUsageRepository.class);

	public TokenUsageRepository(Firestore firestore) {
		super(firestore, TokenUsageRecord.class);
	}

	@Override
	protected String getModuleName() {
		return "data-preferences";
	}

	@Override
	protected String getParentCollectionName() {
		return "users";
	}

	@Override
	protected String getSubcollectionName() {
		return "token_usage";
	}

	/**
	 * Save a token usage record for a user.
	 * @param userId The user ID
	 * @param record The usage record
	 * @return The saved record
	 */
	public TokenUsageRecord save(String userId, TokenUsageRecord record) {
		validateParentId(userId);
		record.setUserId(userId);
		return saveInSubcollection(userId, record, userId);
	}

	/**
	 * Get all usage records for a user.
	 * @param userId The user ID
	 * @return List of usage records
	 */
	public List<TokenUsageRecord> getByUserId(String userId) {
		validateParentId(userId);
		return findAllInSubcollection(userId);
	}

	/**
	 * Get usage records for a user within a billing period.
	 * @param userId The user ID
	 * @param periodStart Start of the billing period
	 * @param periodEnd End of the billing period
	 * @return List of usage records in the period
	 */
	public List<TokenUsageRecord> getByUserIdAndPeriod(String userId, Instant periodStart, Instant periodEnd) {
		validateParentId(userId);
		try {
			var query = getSubcollection(userId).whereEqualTo("isActive", true)
				.whereGreaterThanOrEqualTo("timestamp", periodStart)
				.whereLessThan("timestamp", periodEnd)
				.orderBy("timestamp", Query.Direction.DESCENDING);

			return query.get().get().getDocuments().stream().map(doc -> {
				TokenUsageRecord record = doc.toObject(TokenUsageRecord.class);
				record.setId(doc.getId());
				return record;
			}).collect(Collectors.toList());
		}
		catch (Exception e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TokenUsageRecord",
					userId);
		}
	}

	/**
	 * Get total credits consumed by a user in a billing period.
	 * @param userId The user ID
	 * @param periodStart Start of the billing period
	 * @return Total credits consumed
	 */
	public int getTotalCreditsInPeriod(String userId, Instant periodStart) {
		List<TokenUsageRecord> records = getByUserIdAndPeriod(userId, periodStart, Instant.now());
		return records.stream().mapToInt(r -> r.getCreditsConsumed() != null ? r.getCreditsConsumed() : 0).sum();
	}

	/**
	 * Get usage records grouped by model for analytics.
	 * @param userId The user ID
	 * @param periodStart Start of the billing period
	 * @return Map of model ID to total credits
	 */
	public java.util.Map<String, Integer> getCreditsByModel(String userId, Instant periodStart) {
		List<TokenUsageRecord> records = getByUserIdAndPeriod(userId, periodStart, Instant.now());
		return records.stream()
			.filter(r -> r.getModelId() != null)
			.collect(Collectors.groupingBy(TokenUsageRecord::getModelId,
					Collectors.summingInt(r -> r.getCreditsConsumed() != null ? r.getCreditsConsumed() : 0)));
	}

	/**
	 * Get usage records grouped by request type for analytics.
	 * @param userId The user ID
	 * @param periodStart Start of the billing period
	 * @return Map of request type to total credits
	 */
	public java.util.Map<String, Integer> getCreditsByRequestType(String userId, Instant periodStart) {
		List<TokenUsageRecord> records = getByUserIdAndPeriod(userId, periodStart, Instant.now());
		return records.stream()
			.filter(r -> r.getRequestType() != null)
			.collect(Collectors.groupingBy(TokenUsageRecord::getRequestType,
					Collectors.summingInt(r -> r.getCreditsConsumed() != null ? r.getCreditsConsumed() : 0)));
	}

	/**
	 * Get recent usage records for a user (last N records).
	 * @param userId The user ID
	 * @param limit Maximum number of records to return
	 * @return List of recent usage records
	 */
	public List<TokenUsageRecord> getRecentUsage(String userId, int limit) {
		validateParentId(userId);
		try {
			var query = getSubcollection(userId).whereEqualTo("isActive", true)
				.orderBy("timestamp", Query.Direction.DESCENDING)
				.limit(limit);

			return query.get().get().getDocuments().stream().map(doc -> {
				TokenUsageRecord record = doc.toObject(TokenUsageRecord.class);
				record.setId(doc.getId());
				return record;
			}).collect(Collectors.toList());
		}
		catch (Exception e) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TokenUsageRecord",
					userId);
		}
	}

}
