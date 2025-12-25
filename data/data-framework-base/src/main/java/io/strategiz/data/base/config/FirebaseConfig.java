package io.strategiz.data.base.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.transaction.FirestoreTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase configuration responsible for initializing Firebase app and providing Firestore beans.
 * Moved from service-framework-base to data-framework-base module as Firebase/Firestore is primarily a data storage concern.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE) // Ensure this runs before other beans are created
public class FirebaseConfig {

    private static final String SERVICE_ACCOUNT_FILE = "firebase-service-account.json";
    private boolean firebaseInitialized = false;

    @PostConstruct
    public void initialize() {
        try {
            // Check if Firebase is already initialized
            if (!FirebaseApp.getApps().isEmpty()) {
                System.out.println("Firebase already initialized, skipping initialization.");
                firebaseInitialized = true;
                return;
            }

            System.out.println("Starting Firebase initialization from data-base module...");
            
            // Try to find the service account file
            InputStream serviceAccount = null;
            String foundLocation = null;
            
            // Try in classpath first (recommended approach)
            Resource resource = new ClassPathResource(SERVICE_ACCOUNT_FILE);
            if (resource.exists()) {
                serviceAccount = resource.getInputStream();
                foundLocation = "classpath:" + SERVICE_ACCOUNT_FILE;
                System.out.println("Found Firebase service account in classpath");
            }
            
            // If not found in classpath, try working directory 
            if (serviceAccount == null) {
                File file = new File(SERVICE_ACCOUNT_FILE);
                if (file.exists()) {
                    serviceAccount = new FileInputStream(file);
                    foundLocation = file.getAbsolutePath();
                    System.out.println("Found Firebase service account in working directory");
                }
            }
            
            // If still not found, try environment variable
            if (serviceAccount == null) {
                String envPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH");
                if (envPath != null && !envPath.isEmpty()) {
                    File envFile = new File(envPath);
                    if (envFile.exists()) {
                        serviceAccount = new FileInputStream(envFile);
                        foundLocation = envFile.getAbsolutePath();
                        System.out.println("Found Firebase service account from environment variable at: " + envFile.getAbsolutePath());
                    }
                }
            }

            if (serviceAccount == null) {
                throw new IOException("Firebase service account file not found in any of the expected locations.");
            }

            System.out.println("Loading Firebase configuration from: " + foundLocation);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            System.out.println("✅ Firebase has been initialized successfully!");
            firebaseInitialized = true;

        } catch (Exception e) {
            System.err.println("❌ ERROR initializing Firebase: " + e.getMessage());
            e.printStackTrace();
            // Let the exception propagate to fail application startup
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_INITIALIZATION_FAILED, e, "Firebase");
        }
    }

    @Bean
    @Primary
    public FirebaseApp firebaseApp() {
        if (!firebaseInitialized) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_INITIALIZATION_FAILED,
                "FirebaseApp", "Firebase initialization failed. Cannot provide FirebaseApp bean.");
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    @Primary
    public Firestore firestore() {
        if (!firebaseInitialized) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_INITIALIZATION_FAILED,
                "Firestore", "Firebase initialization failed. Cannot provide Firestore bean.");
        }
        return FirestoreClient.getFirestore();
    }

    /**
     * Firestore transaction manager for Spring @Transactional support.
     * This enables declarative transaction management for Firestore operations.
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(Firestore firestore) {
        return new FirestoreTransactionManager(firestore);
    }
}
