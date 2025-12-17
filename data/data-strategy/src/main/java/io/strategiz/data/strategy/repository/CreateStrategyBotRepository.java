package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyBot;

/**
 * Repository interface for creating strategy bot entities.
 * Following Single Responsibility Principle - focused only on create operations.
 */
public interface CreateStrategyBotRepository {

    /**
     * Create a new strategy bot
     */
    StrategyBot create(StrategyBot bot);

    /**
     * Create a new strategy bot with user ID
     */
    StrategyBot createWithUserId(StrategyBot bot, String userId);
}
