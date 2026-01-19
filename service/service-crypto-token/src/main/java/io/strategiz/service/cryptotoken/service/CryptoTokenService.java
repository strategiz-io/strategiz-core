package io.strategiz.service.cryptotoken.service;

import io.strategiz.business.cryptotoken.CryptoTokenBusiness;
import io.strategiz.business.cryptotoken.CryptoTokenErrors;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.cryptotoken.model.ConversionRatesResponse;
import io.strategiz.service.cryptotoken.model.CryptoTransactionResponse;
import io.strategiz.service.cryptotoken.model.CryptoWalletResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for STRAT crypto token operations.
 * Delegates to CryptoTokenBusiness and converts to response DTOs.
 *
 * <p>NOTE: STRAT tokens CANNOT be converted to AI credits.
 * AI credits come from platform subscription tier only.</p>
 */
@Service
public class CryptoTokenService {

	private final CryptoTokenBusiness cryptoTokenBusiness;

	public CryptoTokenService(CryptoTokenBusiness cryptoTokenBusiness) {
		this.cryptoTokenBusiness = cryptoTokenBusiness;
	}

	/**
	 * Get wallet for user, creating if not exists.
	 */
	public CryptoWalletResponse getWallet(String userId) {
		return CryptoWalletResponse.fromEntity(cryptoTokenBusiness.getWallet(userId));
	}

	/**
	 * Get transaction history for user.
	 */
	public List<CryptoTransactionResponse> getTransactions(String userId, int limit) {
		return cryptoTokenBusiness.getTransactions(userId, limit)
			.stream()
			.map(CryptoTransactionResponse::fromEntity)
			.toList();
	}

	/**
	 * Get conversion rates.
	 */
	public ConversionRatesResponse getConversionRates() {
		return ConversionRatesResponse.getDefault();
	}

	/**
	 * Credit tokens to user (used after Stripe purchase completes).
	 */
	public CryptoTransactionResponse creditPurchasedTokens(String userId, long amountInCents, String stripeSessionId) {
		return CryptoTransactionResponse.fromEntity(
				cryptoTokenBusiness.creditPurchasedTokens(userId, amountInCents, stripeSessionId));
	}

	/**
	 * Credit STRAT tokens from a pack purchase.
	 */
	public CryptoTransactionResponse creditStratPackPurchase(String userId, long stratAmount, String packId,
			String stripeSessionId) {
		return CryptoTransactionResponse
			.fromEntity(cryptoTokenBusiness.creditStratPackPurchase(userId, stratAmount, packId, stripeSessionId));
	}

	/**
	 * Credit reward tokens to user.
	 */
	public CryptoTransactionResponse creditRewardTokens(String userId, long tokenAmount, String rewardType,
			String description) {
		return CryptoTransactionResponse
			.fromEntity(cryptoTokenBusiness.creditRewardTokens(userId, tokenAmount, rewardType, description));
	}

	/**
	 * Transfer tokens to another user.
	 */
	public CryptoTransactionResponse transferTokens(String fromUserId, String toUserId, long amount,
			String description) {
		return CryptoTransactionResponse
			.fromEntity(cryptoTokenBusiness.transferTokens(fromUserId, toUserId, amount, description));
	}

	/**
	 * Tip a creator with tokens.
	 */
	public CryptoTransactionResponse tipCreator(String fromUserId, String creatorId, long amount) {
		return CryptoTransactionResponse.fromEntity(cryptoTokenBusiness.tipCreator(fromUserId, creatorId, amount));
	}

	/**
	 * Spend tokens for subscription payment.
	 */
	public CryptoTransactionResponse spendOnSubscription(String userId, String ownerId, long amount,
			String subscriptionId) {
		return CryptoTransactionResponse
			.fromEntity(cryptoTokenBusiness.spendOnSubscription(userId, ownerId, amount, subscriptionId));
	}

	/**
	 * Convert tokens to AI credits.
	 *
	 * @deprecated STRAT tokens CANNOT be converted to AI credits.
	 *             AI credits come from platform subscription tier only.
	 */
	@Deprecated(forRemoval = true)
	public CryptoTransactionResponse convertToAiCredits(String userId, long tokenAmount) {
		throw new StrategizException(CryptoTokenErrors.CONVERSION_NOT_ALLOWED, "service-crypto-token");
	}

}
