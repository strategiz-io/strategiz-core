package io.strategiz.strategy.repository;

import io.strategiz.strategy.model.Strategy;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Strategy data access operations
 */
public interface StrategyRepository {
    
    /**
     * Find all strategies for a user
     * 
     * @param userId the user ID
     * @return list of strategies
     */
    List<Strategy> findAllByUserId(String userId);
    
    /**
     * Find a strategy by ID for a specific user
     * 
     * @param strategyId the strategy ID
     * @param userId the user ID
     * @return optional strategy
     */
    Optional<Strategy> findByIdAndUserId(String strategyId, String userId);
    
    /**
     * Save a strategy
     * 
     * @param strategy the strategy to save
     * @return the saved strategy with ID
     */
    Strategy save(Strategy strategy);
    
    /**
     * Delete a strategy
     * 
     * @param strategyId the strategy ID
     * @param userId the user ID
     * @return true if deleted, false otherwise
     */
    boolean deleteByIdAndUserId(String strategyId, String userId);
    
    /**
     * Update strategy status
     * 
     * @param strategyId the strategy ID
     * @param userId the user ID
     * @param status the new status
     * @param deploymentInfo additional deployment info (optional)
     * @return true if updated, false otherwise
     */
    boolean updateStatus(String strategyId, String userId, String status, Object deploymentInfo);
}
