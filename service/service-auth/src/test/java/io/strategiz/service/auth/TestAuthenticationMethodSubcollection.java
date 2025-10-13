package io.strategiz.service.auth;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService;
import io.strategiz.service.auth.service.totp.TotpRegistrationService;
import io.strategiz.service.auth.service.smsotp.SmsOtpRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that authentication methods are being properly stored
 * in the users/{userId}/authentication_methods subcollection
 */
@SpringBootTest
@ActiveProfiles("test")
public class TestAuthenticationMethodSubcollection {
    
    @Autowired
    private AuthenticationMethodRepository authMethodRepository;
    
    @Autowired
    private PasskeyRegistrationService passkeyRegistrationService;
    
    @Autowired
    private TotpRegistrationService totpRegistrationService;
    
    @Autowired
    private SmsOtpRegistrationService smsOtpRegistrationService;
    
    @Autowired
    private Firestore firestore;
    
    private final String testUserId = "test-user-" + UUID.randomUUID().toString();
    
    @Test
    public void testPasskeySubcollectionCreation() throws ExecutionException, InterruptedException {
        System.out.println("Testing Passkey subcollection creation for user: " + testUserId);
        
        // Create a passkey registration
        var request = new PasskeyRegistrationService.RegistrationRequest(testUserId, "test@example.com");
        var registrationOptions = passkeyRegistrationService.beginRegistration(request);
        assertNotNull(registrationOptions, "Registration options should not be null");
        
        // Check if authentication method was created in subcollection
        QuerySnapshot snapshot = firestore.collection("users")
            .document(testUserId)
            .collection("authentication_methods")
            .whereEqualTo("type", AuthenticationMethodType.PASSKEY.getValue())
            .get()
            .get();
            
        assertFalse(snapshot.isEmpty(), "Subcollection should contain passkey authentication method");
        assertEquals(1, snapshot.size(), "Should have exactly one passkey method");
        
        DocumentSnapshot doc = snapshot.getDocuments().get(0);
        System.out.println("Passkey document ID: " + doc.getId());
        System.out.println("Passkey document data: " + doc.getData());
        
        // Verify the data structure
        assertEquals(AuthenticationMethodType.PASSKEY.getValue(), doc.getString("type"));
        assertNotNull(doc.get("metadata"), "Metadata should not be null");
        assertNotNull(doc.get("createdAt"), "CreatedAt should not be null");
    }
    
    @Test
    public void testTotpSubcollectionCreation() throws ExecutionException, InterruptedException {
        System.out.println("Testing TOTP subcollection creation for user: " + testUserId);
        
        // Create a TOTP registration
        var qrCode = totpRegistrationService.generateTotpSecret(testUserId);
        assertNotNull(qrCode, "TOTP QR code should not be null");
        
        // Check if authentication method was created in subcollection
        QuerySnapshot snapshot = firestore.collection("users")
            .document(testUserId)
            .collection("authentication_methods")
            .whereEqualTo("type", AuthenticationMethodType.TOTP.getValue())
            .get()
            .get();
            
        assertFalse(snapshot.isEmpty(), "Subcollection should contain TOTP authentication method");
        assertEquals(1, snapshot.size(), "Should have exactly one TOTP method");
        
        DocumentSnapshot doc = snapshot.getDocuments().get(0);
        System.out.println("TOTP document ID: " + doc.getId());
        System.out.println("TOTP document data: " + doc.getData());
        
        // Verify the data structure
        assertEquals(AuthenticationMethodType.TOTP.getValue(), doc.getString("type"));
        assertNotNull(doc.get("metadata"), "Metadata should not be null");
        assertFalse(doc.getBoolean("isActive"), "Should not be active until verified");
    }
    
    @Test
    public void testSmsOtpSubcollectionCreation() throws ExecutionException, InterruptedException {
        System.out.println("Testing SMS OTP subcollection creation for user: " + testUserId);
        
        // Create an SMS OTP registration
        boolean registered = smsOtpRegistrationService.registerPhoneNumber(
            testUserId, 
            "+1234567890", 
            "127.0.0.1", 
            "US"
        );
        assertTrue(registered, "Phone number registration should succeed");
        
        // Check if authentication method was created in subcollection
        QuerySnapshot snapshot = firestore.collection("users")
            .document(testUserId)
            .collection("authentication_methods")
            .whereEqualTo("type", AuthenticationMethodType.SMS_OTP.getValue())
            .get()
            .get();
            
        assertFalse(snapshot.isEmpty(), "Subcollection should contain SMS OTP authentication method");
        assertEquals(1, snapshot.size(), "Should have exactly one SMS OTP method");
        
        DocumentSnapshot doc = snapshot.getDocuments().get(0);
        System.out.println("SMS OTP document ID: " + doc.getId());
        System.out.println("SMS OTP document data: " + doc.getData());
        
        // Verify the data structure
        assertEquals(AuthenticationMethodType.SMS_OTP.getValue(), doc.getString("type"));
        assertNotNull(doc.get("metadata"), "Metadata should not be null");
        
        // Check metadata contains phone number
        @SuppressWarnings("unchecked")
        var metadata = (java.util.Map<String, Object>) doc.get("metadata");
        assertEquals("+1234567890", metadata.get("phoneNumber"));
        assertEquals(false, metadata.get("isVerified"));
    }
    
    @Test
    public void testListAllAuthenticationMethods() throws ExecutionException, InterruptedException {
        System.out.println("Testing listing all authentication methods for user: " + testUserId);
        
        // Create multiple authentication methods
        var request = new PasskeyRegistrationService.RegistrationRequest(testUserId, "test@example.com");
        passkeyRegistrationService.beginRegistration(request);
        totpRegistrationService.generateTotpSecret(testUserId);
        smsOtpRegistrationService.registerPhoneNumber(testUserId, "+1234567890", "127.0.0.1", "US");
        
        // List all methods using repository
        List<AuthenticationMethodEntity> allMethods = authMethodRepository.findByUserId(testUserId);
        assertFalse(allMethods.isEmpty(), "Should have authentication methods");
        assertTrue(allMethods.size() >= 3, "Should have at least 3 authentication methods");
        
        // Verify directly in Firestore
        QuerySnapshot snapshot = firestore.collection("users")
            .document(testUserId)
            .collection("authentication_methods")
            .get()
            .get();
            
        System.out.println("Total authentication methods in subcollection: " + snapshot.size());
        
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            System.out.println("Method ID: " + doc.getId());
            System.out.println("Method type: " + doc.getString("type"));
            System.out.println("Method data: " + doc.getData());
            System.out.println("---");
        }
    }
    
    @Test
    public void testSubcollectionPathStructure() throws ExecutionException, InterruptedException {
        System.out.println("Testing subcollection path structure");
        
        // Create a test authentication method directly
        AuthenticationMethodEntity testMethod = new AuthenticationMethodEntity();
        testMethod.setAuthenticationMethod(AuthenticationMethodType.PASSKEY);
        testMethod.putMetadata("testKey", "testValue");
        
        // Save using repository
        AuthenticationMethodEntity saved = authMethodRepository.saveForUser(testUserId, testMethod);
        assertNotNull(saved.getId(), "Saved method should have an ID");
        
        // Verify the exact path in Firestore
        String expectedPath = "users/" + testUserId + "/authentication_methods/" + saved.getId();
        DocumentSnapshot doc = firestore.document(expectedPath).get().get();
        
        assertTrue(doc.exists(), "Document should exist at path: " + expectedPath);
        assertEquals(AuthenticationMethodType.PASSKEY.getValue(), doc.getString("type"));
        
        System.out.println("Successfully verified subcollection path: " + expectedPath);
    }
    
    @Test
    public void testCleanup() throws ExecutionException, InterruptedException {
        // Clean up test data
        QuerySnapshot snapshot = firestore.collection("users")
            .document(testUserId)
            .collection("authentication_methods")
            .get()
            .get();
            
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            doc.getReference().delete().get();
        }
        
        System.out.println("Cleaned up " + snapshot.size() + " test documents");
    }
}