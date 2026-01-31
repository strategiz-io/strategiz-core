package io.strategiz.service.auth.service.signup;

import io.strategiz.data.user.entity.EmailReservationEntity;
import io.strategiz.data.user.entity.EmailReservationStatus;
import io.strategiz.data.user.repository.EmailReservationRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for email reservation and uniqueness enforcement.
 *
 * These tests verify that: 1. Email reservations work correctly during signup 2.
 * Duplicate email signups are properly rejected 3. Concurrent signup attempts with the
 * same email are handled correctly 4. Email reservation cleanup works as expected
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Email Reservation Integration Tests")
public class EmailReservationIntegrationTest {

	@Autowired
	private EmailReservationService emailReservationService;

	@Autowired
	private EmailReservationRepository emailReservationRepository;

	// Track emails created during tests for cleanup
	private final List<String> testEmails = new ArrayList<>();

	@BeforeEach
	void setUp() {
		testEmails.clear();
	}

	@AfterEach
	void cleanUp() {
		// Clean up all test emails
		for (String email : testEmails) {
			try {
				emailReservationRepository.delete(email);
			}
			catch (Exception e) {
				// Ignore cleanup errors
			}
		}
	}

	private String generateTestEmail() {
		String email = "test-" + UUID.randomUUID().toString() + "@test-strategiz.io";
		testEmails.add(email);
		return email;
	}

	@Nested
	@DisplayName("Single Email Reservation Tests")
	class SingleEmailReservationTests {

		@Test
		@DisplayName("Should successfully reserve a new email")
		void reserveEmail_Success() {
			// Given
			String email = generateTestEmail();
			String sessionId = UUID.randomUUID().toString();

			// When
			String userId = emailReservationService.reserveEmail(email, "email_otp", sessionId);

			// Then
			assertNotNull(userId, "Reserved userId should not be null");
			assertTrue(userId.matches("[a-f0-9-]{36}"), "UserId should be a valid UUID");

			// Verify reservation exists
			Optional<EmailReservationEntity> reservation = emailReservationService.getReservation(email);
			assertTrue(reservation.isPresent(), "Reservation should exist");
			assertEquals(EmailReservationStatus.PENDING, reservation.get().getStatus());
			assertEquals(userId, reservation.get().getUserId());
			assertEquals("email_otp", reservation.get().getSignupType());
			assertEquals(sessionId, reservation.get().getSessionId());
		}

		@Test
		@DisplayName("Should confirm a pending reservation")
		void confirmReservation_Success() {
			// Given
			String email = generateTestEmail();
			String sessionId = UUID.randomUUID().toString();
			String userId = emailReservationService.reserveEmail(email, "email_otp", sessionId);

			// When
			EmailReservationEntity confirmed = emailReservationService.confirmReservation(email);

			// Then
			assertNotNull(confirmed);
			assertEquals(EmailReservationStatus.CONFIRMED, confirmed.getStatus());
			assertEquals(userId, confirmed.getUserId());
			assertNotNull(confirmed.getConfirmedAtEpochSecond());
		}

		@Test
		@DisplayName("Should release a reservation successfully")
		void releaseReservation_Success() {
			// Given
			String email = generateTestEmail();
			String sessionId = UUID.randomUUID().toString();
			emailReservationService.reserveEmail(email, "email_otp", sessionId);

			// Verify reservation exists
			assertTrue(emailReservationService.getReservation(email).isPresent());

			// When
			emailReservationService.releaseReservation(email);

			// Then
			assertFalse(emailReservationService.getReservation(email).isPresent(),
					"Reservation should be deleted after release");
		}

		@Test
		@DisplayName("Should return pre-generated userId for reserved email")
		void getReservedUserId_Success() {
			// Given
			String email = generateTestEmail();
			String sessionId = UUID.randomUUID().toString();
			String expectedUserId = emailReservationService.reserveEmail(email, "oauth_google", sessionId);

			// When
			Optional<String> reservedUserId = emailReservationService.getReservedUserId(email);

			// Then
			assertTrue(reservedUserId.isPresent());
			assertEquals(expectedUserId, reservedUserId.get());
		}

		@Test
		@DisplayName("Should check email availability correctly")
		void isEmailAvailable_Tests() {
			// Given
			String availableEmail = generateTestEmail();
			String reservedEmail = generateTestEmail();

			// Reserve one email
			emailReservationService.reserveEmail(reservedEmail, "email_otp", "session1");

			// Then
			assertTrue(emailReservationService.isEmailAvailable(availableEmail),
					"Non-reserved email should be available");
			assertFalse(emailReservationService.isEmailAvailable(reservedEmail),
					"Reserved email should not be available");
		}

	}

	@Nested
	@DisplayName("Duplicate Email Signup Tests")
	class DuplicateEmailSignupTests {

		@Test
		@DisplayName("Should reject second signup attempt with same email")
		void reserveEmail_DuplicateEmail_ShouldFail() {
			// Given
			String email = generateTestEmail();
			String sessionId1 = UUID.randomUUID().toString();
			String sessionId2 = UUID.randomUUID().toString();

			// First signup - should succeed
			String userId1 = emailReservationService.reserveEmail(email, "email_otp", sessionId1);
			assertNotNull(userId1);

			// When & Then - Second signup with same email should fail
			StrategizException exception = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(email, "email_otp", sessionId2);
			});

			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception.getErrorCode());
			assertTrue(exception.getMessage().contains("already"));
		}

		@Test
		@DisplayName("Should reject OAuth signup after email OTP signup with same email")
		void reserveEmail_MixedSignupTypes_ShouldFail() {
			// Given
			String email = generateTestEmail();
			String sessionId1 = UUID.randomUUID().toString();
			String sessionId2 = UUID.randomUUID().toString();

			// First signup via email OTP - should succeed
			String userId1 = emailReservationService.reserveEmail(email, "email_otp", sessionId1);
			assertNotNull(userId1);

			// When & Then - OAuth signup with same email should fail
			StrategizException exception = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(email, "oauth_google", sessionId2);
			});

			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception.getErrorCode());
		}

		@Test
		@DisplayName("Should reject signup after email is confirmed")
		void reserveEmail_AfterConfirmation_ShouldFail() {
			// Given
			String email = generateTestEmail();
			String sessionId1 = UUID.randomUUID().toString();
			String sessionId2 = UUID.randomUUID().toString();

			// First signup and confirm
			emailReservationService.reserveEmail(email, "email_otp", sessionId1);
			emailReservationService.confirmReservation(email);

			// When & Then - Second signup should fail
			StrategizException exception = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(email, "email_otp", sessionId2);
			});

			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception.getErrorCode());
		}

		@Test
		@DisplayName("Should handle case-insensitive email matching")
		void reserveEmail_CaseInsensitive_ShouldFail() {
			// Given
			String emailLower = generateTestEmail().toLowerCase();
			String emailUpper = emailLower.toUpperCase();
			String emailMixed = emailLower.substring(0, 5).toUpperCase() + emailLower.substring(5);

			// First signup with lowercase - should succeed
			emailReservationService.reserveEmail(emailLower, "email_otp", "session1");

			// When & Then - Uppercase version should fail
			StrategizException exception1 = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(emailUpper, "email_otp", "session2");
			});
			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception1.getErrorCode());

			// When & Then - Mixed case version should fail
			StrategizException exception2 = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(emailMixed, "email_otp", "session3");
			});
			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception2.getErrorCode());
		}

	}

	@Nested
	@DisplayName("Concurrent Signup Tests")
	class ConcurrentSignupTests {

		@Test
		@DisplayName("Should handle concurrent signups with same email - only one succeeds")
		void concurrentSignup_SameEmail_OnlyOneSucceeds() throws InterruptedException {
			// Given
			String email = generateTestEmail();
			int numThreads = 10;
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(numThreads);
			AtomicInteger successCount = new AtomicInteger(0);
			AtomicInteger failureCount = new AtomicInteger(0);
			ExecutorService executor = Executors.newFixedThreadPool(numThreads);

			// When - Launch concurrent signup attempts
			for (int i = 0; i < numThreads; i++) {
				final int threadNum = i;
				executor.submit(() -> {
					try {
						startLatch.await(); // Wait for signal to start simultaneously
						String sessionId = "session-" + threadNum;
						emailReservationService.reserveEmail(email, "email_otp", sessionId);
						successCount.incrementAndGet();
						System.out.println("Thread " + threadNum + " succeeded");
					}
					catch (StrategizException e) {
						if (AuthErrors.EMAIL_ALREADY_EXISTS.name().equals(e.getErrorCode())) {
							failureCount.incrementAndGet();
							System.out.println("Thread " + threadNum + " failed: " + e.getMessage());
						}
						else {
							fail("Unexpected error: " + e.getMessage());
						}
					}
					catch (Exception e) {
						fail("Unexpected exception: " + e.getMessage());
					}
					finally {
						doneLatch.countDown();
					}
				});
			}

			// Start all threads simultaneously
			startLatch.countDown();

			// Wait for all threads to complete
			assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete within timeout");
			executor.shutdown();

			// Then - Exactly one should succeed, rest should fail
			assertEquals(1, successCount.get(), "Exactly one signup should succeed");
			assertEquals(numThreads - 1, failureCount.get(), "All other signups should fail with EMAIL_ALREADY_EXISTS");

			// Verify single reservation exists
			Optional<EmailReservationEntity> reservation = emailReservationService.getReservation(email);
			assertTrue(reservation.isPresent());
			assertEquals(EmailReservationStatus.PENDING, reservation.get().getStatus());
		}

		@Test
		@DisplayName("Should handle concurrent signups with different emails - all succeed")
		void concurrentSignup_DifferentEmails_AllSucceed() throws InterruptedException {
			// Given
			int numThreads = 10;
			List<String> emails = new ArrayList<>();
			for (int i = 0; i < numThreads; i++) {
				emails.add(generateTestEmail());
			}

			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(numThreads);
			AtomicInteger successCount = new AtomicInteger(0);
			ExecutorService executor = Executors.newFixedThreadPool(numThreads);

			// When - Launch concurrent signup attempts with different emails
			for (int i = 0; i < numThreads; i++) {
				final int threadNum = i;
				final String email = emails.get(i);
				executor.submit(() -> {
					try {
						startLatch.await();
						String sessionId = "session-" + threadNum;
						emailReservationService.reserveEmail(email, "email_otp", sessionId);
						successCount.incrementAndGet();
					}
					catch (Exception e) {
						fail("Thread " + threadNum + " failed unexpectedly: " + e.getMessage());
					}
					finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
			executor.shutdown();

			// Then - All should succeed
			assertEquals(numThreads, successCount.get(), "All signups with unique emails should succeed");
		}

	}

	@Nested
	@DisplayName("Email Release and Retry Tests")
	class EmailReleaseAndRetryTests {

		@Test
		@DisplayName("Should allow signup after previous reservation is released")
		void reserveEmail_AfterRelease_ShouldSucceed() {
			// Given
			String email = generateTestEmail();
			String sessionId1 = UUID.randomUUID().toString();
			String sessionId2 = UUID.randomUUID().toString();

			// First signup
			String userId1 = emailReservationService.reserveEmail(email, "email_otp", sessionId1);
			assertNotNull(userId1);

			// Release the reservation (simulating signup failure)
			emailReservationService.releaseReservation(email);

			// When - Second signup should succeed
			String userId2 = emailReservationService.reserveEmail(email, "email_otp", sessionId2);

			// Then
			assertNotNull(userId2);
			assertNotEquals(userId1, userId2, "New signup should get a new userId");
		}

		@Test
		@DisplayName("Should silently handle release of non-existent reservation")
		void releaseReservation_NonExistent_NoError() {
			// Given
			String nonExistentEmail = "non-existent-" + UUID.randomUUID() + "@test.io";

			// When & Then - Should not throw exception
			assertDoesNotThrow(() -> {
				emailReservationService.releaseReservation(nonExistentEmail);
			});
		}

	}

	@Nested
	@DisplayName("OAuth Signup Tests")
	class OAuthSignupTests {

		@Test
		@DisplayName("Should reserve email for Google OAuth signup")
		void reserveEmail_GoogleOAuth_Success() {
			// Given
			String email = generateTestEmail();
			String sessionId = "oauth_google_" + System.currentTimeMillis();

			// When
			String userId = emailReservationService.reserveEmail(email, "oauth_google", sessionId);

			// Then
			assertNotNull(userId);

			Optional<EmailReservationEntity> reservation = emailReservationService.getReservation(email);
			assertTrue(reservation.isPresent());
			assertEquals("oauth_google", reservation.get().getSignupType());
		}

		@Test
		@DisplayName("Should reject Google OAuth signup after Facebook OAuth with same email")
		void reserveEmail_DifferentOAuthProviders_ShouldFail() {
			// Given
			String email = generateTestEmail();

			// First OAuth signup via Facebook
			emailReservationService.reserveEmail(email, "oauth_facebook", "fb_session");

			// When & Then - Google OAuth with same email should fail
			StrategizException exception = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(email, "oauth_google", "google_session");
			});

			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception.getErrorCode());
		}

	}

}
