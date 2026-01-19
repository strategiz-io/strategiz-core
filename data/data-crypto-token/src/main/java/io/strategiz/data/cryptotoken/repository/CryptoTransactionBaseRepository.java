package io.strategiz.data.cryptotoken.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.cryptotoken.entity.CryptoTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of CryptoTransactionRepository.
 */
@Repository
public class CryptoTransactionBaseRepository extends BaseRepository<CryptoTransaction>
		implements CryptoTransactionRepository {

	private static final Logger logger = LoggerFactory.getLogger(CryptoTransactionBaseRepository.class);

	public CryptoTransactionBaseRepository(Firestore firestore) {
		super(firestore, CryptoTransaction.class);
	}

	@Override
	protected String getModuleName() {
		return "data-crypto-token";
	}

	@Override
	public CryptoTransaction save(CryptoTransaction transaction, String performingUserId) {
		try {
			if (transaction.getId() == null) {
				transaction.setId(UUID.randomUUID().toString());
			}
			if (transaction.getCreatedAt() == null) {
				transaction.setCreatedAt(Timestamp.now());
			}
			getCollection().document(transaction.getId()).set(transaction).get();
			return transaction;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to save crypto transaction: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to save crypto transaction", e);
		}
	}

	@Override
	public Optional<CryptoTransaction> findById(String transactionId) {
		try {
			DocumentSnapshot doc = getCollection().document(transactionId).get().get();
			if (doc.exists()) {
				return Optional.ofNullable(doc.toObject(CryptoTransaction.class));
			}
			return Optional.empty();
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to find transaction by id: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to find transaction", e);
		}
	}

	@Override
	public List<CryptoTransaction> findByUserId(String userId) {
		return findByUserId(userId, 100);
	}

	@Override
	public List<CryptoTransaction> findByUserId(String userId, int limit) {
		try {
			var query = getCollection().whereEqualTo("userId", userId)
				.orderBy("createdAt", Query.Direction.DESCENDING)
				.limit(limit)
				.get()
				.get();

			List<CryptoTransaction> transactions = new ArrayList<>();
			for (var doc : query.getDocuments()) {
				transactions.add(doc.toObject(CryptoTransaction.class));
			}
			return transactions;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to find transactions by userId: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to find transactions", e);
		}
	}

	@Override
	public List<CryptoTransaction> findByUserIdAndType(String userId, String type) {
		try {
			var query = getCollection().whereEqualTo("userId", userId)
				.whereEqualTo("type", type)
				.orderBy("createdAt", Query.Direction.DESCENDING)
				.get()
				.get();

			List<CryptoTransaction> transactions = new ArrayList<>();
			for (var doc : query.getDocuments()) {
				transactions.add(doc.toObject(CryptoTransaction.class));
			}
			return transactions;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to find transactions by type: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to find transactions", e);
		}
	}

	@Override
	public List<CryptoTransaction> findByReferenceTypeAndReferenceId(String referenceType, String referenceId) {
		try {
			var query = getCollection().whereEqualTo("referenceType", referenceType)
				.whereEqualTo("referenceId", referenceId)
				.get()
				.get();

			List<CryptoTransaction> transactions = new ArrayList<>();
			for (var doc : query.getDocuments()) {
				transactions.add(doc.toObject(CryptoTransaction.class));
			}
			return transactions;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to find transactions by reference: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to find transactions", e);
		}
	}

	@Override
	public List<CryptoTransaction> findPendingByUserId(String userId) {
		try {
			var query = getCollection().whereEqualTo("userId", userId)
				.whereEqualTo("status", CryptoTransaction.STATUS_PENDING)
				.get()
				.get();

			List<CryptoTransaction> transactions = new ArrayList<>();
			for (var doc : query.getDocuments()) {
				transactions.add(doc.toObject(CryptoTransaction.class));
			}
			return transactions;
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to find pending transactions: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to find pending transactions", e);
		}
	}

	@Override
	public CryptoTransaction updateStatus(String transactionId, String status, Long balanceAfter) {
		try {
			CryptoTransaction transaction = findById(transactionId)
				.orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

			transaction.setStatus(status);
			if (balanceAfter != null) {
				transaction.setBalanceAfter(balanceAfter);
			}
			if (CryptoTransaction.STATUS_COMPLETED.equals(status)) {
				transaction.setCompletedAt(Timestamp.now());
			}

			return save(transaction, transaction.getUserId());
		}
		catch (Exception e) {
			logger.error("Failed to update transaction status: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to update transaction status", e);
		}
	}

	@Override
	public void deleteById(String transactionId) {
		try {
			getCollection().document(transactionId).delete().get();
		}
		catch (InterruptedException | ExecutionException e) {
			logger.error("Failed to delete transaction: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to delete transaction", e);
		}
	}

}
