package io.strategiz.service.labs.service;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for reading strategies
 */
@Service
public class ReadStrategyService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReadStrategyService.class);
    
    private final ReadStrategyRepository readStrategyRepository;
    
    @Autowired
    public ReadStrategyService(ReadStrategyRepository readStrategyRepository) {
        this.readStrategyRepository = readStrategyRepository;
    }
    
    /**
     * Get all strategies for a user
     */
    public List<Strategy> getUserStrategies(String userId) {
        logger.debug("Fetching strategies for user: {}", userId);
        return readStrategyRepository.findByUserId(userId);
    }
    
    /**
     * Get strategies by status for a user
     */
    public List<Strategy> getUserStrategiesByStatus(String userId, String status) {
        logger.debug("Fetching strategies for user: {} with status: {}", userId, status);
        return readStrategyRepository.findByUserIdAndStatus(userId, status);
    }
    
    /**
     * Get strategies by language for a user
     */
    public List<Strategy> getUserStrategiesByLanguage(String userId, String language) {
        logger.debug("Fetching strategies for user: {} with language: {}", userId, language);
        return readStrategyRepository.findByUserIdAndLanguage(userId, language);
    }
    
    /**
     * Get a specific strategy by ID and user ID
     */
    public Optional<Strategy> getStrategyById(String strategyId, String userId) {
        logger.debug("Fetching strategy: {} for user: {}", strategyId, userId);
        return readStrategyRepository.findById(strategyId)
                .filter(s -> userId.equals(s.getUserId()) || s.isPublic());
    }
    
    /**
     * Get all public strategies
     */
    public List<Strategy> getPublicStrategies() {
        logger.debug("Fetching public strategies");
        return readStrategyRepository.findPublicStrategies();
    }
    
    /**
     * Get public strategies by language
     */
    public List<Strategy> getPublicStrategiesByLanguage(String language) {
        logger.debug("Fetching public strategies with language: {}", language);
        return readStrategyRepository.findPublicStrategiesByLanguage(language);
    }
    
    /**
     * Get public strategies by tags
     * Note: This requires a custom query since Firestore doesn't support array-contains-any in Spring Data
     */
    public List<Strategy> getPublicStrategiesByTags(List<String> tags) {
        logger.debug("Fetching public strategies with tags: {}", tags);
        return readStrategyRepository.findPublicStrategiesByTags(tags);
    }
    
    /**
     * Search strategies by name
     */
    public List<Strategy> searchStrategiesByName(String userId, String searchTerm) {
        logger.debug("Searching strategies for user: {} with term: {}", userId, searchTerm);
        return readStrategyRepository.searchByName(userId, searchTerm);
    }
    
    /**
     * Check if a strategy exists
     */
    public boolean strategyExists(String strategyId) {
        return readStrategyRepository.existsById(strategyId);
    }
}