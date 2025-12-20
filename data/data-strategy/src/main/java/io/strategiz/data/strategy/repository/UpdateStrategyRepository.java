package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.Strategy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for updating strategy entities
 * Following Single Responsibility Principle - focused only on update operations
 */
public interface UpdateStrategyRepository {
    
    /**
     * Update a strategy
     */
    Strategy update(String id, String userId, Strategy strategy);
    
    /**
     * Update strategy status
     */
    boolean updateStatus(String id, String userId, String status);
    
    /**
     * Update strategy code
     */
    Optional<Strategy> updateCode(String id, String userId, String code);
    
    /**
     * Update strategy tags
     */
    Optional<Strategy> updateTags(String id, String userId, List<String> tags);
    
    /**
     * Update strategy performance metrics
     */
    Optional<Strategy> updatePerformance(String id, String userId, Map<String, Object> performance);
    
    /**
     * Update strategy backtest results
     */
    Optional<Strategy> updateBacktestResults(String id, String userId, Map<String, Object> backtestResults);
    
    /**
     * Update strategy visibility (public/private)
     */
    Optional<Strategy> updateVisibility(String id, String userId, boolean isPublic);
    
    /**
     * Update strategy parameters
     */
    Optional<Strategy> updateParameters(String id, String userId, Map<String, Object> parameters);
    
    /**
     * Update strategy description
     */
    Optional<Strategy> updateDescription(String id, String userId, String description);

    /**
     * Update strategy deployment status when deploying as alert or bot
     * Sets status to "deployed", deploymentType, deploymentId, and deployedAt
     */
    Optional<Strategy> updateDeploymentStatus(String id, String userId, String deploymentType, String deploymentId);
}