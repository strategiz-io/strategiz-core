package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.Strategy;

import java.util.List;
import java.util.Optional;

public interface ReadStrategyRepository {
    
    Optional<Strategy> findById(String id);
    
    List<Strategy> findByUserId(String userId);
    
    List<Strategy> findByUserIdAndStatus(String userId, String status);
    
    List<Strategy> findByUserIdAndLanguage(String userId, String language);
    
    List<Strategy> findPublicStrategies();
    
    List<Strategy> findPublicStrategiesByLanguage(String language);
    
    List<Strategy> findPublicStrategiesByTags(List<String> tags);
    
    List<Strategy> searchByName(String userId, String searchTerm);
    
    boolean existsById(String id);
}