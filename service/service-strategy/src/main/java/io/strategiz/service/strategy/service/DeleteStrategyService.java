package io.strategiz.service.strategy.service;

import io.strategiz.data.strategy.repository.DeleteStrategyRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for deleting strategies
 */
@Service
public class DeleteStrategyService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeleteStrategyService.class);
    
    private final DeleteStrategyRepository deleteStrategyRepository;
    private final ReadStrategyRepository readStrategyRepository;
    
    @Autowired
    public DeleteStrategyService(DeleteStrategyRepository deleteStrategyRepository,
                               ReadStrategyRepository readStrategyRepository) {
        this.deleteStrategyRepository = deleteStrategyRepository;
        this.readStrategyRepository = readStrategyRepository;
    }
    
    /**
     * Delete a strategy
     * 
     * @param strategyId The strategy ID
     * @param userId The user ID
     * @return true if deleted successfully
     */
    public boolean deleteStrategy(String strategyId, String userId) {
        logger.info("Deleting strategy: {} for user: {}", strategyId, userId);
        
        // Check if strategy exists and user has access
        boolean hasAccess = readStrategyRepository.findById(strategyId)
                .map(strategy -> userId.equals(strategy.getUserId()))
                .orElse(false);
        
        if (!hasAccess) {
            logger.warn("User {} does not have access to delete strategy {}", userId, strategyId);
            return false;
        }
        
        boolean deleted = deleteStrategyRepository.deleteByIdAndUserId(strategyId, userId);
        
        if (deleted) {
            logger.info("Successfully deleted strategy: {} for user: {}", strategyId, userId);
        } else {
            logger.error("Failed to delete strategy: {} for user: {}", strategyId, userId);
        }
        
        return deleted;
    }
    
    /**
     * Delete all strategies for a user
     * WARNING: This is a dangerous operation and should be used with caution
     * 
     * @param userId The user ID
     * @return The number of strategies deleted
     */
    public int deleteAllUserStrategies(String userId) {
        logger.warn("Deleting ALL strategies for user: {}", userId);
        
        int deletedCount = deleteStrategyRepository.deleteAllByUserId(userId);
        
        logger.info("Deleted {} strategies for user: {}", deletedCount, userId);
        return deletedCount;
    }
}