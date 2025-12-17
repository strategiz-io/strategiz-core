package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyBot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ReadStrategyBotRepository.
 */
@Service
public class ReadStrategyBotRepositoryImpl implements ReadStrategyBotRepository {

    private final StrategyBotBaseRepository baseRepository;

    public ReadStrategyBotRepositoryImpl(StrategyBotBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public Optional<StrategyBot> findById(String id) {
        return baseRepository.findById(id);
    }

    @Override
    public List<StrategyBot> findByUserId(String userId) {
        return baseRepository.findAllByUserId(userId);
    }

    @Override
    public List<StrategyBot> findByUserIdAndStatus(String userId, String status) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(bot -> status.equals(bot.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyBot> findByStrategyId(String strategyId) {
        return baseRepository.findAllByStrategyId(strategyId);
    }

    @Override
    public List<StrategyBot> findActiveByUserId(String userId) {
        return findByUserIdAndStatus(userId, "ACTIVE");
    }

    @Override
    public List<StrategyBot> findByProviderId(String userId, String providerId) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(bot -> providerId.equals(bot.getProviderId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyBot> findByEnvironment(String userId, String environment) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(bot -> environment.equals(bot.getEnvironment()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(String id) {
        return baseRepository.findById(id).isPresent();
    }

    @Override
    public int countActiveByUserId(String userId) {
        return findActiveByUserId(userId).size();
    }

    @Override
    public List<StrategyBot> findAllActive() {
        return baseRepository.findAllByStatus("ACTIVE");
    }

    @Override
    public List<StrategyBot> findActiveBotsByTier(String subscriptionTier) {
        return baseRepository.findAllByStatusAndTier("ACTIVE", subscriptionTier);
    }

    @Override
    public List<StrategyBot> findActivePaperBots() {
        return baseRepository.findAllByStatusAndEnvironment("ACTIVE", "PAPER");
    }

    @Override
    public List<StrategyBot> findActiveLiveBots() {
        return baseRepository.findAllByStatusAndEnvironment("ACTIVE", "LIVE");
    }
}
