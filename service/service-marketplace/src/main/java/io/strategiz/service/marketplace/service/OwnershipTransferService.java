package io.strategiz.service.marketplace.service;

import io.strategiz.client.stripe.StripeService;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.StrategyOwnershipTransfer;
import io.strategiz.data.strategy.entity.StrategySubscriptionEntity;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.data.strategy.repository.UpdateStrategyRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.marketplace.exception.MarketplaceErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for handling strategy ownership transfers.
 *
 * Orchestrates the complete ownership transfer process including:
 * - Updating strategy owner
 * - Transferring all active subscriptions
 * - Updating Stripe payment routing
 * - Creating audit trail
 * - Notifying subscribers
 */
@Service
public class OwnershipTransferService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(OwnershipTransferService.class);

    private final ReadStrategyRepository readStrategyRepository;
    private final UpdateStrategyRepository updateStrategyRepository;
    private final io.strategiz.data.strategy.repository.StrategySubscriptionRepository subscriptionRepository;
    private final StripeService stripeService;

    @Autowired
    public OwnershipTransferService(
            ReadStrategyRepository readStrategyRepository,
            UpdateStrategyRepository updateStrategyRepository,
            io.strategiz.data.strategy.repository.StrategySubscriptionRepository subscriptionRepository,
            StripeService stripeService) {
        this.readStrategyRepository = readStrategyRepository;
        this.updateStrategyRepository = updateStrategyRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.stripeService = stripeService;
    }

    @Override
    protected String getModuleName() {
        return "service-marketplace";
    }

    /**
     * Transfer ownership of a strategy from current owner to new owner.
     *
     * This is the master orchestration method that:
     * 1. Validates the transfer
     * 2. Updates strategy ownership
     * 3. Transfers all active subscriptions
     * 4. Updates Stripe payment routing
     * 5. Creates ownership transfer record
     * 6. Notifies subscribers
     *
     * @param strategyId The strategy to transfer
     * @param newOwnerId The user who will become the new owner
     * @param purchasePrice The price paid for the strategy
     * @param transactionId Stripe transaction ID
     * @return The ownership transfer record
     */
    public StrategyOwnershipTransfer transferOwnership(
            String strategyId,
            String newOwnerId,
            BigDecimal purchasePrice,
            String transactionId) {

        log.info("Starting ownership transfer for strategy {} to new owner {}", strategyId, newOwnerId);

        // 1. Validate the transfer
        Strategy strategy = validateOwnershipTransfer(strategyId, newOwnerId);
        String fromOwnerId = strategy.getOwnerId();

        // 2. Transfer all active subscriptions to new owner
        TransferResult transferResult = transferSubscriptions(strategyId, newOwnerId);
        log.info("Transferred {} subscriptions with monthly revenue of ${}",
                transferResult.getSubscribersTransferred(),
                transferResult.getMonthlyRevenueTransferred());

        // 3. Update strategy ownership
        updateStrategyOwnership(strategyId, newOwnerId);

        // 4. Create ownership transfer record (audit trail)
        StrategyOwnershipTransfer transfer = createTransferRecord(
                strategy,
                fromOwnerId,
                newOwnerId,
                purchasePrice,
                transactionId,
                transferResult.getSubscribersTransferred(),
                transferResult.getMonthlyRevenueTransferred());

        // 5. TODO: Notify subscribers of ownership change
        // notifySubscribersOfOwnershipChange(strategyId, fromOwnerId, newOwnerId);

        log.info("Successfully transferred ownership of strategy {} from {} to {}",
                strategyId, fromOwnerId, newOwnerId);

        return transfer;
    }

    /**
     * Transfer all active subscriptions from current owner to new owner.
     *
     * @param strategyId The strategy whose subscriptions to transfer
     * @param newOwnerId The new owner who will receive subscription payments
     * @return Transfer result with counts and revenue
     */
    public TransferResult transferSubscriptions(String strategyId, String newOwnerId) {
        log.info("Transferring subscriptions for strategy {} to new owner {}", strategyId, newOwnerId);

        // 1. Query all active subscriptions for this strategy
        List<StrategySubscriptionEntity> activeSubscriptions =
                subscriptionRepository.getStrategySubscribers(strategyId, 10000); // Get all

        int subscribersTransferred = 0;
        BigDecimal monthlyRevenueTransferred = BigDecimal.ZERO;

        // 2. Update each subscription
        for (StrategySubscriptionEntity subscription : activeSubscriptions) {
            if (!subscription.isValid()) {
                continue; // Skip inactive/expired subscriptions
            }

            try {
                // Update ownerId in Firestore
                subscription.setOwnerId(newOwnerId);
                subscriptionRepository.update(subscription, "SYSTEM");

                // TODO: Update payment processor metadata when implemented
                // Note: Owner subscription model doesn't use Stripe per-strategy subscriptions
                // Subscriptions are to the OWNER, not individual strategies
                // Payment routing handled at owner subscription level

                // Calculate revenue
                if (subscription.getPricePaid() != null) {
                    monthlyRevenueTransferred = monthlyRevenueTransferred.add(subscription.getPricePaid());
                }

                subscribersTransferred++;

                log.debug("Transferred subscription {} to new owner {}", subscription.getId(), newOwnerId);
            }
            catch (Exception e) {
                log.error("Failed to transfer subscription {}: {}", subscription.getId(), e.getMessage());
                // Continue with next subscription rather than failing entire transfer
            }
        }

        log.info("Successfully transferred {} subscriptions with ${}/mo revenue",
                subscribersTransferred, monthlyRevenueTransferred);

        TransferResult result = new TransferResult();
        result.setSubscribersTransferred(subscribersTransferred);
        result.setMonthlyRevenueTransferred(monthlyRevenueTransferred);

        return result;
    }

    /**
     * Update the strategy's owner field.
     *
     * @param strategyId The strategy to update
     * @param newOwnerId The new owner ID
     */
    private void updateStrategyOwnership(String strategyId, String newOwnerId) {
        Strategy strategy = readStrategyRepository.findById(strategyId)
                .orElseThrow(() -> new StrategizException(
                        MarketplaceErrorDetails.STRATEGY_NOT_FOUND,
                        getModuleName(),
                        "Strategy not found: " + strategyId));

        strategy.setOwnerId(newOwnerId);
        // Use standard update method - UpdateStrategyRepository doesn't have updateOwner() method
        updateStrategyRepository.update(strategyId, newOwnerId, strategy);

        log.info("Updated strategy {} owner to {}", strategyId, newOwnerId);
    }

    /**
     * Create an ownership transfer record for audit trail.
     *
     * @param strategy The strategy being transferred
     * @param fromOwnerId Previous owner
     * @param toOwnerId New owner
     * @param purchasePrice Price paid
     * @param transactionId Stripe transaction ID
     * @param subscribersTransferred Number of subscribers transferred
     * @param monthlyRevenueTransferred Monthly revenue transferred
     * @return The created transfer record
     */
    private StrategyOwnershipTransfer createTransferRecord(
            Strategy strategy,
            String fromOwnerId,
            String toOwnerId,
            BigDecimal purchasePrice,
            String transactionId,
            int subscribersTransferred,
            BigDecimal monthlyRevenueTransferred) {

        StrategyOwnershipTransfer transfer = new StrategyOwnershipTransfer(
                strategy.getId(),
                fromOwnerId,
                toOwnerId,
                purchasePrice,
                subscribersTransferred,
                monthlyRevenueTransferred);

        transfer.setTransactionId(transactionId);
        transfer.setStrategyName(strategy.getName());

        // TODO: Save to database
        // ownershipTransferRepository.create(transfer);

        log.info("Created ownership transfer record: {}", transfer.getId());

        return transfer;
    }

    /**
     * Validate that the ownership transfer is allowed.
     *
     * @param strategyId The strategy to transfer
     * @param newOwnerId The prospective new owner
     * @return The strategy if validation passes
     * @throws StrategizException if validation fails
     */
    private Strategy validateOwnershipTransfer(String strategyId, String newOwnerId) {
        // Fetch the strategy
        Strategy strategy = readStrategyRepository.findById(strategyId)
                .orElseThrow(() -> new StrategizException(
                        MarketplaceErrorDetails.STRATEGY_NOT_FOUND,
                        getModuleName(),
                        "Strategy not found: " + strategyId));

        // Validate strategy is published and can be sold
        if (strategy.getPublishStatus() == null || !"PUBLISHED".equals(strategy.getPublishStatus())) {
            throw new StrategizException(
                    MarketplaceErrorDetails.STRATEGY_NOT_PUBLISHED,
                    getModuleName(),
                    "Strategy must be published before ownership can be transferred");
        }

        // Validate new owner is different from current owner
        if (newOwnerId.equals(strategy.getOwnerId())) {
            throw new StrategizException(
                    MarketplaceErrorDetails.ALREADY_OWNS_STRATEGY,
                    getModuleName(),
                    "User already owns this strategy");
        }

        // Validate pricing is set for purchase
        if (strategy.getPricing() == null || strategy.getPricing().getOneTimePrice() == null) {
            throw new StrategizException(
                    MarketplaceErrorDetails.STRATEGY_NOT_FOR_SALE,
                    getModuleName(),
                    "Strategy does not have a one-time purchase price set");
        }

        return strategy;
    }

    /**
     * Result object for subscription transfer operations.
     */
    public static class TransferResult {
        private int subscribersTransferred;
        private BigDecimal monthlyRevenueTransferred;

        public int getSubscribersTransferred() {
            return subscribersTransferred;
        }

        public void setSubscribersTransferred(int subscribersTransferred) {
            this.subscribersTransferred = subscribersTransferred;
        }

        public BigDecimal getMonthlyRevenueTransferred() {
            return monthlyRevenueTransferred;
        }

        public void setMonthlyRevenueTransferred(BigDecimal monthlyRevenueTransferred) {
            this.monthlyRevenueTransferred = monthlyRevenueTransferred;
        }
    }
}
