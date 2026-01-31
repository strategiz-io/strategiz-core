package io.strategiz.service.auth;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentSnapshot;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify subcollection creation works
 */
@SpringBootTest
@ActiveProfiles("test")
public class SimpleSubcollectionTest {

	@Autowired
	private AuthenticationMethodRepository authMethodRepository;

	@Autowired
	private Firestore firestore;

	@Test
	public void testDirectSubcollectionCreation() throws ExecutionException, InterruptedException {
		String testUserId = "test-user-" + UUID.randomUUID().toString();
		System.out.println("Testing direct subcollection creation for user: " + testUserId);

		// Create a test authentication method
		AuthenticationMethodEntity testMethod = new AuthenticationMethodEntity();
		testMethod.setAuthenticationMethod(AuthenticationMethodType.PASSKEY);
		testMethod.putMetadata("testKey", "testValue");
		testMethod.setIsActive(true);

		// Save using repository
		AuthenticationMethodEntity saved = authMethodRepository.saveForUser(testUserId, testMethod);
		assertNotNull(saved.getId(), "Saved method should have an ID");
		System.out.println("Saved authentication method with ID: " + saved.getId());

		// Verify the document exists in the subcollection
		String documentPath = "users/" + testUserId + "/authentication_methods/" + saved.getId();
		System.out.println("Checking document at path: " + documentPath);

		DocumentSnapshot doc = firestore.document(documentPath).get().get();
		assertTrue(doc.exists(), "Document should exist at path: " + documentPath);

		// Verify the data
		assertEquals(AuthenticationMethodType.PASSKEY.getValue(), doc.getString("type"));
		assertNotNull(doc.get("metadata"), "Metadata should not be null");
		assertNotNull(doc.get("createdAt"), "CreatedAt should not be null");

		System.out.println("Document data: " + doc.getData());
		System.out.println("Test passed - subcollection creation works!");

		// Clean up
		authMethodRepository.deleteForUser(testUserId, saved.getId());
	}

}