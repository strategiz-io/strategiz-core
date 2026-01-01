package io.strategiz.service.marketplace.controller;

import io.strategiz.client.stripe.StripeService;
import io.strategiz.data.strategy.entity.StrategyOwnershipTransfer;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.marketplace.service.OwnershipTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for strategy ownership transfer operations.
 *
 * Supports:
 * - Purchase strategy (one-time purchase for full ownership)
 * - View ownership history
 * - Manual transfer (gifts/transfers)
 */
@RestController
@RequestMapping("/v1/marketplace/strategies/{strategyId}/ownership")
@Tag(name = "Ownership Transfer", description = "Strategy ownership transfer and purchase operations")
public class OwnershipTransferController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(OwnershipTransferController.class);

    private final OwnershipTransferService ownershipTransferService;
    private final StripeService stripeService;

    @Autowired
    public OwnershipTransferController(
            OwnershipTransferService ownershipTransferService,
            StripeService stripeService) {
        this.ownershipTransferService = ownershipTransferService;
        this.stripeService = stripeService;
    }

    @Override
    protected String getModuleName() {
        return "service-marketplace";
    }

    /**
     * Purchase a strategy (one-time payment for full ownership).
     *
     * Creates a Stripe checkout session for the strategy purchase.
     *
     * @param strategyId The strategy to purchase
     * @param user The authenticated user
     * @return Checkout session details
     */
    @PostMapping("/purchase")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Purchase strategy",
            description = "Create Stripe checkout session to purchase strategy ownership")
    public ResponseEntity<PurchaseResponse> purchaseStrategy(
            @PathVariable String strategyId,
            @io.strategiz.framework.authorization.annotation.AuthUser AuthenticatedUser user,
            @RequestBody(required = false) PurchaseRequest request) {

        log.info("User {} initiating purchase of strategy {}", user.getUserId(), strategyId);

        // TODO: Validate user can purchase (not already owner, strategy is for sale, etc.)
        // This will be handled by StrategyAccessService

        // TODO: Get strategy details for pricing
        // Strategy strategy = strategyService.getStrategy(strategyId);
        // BigDecimal price = strategy.getPricing().getOneTimePrice();

        // For now, use placeholder values
        BigDecimal priceInDollars = new BigDecimal("99.00");
        long priceInCents = priceInDollars.multiply(new BigDecimal("100")).longValue();

        // Create Stripe checkout session
        // TODO: Get user email from user profile service
        StripeService.CheckoutResult checkoutResult = stripeService.createStrategyCheckoutSession(
                user.getUserId(),
                null, // Email not in AuthenticatedUser - fetch from user profile if needed
                strategyId,
                "Strategy " + strategyId, // TODO: Get actual strategy name
                priceInCents,
                "USD",
                null // Let Stripe create customer if needed
        );

        PurchaseResponse response = new PurchaseResponse(
                checkoutResult.sessionId(),
                checkoutResult.url()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Complete ownership transfer after successful Stripe payment.
     *
     * This is called by a webhook or after checkout success.
     *
     * @param strategyId The strategy being transferred
     * @param request Transfer completion request
     * @return The ownership transfer record
     */
    @PostMapping("/complete")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Complete ownership transfer",
            description = "Finalize ownership transfer after payment confirmation")
    public ResponseEntity<OwnershipTransferResponse> completeTransfer(
            @PathVariable String strategyId,
            @io.strategiz.framework.authorization.annotation.AuthUser AuthenticatedUser user,
            @RequestBody TransferCompletionRequest request) {

        log.info("Completing ownership transfer of strategy {} to user {}",
                strategyId, user.getUserId());

        // Execute the ownership transfer
        StrategyOwnershipTransfer transfer = ownershipTransferService.transferOwnership(
                strategyId,
                user.getUserId(),
                request.purchasePrice(),
                request.transactionId()
        );

        OwnershipTransferResponse response = new OwnershipTransferResponse(
                transfer.getId(),
                transfer.getStrategyId(),
                transfer.getFromOwnerId(),
                transfer.getToOwnerId(),
                transfer.getPurchasePrice(),
                transfer.getSubscribersTransferred(),
                transfer.getMonthlyRevenueTransferred(),
                transfer.getTransferredAt()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get ownership history for a strategy.
     *
     * Shows all previous ownership transfers with dates and prices.
     *
     * @param strategyId The strategy ID
     * @return List of ownership transfers
     */
    @GetMapping("/history")
    @Operation(summary = "Get ownership history",
            description = "View all previous ownership transfers for this strategy")
    public ResponseEntity<List<OwnershipTransferResponse>> getOwnershipHistory(
            @PathVariable String strategyId) {

        log.info("Fetching ownership history for strategy {}", strategyId);

        // TODO: Implement ownership history retrieval
        // List<StrategyOwnershipTransfer> transfers =
        //     ownershipTransferRepository.findByStrategyId(strategyId);

        // For now, return empty list
        return ResponseEntity.ok(List.of());
    }

    /**
     * Manual ownership transfer (for gifts or internal transfers).
     *
     * Only the current owner can initiate this.
     *
     * @param strategyId The strategy to transfer
     * @param user The authenticated user (must be current owner)
     * @param request Transfer request with new owner ID
     * @return The ownership transfer record
     */
    @PostMapping("/transfer")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Manual ownership transfer",
            description = "Transfer ownership without payment (gifts, etc.)")
    public ResponseEntity<OwnershipTransferResponse> manualTransfer(
            @PathVariable String strategyId,
            @io.strategiz.framework.authorization.annotation.AuthUser AuthenticatedUser user,
            @RequestBody ManualTransferRequest request) {

        log.info("User {} transferring strategy {} to user {}",
                user.getUserId(), strategyId, request.newOwnerId());

        // TODO: Validate current user is owner (use StrategyAccessService)

        // Execute the ownership transfer (no payment)
        StrategyOwnershipTransfer transfer = ownershipTransferService.transferOwnership(
                strategyId,
                request.newOwnerId(),
                BigDecimal.ZERO, // No payment for manual transfer
                null // No transaction ID
        );

        OwnershipTransferResponse response = new OwnershipTransferResponse(
                transfer.getId(),
                transfer.getStrategyId(),
                transfer.getFromOwnerId(),
                transfer.getToOwnerId(),
                transfer.getPurchasePrice(),
                transfer.getSubscribersTransferred(),
                transfer.getMonthlyRevenueTransferred(),
                transfer.getTransferredAt()
        );

        return ResponseEntity.ok(response);
    }

    // === DTOs ===

    public record PurchaseRequest(
            String customerId // Optional - existing Stripe customer ID
    ) {}

    public record PurchaseResponse(
            String checkoutSessionId,
            String checkoutUrl
    ) {}

    public record TransferCompletionRequest(
            BigDecimal purchasePrice,
            String transactionId
    ) {}

    public record ManualTransferRequest(
            String newOwnerId,
            String reason // Optional - gift, internal transfer, etc.
    ) {}

    public record OwnershipTransferResponse(
            String id,
            String strategyId,
            String fromOwnerId,
            String toOwnerId,
            BigDecimal purchasePrice,
            Integer subscribersTransferred,
            BigDecimal monthlyRevenueTransferred,
            com.google.cloud.Timestamp transferredAt
    ) {}
}
