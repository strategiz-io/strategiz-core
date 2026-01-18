package io.strategiz.data.cryptotoken.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import io.strategiz.data.base.repository.BaseFirestoreRepository;
import io.strategiz.data.cryptotoken.entity.CryptoWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of CryptoWalletRepository.
 */
@Repository
public class CryptoWalletBaseRepository extends BaseFirestoreRepository<CryptoWallet>
		implements CryptoWalletRepository {

	private static final Logger logger = LoggerFactory.getLogger(CryptoWalletBaseRepository.class);

	private static final String COLLECTION_NAME = "cryptoWallets";

	public CryptoWalletBaseRepository(Firestore firestore) {
		super(firestore, COLLECTION_NAME, CryptoWallet.class);
	}

	@Override
	public CryptoWallet save(CryptoWallet wallet, String performingUserId) {
		try {
			if (wallet.getId() == null) {
				wallet.setId(UUID.randomUUID().toString());
			}
			wallet.setUpdatedAt(Timestamp.now());
			if (wallet.getCreatedAt() == null) {
				wallet.setCreatedAt(Timestamp.now());
			}
			getCollection().document(wallet.getId()).set(wallet).get();
			return wallet;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to save crypto wallet: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to save crypto wallet", e);
		}
	}

	@Override
	public Optional<CryptoWallet> findById(String walletId) {
		try {
			DocumentSnapshot doc = getCollection().document(walletId).get().get();
			if (doc.exists()) {
				return Optional.ofNullable(doc.toObject(CryptoWallet.class));
			}
			return Optional.empty();
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to find crypto wallet by id: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to find crypto wallet", e);
		}
	}

	@Override
	public Optional<CryptoWallet> findByUserId(String userId) {
		try {
			var query = getCollection().whereEqualTo("userId", userId).limit(1).get().get();
			if (!query.isEmpty()) {
				return Optional.ofNullable(query.getDocuments().get(0).toObject(CryptoWallet.class));
			}
			return Optional.empty();
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to find crypto wallet by userId: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to find crypto wallet", e);
		}
	}

	@Override
	public CryptoWallet getOrCreateWallet(String userId) {
		Optional<CryptoWallet> existing = findByUserId(userId);
		if (existing.isPresent()) {
			return existing.get();
		}

		CryptoWallet wallet = new CryptoWallet();
		wallet.setId(UUID.randomUUID().toString());
		wallet.setUserId(userId);
		wallet.setCreatedAt(Timestamp.now());
		wallet.setUpdatedAt(Timestamp.now());
		return save(wallet, userId);
	}

	@Override
	public CryptoWallet credit(String userId, long amount, String performingUserId) {
		return updateBalance(userId, amount, true, performingUserId);
	}

	@Override
	public CryptoWallet debit(String userId, long amount, String performingUserId) {
		return updateBalance(userId, -amount, false, performingUserId);
	}

	private CryptoWallet updateBalance(String userId, long delta, boolean isCredit, String performingUserId) {
		try {
			CryptoWallet wallet = getOrCreateWallet(userId);
			DocumentReference docRef = getCollection().document(wallet.getId());

			return firestore.runTransaction((Transaction transaction) -> {
				DocumentSnapshot snapshot = transaction.get(docRef).get();
				CryptoWallet current = snapshot.toObject(CryptoWallet.class);
				if (current == null) {
					throw new RuntimeException("Wallet not found during transaction");
				}

				long newBalance = current.getBalance() + delta;
				if (newBalance < 0) {
					throw new RuntimeException("Insufficient balance");
				}

				current.setBalance(newBalance);
				current.setUpdatedAt(Timestamp.now());

				if (isCredit && delta > 0) {
					current.setTotalEarned(current.getTotalEarned() + delta);
				}
				else if (!isCredit && delta < 0) {
					current.setTotalSpent(current.getTotalSpent() + Math.abs(delta));
				}

				transaction.set(docRef, current);
				return current;
			}).get();
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to update wallet balance: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to update wallet balance", e);
		}
	}

	@Override
	public CryptoWallet lockFunds(String userId, long amount, String performingUserId) {
		try {
			CryptoWallet wallet = getOrCreateWallet(userId);
			DocumentReference docRef = getCollection().document(wallet.getId());

			return firestore.runTransaction((Transaction transaction) -> {
				DocumentSnapshot snapshot = transaction.get(docRef).get();
				CryptoWallet current = snapshot.toObject(CryptoWallet.class);
				if (current == null) {
					throw new RuntimeException("Wallet not found");
				}

				if (current.getAvailableBalance() < amount) {
					throw new RuntimeException("Insufficient available balance to lock");
				}

				current.setLockedBalance(current.getLockedBalance() + amount);
				current.setUpdatedAt(Timestamp.now());
				transaction.set(docRef, current);
				return current;
			}).get();
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to lock funds: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to lock funds", e);
		}
	}

	@Override
	public CryptoWallet unlockFunds(String userId, long amount, String performingUserId) {
		try {
			CryptoWallet wallet = findByUserId(userId)
				.orElseThrow(() -> new RuntimeException("Wallet not found"));
			DocumentReference docRef = getCollection().document(wallet.getId());

			return firestore.runTransaction((Transaction transaction) -> {
				DocumentSnapshot snapshot = transaction.get(docRef).get();
				CryptoWallet current = snapshot.toObject(CryptoWallet.class);
				if (current == null) {
					throw new RuntimeException("Wallet not found");
				}

				long newLocked = current.getLockedBalance() - amount;
				if (newLocked < 0) {
					newLocked = 0;
				}

				current.setLockedBalance(newLocked);
				current.setUpdatedAt(Timestamp.now());
				transaction.set(docRef, current);
				return current;
			}).get();
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to unlock funds: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to unlock funds", e);
		}
	}

	@Override
	public void deleteByUserId(String userId) {
		try {
			var query = getCollection().whereEqualTo("userId", userId).get().get();
			for (var doc : query.getDocuments()) {
				doc.getReference().delete().get();
			}
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to delete wallet: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to delete wallet", e);
		}
	}

}
