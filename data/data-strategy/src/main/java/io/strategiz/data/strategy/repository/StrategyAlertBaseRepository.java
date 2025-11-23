package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.strategy.entity.StrategyAlert;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

/**
 * Base repository for StrategyAlert entities using Firestore
 * Used internally by CRUD repository implementations
 */
@Repository
public class StrategyAlertBaseRepository extends BaseRepository<StrategyAlert> {

    public StrategyAlertBaseRepository(Firestore firestore) {
        super(firestore, StrategyAlert.class);
    }

    /**
     * Find strategy alerts by userId field
     */
    public java.util.List<StrategyAlert> findAllByUserId(String userId) {
        return findByField("userId", userId);
    }

    /**
     * Find strategy alerts by strategyId field
     */
    public java.util.List<StrategyAlert> findAllByStrategyId(String strategyId) {
        return findByField("strategyId", strategyId);
    }

    /**
     * Find strategy alerts by status
     */
    public java.util.List<StrategyAlert> findAllByStatus(String status) {
        return findByField("status", status);
    }
}
