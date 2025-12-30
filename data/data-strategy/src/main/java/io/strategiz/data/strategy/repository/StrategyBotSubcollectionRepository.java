package io.strategiz.data.strategy.repository;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.base.repository.SubcollectionRepository;
import io.strategiz.data.strategy.entity.StrategyBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for StrategyBot stored at users/{userId}/strategyBots/{botId}
 *
 * This is a subcollection repository - bots are owned by users and scoped under their document.
 *
 * Benefits of subcollection approach:
 * - Ownership clarity: Bots clearly belong to users
 * - Automatic cleanup: Delete user → automatically deletes all bots
 * - Security: Simpler Firebase rules (users can only access their own bots)
 * - Performance: Faster user queries (no need to filter top-level collection)
 * - Cost: Cheaper queries (don't scan entire database)
 *
 * For cross-user queries (e.g., "find all bots for strategy X"), use collection group queries.
 */
@Repository
public class StrategyBotSubcollectionRepository extends SubcollectionRepository<StrategyBot> {

    private static final Logger logger = LoggerFactory.getLogger(StrategyBotSubcollectionRepository.class);

    public StrategyBotSubcollectionRepository(Firestore firestore) {
        super(firestore, StrategyBot.class);
    }

    @Override
    protected String getParentCollectionName() {
        return "users";
    }

    @Override
    protected String getSubcollectionName() {
        return "strategyBots";
    }

    /**
     * Get all bots for a user.
     *
     * @param userId The user ID
     * @return List of bots
     */
    public List<StrategyBot> getByUserId(String userId) {
        validateParentId(userId);
        return findAllInSubcollection(userId);
    }

    /**
     * Get bot by ID for a specific user.
     *
     * @param userId The user ID
     * @param botId The bot ID
     * @return Optional bot
     */
    public Optional<StrategyBot> getById(String userId, String botId) {
        validateParentId(userId);
        return findByIdInSubcollection(userId, botId);
    }

    /**
     * Save bot for a user.
     *
     * @param userId The user ID
     * @param bot The bot to save
     * @return The saved bot
     */
    public StrategyBot save(String userId, StrategyBot bot) {
        validateParentId(userId);
        return saveInSubcollection(userId, bot, userId);
    }

    /**
     * Delete bot for a user (soft delete).
     *
     * @param userId The user ID
     * @param botId The bot ID
     * @return True if deleted
     */
    public boolean delete(String userId, String botId) {
        validateParentId(userId);
        return deleteInSubcollection(userId, botId, userId);
    }

    /**
     * Get all running bots for a user.
     *
     * @param userId The user ID
     * @return List of running bots
     */
    public List<StrategyBot> getRunningBots(String userId) {
        validateParentId(userId);

        return findAllInSubcollection(userId).stream()
                .filter(bot -> "RUNNING".equals(bot.getStatus()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get all bots for a specific strategy (owned by this user).
     *
     * @param userId The user ID
     * @param strategyId The strategy ID
     * @return List of bots for this strategy
     */
    public List<StrategyBot> getByStrategyId(String userId, String strategyId) {
        validateParentId(userId);

        return findAllInSubcollection(userId).stream()
                .filter(bot -> strategyId.equals(bot.getStrategyId()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get all LIVE (real money) bots for a user.
     *
     * @param userId The user ID
     * @return List of live bots
     */
    public List<StrategyBot> getLiveBots(String userId) {
        validateParentId(userId);

        return findAllInSubcollection(userId).stream()
                .filter(bot -> "LIVE".equals(bot.getEnvironment()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get all PAPER (simulated) bots for a user.
     *
     * @param userId The user ID
     * @return List of paper bots
     */
    public List<StrategyBot> getPaperBots(String userId) {
        validateParentId(userId);

        return findAllInSubcollection(userId).stream()
                .filter(bot -> "PAPER".equals(bot.getEnvironment()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Count running bots for a user.
     *
     * @param userId The user ID
     * @return Number of running bots
     */
    public long countRunningBots(String userId) {
        validateParentId(userId);

        return findAllInSubcollection(userId).stream()
                .filter(bot -> "RUNNING".equals(bot.getStatus()))
                .count();
    }

    /**
     * Get all bots across ALL users for a specific strategy (collection group query).
     *
     * This is useful for calculating strategy.deploymentCount.
     * Note: This is a collection group query - more expensive than subcollection queries.
     *
     * @param strategyId The strategy ID
     * @return List of bots for this strategy across all users
     */
    public List<StrategyBot> getAllBotsForStrategy(String strategyId) {
        try {
            return firestore.collectionGroup("strategyBots")
                    .whereEqualTo("strategyId", strategyId)
                    .whereEqualTo("isActive", true)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> {
                        StrategyBot bot = doc.toObject(StrategyBot.class);
                        bot.setId(doc.getId());
                        return bot;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Error querying collection group for strategy {}", strategyId, e);
            throw new RuntimeException("Failed to query bots for strategy", e);
        }
    }
}
