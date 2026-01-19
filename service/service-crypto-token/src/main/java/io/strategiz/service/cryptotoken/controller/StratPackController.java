package io.strategiz.service.cryptotoken.controller;

import io.strategiz.client.stripe.StripeService;
import io.strategiz.data.preferences.entity.PlatformSubscription;
import io.strategiz.data.preferences.entity.StratPackConfig;
import io.strategiz.data.preferences.repository.StratPackConfigRepository;
import io.strategiz.data.preferences.repository.SubscriptionRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.annotation.RequireAuth;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.cryptotoken.exception.CryptoTokenErrors;
import io.strategiz.service.cryptotoken.model.StratPackResponse;
import io.strategiz.service.cryptotoken.service.CryptoTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public controller for STRAT pack purchases.
 *
 * <p>This controller allows users to:</p>
 * <ul>
 *   <li>View available STRAT packs</li>
 *   <li>Purchase STRAT packs via Stripe</li>
 * </ul>
 *
 * <p>STRAT packs are available to ALL tiers, including Explorer (free).</p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET /v1/strat-packs - List available STRAT packs</li>
 *   <li>GET /v1/strat-packs/{packId} - Get specific pack details</li>
 *   <li>POST /v1/strat-packs/{packId}/checkout - Create Stripe checkout session</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/strat-packs")
@Tag(name = "STRAT Packs", description = "STRAT token pack purchases")
public class StratPackController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(StratPackController.class);

	private final StratPackConfigRepository stratPackConfigRepository;

	private final CryptoTokenService cryptoTokenService;

	private final StripeService stripeService;

	private final UserRepository userRepository;

	private final SubscriptionRepository subscriptionRepository;

	public StratPackController(StratPackConfigRepository stratPackConfigRepository,
			CryptoTokenService cryptoTokenService, StripeService stripeService, UserRepository userRepository,
			SubscriptionRepository subscriptionRepository) {
		this.stratPackConfigRepository = stratPackConfigRepository;
		this.cryptoTokenService = cryptoTokenService;
		this.stripeService = stripeService;
		this.userRepository = userRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	/**
	 * Get all available STRAT packs.
	 * Returns only enabled packs, sorted by display order.
	 */
	@GetMapping
	@Operation(summary = "List STRAT packs", description = "Returns all available STRAT packs for purchase")
	public ResponseEntity<List<StratPackResponse>> getAvailablePacks() {
		logger.debug("Getting available STRAT packs");

		List<StratPackConfig> packs = stratPackConfigRepository.getEnabledPacks();
		List<StratPackResponse> responses = packs.stream().map(StratPackResponse::fromConfig).toList();

		return ResponseEntity.ok(responses);
	}

	/**
	 * Get a specific STRAT pack by ID.
	 */
	@GetMapping("/{packId}")
	@Operation(summary = "Get STRAT pack", description = "Returns details for a specific STRAT pack")
	public ResponseEntity<StratPackResponse> getPack(@PathVariable String packId) {
		logger.debug("Getting STRAT pack {}", packId);

		StratPackConfig pack = stratPackConfigRepository.getByPackId(packId)
			.orElseThrow(() -> new StrategizException(CryptoTokenErrors.PACK_NOT_FOUND, "service-crypto-token"));

		if (!Boolean.TRUE.equals(pack.getEnabled())) {
			throw new StrategizException(CryptoTokenErrors.PACK_DISABLED, "service-crypto-token");
		}

		return ResponseEntity.ok(StratPackResponse.fromConfig(pack));
	}

	/**
	 * Create Stripe checkout session for purchasing a STRAT pack.
	 */
	@PostMapping("/{packId}/checkout")
	@RequireAuth
	@Operation(summary = "Purchase STRAT pack", description = "Creates a Stripe checkout session to purchase a STRAT pack")
	public ResponseEntity<Map<String, Object>> createCheckout(@PathVariable String packId,
			@RequestBody(required = false) Map<String, String> request) {
		String userId = getCurrentUserId();
		logger.info("User {} creating checkout for STRAT pack {}", userId, packId);

		// Get pack config
		StratPackConfig pack = stratPackConfigRepository.getByPackId(packId)
			.orElseThrow(() -> new StrategizException(CryptoTokenErrors.PACK_NOT_FOUND, "service-crypto-token"));

		if (!Boolean.TRUE.equals(pack.getEnabled())) {
			throw new StrategizException(CryptoTokenErrors.PACK_DISABLED, "service-crypto-token");
		}

		// Get user email
		UserEntity user = userRepository.findById(userId).orElse(null);
		String userEmail = user != null ? user.getEmail() : null;
		if (userEmail == null) {
			logger.warn("No email found for user {}", userId);
			userEmail = userId + "@strategiz.io"; // Fallback
		}

		// Get existing Stripe customer ID if available
		PlatformSubscription subscription = subscriptionRepository.getByUserId(userId);
		String stripeCustomerId = subscription != null ? subscription.getStripeCustomerId() : null;

		// Get optional success/cancel URLs from request
		String successUrl = request != null ? request.get("successUrl") : null;
		String cancelUrl = request != null ? request.get("cancelUrl") : null;

		// Create Stripe checkout session
		StripeService.CheckoutResult checkout = stripeService.createStratPackCheckoutSession(userId, userEmail,
				pack.getPackId(), pack.getDisplayName(), pack.getPriceCents(), pack.getTotalStrat(), stripeCustomerId,
				successUrl, cancelUrl);

		// Return checkout session details
		Map<String, Object> response = new HashMap<>();
		response.put("sessionId", checkout.sessionId());
		response.put("checkoutUrl", checkout.url());
		response.put("customerId", checkout.customerId());
		response.put("packId", packId);
		response.put("displayName", pack.getDisplayName());
		response.put("priceCents", pack.getPriceCents());
		response.put("stratAmount", pack.getTotalStrat());

		return ResponseEntity.ok(response);
	}

	/**
	 * Handle Stripe webhook for completed STRAT pack purchase. This is called by the
	 * Stripe webhook handler after checkout.session.completed. Not exposed as public
	 * endpoint - called internally.
	 */
	public void handlePurchaseComplete(String userId, String packId, String stripeSessionId) {
		logger.info("Processing STRAT pack purchase completion for user {} pack {} session {}", userId, packId,
				stripeSessionId);

		StratPackConfig pack = stratPackConfigRepository.getByPackId(packId)
			.orElseThrow(() -> new StrategizException(CryptoTokenErrors.PACK_NOT_FOUND, "service-crypto-token"));

		// Credit STRAT tokens to user's wallet
		long totalStrat = pack.getTotalStrat();
		cryptoTokenService.creditStratPackPurchase(userId, totalStrat, packId, stripeSessionId);

		logger.info("User {} credited {} STRAT from pack {} purchase", userId, totalStrat, packId);
	}

}
