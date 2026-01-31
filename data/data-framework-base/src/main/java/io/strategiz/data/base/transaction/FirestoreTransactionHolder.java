package io.strategiz.data.base.transaction;

import com.google.cloud.firestore.Transaction;

/**
 * Holds the current Firestore transaction in a ThreadLocal for use across repository
 * operations. This allows Spring's @Transactional to manage Firestore transactions
 * declaratively.
 */
public class FirestoreTransactionHolder {

	private static final ThreadLocal<Transaction> currentTransaction = new ThreadLocal<>();

	private static final ThreadLocal<Boolean> transactionActive = ThreadLocal.withInitial(() -> false);

	/**
	 * Set the current transaction for this thread
	 */
	public static void setTransaction(Transaction transaction) {
		currentTransaction.set(transaction);
		transactionActive.set(true);
	}

	/**
	 * Get the current transaction for this thread
	 * @return The current transaction, or null if none is active
	 */
	public static Transaction getTransaction() {
		return currentTransaction.get();
	}

	/**
	 * Check if a transaction is currently active on this thread
	 */
	public static boolean isTransactionActive() {
		return Boolean.TRUE.equals(transactionActive.get());
	}

	/**
	 * Clear the current transaction (called when transaction completes)
	 */
	public static void clear() {
		currentTransaction.remove();
		transactionActive.set(false);
	}

}
