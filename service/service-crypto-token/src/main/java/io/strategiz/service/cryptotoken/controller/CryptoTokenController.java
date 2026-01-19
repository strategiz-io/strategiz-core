package io.strategiz.service.cryptotoken.controller;

import io.strategiz.service.auth.annotation.RequireAuth;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.cryptotoken.model.*;
import io.strategiz.service.cryptotoken.service.CryptoTokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for STRAT crypto token operations.
 *
 * Endpoints:
 * - GET /v1/crypto-token/wallet - Get user's wallet
 * - GET /v1/crypto-token/transactions - Get transaction history
 * - GET /v1/crypto-token/rates - Get conversion rates
 * - POST /v1/crypto-token/purchase/checkout - Create Stripe checkout for token purchase
 * - POST /v1/crypto-token/transfer - Transfer tokens to another user
 * - POST /v1/crypto-token/tip - Tip a creator
 * - POST /v1/crypto-token/convert/ai-credits - Convert tokens to AI credits
 */
@RestController
@RequestMapping("/v1/crypto-token")
public class CryptoTokenController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(CryptoTokenController.class);

	private final CryptoTokenService cryptoTokenService;

	public CryptoTokenController(CryptoTokenService cryptoTokenService) {
		this.cryptoTokenService = cryptoTokenService;
	}

	/**
	 * Get user's crypto wallet.
	 */
	@GetMapping("/wallet")
	@RequireAuth
	public ResponseEntity<CryptoWalletResponse> getWallet() {
		String userId = getCurrentUserId();
		logger.debug("Getting wallet for user {}", userId);
		return ResponseEntity.ok(cryptoTokenService.getWallet(userId));
	}

	/**
	 * Get transaction history.
	 */
	@GetMapping("/transactions")
	@RequireAuth
	public ResponseEntity<List<CryptoTransactionResponse>> getTransactions(
			@RequestParam(defaultValue = "50") int limit) {
		String userId = getCurrentUserId();
		logger.debug("Getting transactions for user {}", userId);
		return ResponseEntity.ok(cryptoTokenService.getTransactions(userId, Math.min(limit, 100)));
	}

	/**
	 * Get current conversion rates.
	 */
	@GetMapping("/rates")
	public ResponseEntity<ConversionRatesResponse> getConversionRates() {
		return ResponseEntity.ok(cryptoTokenService.getConversionRates());
	}

	/**
	 * Create Stripe checkout session for purchasing tokens.
	 */
	@PostMapping("/purchase/checkout")
	@RequireAuth
	public ResponseEntity<Map<String, Object>> createPurchaseCheckout(@Valid @RequestBody PurchaseTokensRequest request) {
		String userId = getCurrentUserId();
		logger.info("Creating purchase checkout for user {} amount {} cents", userId, request.amountInCents());

		// TODO: Create Stripe checkout session
		// For now, return a placeholder
		return ResponseEntity.ok(Map.of("message", "Stripe checkout not yet implemented", "amountCents",
				request.amountInCents(), "tokensToReceive", request.amountInCents())); // $1 = 100 tokens, so cents =
																						// tokens
	}

	/**
	 * Transfer tokens to another user.
	 */
	@PostMapping("/transfer")
	@RequireAuth
	public ResponseEntity<CryptoTransactionResponse> transferTokens(@Valid @RequestBody TransferTokensRequest request) {
		String userId = getCurrentUserId();
		logger.info("User {} transferring {} tokens to {}", userId, request.amount(), request.recipientUserId());
		return ResponseEntity
			.ok(cryptoTokenService.transferTokens(userId, request.recipientUserId(), request.amount(),
					request.description()));
	}

	/**
	 * Tip a creator with tokens.
	 */
	@PostMapping("/tip")
	@RequireAuth
	public ResponseEntity<CryptoTransactionResponse> tipCreator(@RequestBody Map<String, Object> request) {
		String userId = getCurrentUserId();
		String creatorId = (String) request.get("creatorId");
		Long amount = ((Number) request.get("amount")).longValue();

		logger.info("User {} tipping {} tokens to creator {}", userId, amount, creatorId);
		return ResponseEntity.ok(cryptoTokenService.tipCreator(userId, creatorId, amount));
	}

	/**
	 * Convert tokens to AI credits.
	 *
	 * @deprecated STRAT tokens CANNOT be converted to AI credits.
	 *             AI credits come from platform subscription tier only.
	 *             This endpoint will return an error.
	 */
	@Deprecated(forRemoval = true)
	@PostMapping("/convert/ai-credits")
	@RequireAuth
	public ResponseEntity<CryptoTransactionResponse> convertToAiCredits(@RequestBody Map<String, Object> request) {
		String userId = getCurrentUserId();
		Long tokenAmount = ((Number) request.get("tokenAmount")).longValue();

		logger.warn("User {} attempted to convert {} tokens to AI credits (deprecated endpoint)", userId, tokenAmount);
		return ResponseEntity.ok(cryptoTokenService.convertToAiCredits(userId, tokenAmount));
	}

}
