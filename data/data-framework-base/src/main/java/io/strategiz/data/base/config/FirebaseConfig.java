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

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Firebase configuration responsible for initializing Firebase app and providing
 * Firestore beans. Moved from service-framework-base to data-framework-base module as
 * Firebase/Firestore is primarily a data storage concern.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // Runs after FirebaseVaultConfig loads credentials
public class FirebaseConfig {

	private static final String SERVICE_ACCOUNT_FILE = "firebase-service-account.json";

	private boolean firebaseInitialized = false;

	/**
	 * Injecting FirebaseVaultConfig ensures Spring initializes it (and runs its
	 * {@code @PostConstruct}) before this config's {@code @PostConstruct}.
	 */
	@SuppressWarnings("unused")
	private final FirebaseVaultConfig firebaseVaultConfig;

	@Autowired
	public FirebaseConfig(FirebaseVaultConfig firebaseVaultConfig) {
		this.firebaseVaultConfig = firebaseVaultConfig;
	}

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

			// Try to find the service account credentials in priority order
			InputStream serviceAccount = null;
			String foundLocation = null;

			// 1. Try Vault (highest priority - production)
			String vaultJson = FirebaseVaultConfig.getServiceAccountJson();
			if (vaultJson != null && !vaultJson.isEmpty()) {
				serviceAccount = new ByteArrayInputStream(vaultJson.getBytes(StandardCharsets.UTF_8));
				foundLocation = "Vault (secret/strategiz/firebase)";
				System.out.println("Found Firebase service account in Vault");
			}

			// 2. Try FIREBASE_SERVICE_ACCOUNT_KEY env var / Spring property (inline JSON)
			if (serviceAccount == null) {
				String envKey = System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY");
				if (envKey != null && !envKey.isEmpty()) {
					serviceAccount = new ByteArrayInputStream(envKey.getBytes(StandardCharsets.UTF_8));
					foundLocation = "FIREBASE_SERVICE_ACCOUNT_KEY environment variable";
					System.out.println("Found Firebase service account from FIREBASE_SERVICE_ACCOUNT_KEY env var");
				}
			}

			// 3. Try classpath (local dev fallback)
			if (serviceAccount == null) {
				Resource resource = new ClassPathResource(SERVICE_ACCOUNT_FILE);
				if (resource.exists()) {
					serviceAccount = resource.getInputStream();
					foundLocation = "classpath:" + SERVICE_ACCOUNT_FILE;
					System.out.println("Found Firebase service account in classpath");
				}
			}

			// 4. Try FIREBASE_SERVICE_ACCOUNT_PATH env var (file path)
			if (serviceAccount == null) {
				String envPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH");
				if (envPath != null && !envPath.isEmpty()) {
					File envFile = new File(envPath);
					if (envFile.exists()) {
						serviceAccount = new FileInputStream(envFile);
						foundLocation = envFile.getAbsolutePath();
						System.out.println("Found Firebase service account from FIREBASE_SERVICE_ACCOUNT_PATH at: "
								+ envFile.getAbsolutePath());
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

		}
		catch (Exception e) {
			System.err.println("❌ ERROR initializing Firebase: " + e.getMessage());
			e.printStackTrace();
			// Let the exception propagate to fail application startup
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_INITIALIZATION_FAILED, e,
					"Firebase");
		}
	}

	@Bean
	@Primary
	public FirebaseApp firebaseApp() {
		if (!firebaseInitialized) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_INITIALIZATION_FAILED, "FirebaseApp",
					"Firebase initialization failed. Cannot provide FirebaseApp bean.");
		}
		return FirebaseApp.getInstance();
	}

	@Bean
	@Primary
	public Firestore firestore() {
		if (!firebaseInitialized) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_INITIALIZATION_FAILED, "Firestore",
					"Firebase initialization failed. Cannot provide Firestore bean.");
		}
		return FirestoreClient.getFirestore();
	}

	/**
	 * Firestore transaction manager for Spring @Transactional support. This enables
	 * declarative transaction management for Firestore operations.
	 */
	@Bean
	@Primary
	public PlatformTransactionManager transactionManager(Firestore firestore) {
		return new FirestoreTransactionManager(firestore);
	}

}
