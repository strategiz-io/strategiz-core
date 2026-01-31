package io.strategiz.business.cryptotoken;

import com.google.cloud.Timestamp;
import io.strategiz.data.cryptotoken.entity.CryptoTransaction;
import io.strategiz.data.cryptotoken.entity.CryptoWallet;
import io.strategiz.data.cryptotoken.repository.CryptoTransactionRepository;
import io.strategiz.data.cryptotoken.repository.CryptoWalletRepository;
import io.strategiz.data.preferences.entity.PlatformConfig;
import io.strategiz.data.preferences.repository.PlatformConfigRepository;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for STRAT crypto token operations.
 *
 * <p>
 * Platform fee is configurable via PlatformConfig entity (admin console). STRAT tokens
 * are the universal currency for AI usage, tips, and owner subscriptions.
 * </p>
 *
 * <p>
 * Monthly STRAT allocations are credited to user wallets on subscription creation and
 * renewal. AI usage debits STRAT from the wallet.
 * </p>
 */
@Component
public class CryptoTokenBusiness {

	private static final Logger logger = LoggerFactory.getLogger(CryptoTokenBusiness.class);

	private static final String MODULE_NAME = "business-crypto-token";

	private final CryptoWalletRepository walletRepository;

	private final CryptoTransactionRepository transactionRepository;

	private final PlatformConfigRepository platformConfigRepository;

	public CryptoTokenBusiness(CryptoWalletRepository walletRepository,
			CryptoTransactionRepository transactionRepository, PlatformConfigRepository platformConfigRepository) {
		this.walletRepository = walletRepository;
		this.transactionRepository = transactionRepository;
		this.platformConfigRepository = platformConfigRepository;
	}

	/**
	 * Get the current platform fee percentage from config.
	 * @return Platform fee as decimal (e.g., 0.15 for 15%)
	 */
	public double getPlatformFeePercent() {
		PlatformConfig config = platformConfigRepository.getCurrent();
		BigDecimal feePercent = config.getPlatformFeePercent();
		return feePercent != null ? feePercent.doubleValue() : 0.15;
	}

	/**
	 * Get wallet for user, creating if not exists.
	 */
	public CryptoWallet getWallet(String userId) {
		return walletRepository.getOrCreateWallet(userId);
	}

	/**
	 * Get transaction history for user.
	 */
	public List<CryptoTransaction> getTransactions(String userId, int limit) {
		return transactionRepository.findByUserId(userId, limit);
	}

	/**
	 * Credit tokens to user (used after Stripe purchase completes).
	 */
	public CryptoTransaction creditPurchasedTokens(String userId, long amountInCents, String stripeSessionId) {
		long tokenAmount = amountInCents * CryptoWallet.MICRO_UNITS; // $1 = 100 STRAT =
																		// 100,000,000
																		// micro-units

		CryptoWallet wallet = walletRepository.credit(userId, tokenAmount, userId);

		// Update total purchased
		wallet.setTotalPurchased(wallet.getTotalPurchased() + tokenAmount);
		walletRepository.save(wallet, userId);

		// Create transaction record
		CryptoTransaction tx = new CryptoTransaction();
		tx.setId(UUID.randomUUID().toString());
		tx.setUserId(userId);
		tx.setType(CryptoTransaction.TYPE_PURCHASE);
		tx.setAmount(tokenAmount);
		tx.setBalanceAfter(wallet.getBalance());
		tx.setReferenceType(CryptoTransaction.REF_STRIPE);
		tx.setReferenceId(stripeSessionId);
		tx.setDescription("Purchased STRAT tokens");
		tx.setStatus(CryptoTransaction.STATUS_COMPLETED);
		tx.setCreatedAt(Timestamp.now());
		tx.setCompletedAt(Timestamp.now());

		transactionRepository.save(tx, userId);
		logger.info("Credited {} tokens to user {} from purchase {}", tokenAmount, userId, stripeSessionId);

		return tx;
	}

	/**
	 * Credit STRAT tokens from a pack purchase.
	 * @param userId User to credit
	 * @param stratAmount Amount of STRAT tokens to credit (not micro-units)
	 * @param packId The STRAT pack ID
	 * @param stripeSessionId Stripe checkout session ID for reference
	 * @return Transaction record
	 */
	public CryptoTransaction creditStratPackPurchase(String userId, long stratAmount, String packId,
			String stripeSessionId) {
		long microUnits = stratAmount * CryptoWallet.MICRO_UNITS;

		CryptoWallet wallet = walletRepository.credit(userId, microUnits, userId);

		// Update total purchased
		wallet.setTotalPurchased(wallet.getTotalPurchased() + microUnits);
		walletRepository.save(wallet, userId);

		// Create transaction record
		CryptoTransaction tx = new CryptoTransaction();
		tx.setId(UUID.randomUUID().toString());
		tx.setUserId(userId);
		tx.setType(CryptoTransaction.TYPE_PURCHASE);
		tx.setAmount(microUnits);
		tx.setBalanceAfter(wallet.getBalance());
		tx.setReferenceType(CryptoTransaction.REF_STRAT_PACK);
		tx.setReferenceId(packId + ":" + stripeSessionId);
		tx.setDescription("STRAT pack purchase: " + packId);
		tx.setStatus(CryptoTransaction.STATUS_COMPLETED);
		tx.setCreatedAt(Timestamp.now());
		tx.setCompletedAt(Timestamp.now());

		transactionRepository.save(tx, userId);
		logger.info("Credited {} STRAT to user {} from pack {} purchase (session: {})", stratAmount, userId, packId,
				stripeSessionId);

		return tx;
	}

	/**
	 * Credit reward tokens to user.
	 */
	public CryptoTransaction creditRewardTokens(String userId, long tokenAmount, String rewardType,
			String description) {
		long microUnits = tokenAmount * CryptoWallet.MICRO_UNITS;

		CryptoWallet wallet = walletRepository.credit(userId, microUnits, userId);

		CryptoTransaction tx = new CryptoTransaction();
		tx.setId(UUID.randomUUID().toString());
		tx.setUserId(userId);
		tx.setType(CryptoTransaction.TYPE_EARN);
		tx.setAmount(microUnits);
		tx.setBalanceAfter(wallet.getBalance());
		tx.setReferenceType(CryptoTransaction.REF_REWARD);
		tx.setReferenceId(rewardType);
		tx.setDescription(description);
		tx.setStatus(CryptoTransaction.STATUS_COMPLETED);
		tx.setCreatedAt(Timestamp.now());
		tx.setCompletedAt(Timestamp.now());

		transactionRepository.save(tx, userId);
		logger.info("Credited {} reward tokens to user {} for {}", tokenAmount, userId, rewardType);

		return tx;
	}

	/**
	 * Transfer tokens to another user.
	 */
	public CryptoTransaction transferTokens(String fromUserId, String toUserId, long amount, String description) {
		if (fromUserId.equals(toUserId)) {
			throw new StrategizException(CryptoTokenErrors.TRANSFER_TO_SELF, MODULE_NAME);
		}

		CryptoWallet fromWallet = walletRepository.findByUserId(fromUserId)
			.orElseThrow(() -> new StrategizException(CryptoTokenErrors.WALLET_NOT_FOUND, MODULE_NAME));

		if (!fromWallet.hasSufficientBalance(amount)) {
			throw new StrategizException(CryptoTokenErrors.INSUFFICIENT_BALANCE, MODULE_NAME);
		}

		// Calculate platform fee (configurable via PlatformConfig)
		double feePercent = getPlatformFeePercent();
		long platformFee = (long) (amount * feePercent);
		long recipientAmount = amount - platformFee;

		// Debit sender
		walletRepository.debit(fromUserId, amount, fromUserId);

		// Credit recipient (minus platform fee)
		CryptoWallet toWallet = walletRepository.credit(toUserId, recipientAmount, fromUserId);

		// Create sender transaction
		CryptoTransaction senderTx = new CryptoTransaction();
		senderTx.setId(UUID.randomUUID().toString());
		senderTx.setUserId(fromUserId);
		senderTx.setType(CryptoTransaction.TYPE_TRANSFER);
		senderTx.setAmount(-amount);
		senderTx.setBalanceAfter(fromWallet.getBalance() - amount);
		senderTx.setCounterpartyId(toUserId);
		senderTx.setDescription(description != null ? description : "Transfer to user");
		senderTx.setPlatformFee(platformFee);
		senderTx.setStatus(CryptoTransaction.STATUS_COMPLETED);
		senderTx.setCreatedAt(Timestamp.now());
		senderTx.setCompletedAt(Timestamp.now());

		transactionRepository.save(senderTx, fromUserId);

		// Create recipient transaction
		CryptoTransaction recipientTx = new CryptoTransaction();
		recipientTx.setId(UUID.randomUUID().toString());
		recipientTx.setUserId(toUserId);
		recipientTx.setType(CryptoTransaction.TYPE_TRANSFER);
		recipientTx.setAmount(recipientAmount);
		recipientTx.setBalanceAfter(toWallet.getBalance());
		recipientTx.setCounterpartyId(fromUserId);
		recipientTx.setDescription(description != null ? description : "Transfer from user");
		recipientTx.setStatus(CryptoTransaction.STATUS_COMPLETED);
		recipientTx.setCreatedAt(Timestamp.now());
		recipientTx.setCompletedAt(Timestamp.now());

		transactionRepository.save(recipientTx, fromUserId);

		logger.info("Transferred {} tokens from {} to {} (fee: {})", amount, fromUserId, toUserId, platformFee);

		return senderTx;
	}

	/**
	 * Tip a creator with tokens.
	 */
	public CryptoTransaction tipCreator(String fromUserId, String creatorId, long amount) {
		return transferTokens(fromUserId, creatorId, amount, "Tip to creator");
	}

	/**
	 * Spend tokens for subscription payment.
	 */
	public CryptoTransaction spendOnSubscription(String userId, String ownerId, long amount, String subscriptionId) {
		CryptoWallet wallet = walletRepository.findByUserId(userId)
			.orElseThrow(() -> new StrategizException(CryptoTokenErrors.WALLET_NOT_FOUND, MODULE_NAME));

		if (!wallet.hasSufficientBalance(amount)) {
			throw new StrategizException(CryptoTokenErrors.INSUFFICIENT_BALANCE, MODULE_NAME);
		}

		// Calculate platform fee (configurable via PlatformConfig)
		double feePercent = getPlatformFeePercent();
		long platformFee = (long) (amount * feePercent);
		long ownerAmount = amount - platformFee;

		// Debit subscriber
		walletRepository.debit(userId, amount, userId);

		// Credit owner (minus platform fee)
		CryptoWallet ownerWallet = walletRepository.credit(ownerId, ownerAmount, userId);

		// Create subscriber transaction
		CryptoTransaction tx = new CryptoTransaction();
		tx.setId(UUID.randomUUID().toString());
		tx.setUserId(userId);
		tx.setType(CryptoTransaction.TYPE_SPEND);
		tx.setAmount(-amount);
		tx.setBalanceAfter(wallet.getBalance() - amount);
		tx.setReferenceType(CryptoTransaction.REF_SUBSCRIPTION);
		tx.setReferenceId(subscriptionId);
		tx.setCounterpartyId(ownerId);
		tx.setDescription("Subscription payment");
		tx.setPlatformFee(platformFee);
		tx.setStatus(CryptoTransaction.STATUS_COMPLETED);
		tx.setCreatedAt(Timestamp.now());
		tx.setCompletedAt(Timestamp.now());

		transactionRepository.save(tx, userId);

		// Create owner earning transaction
		CryptoTransaction ownerTx = new CryptoTransaction();
		ownerTx.setId(UUID.randomUUID().toString());
		ownerTx.setUserId(ownerId);
		ownerTx.setType(CryptoTransaction.TYPE_EARN);
		ownerTx.setAmount(ownerAmount);
		ownerTx.setBalanceAfter(ownerWallet.getBalance());
		ownerTx.setReferenceType(CryptoTransaction.REF_SUBSCRIPTION);
		ownerTx.setReferenceId(subscriptionId);
		ownerTx.setCounterpartyId(userId);
		ownerTx.setDescription("Subscription earning");
		ownerTx.setStatus(CryptoTransaction.STATUS_COMPLETED);
		ownerTx.setCreatedAt(Timestamp.now());
		ownerTx.setCompletedAt(Timestamp.now());

		transactionRepository.save(ownerTx, userId);

		logger.info("Subscription payment: {} paid {} tokens to {} (fee: {})", userId, amount, ownerId, platformFee);

		return tx;
	}

	/**
	 * Credit monthly STRAT allocation to user's wallet. Called when a subscription is
	 * created or renewed (invoice.paid).
	 * @param userId User to credit
	 * @param stratAmount Amount of STRAT to credit (not micro-units)
	 * @return Transaction record
	 */
	public CryptoTransaction creditMonthlyAllocation(String userId, long stratAmount) {
		long microUnits = stratAmount * CryptoWallet.MICRO_UNITS;

		CryptoWallet wallet = walletRepository.credit(userId, microUnits, userId);

		CryptoTransaction tx = new CryptoTransaction();
		tx.setId(UUID.randomUUID().toString());
		tx.setUserId(userId);
		tx.setType(CryptoTransaction.TYPE_ALLOCATION);
		tx.setAmount(microUnits);
		tx.setBalanceAfter(wallet.getBalance());
		tx.setReferenceType(CryptoTransaction.REF_SUBSCRIPTION_ALLOCATION);
		tx.setDescription("Monthly STRAT allocation");
		tx.setStatus(CryptoTransaction.STATUS_COMPLETED);
		tx.setCreatedAt(Timestamp.now());
		tx.setCompletedAt(Timestamp.now());

		transactionRepository.save(tx, userId);
		logger.info("Credited {} STRAT monthly allocation to user {}", stratAmount, userId);

		return tx;
	}

	/**
	 * Debit STRAT from user's wallet for AI usage.
	 * @param userId User to debit
	 * @param stratAmount Amount of STRAT to debit (not micro-units)
	 * @param referenceType Reference type (e.g., REF_AI_CHAT, REF_STRATEGY_GENERATION)
	 * @param referenceId Optional reference ID (e.g., session ID)
	 * @param modelId The AI model used
	 * @return Transaction record
	 */
	public CryptoTransaction debitAiUsage(String userId, long stratAmount, String referenceType, String referenceId,
			String modelId) {
		long microUnits = stratAmount * CryptoWallet.MICRO_UNITS;

		CryptoWallet wallet = walletRepository.debit(userId, microUnits, userId);

		CryptoTransaction tx = new CryptoTransaction();
		tx.setId(UUID.randomUUID().toString());
		tx.setUserId(userId);
		tx.setType(CryptoTransaction.TYPE_AI_USAGE);
		tx.setAmount(-microUnits);
		tx.setBalanceAfter(wallet.getBalance());
		tx.setReferenceType(referenceType);
		tx.setReferenceId(referenceId);
		tx.setDescription("AI usage: " + modelId);
		tx.setStatus(CryptoTransaction.STATUS_COMPLETED);
		tx.setCreatedAt(Timestamp.now());
		tx.setCompletedAt(Timestamp.now());

		transactionRepository.save(tx, userId);
		logger.info("Debited {} STRAT from user {} for AI usage (model: {})", stratAmount, userId, modelId);

		return tx;
	}

	/**
	 * Convert tokens to AI credits.
	 * @deprecated STRAT tokens CANNOT be converted to AI credits. AI credits come from
	 * platform subscription tier only. This method is kept for backward compatibility but
	 * will throw an exception.
	 */
	@Deprecated(forRemoval = true)
	public CryptoTransaction convertToAiCredits(String userId, long tokenAmount) {
		// STRAT tokens cannot be converted to AI credits.
		// AI credits come from platform subscription tier only.
		// See: SubscriptionTier.java for tier-based AI credit allocation.
		throw new StrategizException(CryptoTokenErrors.CONVERSION_NOT_ALLOWED, MODULE_NAME);
	}

}
