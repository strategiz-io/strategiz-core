package io.strategiz.application.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * @deprecated This class has been deprecated as Firebase initialization has been moved to the data-base module.
 * This implementation is kept for reference but is permanently disabled.
 */
@Configuration
@Profile("disabled") // Explicitly disable this configuration since Firebase is now initialized in data-base module
@Order(Ordered.HIGHEST_PRECEDENCE)
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = FirebaseInitializer.DataBaseFirebaseConfigFilter.class)
})
public class FirebaseInitializer {

    private static final String SERVICE_ACCOUNT_FILE = "firebase-service-account.json";
    
    @Value("${firebase.mock:false}")
    private boolean mockFirebase;
    
    @PostConstruct
    public void initialize() {
        try {
            // Check if Firebase is already initialized
            if (!FirebaseApp.getApps().isEmpty()) {
                System.out.println("Firebase already initialized, skipping initialization.");
                return;
            }
            
            System.out.println("Starting Firebase initialization from application module...");
            
            // Try to find the service account file
            InputStream serviceAccount = null;
            Resource resource = new ClassPathResource(SERVICE_ACCOUNT_FILE);
            
            if (resource.exists()) {
                serviceAccount = resource.getInputStream();
                System.out.println("Found Firebase service account in classpath");
            } else {
                // Try in working directory
                File file = new File(SERVICE_ACCOUNT_FILE);
                if (file.exists()) {
                    serviceAccount = new FileInputStream(file);
                    System.out.println("Found Firebase service account in working directory");
                } else {
                    // Try in user home directory
                    String userHome = System.getProperty("user.home");
                    file = new File(userHome, SERVICE_ACCOUNT_FILE);
                    if (file.exists()) {
                        serviceAccount = new FileInputStream(file);
                        System.out.println("Found Firebase service account in user home directory");
                    } else {
                        // Check environment variable for service account path
                        String firebaseServiceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH");
                        if (firebaseServiceAccountPath != null) {
                            file = new File(firebaseServiceAccountPath);
                            if (file.exists()) {
                                serviceAccount = new FileInputStream(file);
                                System.out.println("Found Firebase service account at FIREBASE_SERVICE_ACCOUNT_PATH");
                            }
                        }
                    }
                }
            }
            
            if (serviceAccount != null) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                
                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase has been initialized successfully!");
            } else {
                System.err.println("❌ Firebase service account file not found in any expected location.");
                System.err.println("The application will run but functionality requiring Firebase will not work.");
            }
                
        } catch (IOException e) {
            System.err.println("❌ ERROR initializing Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Bean
    @Primary
    public FirebaseApp firebaseApp() {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("Firebase not initialized. Make sure firebase-service-account.json is available.");
        }
        return FirebaseApp.getInstance();
    }
    
    /**
     * Creates a proxy Firestore bean to satisfy dependencies.
     * Since we've disabled the Firestore initialization in the original FirebaseConfig,
     * we need to provide this bean to satisfy dependencies in other modules.
     */
    @Bean
    @Primary
    public Firestore firestore() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirestoreClient.getFirestore();
            }
            throw new IllegalStateException("Firebase not initialized, cannot provide Firestore instance.");
        } catch (Exception e) {
            System.err.println("❌ Error creating Firestore bean: " + e.getMessage());
            throw new RuntimeException("Failed to create Firestore bean", e);
        }
    }
    
    /**
     * Custom filter to exclude the FirebaseConfig class from the data-base module
     * This avoids conflicts with our new initialization approach
     */
    public static class DataBaseFirebaseConfigFilter extends RegexPatternTypeFilter {
        public DataBaseFirebaseConfigFilter() {
            super(Pattern.compile("io\\.strategiz\\.data\\.base\\.config\\.FirebaseConfig"));
        }
    }
}
