package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.strategy.entity.AlertDeploymentHistory;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

/**
 * Base repository for AlertDeploymentHistory entities using Firestore
 * Used internally by CRUD repository implementations
 */
@Repository
public class AlertDeploymentHistoryBaseRepository extends BaseRepository<AlertDeploymentHistory> {

    public AlertDeploymentHistoryBaseRepository(Firestore firestore) {
        super(firestore, AlertDeploymentHistory.class);
    }

    /**
     * Find alert history by userId field
     */
    public java.util.List<AlertDeploymentHistory> findAllByUserId(String userId) {
        return findByField("userId", userId);
    }

    /**
     * Find alert history by alertId field
     */
    public java.util.List<AlertDeploymentHistory> findAllByAlertId(String alertId) {
        return findByField("alertId", alertId);
    }

    /**
     * Find alert history by symbol
     */
    public java.util.List<AlertDeploymentHistory> findAllBySymbol(String symbol) {
        return findByField("symbol", symbol);
    }

    /**
     * Find alert history by signal
     */
    public java.util.List<AlertDeploymentHistory> findAllBySignal(String signal) {
        return findByField("signal", signal);
    }
}
