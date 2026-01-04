package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.strategy.entity.Strategy;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

/**
 * Base repository for Strategy entities using Firestore
 * Used internally by CRUD repository implementations
 */
@Repository
public class StrategyBaseRepository extends BaseRepository<Strategy> {

    public StrategyBaseRepository(Firestore firestore) {
        super(firestore, Strategy.class);
    }

    @Override
    protected String getModuleName() {
        return "data-strategy";
    }

    /**
     * Find strategies by ownerId field (owner of the strategy)
     */
    public java.util.List<Strategy> findAllByUserId(String userId) {
        return findByField("ownerId", userId);
    }

    /**
     * Find all versions of a strategy by parentStrategyId
     */
    public java.util.List<Strategy> findAllByParentStrategyId(String parentStrategyId) {
        return findByField("parentStrategyId", parentStrategyId);
    }
}