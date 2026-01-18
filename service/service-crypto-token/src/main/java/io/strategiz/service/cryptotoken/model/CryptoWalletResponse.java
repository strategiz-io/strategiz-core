package io.strategiz.service.cryptotoken.model;

import io.strategiz.data.cryptotoken.entity.CryptoWallet;

/**
 * Response DTO for crypto wallet information.
 */
public record CryptoWalletResponse(String walletId, String userId, double balance, double availableBalance,
		double lockedBalance, double totalEarned, double totalSpent, double totalPurchased,
		String externalWalletAddress, String status) {

	public static CryptoWalletResponse fromEntity(CryptoWallet wallet) {
		return new CryptoWalletResponse(wallet.getId(), wallet.getUserId(),
				CryptoWallet.toDisplayValue(wallet.getBalance()),
				CryptoWallet.toDisplayValue(wallet.getAvailableBalance()),
				CryptoWallet.toDisplayValue(wallet.getLockedBalance() != null ? wallet.getLockedBalance() : 0L),
				CryptoWallet.toDisplayValue(wallet.getTotalEarned() != null ? wallet.getTotalEarned() : 0L),
				CryptoWallet.toDisplayValue(wallet.getTotalSpent() != null ? wallet.getTotalSpent() : 0L),
				CryptoWallet.toDisplayValue(wallet.getTotalPurchased() != null ? wallet.getTotalPurchased() : 0L),
				wallet.getExternalWalletAddress(), wallet.getStatus());
	}

}
