package io.strategiz.client.client_firebase;

import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Client for interacting with Firestore
 * This centralizes all Firestore access to avoid scattering Firestore dependencies
 */
@Component
public class FirestoreClient {

    private final Firestore firestore;

    @Autowired
    public FirestoreClient(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Get the Firestore instance
     */
    public Firestore getFirestore() {
        return firestore;
    }
}
