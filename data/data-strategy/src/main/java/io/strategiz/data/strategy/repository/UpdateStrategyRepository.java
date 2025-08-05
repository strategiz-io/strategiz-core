package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.Strategy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UpdateStrategyRepository {
    
    Optional<Strategy> update(String id, Strategy strategy);
    
    Optional<Strategy> updateStatus(String id, String status);
    
    Optional<Strategy> updateCode(String id, String code);
    
    Optional<Strategy> updateTags(String id, List<String> tags);
    
    Optional<Strategy> updatePerformance(String id, Map<String, Object> performance);
}