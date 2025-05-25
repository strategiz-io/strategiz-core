package strategiz.api.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE) // Ensure this runs before other beans are created
public class FirebaseConfig {

    private static final String SERVICE_ACCOUNT_FILE = "firebase-service-account.json";
    
    @PostConstruct
    public void initialize() {
        try {
            // Check if Firebase is already initialized
            if (!FirebaseApp.getApps().isEmpty()) {
                System.out.println("Firebase already initialized, skipping initialization.");
                return;
            }
            
            System.out.println("Starting Firebase initialization...");
            
            // Define possible locations for the service account file
            List<String> possibleLocations = Arrays.asList(
                SERVICE_ACCOUNT_FILE,
                "./" + SERVICE_ACCOUNT_FILE,
                "../" + SERVICE_ACCOUNT_FILE,
                System.getProperty("user.dir") + "/" + SERVICE_ACCOUNT_FILE,
                System.getProperty("user.dir") + "/src/main/resources/" + SERVICE_ACCOUNT_FILE,
                System.getProperty("user.dir") + "/target/classes/" + SERVICE_ACCOUNT_FILE
            );
            
            InputStream serviceAccount = null;
            String foundLocation = null;
            
            // Try each location until we find the file
            for (String location : possibleLocations) {
                File file = new File(location);
                if (file.exists()) {
                    System.out.println("Found Firebase service account file at: " + file.getAbsolutePath());
                    serviceAccount = new FileInputStream(file);
                    foundLocation = file.getAbsolutePath();
                    break;
                } else {
                    System.out.println("Firebase service account file not found at: " + file.getAbsolutePath());
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
                    } else {
                        System.out.println("Firebase service account file not found in classpath");
                    }
                } catch (Exception e) {
                    System.out.println("Error loading Firebase service account from classpath: " + e.getMessage());
                    e.printStackTrace();
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
                    } else {
                        System.out.println("Firebase service account file not found at environment path: " + envFile.getAbsolutePath());
                    }
                } else {
                    System.out.println("FIREBASE_SERVICE_ACCOUNT_PATH environment variable not set");
                }
            }
            
            if (serviceAccount == null) {
                System.err.println("CRITICAL ERROR: Firebase service account file not found in any of these locations:");
                for (String location : possibleLocations) {
                    System.err.println("- " + new File(location).getAbsolutePath());
                }
                System.err.println("Current directory: " + new File(".").getAbsolutePath());
                System.err.println("Application will fail as Firebase is required for authentication and data storage.");
                throw new RuntimeException("Firebase service account file not found. Please provide a valid service account key file.");
            }
            
            System.out.println("Loading Firebase configuration from: " + foundLocation);
            
            try {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase has been initialized successfully!");
            } catch (Exception e) {
                System.err.println("Error initializing Firebase with the found service account file: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Firebase initialization failed with the found service account file.", e);
            }
        } catch (IOException e) {
            System.err.println("Error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Firebase initialization failed due to IO error.", e);
        } catch (Exception e) {
            System.err.println("Unexpected error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Firebase initialization failed due to unexpected error.", e);
        }
    }
    
    // Create a bean to ensure Firebase is initialized before other beans that depend on it
    @Bean
    public FirebaseApp firebaseApp() {
        // Firebase should already be initialized by the @PostConstruct method
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("FirebaseApp initialization failed. Check logs for details.");
        }
        return FirebaseApp.getInstance();
    }
    
    // Add Firestore bean for dependency injection
    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }
}
