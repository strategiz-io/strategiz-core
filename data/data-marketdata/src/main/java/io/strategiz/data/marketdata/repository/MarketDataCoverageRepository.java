package io.strategiz.data.marketdata.repository;

import com.google.cloud.Timestamp;
import io.strategiz.data.marketdata.entity.MarketDataCoverageEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for market data coverage statistics.
 * Stores periodic snapshots of data completeness metrics.
 *
 * Coverage snapshots are calculated daily (or on-demand) and stored in Firestore
 * for historical tracking and trend analysis.
 */
public interface MarketDataCoverageRepository {

    /**
     * Save a coverage snapshot.
     * Creates or updates the snapshot with audit fields.
     *
     * @param entity The coverage entity to save
     * @param userId The user ID triggering the save (typically "system" for scheduled jobs)
     * @return The saved entity
     */
    MarketDataCoverageEntity save(MarketDataCoverageEntity entity, String userId);

    /**
     * Get the most recent coverage snapshot.
     * Sorted by calculatedAt descending.
     *
     * @return Optional containing the latest snapshot, or empty if none exist
     */
    Optional<MarketDataCoverageEntity> findLatest();

    /**
     * Find coverage snapshots within a date range.
     * Useful for viewing historical trends.
     *
     * @param start Start of date range (inclusive)
     * @param end End of date range (exclusive)
     * @return List of snapshots within the range, sorted by calculatedAt descending
     */
    List<MarketDataCoverageEntity> findByDateRange(Timestamp start, Timestamp end);

    /**
     * Find a specific snapshot by ID.
     *
     * @param snapshotId The snapshot ID (e.g., "coverage_2025-12-24T10:00:00Z")
     * @return Optional containing the snapshot if found
     */
    Optional<MarketDataCoverageEntity> findById(String snapshotId);

    /**
     * Find the N most recent snapshots.
     * Useful for displaying recent history.
     *
     * @param limit Maximum number of snapshots to return
     * @return List of recent snapshots, sorted by calculatedAt descending
     */
    List<MarketDataCoverageEntity> findRecent(int limit);

    /**
     * Delete old coverage snapshots based on retention policy.
     * Typically deletes snapshots older than 90 days.
     *
     * @param cutoff Delete all snapshots older than this timestamp
     * @return Number of snapshots deleted
     */
    int deleteOlderThan(Timestamp cutoff);

    /**
     * Count total number of coverage snapshots.
     *
     * @return Count of snapshots in the collection
     */
    long count();

    /**
     * Delete all snapshots (use with caution).
     *
     * @return Number of snapshots deleted
     */
    int deleteAll();
}
