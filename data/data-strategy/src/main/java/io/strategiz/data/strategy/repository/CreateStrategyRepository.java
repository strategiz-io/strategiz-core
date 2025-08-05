package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.Strategy;

public interface CreateStrategyRepository {
    
    Strategy create(Strategy strategy);
    
    Strategy createWithUserId(Strategy strategy, String userId);
}