package io.strategiz.data.cryptotoken.repository;

import io.strategiz.data.cryptotoken.entity.CryptoWallet;

import java.util.Optional;

/**
 * Repository interface for CryptoWallet operations.
 */
public interface CryptoWalletRepository {

	CryptoWallet save(CryptoWallet wallet, String performingUserId);

	Optional<CryptoWallet> findById(String walletId);

	Optional<CryptoWallet> findByUserId(String userId);

	CryptoWallet getOrCreateWallet(String userId);

	CryptoWallet credit(String userId, long amount, String performingUserId);

	CryptoWallet debit(String userId, long amount, String performingUserId);

	CryptoWallet lockFunds(String userId, long amount, String performingUserId);

	CryptoWallet unlockFunds(String userId, long amount, String performingUserId);

	void deleteByUserId(String userId);

}
