package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.strategy.entity.StrategyAlertHistory;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

/**
 * Base repository for StrategyAlertHistory entities using Firestore
 * Used internally by CRUD repository implementations
 */
@Repository
public class StrategyAlertHistoryBaseRepository extends BaseRepository<StrategyAlertHistory> {

    public StrategyAlertHistoryBaseRepository(Firestore firestore) {
        super(firestore, StrategyAlertHistory.class);
    }

    /**
     * Find alert history by userId field
     */
    public java.util.List<StrategyAlertHistory> findAllByUserId(String userId) {
        return findByField("userId", userId);
    }

    /**
     * Find alert history by alertId field
     */
    public java.util.List<StrategyAlertHistory> findAllByAlertId(String alertId) {
        return findByField("alertId", alertId);
    }

    /**
     * Find alert history by symbol
     */
    public java.util.List<StrategyAlertHistory> findAllBySymbol(String symbol) {
        return findByField("symbol", symbol);
    }

    /**
     * Find alert history by signal
     */
    public java.util.List<StrategyAlertHistory> findAllBySignal(String signal) {
        return findByField("signal", signal);
    }
}
