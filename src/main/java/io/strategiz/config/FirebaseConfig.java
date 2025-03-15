package io.strategiz.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            // Check if Firebase is already initialized
            if (!FirebaseApp.getApps().isEmpty()) {
                System.out.println("Firebase already initialized, skipping initialization.");
                return;
            }
            
            // Try to load from the file
            File file = new File("firebase-service-account.json");
            if (!file.exists()) {
                System.out.println("Firebase service account file not found at: " + file.getAbsolutePath());
                System.out.println("Absolute path: " + file.getAbsoluteFile());
                System.out.println("Current directory: " + new File(".").getAbsolutePath());
                System.out.println("Skipping Firebase initialization. Some features may not work properly.");
                return;
            }
            
            System.out.println("Loading Firebase configuration from: " + file.getAbsolutePath());
            FileInputStream serviceAccount = new FileInputStream(file);
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            
            FirebaseApp.initializeApp(options);
            System.out.println("Firebase has been initialized successfully.");
        } catch (IOException e) {
            System.out.println("Error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Continuing application startup without Firebase.");
        } catch (Exception e) {
            System.out.println("Unexpected error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Continuing application startup without Firebase.");
        }
    }
}
