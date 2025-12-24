package io.strategiz.service.labs.service;

import io.strategiz.data.strategy.repository.DeleteStrategyRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for deleting strategies
 */
@Service
public class DeleteStrategyService extends BaseService {

    private final DeleteStrategyRepository deleteStrategyRepository;
    private final ReadStrategyRepository readStrategyRepository;

    @Autowired
    public DeleteStrategyService(DeleteStrategyRepository deleteStrategyRepository,
                               ReadStrategyRepository readStrategyRepository) {
        this.deleteStrategyRepository = deleteStrategyRepository;
        this.readStrategyRepository = readStrategyRepository;
    }

    @Override
    protected String getModuleName() {
        return "service-labs";
    }
    
    /**
     * Delete a strategy
     * 
     * @param strategyId The strategy ID
     * @param userId The user ID
     * @return true if deleted successfully
     */
    public boolean deleteStrategy(String strategyId, String userId) {
        log.info("Deleting strategy: {} for user: {}", strategyId, userId);
        
        // Check if strategy exists and user has access
        boolean hasAccess = readStrategyRepository.findById(strategyId)
                .map(strategy -> userId.equals(strategy.getUserId()))
                .orElse(false);
        
        if (!hasAccess) {
            log.warn("User {} does not have access to delete strategy {}", userId, strategyId);
            return false;
        }
        
        boolean deleted = deleteStrategyRepository.deleteByIdAndUserId(strategyId, userId);
        
        if (deleted) {
            log.info("Successfully deleted strategy: {} for user: {}", strategyId, userId);
        } else {
            log.error("Failed to delete strategy: {} for user: {}", strategyId, userId);
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
        log.warn("Deleting ALL strategies for user: {}", userId);
        
        int deletedCount = deleteStrategyRepository.deleteAllByUserId(userId);
        
        log.info("Deleted {} strategies for user: {}", deletedCount, userId);
        return deletedCount;
    }
}