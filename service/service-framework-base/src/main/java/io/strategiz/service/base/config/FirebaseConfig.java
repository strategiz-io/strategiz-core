package io.strategiz.service.base.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.exception.ServiceBaseErrorDetails;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * @deprecated This class has been moved to io.strategiz.data.base.config.FirebaseConfig
 * This implementation is kept for reference but is permanently disabled.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@org.springframework.context.annotation.Profile("disabled") // Permanently disabled as configuration moved to data-base module
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

            System.out.println("Starting Firebase initialization...");

            // Define possible locations for the service account file
            List<String> possibleLocations = Arrays.asList(
                SERVICE_ACCOUNT_FILE,
                "." + File.separator + SERVICE_ACCOUNT_FILE,
                "." + File.separator + "config" + File.separator + SERVICE_ACCOUNT_FILE,
                System.getProperty("user.home") + File.separator + SERVICE_ACCOUNT_FILE,
                System.getProperty("user.dir") + File.separator + SERVICE_ACCOUNT_FILE,
                System.getProperty("user.dir") + File.separator + "config" + File.separator + SERVICE_ACCOUNT_FILE,
                System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + SERVICE_ACCOUNT_FILE,
                System.getProperty("user.dir") + File.separator + "target" + File.separator + "classes" + File.separator + SERVICE_ACCOUNT_FILE
            );

            InputStream serviceAccount = null;
            String foundLocation = null;

            // Try each location until we find the file
            for (String location : possibleLocations) {
                File file = new File(location);
                if (file.exists() && file.isFile()) {
                    System.out.println("Found Firebase service account file at: " + file.getAbsolutePath());
                    serviceAccount = new FileInputStream(file);
                    foundLocation = file.getAbsolutePath();
                    break;
                }
            }

            // If not found in file system, try classpath
            if (serviceAccount == null) {
                try {
                    Resource resource = new ClassPathResource(SERVICE_ACCOUNT_FILE);
                    if (resource.exists()) {
                        System.out.println("Found Firebase service account file in classpath");
                        serviceAccount = resource.getInputStream();
                        foundLocation = "classpath:" + SERVICE_ACCOUNT_FILE;
                    }
                } catch (Exception e) {
                    System.err.println("Error loading Firebase service account from classpath: " + e.getMessage());
                }
            }

            // Last resort: try to use environment variable or system property
            if (serviceAccount == null) {
                String envPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH");
                if (envPath != null && !envPath.isEmpty()) {
                    File envFile = new File(envPath);
                    if (envFile.exists()) {
                        System.out.println("Found Firebase service account file from environment variable at: " + envFile.getAbsolutePath());
                        serviceAccount = new FileInputStream(envFile);
                        foundLocation = envFile.getAbsolutePath();
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
            System.out.println("Firebase has been initialized successfully!");
            firebaseInitialized = true;

        } catch (Exception e) {
            System.err.println("ERROR initializing Firebase. The application will not function correctly: " + e.getMessage());
            e.printStackTrace();
            // Let the exception propagate to fail application startup
            throw new StrategizException(ServiceBaseErrorDetails.FIREBASE_INITIALIZATION_FAILED, "service-framework-base", e);
        }
    }

    @Bean
    public FirebaseApp firebaseApp() {
        if (!firebaseInitialized) {
            throw new StrategizException(ServiceBaseErrorDetails.FIREBASE_INITIALIZATION_FAILED,
                "FirebaseConfig", "Firebase initialization failed. Cannot provide FirebaseApp bean.");
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore() {
        if (!firebaseInitialized) {
            throw new StrategizException(ServiceBaseErrorDetails.FIREBASE_INITIALIZATION_FAILED,
                "FirebaseConfig", "Firebase initialization failed. Cannot provide Firestore bean.");
        }
        return FirestoreClient.getFirestore();
    }
}
