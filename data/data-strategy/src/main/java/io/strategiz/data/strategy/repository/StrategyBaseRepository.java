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
    
    /**
     * Find strategies by userId field
     */
    public java.util.List<Strategy> findAllByUserId(String userId) {
        return findByField("userId", userId);
    }
}