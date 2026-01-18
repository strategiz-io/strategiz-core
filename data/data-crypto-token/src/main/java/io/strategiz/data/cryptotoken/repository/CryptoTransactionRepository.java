package io.strategiz.data.cryptotoken.repository;

import io.strategiz.data.cryptotoken.entity.CryptoTransaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CryptoTransaction operations.
 */
public interface CryptoTransactionRepository {

	CryptoTransaction save(CryptoTransaction transaction, String performingUserId);

	Optional<CryptoTransaction> findById(String transactionId);

	List<CryptoTransaction> findByUserId(String userId);

	List<CryptoTransaction> findByUserId(String userId, int limit);

	List<CryptoTransaction> findByUserIdAndType(String userId, String type);

	List<CryptoTransaction> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

	List<CryptoTransaction> findPendingByUserId(String userId);

	CryptoTransaction updateStatus(String transactionId, String status, Long balanceAfter);

	void deleteById(String transactionId);

}
