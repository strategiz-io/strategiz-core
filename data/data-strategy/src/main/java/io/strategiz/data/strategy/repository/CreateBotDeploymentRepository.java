package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.BotDeployment;

/**
 * Repository interface for creating strategy bot entities. Following Single
 * Responsibility Principle - focused only on create operations.
 */
public interface CreateBotDeploymentRepository {

	/**
	 * Create a new strategy bot
	 */
	BotDeployment create(BotDeployment bot);

	/**
	 * Create a new strategy bot with user ID
	 */
	BotDeployment createWithUserId(BotDeployment bot, String userId);

}
