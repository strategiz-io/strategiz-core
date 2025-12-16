package io.strategiz.data.base.transaction;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Spring PlatformTransactionManager implementation for Google Cloud Firestore.
 *
 * This allows using Spring's @Transactional annotation with Firestore operations.
 * All repository operations within a @Transactional method will participate in
 * the same Firestore transaction, ensuring atomicity.
 *
 * Usage:
 * <pre>
 * {@literal @}Transactional
 * public void myService() {
 *     // All repository operations here are atomic
 *     userRepository.createUser(user);
 *     authRepository.saveAuthMethod(authMethod);
 * }
 * </pre>
 */
public class FirestoreTransactionManager extends AbstractPlatformTransactionManager {

    private static final Logger log = LoggerFactory.getLogger(FirestoreTransactionManager.class);

    private final Firestore firestore;

    public FirestoreTransactionManager(Firestore firestore) {
        this.firestore = firestore;
        setNestedTransactionAllowed(false);
    }

    @Override
    protected Object doGetTransaction() throws TransactionException {
        FirestoreTransactionObject txObject = new FirestoreTransactionObject();
        txObject.setExistingTransaction(FirestoreTransactionHolder.getTransaction());
        return txObject;
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        FirestoreTransactionObject txObject = (FirestoreTransactionObject) transaction;
        return txObject.getExistingTransaction() != null;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        log.debug("Beginning Firestore transaction");
        FirestoreTransactionObject txObject = (FirestoreTransactionObject) transaction;

        try {
            // Start a new Firestore transaction
            // We use a CompletableFuture to bridge Firestore's callback-based API
            CompletableFuture<Void> transactionFuture = new CompletableFuture<>();

            firestore.runTransaction(firestoreTransaction -> {
                // Store the transaction in ThreadLocal for repository access
                FirestoreTransactionHolder.setTransaction(firestoreTransaction);
                txObject.setTransaction(firestoreTransaction);
                txObject.setTransactionFuture(transactionFuture);

                log.debug("Firestore transaction started and stored in ThreadLocal");

                // Wait for the Spring transaction to complete (commit or rollback)
                // This keeps the Firestore transaction open until Spring says we're done
                try {
                    transactionFuture.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Transaction interrupted", e);
                } catch (ExecutionException e) {
                    // Rollback case - throw to abort the Firestore transaction
                    throw new RuntimeException("Transaction rolled back", e.getCause());
                }

                return null;
            });

        } catch (Exception e) {
            FirestoreTransactionHolder.clear();
            throw new FirestoreTransactionException("Failed to begin Firestore transaction", e);
        }
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        log.debug("Committing Firestore transaction");
        FirestoreTransactionObject txObject = (FirestoreTransactionObject) status.getTransaction();

        try {
            // Signal the Firestore transaction to complete successfully
            if (txObject.getTransactionFuture() != null) {
                txObject.getTransactionFuture().complete(null);
            }
        } finally {
            FirestoreTransactionHolder.clear();
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        log.debug("Rolling back Firestore transaction");
        FirestoreTransactionObject txObject = (FirestoreTransactionObject) status.getTransaction();

        try {
            // Signal the Firestore transaction to abort
            if (txObject.getTransactionFuture() != null) {
                txObject.getTransactionFuture().completeExceptionally(
                    new RuntimeException("Transaction rolled back by Spring")
                );
            }
        } finally {
            FirestoreTransactionHolder.clear();
        }
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
        FirestoreTransactionObject txObject = (FirestoreTransactionObject) status.getTransaction();
        txObject.setRollbackOnly(true);
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        FirestoreTransactionHolder.clear();
    }

    /**
     * Internal transaction object holding Firestore transaction state
     */
    private static class FirestoreTransactionObject {
        private Transaction existingTransaction;
        private Transaction transaction;
        private CompletableFuture<Void> transactionFuture;
        private boolean rollbackOnly = false;

        public Transaction getExistingTransaction() {
            return existingTransaction;
        }

        public void setExistingTransaction(Transaction existingTransaction) {
            this.existingTransaction = existingTransaction;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        public void setTransaction(Transaction transaction) {
            this.transaction = transaction;
        }

        public CompletableFuture<Void> getTransactionFuture() {
            return transactionFuture;
        }

        public void setTransactionFuture(CompletableFuture<Void> transactionFuture) {
            this.transactionFuture = transactionFuture;
        }

        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        public void setRollbackOnly(boolean rollbackOnly) {
            this.rollbackOnly = rollbackOnly;
        }
    }
}
