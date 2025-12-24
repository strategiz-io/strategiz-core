package io.strategiz.data.base.transaction;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Template for executing Firestore transactions with proper Spring integration.
 *
 * Firestore transactions use a callback model where all operations must happen
 * inside the transaction callback. This template provides a clean way to execute
 * transactional operations across multiple repositories.
 *
 * Usage:
 * <pre>
 * {@literal @}Autowired
 * private FirestoreTransactionTemplate transactionTemplate;
 *
 * public User createUserWithAuth(User user, AuthMethod auth) {
 *     return transactionTemplate.execute(transaction -> {
 *         // All operations here are atomic
 *         User created = userRepository.createInTransaction(user, transaction);
 *         authRepository.saveInTransaction(auth, transaction);
 *         return created;
 *     });
 * }
 * </pre>
 */
@Component
public class FirestoreTransactionTemplate {

    private static final Logger log = LoggerFactory.getLogger(FirestoreTransactionTemplate.class);

    private final Firestore firestore;

    public FirestoreTransactionTemplate(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Execute operations within a Firestore transaction.
     * All operations inside the callback are atomic - either all succeed or all fail.
     *
     * @param callback The transactional operations to execute
     * @param <T> The return type
     * @return The result of the callback
     * @throws FirestoreTransactionException if the transaction fails
     */
    public <T> T execute(Function<Transaction, T> callback) {
        log.debug("Starting Firestore transaction");

        try {
            T result = firestore.runTransaction(transaction -> {
                // Store transaction in ThreadLocal for repositories that need it
                FirestoreTransactionHolder.setTransaction(transaction);

                try {
                    return callback.apply(transaction);
                } finally {
                    // Don't clear here - let transaction complete first
                }
            }).get();

            log.debug("Firestore transaction completed successfully");
            return result;

        } catch (ExecutionException e) {
            log.error("Firestore transaction failed: {}", e.getMessage());
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new FirestoreTransactionException("Transaction failed", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreTransactionException("Transaction interrupted", e);
        } finally {
            FirestoreTransactionHolder.clear();
        }
    }

    /**
     * Execute operations within a Firestore transaction (void return).
     *
     * @param callback The transactional operations to execute
     * @throws FirestoreTransactionException if the transaction fails
     */
    public void executeWithoutResult(TransactionCallback callback) {
        execute(transaction -> {
            callback.doInTransaction(transaction);
            return null;
        });
    }

    /**
     * Functional interface for void transaction callbacks.
     */
    @FunctionalInterface
    public interface TransactionCallback {
        void doInTransaction(Transaction transaction);
    }
}
