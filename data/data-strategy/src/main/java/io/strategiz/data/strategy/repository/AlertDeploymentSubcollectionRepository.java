package io.strategiz.data.strategy.repository;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.base.repository.SubcollectionRepository;
import io.strategiz.data.strategy.entity.AlertDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AlertDeployment stored at users/{userId}/strategyAlerts/{alertId}
 *
 * This is a subcollection repository - alerts are owned by users and scoped under their document.
 *
 * Benefits of subcollection approach:
 * - Ownership clarity: Alerts clearly belong to users
 * - Automatic cleanup: Delete user → automatically deletes all alerts
 * - Security: Simpler Firebase rules (users can only access their own alerts)
 * - Performance: Faster user queries (no need to filter top-level collection)
 * - Cost: Cheaper queries (don't scan entire database)
 *
 * For cross-user queries (e.g., "find all alerts for strategy X"), use collection group queries.
 */
@Repository
public class AlertDeploymentSubcollectionRepository extends SubcollectionRepository<AlertDeployment> {

    private static final Logger logger = LoggerFactory.getLogger(AlertDeploymentSubcollectionRepository.class);

    public AlertDeploymentSubcollectionRepository(Firestore firestore) {
        super(firestore, AlertDeployment.class);
    }

    @Override
    protected String getParentCollectionName() {
        return "users";
    }

    @Override
    protected String getSubcollectionName() {
        return "alertDeployments";
    }

    /**
     * Get all alerts for a user.
     *
     * @param userId The user ID
     * @return List of alerts
     */
    public List<AlertDeployment> getByUserId(String userId) {
        validateParentId(userId);
        return findAllInSubcollection(userId);
    }

    /**
     * Get alert by ID for a specific user.
     *
     * @param userId The user ID
     * @param alertId The alert ID
     * @return Optional alert
     */
    public Optional<AlertDeployment> getById(String userId, String alertId) {
        validateParentId(userId);
        return findByIdInSubcollection(userId, alertId);
    }

    /**
     * Save alert for a user.
     *
     * @param userId The user ID
     * @param alert The alert to save
     * @return The saved alert
     */
    public AlertDeployment save(String userId, AlertDeployment alert) {
        validateParentId(userId);
        return saveInSubcollection(userId, alert, userId);
    }

    /**
     * Delete alert for a user (soft delete).
     *
     * @param userId The user ID
     * @param alertId The alert ID
     * @return True if deleted
     */
    public boolean delete(String userId, String alertId) {
        validateParentId(userId);
        return deleteInSubcollection(userId, alertId, userId);
    }

    /**
     * Get all active alerts for a user.
     *
     * @param userId The user ID
     * @return List of active alerts
     */
    public List<AlertDeployment> getActiveAlerts(String userId) {
        validateParentId(userId);

        return findAllInSubcollection(userId).stream()
                .filter(alert -> "ACTIVE".equals(alert.getStatus()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get all alerts for a specific strategy (owned by this user).
     *
     * @param userId The user ID
     * @param strategyId The strategy ID
     * @return List of alerts for this strategy
     */
    public List<AlertDeployment> getByStrategyId(String userId, String strategyId) {
        validateParentId(userId);

        return findAllInSubcollection(userId).stream()
                .filter(alert -> strategyId.equals(alert.getStrategyId()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Count active alerts for a user.
     *
     * @param userId The user ID
     * @return Number of active alerts
     */
    public long countActiveAlerts(String userId) {
        validateParentId(userId);

        return findAllInSubcollection(userId).stream()
                .filter(alert -> "ACTIVE".equals(alert.getStatus()))
                .count();
    }

    /**
     * Get all alerts across ALL users for a specific strategy (collection group query).
     *
     * This is useful for calculating strategy.deploymentCount.
     * Note: This is a collection group query - more expensive than subcollection queries.
     *
     * @param strategyId The strategy ID
     * @return List of alerts for this strategy across all users
     */
    public List<AlertDeployment> getAllAlertsForStrategy(String strategyId) {
        try {
            return firestore.collectionGroup("alertDeployments")
                    .whereEqualTo("strategyId", strategyId)
                    .whereEqualTo("isActive", true)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> {
                        AlertDeployment alert = doc.toObject(AlertDeployment.class);
                        alert.setId(doc.getId());
                        return alert;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Error querying collection group for strategy {}", strategyId, e);
            throw new RuntimeException("Failed to query alerts for strategy", e);
        }
    }
}
