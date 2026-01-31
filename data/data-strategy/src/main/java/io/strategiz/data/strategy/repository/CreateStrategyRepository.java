package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.Strategy;

/**
 * Repository interface for creating strategy entities Following Single Responsibility
 * Principle - focused only on create operations
 */
public interface CreateStrategyRepository {

	/**
	 * Create a new strategy
	 * @param strategy The strategy to create
	 * @return The created strategy
	 */
	Strategy create(Strategy strategy);

	/**
	 * Create a new strategy with a specific user ID
	 * @param strategy The strategy to create
	 * @param userId The user ID
	 * @return The created strategy
	 */
	Strategy createWithUserId(Strategy strategy, String userId);

}