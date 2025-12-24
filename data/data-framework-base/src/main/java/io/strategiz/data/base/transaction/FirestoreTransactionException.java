package io.strategiz.data.base.transaction;

import org.springframework.transaction.TransactionException;

/**
 * Exception thrown when Firestore transaction operations fail.
 */
public class FirestoreTransactionException extends TransactionException {

    public FirestoreTransactionException(String msg) {
        super(msg);
    }

    public FirestoreTransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
