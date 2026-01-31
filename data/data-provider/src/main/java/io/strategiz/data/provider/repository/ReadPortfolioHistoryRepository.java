package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioHistoryEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for reading portfolio history entities from Firestore.
 */
public interface ReadPortfolioHistoryRepository {

	/**
	 * Find a history snapshot by user ID and date.
	 * @param userId User ID
	 * @param date Snapshot date
	 * @return Optional containing the snapshot if found
	 */
	Optional<PortfolioHistoryEntity> findByUserIdAndDate(String userId, LocalDate date);

	/**
	 * Find all history snapshots for a user.
	 * @param userId User ID
	 * @return List of all snapshots for the user
	 */
	List<PortfolioHistoryEntity> findByUserId(String userId);

	/**
	 * Find history snapshots for a user within a date range.
	 * @param userId User ID
	 * @param startDate Start date (inclusive)
	 * @param endDate End date (inclusive)
	 * @return List of snapshots within the date range
	 */
	List<PortfolioHistoryEntity> findByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate);

	/**
	 * Find the most recent history snapshot for a user.
	 * @param userId User ID
	 * @return Optional containing the most recent snapshot if found
	 */
	Optional<PortfolioHistoryEntity> findLatestByUserId(String userId);

	/**
	 * Find the last N history snapshots for a user.
	 * @param userId User ID
	 * @param limit Maximum number of snapshots to return
	 * @return List of the most recent snapshots
	 */
	List<PortfolioHistoryEntity> findRecentByUserId(String userId, int limit);

}
