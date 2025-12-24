package io.strategiz.service.marketplace.service;

import io.strategiz.data.strategy.entity.PricingType;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.StrategyPricing;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.data.strategy.repository.UpdateStrategyRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.marketplace.exception.MarketplaceErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for managing strategy publishing and pricing.
 *
 * Key rules:
 * - Only strategy owner can publish/unpublish
 * - Publishing requires pricing configuration
 * - Once published, pricing can be updated but not removed
 */
@Service
public class StrategyPublishService {

    private static final Logger log = LoggerFactory.getLogger(StrategyPublishService.class);
    private static final String MODULE_NAME = "service-marketplace";

    private final ReadStrategyRepository readStrategyRepo;
    private final UpdateStrategyRepository updateStrategyRepo;

    @Autowired
    public StrategyPublishService(
            ReadStrategyRepository readStrategyRepo,
            UpdateStrategyRepository updateStrategyRepo) {
        this.readStrategyRepo = readStrategyRepo;
        this.updateStrategyRepo = updateStrategyRepo;
    }

    /**
     * Publish a strategy with pricing configuration.
     *
     * @param strategyId  The strategy to publish
     * @param userId      The user publishing (must be owner)
     * @param pricingType The pricing type (FREE, ONE_TIME, SUBSCRIPTION)
     * @param oneTimePrice Price for one-time purchase (if applicable)
     * @param monthlyPrice Price for monthly subscription (if applicable)
     * @return The updated strategy
     */
    public Strategy publishStrategy(
            String strategyId,
            String userId,
            PricingType pricingType,
            BigDecimal oneTimePrice,
            BigDecimal monthlyPrice) {

        Strategy strategy = getStrategyAndVerifyOwner(strategyId, userId);

        // Create pricing configuration
        StrategyPricing pricing;
        switch (pricingType) {
            case FREE:
                pricing = StrategyPricing.free();
                break;
            case ONE_TIME:
                if (oneTimePrice == null || oneTimePrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new StrategizException(
                            MarketplaceErrorDetails.INVALID_ONE_TIME_PRICE,
                            MODULE_NAME,
                            "One-time price is required and must be positive"
                    );
                }
                pricing = StrategyPricing.oneTime(oneTimePrice, "USD");
                break;
            case SUBSCRIPTION:
                if (monthlyPrice == null || monthlyPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new StrategizException(
                            MarketplaceErrorDetails.INVALID_MONTHLY_PRICE,
                            MODULE_NAME,
                            "Monthly price is required and must be positive"
                    );
                }
                pricing = StrategyPricing.subscription(monthlyPrice, "USD", null);
                break;
            default:
                throw new StrategizException(
                        MarketplaceErrorDetails.INVALID_PRICING_TYPE,
                        MODULE_NAME,
                        pricingType.toString()
                );
        }

        // Update strategy and publish
        strategy.publish(pricing);

        Strategy updated = updateStrategyRepo.update(strategy.getId(), userId, strategy);
        log.info("User {} published strategy {} with pricing type {}", userId, strategyId, pricingType);
        return updated;
    }

    /**
     * Unpublish a strategy (make it private again).
     */
    public Strategy unpublishStrategy(String strategyId, String userId) {
        Strategy strategy = getStrategyAndVerifyOwner(strategyId, userId);

        if (!strategy.isPublished()) {
            // Already unpublished, no action needed
            return strategy;
        }

        strategy.unpublish();
        Strategy updated = updateStrategyRepo.update(strategy.getId(), userId, strategy);
        log.info("User {} unpublished strategy {}", userId, strategyId);
        return updated;
    }

    /**
     * Update pricing for a published strategy.
     */
    public Strategy updatePricing(
            String strategyId,
            String userId,
            PricingType pricingType,
            BigDecimal oneTimePrice,
            BigDecimal monthlyPrice) {

        Strategy strategy = getStrategyAndVerifyOwner(strategyId, userId);

        if (!strategy.isPublished()) {
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_PUBLISHED, MODULE_NAME);
        }

        // Create new pricing configuration
        StrategyPricing pricing;
        switch (pricingType) {
            case FREE:
                pricing = StrategyPricing.free();
                break;
            case ONE_TIME:
                if (oneTimePrice == null || oneTimePrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new StrategizException(
                            MarketplaceErrorDetails.INVALID_ONE_TIME_PRICE,
                            MODULE_NAME,
                            "One-time price is required and must be positive"
                    );
                }
                pricing = StrategyPricing.oneTime(oneTimePrice, "USD");
                break;
            case SUBSCRIPTION:
                if (monthlyPrice == null || monthlyPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new StrategizException(
                            MarketplaceErrorDetails.INVALID_MONTHLY_PRICE,
                            MODULE_NAME,
                            "Monthly price is required and must be positive"
                    );
                }
                pricing = StrategyPricing.subscription(monthlyPrice, "USD", null);
                break;
            default:
                throw new StrategizException(
                        MarketplaceErrorDetails.INVALID_PRICING_TYPE,
                        MODULE_NAME,
                        pricingType.toString()
                );
        }

        strategy.setPricing(pricing);
        Strategy updated = updateStrategyRepo.update(strategy.getId(), userId, strategy);
        log.info("User {} updated pricing for strategy {} to {}", userId, strategyId, pricingType);
        return updated;
    }

    /**
     * Get strategy details including publishing status.
     */
    public Strategy getStrategyDetails(String strategyId) {
        return readStrategyRepo.findById(strategyId)
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, MODULE_NAME));
    }

    // Private helper methods

    private Strategy getStrategyAndVerifyOwner(String strategyId, String userId) {
        Strategy strategy = readStrategyRepo.findById(strategyId)
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, MODULE_NAME));

        if (!strategy.getUserId().equals(userId)) {
            throw new StrategizException(MarketplaceErrorDetails.UNAUTHORIZED_UPDATE, MODULE_NAME);
        }

        return strategy;
    }
}
