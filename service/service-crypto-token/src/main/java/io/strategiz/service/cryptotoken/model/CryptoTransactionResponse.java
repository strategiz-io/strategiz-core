package io.strategiz.service.cryptotoken.model;

import io.strategiz.data.cryptotoken.entity.CryptoTransaction;
import io.strategiz.data.cryptotoken.entity.CryptoWallet;

import java.time.Instant;

/**
 * Response DTO for crypto transaction information.
 */
public record CryptoTransactionResponse(String transactionId, String userId, String type, double amount,
		double balanceAfter, String referenceType, String referenceId, String counterpartyId, String description,
		double platformFee, String status, Instant createdAt, Instant completedAt) {

	public static CryptoTransactionResponse fromEntity(CryptoTransaction tx) {
		return new CryptoTransactionResponse(tx.getId(), tx.getUserId(), tx.getType(),
				CryptoWallet.toDisplayValue(tx.getAmount() != null ? tx.getAmount() : 0L),
				CryptoWallet.toDisplayValue(tx.getBalanceAfter() != null ? tx.getBalanceAfter() : 0L),
				tx.getReferenceType(), tx.getReferenceId(), tx.getCounterpartyId(), tx.getDescription(),
				CryptoWallet.toDisplayValue(tx.getPlatformFee() != null ? tx.getPlatformFee() : 0L), tx.getStatus(),
				tx.getCreatedAt() != null ? Instant.ofEpochSecond(tx.getCreatedAt().getSeconds()) : null,
				tx.getCompletedAt() != null ? Instant.ofEpochSecond(tx.getCompletedAt().getSeconds()) : null);
	}

}
