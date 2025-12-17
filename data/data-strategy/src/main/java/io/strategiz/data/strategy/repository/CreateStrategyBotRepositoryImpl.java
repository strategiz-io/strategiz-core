package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyBot;
import org.springframework.stereotype.Service;

/**
 * Implementation of CreateStrategyBotRepository.
 */
@Service
public class CreateStrategyBotRepositoryImpl implements CreateStrategyBotRepository {

    private final StrategyBotBaseRepository baseRepository;

    public CreateStrategyBotRepositoryImpl(StrategyBotBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public StrategyBot create(StrategyBot bot) {
        return baseRepository.save(bot, bot.getUserId());
    }

    @Override
    public StrategyBot createWithUserId(StrategyBot bot, String userId) {
        bot.setUserId(userId);
        return baseRepository.save(bot, userId);
    }
}
