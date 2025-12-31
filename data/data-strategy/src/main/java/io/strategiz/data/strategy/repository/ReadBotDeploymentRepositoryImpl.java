package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.BotDeployment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ReadBotDeploymentRepository.
 */
@Service
public class ReadBotDeploymentRepositoryImpl implements ReadBotDeploymentRepository {

    private final BotDeploymentBaseRepository baseRepository;

    public ReadBotDeploymentRepositoryImpl(BotDeploymentBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public Optional<BotDeployment> findById(String id) {
        return baseRepository.findById(id);
    }

    @Override
    public List<BotDeployment> findByUserId(String userId) {
        return baseRepository.findAllByUserId(userId);
    }

    @Override
    public List<BotDeployment> findByUserIdAndStatus(String userId, String status) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(bot -> status.equals(bot.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<BotDeployment> findByStrategyId(String strategyId) {
        return baseRepository.findAllByStrategyId(strategyId);
    }

    @Override
    public List<BotDeployment> findActiveByUserId(String userId) {
        return findByUserIdAndStatus(userId, "ACTIVE");
    }

    @Override
    public List<BotDeployment> findByProviderId(String userId, String providerId) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(bot -> providerId.equals(bot.getProviderId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<BotDeployment> findByEnvironment(String userId, String environment) {
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
    public List<BotDeployment> findAllActive() {
        return baseRepository.findAllByStatus("ACTIVE");
    }

    @Override
    public List<BotDeployment> findActiveBotsByTier(String subscriptionTier) {
        return baseRepository.findAllByStatusAndTier("ACTIVE", subscriptionTier);
    }

    @Override
    public List<BotDeployment> findActivePaperBots() {
        return baseRepository.findAllByStatusAndEnvironment("ACTIVE", "PAPER");
    }

    @Override
    public List<BotDeployment> findActiveLiveBots() {
        return baseRepository.findAllByStatusAndEnvironment("ACTIVE", "LIVE");
    }
}
