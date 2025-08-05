package io.strategiz.data.strategy.repository;

public interface DeleteStrategyRepository {
    
    boolean deleteById(String id);
    
    boolean deleteByIdAndUserId(String id, String userId);
    
    int deleteByUserId(String userId);
    
    int deleteByStatus(String status);
}