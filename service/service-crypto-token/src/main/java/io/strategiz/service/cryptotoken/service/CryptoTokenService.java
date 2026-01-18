package io.strategiz.service.cryptotoken.service;

import com.google.cloud.Timestamp;
import io.strategiz.data.cryptotoken.entity.CryptoTransaction;
import io.strategiz.data.cryptotoken.entity.CryptoWallet;
import io.strategiz.data.cryptotoken.repository.CryptoTransactionRepository;
import io.strategiz.data.cryptotoken.repository.CryptoWalletRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.cryptotoken.exception.CryptoTokenErrors;
import io.strategiz.service.cryptotoken.model.CryptoTransactionResponse;
import io.strategiz.service.cryptotoken.model.CryptoWalletResponse;
import io.strategiz.service.cryptotoken.model.ConversionRatesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing STRAT crypto token operations.
 */
@Service
public class CryptoTokenService {

	private static final Logger logger = LoggerFactory.getLogger(CryptoTokenService.class);

	private static final double PLATFORM_FEE_PERCENT = 0.15;

	private final CryptoWalletRepository walletRepository;

	private final CryptoTransactionRepository transactionRepository;

	public CryptoTokenService(CryptoWalletRepository walletRepository,
			CryptoTransactionRepository transactionRepository) {
		this.walletRepository = walletRepository;
		this.transactionRepository = transactionRepository;
	}

	/**
	 * Get wallet for user, creating if not exists.
	 */
	public CryptoWalletResponse getWallet(String userId) {
		CryptoWallet wallet = walletRepository.getOrCreateWallet(userId);
		return CryptoWalletResponse.fromEntity(wallet);
	}

	/**
	 * Get transaction history for user.
	 */
	public List<CryptoTransactionResponse> getTransactions(String userId, int limit) {
		return transactionRepository.findByUserId(userId, limit)
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
		long tokenAmount = amountInCents * CryptoWallet.MICRO_UNITS; // $1 = 100 STRAT = 100,000,000 micro-units

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

		return CryptoTransactionResponse.fromEntity(tx);
	}

	/**
	 * Credit reward tokens to user.
	 */
	public CryptoTransactionResponse creditRewardTokens(String userId, long tokenAmount, String rewardType,
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

		return CryptoTransactionResponse.fromEntity(tx);
	}

	/**
	 * Transfer tokens to another user.
	 */
	public CryptoTransactionResponse transferTokens(String fromUserId, String toUserId, long amount,
			String description) {
		if (fromUserId.equals(toUserId)) {
			throw new StrategizException(CryptoTokenErrors.TRANSFER_TO_SELF, "service-crypto-token");
		}

		CryptoWallet fromWallet = walletRepository.findByUserId(fromUserId)
			.orElseThrow(() -> new StrategizException(CryptoTokenErrors.WALLET_NOT_FOUND, "service-crypto-token"));

		if (!fromWallet.hasSufficientBalance(amount)) {
			throw new StrategizException(CryptoTokenErrors.INSUFFICIENT_BALANCE, "service-crypto-token");
		}

		// Calculate platform fee
		long platformFee = (long) (amount * PLATFORM_FEE_PERCENT);
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

		return CryptoTransactionResponse.fromEntity(senderTx);
	}

	/**
	 * Tip a creator with tokens.
	 */
	public CryptoTransactionResponse tipCreator(String fromUserId, String creatorId, long amount) {
		return transferTokens(fromUserId, creatorId, amount, "Tip to creator");
	}

	/**
	 * Spend tokens for subscription payment.
	 */
	public CryptoTransactionResponse spendOnSubscription(String userId, String ownerId, long amount,
			String subscriptionId) {
		CryptoWallet wallet = walletRepository.findByUserId(userId)
			.orElseThrow(() -> new StrategizException(CryptoTokenErrors.WALLET_NOT_FOUND, "service-crypto-token"));

		if (!wallet.hasSufficientBalance(amount)) {
			throw new StrategizException(CryptoTokenErrors.INSUFFICIENT_BALANCE, "service-crypto-token");
		}

		// Calculate platform fee
		long platformFee = (long) (amount * PLATFORM_FEE_PERCENT);
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

		return CryptoTransactionResponse.fromEntity(tx);
	}

	/**
	 * Convert tokens to AI credits.
	 */
	public CryptoTransactionResponse convertToAiCredits(String userId, long tokenAmount) {
		CryptoWallet wallet = walletRepository.findByUserId(userId)
			.orElseThrow(() -> new StrategizException(CryptoTokenErrors.WALLET_NOT_FOUND, "service-crypto-token"));

		long microUnits = tokenAmount * CryptoWallet.MICRO_UNITS;

		if (!wallet.hasSufficientBalance(microUnits)) {
			throw new StrategizException(CryptoTokenErrors.INSUFFICIENT_BALANCE, "service-crypto-token");
		}

		// Debit tokens
		walletRepository.debit(userId, microUnits, userId);

		// Calculate AI credits (1 STRAT = 10 AI credits)
		long aiCredits = tokenAmount * 10;

		// TODO: Credit AI credits to user's account

		// Create transaction
		CryptoTransaction tx = new CryptoTransaction();
		tx.setId(UUID.randomUUID().toString());
		tx.setUserId(userId);
		tx.setType(CryptoTransaction.TYPE_CONVERT);
		tx.setAmount(-microUnits);
		tx.setBalanceAfter(wallet.getBalance() - microUnits);
		tx.setReferenceType(CryptoTransaction.REF_AI_CREDITS);
		tx.setReferenceId(String.valueOf(aiCredits));
		tx.setDescription("Converted to " + aiCredits + " AI credits");
		tx.setStatus(CryptoTransaction.STATUS_COMPLETED);
		tx.setCreatedAt(Timestamp.now());
		tx.setCompletedAt(Timestamp.now());

		transactionRepository.save(tx, userId);

		logger.info("User {} converted {} STRAT to {} AI credits", userId, tokenAmount, aiCredits);

		return CryptoTransactionResponse.fromEntity(tx);
	}

}
