package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyBot;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of DeleteStrategyBotRepository.
 */
@Service
public class DeleteStrategyBotRepositoryImpl implements DeleteStrategyBotRepository {

    private final StrategyBotBaseRepository baseRepository;

    public DeleteStrategyBotRepositoryImpl(StrategyBotBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public boolean delete(String id, String userId) {
        Optional<StrategyBot> existing = baseRepository.findById(id);
        if (existing.isEmpty()) {
            return false;
        }

        StrategyBot bot = existing.get();
        if (!userId.equals(bot.getUserId())) {
            return false;
        }

        // Soft delete using BaseRepository's delete method
        return baseRepository.delete(id, userId);
    }

    @Override
    public void hardDelete(String id) {
        // Note: Hard delete not implemented in BaseRepository
        // Use soft delete instead for now
        throw new UnsupportedOperationException("Hard delete not supported - use soft delete");
    }
}
