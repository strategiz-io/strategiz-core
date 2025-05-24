package io.strategiz.strategy.service;

import io.strategiz.strategy.model.Strategy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for strategy business operations
 */
public interface StrategyService {
    
    /**
     * Get all strategies for a user
     * 
     * @param userId the user ID
     * @return list of strategies
     */
    List<Strategy> getAllStrategiesByUserId(String userId);
    
    /**
     * Get a strategy by ID for a specific user
     * 
     * @param strategyId the strategy ID
     * @param userId the user ID
     * @return optional strategy
     */
    Optional<Strategy> getStrategyByIdAndUserId(String strategyId, String userId);
    
    /**
     * Create a new strategy or update an existing one
     * 
     * @param strategy the strategy to save
     * @return the saved strategy with ID
     */
    Strategy saveStrategy(Strategy strategy);
    
    /**
     * Delete a strategy
     * 
     * @param strategyId the strategy ID
     * @param userId the user ID
     * @return true if deleted, false otherwise
     */
    boolean deleteStrategy(String strategyId, String userId);
    
    /**
     * Update strategy status (DRAFT, TESTING, LIVE)
     * 
     * @param strategyId the strategy ID
     * @param userId the user ID
     * @param status the new status
     * @param exchangeInfo exchange info for LIVE strategies
     * @return true if updated, false otherwise
     */
    boolean updateStrategyStatus(String strategyId, String userId, String status, String exchangeInfo);
    
    /**
     * Run a backtest on a strategy
     * 
     * @param strategyId the strategy ID
     * @param userId the user ID
     * @param backtestParams backtest parameters
     * @return backtest results
     */
    Map<String, Object> runBacktest(String strategyId, String userId, Map<String, Object> backtestParams);
}
