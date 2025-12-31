package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.AlertDeploymentHistory;
import com.google.cloud.Timestamp;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for reading strategy alert history entities
 * Following Single Responsibility Principle - focused only on read operations
 */
public interface ReadAlertDeploymentHistoryRepository {

    /**
     * Find alert history by ID
     */
    Optional<AlertDeploymentHistory> findById(String id);

    /**
     * Find all alert history for a user
     */
    List<AlertDeploymentHistory> findByUserId(String userId);

    /**
     * Find alert history by alert ID
     */
    List<AlertDeploymentHistory> findByAlertId(String alertId);

    /**
     * Find alert history by alert ID and user ID (with auth check)
     */
    List<AlertDeploymentHistory> findByAlertIdAndUserId(String alertId, String userId);

    /**
     * Find alert history by symbol for a user
     */
    List<AlertDeploymentHistory> findBySymbol(String userId, String symbol);

    /**
     * Find alert history by signal type (BUY, SELL, HOLD)
     */
    List<AlertDeploymentHistory> findBySignal(String userId, String signal);

    /**
     * Find recent alert history for a user (last N records)
     */
    List<AlertDeploymentHistory> findRecentByUserId(String userId, int limit);

    /**
     * Find alert history within a time range
     */
    List<AlertDeploymentHistory> findByTimeRange(String userId, Timestamp startTime, Timestamp endTime);

    /**
     * Find unsent notifications
     */
    List<AlertDeploymentHistory> findUnsentNotifications(String userId);

    /**
     * Check if alert history exists by ID
     */
    boolean existsById(String id);

    /**
     * Count alert history for an alert
     */
    int countByAlertId(String alertId);
}
