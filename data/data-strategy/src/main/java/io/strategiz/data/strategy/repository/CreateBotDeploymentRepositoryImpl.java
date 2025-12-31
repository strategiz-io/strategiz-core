package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.BotDeployment;
import org.springframework.stereotype.Service;

/**
 * Implementation of CreateBotDeploymentRepository.
 */
@Service
public class CreateBotDeploymentRepositoryImpl implements CreateBotDeploymentRepository {

    private final BotDeploymentBaseRepository baseRepository;

    public CreateBotDeploymentRepositoryImpl(BotDeploymentBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public BotDeployment create(BotDeployment bot) {
        return baseRepository.save(bot, bot.getUserId());
    }

    @Override
    public BotDeployment createWithUserId(BotDeployment bot, String userId) {
        bot.setUserId(userId);
        return baseRepository.save(bot, userId);
    }
}
