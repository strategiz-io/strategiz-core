package io.strategiz.service.auth.service.signup;

import io.strategiz.client.sendgrid.EmailProvider;
import io.strategiz.client.sendgrid.model.EmailDeliveryResult;
import io.strategiz.client.sendgrid.model.EmailMessage;
import io.strategiz.data.auth.entity.OtpCodeEntity;
import io.strategiz.data.auth.repository.OtpCodeRepository;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.model.signup.EmailSignupInitiateRequest;
import io.strategiz.service.auth.service.fraud.FraudDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailSignupService.
 *
 * Tests the two-step email signup flow: OTP initiation and verification. Uses
 * ReflectionTestUtils because the service uses field injection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailSignupService Tests")
class EmailSignupServiceTest {

	@Mock
	private FeatureFlagService featureFlagService;

	@Mock
	private EmailReservationService emailReservationService;

	@Mock
	private OtpCodeRepository otpCodeRepository;

	@Mock
	private EmailProvider emailProvider;

	@Mock
	private FraudDetectionService fraudDetectionService;

	private EmailSignupService emailSignupService;

	private static final String TEST_EMAIL = "user@example.com";

	private static final String TEST_NAME = "Test User";

	private static final String TEST_RECAPTCHA = "recaptcha-token";

	private static final String ADMIN_EMAIL = "admin@test.io";

	@BeforeEach
	void setUp() {
		emailSignupService = new EmailSignupService();
		ReflectionTestUtils.setField(emailSignupService, "featureFlagService", featureFlagService);
		ReflectionTestUtils.setField(emailSignupService, "emailReservationService", emailReservationService);
		ReflectionTestUtils.setField(emailSignupService, "otpCodeRepository", otpCodeRepository);
		ReflectionTestUtils.setField(emailSignupService, "emailProvider", emailProvider);
		ReflectionTestUtils.setField(emailSignupService, "fraudDetectionService", fraudDetectionService);
		ReflectionTestUtils.setField(emailSignupService, "expirationMinutes", 10);
		ReflectionTestUtils.setField(emailSignupService, "codeLength", 6);
		ReflectionTestUtils.setField(emailSignupService, "fromEmail", "no-reply@test.io");
		ReflectionTestUtils.setField(emailSignupService, "adminBypassEnabled", false);
		ReflectionTestUtils.setField(emailSignupService, "adminEmails", ADMIN_EMAIL);
	}

	@Nested
	@DisplayName("initiateSignup() Tests")
	class InitiateSignupTests {

		@Test
		@DisplayName("Should successfully initiate signup and return sessionId")
		void initiateSignup_Success_ReturnsSessionId() {
			// Given
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			when(emailReservationService.isEmailAvailable(TEST_EMAIL)).thenReturn(true);
			when(emailProvider.isAvailable()).thenReturn(true);
			when(emailProvider.sendEmail(any(EmailMessage.class)))
				.thenReturn(EmailDeliveryResult.success("msg-123", "SendGrid"));

			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, TEST_EMAIL, TEST_RECAPTCHA);

			// When
			String sessionId = emailSignupService.initiateSignup(request);

			// Then
			assertNotNull(sessionId);
			assertFalse(sessionId.isBlank());

			// Verify OTP was saved
			ArgumentCaptor<OtpCodeEntity> captor = ArgumentCaptor.forClass(OtpCodeEntity.class);
			verify(otpCodeRepository).save(captor.capture(), eq("system"));

			OtpCodeEntity saved = captor.getValue();
			assertEquals(TEST_EMAIL, saved.getEmail());
			assertEquals("email_signup", saved.getPurpose());
			assertEquals(sessionId, saved.getSessionId());
			assertEquals(TEST_NAME, saved.getMetadata().get("name"));

			// Verify email was sent
			verify(emailProvider).sendEmail(any(EmailMessage.class));
		}

		@Test
		@DisplayName("Should skip email send for admin bypass and log OTP")
		void initiateSignup_AdminBypass_SkipsEmailSend() {
			// Given
			ReflectionTestUtils.setField(emailSignupService, "adminBypassEnabled", true);
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			when(emailReservationService.isEmailAvailable(ADMIN_EMAIL)).thenReturn(true);

			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, ADMIN_EMAIL, TEST_RECAPTCHA);

			// When
			String sessionId = emailSignupService.initiateSignup(request);

			// Then
			assertNotNull(sessionId);
			verify(otpCodeRepository).save(any(OtpCodeEntity.class), eq("system"));
			verify(emailProvider, never()).sendEmail(any());
		}

		@Test
		@DisplayName("Should throw AUTH_METHOD_DISABLED when feature flag is off")
		void initiateSignup_FeatureFlagDisabled_Throws() {
			// Given
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(false);
			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, TEST_EMAIL, TEST_RECAPTCHA);

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> emailSignupService.initiateSignup(request));
			assertEquals(AuthErrors.AUTH_METHOD_DISABLED.name(), exception.getErrorCode());
		}

		@Test
		@DisplayName("Should throw EMAIL_ALREADY_EXISTS when email is taken")
		void initiateSignup_EmailTaken_Throws() {
			// Given
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			when(emailReservationService.isEmailAvailable(TEST_EMAIL)).thenReturn(false);
			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, TEST_EMAIL, TEST_RECAPTCHA);

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> emailSignupService.initiateSignup(request));
			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception.getErrorCode());
		}

		@Test
		@DisplayName("Should throw EMAIL_SEND_FAILED and clean up OTP when email fails")
		void initiateSignup_EmailSendFails_ThrowsAndCleansUp() {
			// Given
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			when(emailReservationService.isEmailAvailable(TEST_EMAIL)).thenReturn(true);
			when(emailProvider.isAvailable()).thenReturn(true);
			when(emailProvider.sendEmail(any(EmailMessage.class)))
				.thenReturn(EmailDeliveryResult.failure("500", "Send failed", "SendGrid"));

			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, TEST_EMAIL, TEST_RECAPTCHA);

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> emailSignupService.initiateSignup(request));
			assertEquals(AuthErrors.EMAIL_SEND_FAILED.name(), exception.getErrorCode());

			// Verify cleanup
			verify(otpCodeRepository).deleteByEmailAndPurpose(TEST_EMAIL, "email_signup");
		}

		@Test
		@DisplayName("Should propagate fraud detection exception")
		void initiateSignup_FraudDetected_Propagates() {
			// Given
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			doThrow(new StrategizException(AuthErrors.FRAUD_DETECTED, "Fraud detected")).when(fraudDetectionService)
				.verifySignup(anyString(), anyString());

			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, TEST_EMAIL, TEST_RECAPTCHA);

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> emailSignupService.initiateSignup(request));
			assertEquals(AuthErrors.FRAUD_DETECTED.name(), exception.getErrorCode());
		}

		@Test
		@DisplayName("Should normalize uppercase email to lowercase")
		void initiateSignup_UppercaseEmail_NormalizesToLowercase() {
			// Given
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			when(emailReservationService.isEmailAvailable("user@example.com")).thenReturn(true);
			when(emailProvider.isAvailable()).thenReturn(true);
			when(emailProvider.sendEmail(any(EmailMessage.class)))
				.thenReturn(EmailDeliveryResult.success("msg-123", "SendGrid"));

			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, "USER@EXAMPLE.COM",
					TEST_RECAPTCHA);

			// When
			emailSignupService.initiateSignup(request);

			// Then
			ArgumentCaptor<OtpCodeEntity> captor = ArgumentCaptor.forClass(OtpCodeEntity.class);
			verify(otpCodeRepository).save(captor.capture(), eq("system"));
			assertEquals("user@example.com", captor.getValue().getEmail());
		}

		@Test
		@DisplayName("Should throw EMAIL_SEND_FAILED when no email provider configured")
		void initiateSignup_NoEmailProvider_Throws() {
			// Given
			ReflectionTestUtils.setField(emailSignupService, "emailProvider", null);
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			when(emailReservationService.isEmailAvailable(TEST_EMAIL)).thenReturn(true);

			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, TEST_EMAIL, TEST_RECAPTCHA);

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> emailSignupService.initiateSignup(request));
			assertEquals(AuthErrors.EMAIL_SEND_FAILED.name(), exception.getErrorCode());
			verify(otpCodeRepository).deleteByEmailAndPurpose(TEST_EMAIL, "email_signup");
		}

		@Test
		@DisplayName("Should proceed without fraud detection when service is null")
		void initiateSignup_NoFraudDetectionService_Proceeds() {
			// Given
			ReflectionTestUtils.setField(emailSignupService, "fraudDetectionService", null);
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			when(emailReservationService.isEmailAvailable(TEST_EMAIL)).thenReturn(true);
			when(emailProvider.isAvailable()).thenReturn(true);
			when(emailProvider.sendEmail(any(EmailMessage.class)))
				.thenReturn(EmailDeliveryResult.success("msg-123", "SendGrid"));

			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, TEST_EMAIL, TEST_RECAPTCHA);

			// When
			String sessionId = emailSignupService.initiateSignup(request);

			// Then
			assertNotNull(sessionId);
			verify(otpCodeRepository).save(any(OtpCodeEntity.class), eq("system"));
		}

	}

	@Nested
	@DisplayName("verifyEmailOtp() Tests")
	class VerifyEmailOtpTests {

		private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		private OtpCodeEntity createOtpEntity(String email, String rawCode, String sessionId) {
			OtpCodeEntity entity = new OtpCodeEntity(email, "email_signup", encoder.encode(rawCode),
					Instant.now().plus(10, ChronoUnit.MINUTES));
			entity.setId("otp-123");
			entity.setSessionId(sessionId);
			Map<String, String> metadata = new HashMap<>();
			metadata.put("name", TEST_NAME);
			entity.setMetadata(metadata);
			return entity;
		}

		@Test
		@DisplayName("Should verify valid OTP and return result")
		void verifyEmailOtp_ValidOtp_ReturnsResult() {
			// Given
			String sessionId = "session-abc";
			String otpCode = "123456";
			OtpCodeEntity entity = createOtpEntity(TEST_EMAIL, otpCode, sessionId);
			when(otpCodeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(entity));

			// When
			EmailSignupService.EmailVerificationResult result = emailSignupService.verifyEmailOtp(TEST_EMAIL, otpCode,
					sessionId);

			// Then
			assertEquals(TEST_EMAIL, result.email());
			assertEquals(TEST_NAME, result.name());
			verify(otpCodeRepository).deleteById("otp-123");
		}

		@Test
		@DisplayName("Should skip BCrypt check for admin bypass")
		void verifyEmailOtp_AdminBypass_SkipsBCryptCheck() {
			// Given
			ReflectionTestUtils.setField(emailSignupService, "adminBypassEnabled", true);
			String sessionId = "session-admin";
			OtpCodeEntity entity = createOtpEntity(ADMIN_EMAIL, "123456", sessionId);
			when(otpCodeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(entity));

			// When — pass wrong OTP code, should still succeed for admin
			EmailSignupService.EmailVerificationResult result = emailSignupService.verifyEmailOtp(ADMIN_EMAIL,
					"wrong-code", sessionId);

			// Then
			assertEquals(ADMIN_EMAIL, result.email());
			assertEquals(TEST_NAME, result.name());
			verify(otpCodeRepository).deleteById("otp-123");
		}

		@Test
		@DisplayName("Should throw OTP_NOT_FOUND when session not found")
		void verifyEmailOtp_SessionNotFound_Throws() {
			// Given
			when(otpCodeRepository.findBySessionId("nonexistent")).thenReturn(Optional.empty());

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> emailSignupService.verifyEmailOtp(TEST_EMAIL, "123456", "nonexistent"));
			assertEquals(AuthErrors.OTP_NOT_FOUND.name(), exception.getErrorCode());
		}

		@Test
		@DisplayName("Should throw VERIFICATION_FAILED when email mismatches")
		void verifyEmailOtp_EmailMismatch_Throws() {
			// Given
			String sessionId = "session-abc";
			OtpCodeEntity entity = createOtpEntity(TEST_EMAIL, "123456", sessionId);
			when(otpCodeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(entity));

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> emailSignupService.verifyEmailOtp("other@example.com", "123456", sessionId));
			assertEquals(AuthErrors.VERIFICATION_FAILED.name(), exception.getErrorCode());
		}

		@Test
		@DisplayName("Should throw OTP_EXPIRED and delete OTP when expired")
		void verifyEmailOtp_Expired_ThrowsAndDeletes() {
			// Given
			String sessionId = "session-abc";
			OtpCodeEntity entity = new OtpCodeEntity(TEST_EMAIL, "email_signup", encoder.encode("123456"),
					Instant.now().minus(1, ChronoUnit.MINUTES));
			entity.setId("otp-expired");
			entity.setSessionId(sessionId);
			Map<String, String> metadata = new HashMap<>();
			metadata.put("name", TEST_NAME);
			entity.setMetadata(metadata);
			when(otpCodeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(entity));

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> emailSignupService.verifyEmailOtp(TEST_EMAIL, "123456", sessionId));
			assertEquals(AuthErrors.OTP_EXPIRED.name(), exception.getErrorCode());
			verify(otpCodeRepository).deleteById("otp-expired");
		}

		@Test
		@DisplayName("Should throw VERIFICATION_FAILED and increment attempts on wrong OTP")
		void verifyEmailOtp_WrongOtp_ThrowsAndIncrements() {
			// Given
			String sessionId = "session-abc";
			OtpCodeEntity entity = createOtpEntity(TEST_EMAIL, "123456", sessionId);
			when(otpCodeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(entity));

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> emailSignupService.verifyEmailOtp(TEST_EMAIL, "999999", sessionId));
			assertEquals(AuthErrors.VERIFICATION_FAILED.name(), exception.getErrorCode());

			// Verify attempts incremented and entity updated
			assertEquals(1, entity.getAttempts());
			verify(otpCodeRepository).update(entity, "system");
		}

		@Test
		@DisplayName("Should normalize email during verification")
		void verifyEmailOtp_UppercaseEmail_Normalizes() {
			// Given
			String sessionId = "session-abc";
			String otpCode = "123456";
			OtpCodeEntity entity = createOtpEntity(TEST_EMAIL, otpCode, sessionId);
			when(otpCodeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(entity));

			// When
			EmailSignupService.EmailVerificationResult result = emailSignupService
				.verifyEmailOtp("USER@EXAMPLE.COM", otpCode, sessionId);

			// Then
			assertEquals(TEST_EMAIL, result.email());
		}

	}

	@Nested
	@DisplayName("OTP Storage Tests")
	class OtpStorageTests {

		@Test
		@DisplayName("Should store OTP with correct email, purpose, sessionId, and metadata")
		void initiateSignup_StoresCorrectOtpEntity() {
			// Given
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			when(emailReservationService.isEmailAvailable(TEST_EMAIL)).thenReturn(true);
			when(emailProvider.isAvailable()).thenReturn(true);
			when(emailProvider.sendEmail(any(EmailMessage.class)))
				.thenReturn(EmailDeliveryResult.success("msg-123", "SendGrid"));

			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, TEST_EMAIL, TEST_RECAPTCHA);

			// When
			String sessionId = emailSignupService.initiateSignup(request);

			// Then
			ArgumentCaptor<OtpCodeEntity> captor = ArgumentCaptor.forClass(OtpCodeEntity.class);
			verify(otpCodeRepository).save(captor.capture(), eq("system"));

			OtpCodeEntity saved = captor.getValue();
			assertEquals(TEST_EMAIL, saved.getEmail());
			assertEquals("email_signup", saved.getPurpose());
			assertEquals(sessionId, saved.getSessionId());
			assertNotNull(saved.getMetadata());
			assertEquals(TEST_NAME, saved.getMetadata().get("name"));
		}

		@Test
		@DisplayName("Should store BCrypt-hashed OTP code, not plaintext")
		void initiateSignup_OtpCodeIsBCryptHashed() {
			// Given
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			when(emailReservationService.isEmailAvailable(TEST_EMAIL)).thenReturn(true);
			when(emailProvider.isAvailable()).thenReturn(true);
			when(emailProvider.sendEmail(any(EmailMessage.class)))
				.thenReturn(EmailDeliveryResult.success("msg-123", "SendGrid"));

			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, TEST_EMAIL, TEST_RECAPTCHA);

			// When
			emailSignupService.initiateSignup(request);

			// Then
			ArgumentCaptor<OtpCodeEntity> captor = ArgumentCaptor.forClass(OtpCodeEntity.class);
			verify(otpCodeRepository).save(captor.capture(), eq("system"));

			String codeHash = captor.getValue().getCodeHash();
			assertNotNull(codeHash);
			assertTrue(codeHash.startsWith("$2a$"), "Code hash should be BCrypt-encoded");
			assertNotEquals(6, codeHash.length(), "Code hash should not be plaintext OTP length");
		}

		@Test
		@DisplayName("Should set expiration approximately 10 minutes from now")
		void initiateSignup_SetsCorrectExpiration() {
			// Given
			when(featureFlagService.isEmailOtpSignupEnabled()).thenReturn(true);
			when(emailReservationService.isEmailAvailable(TEST_EMAIL)).thenReturn(true);
			when(emailProvider.isAvailable()).thenReturn(true);
			when(emailProvider.sendEmail(any(EmailMessage.class)))
				.thenReturn(EmailDeliveryResult.success("msg-123", "SendGrid"));

			EmailSignupInitiateRequest request = new EmailSignupInitiateRequest(TEST_NAME, TEST_EMAIL, TEST_RECAPTCHA);

			// When
			Instant before = Instant.now().plus(9, ChronoUnit.MINUTES);
			emailSignupService.initiateSignup(request);
			Instant after = Instant.now().plus(11, ChronoUnit.MINUTES);

			// Then
			ArgumentCaptor<OtpCodeEntity> captor = ArgumentCaptor.forClass(OtpCodeEntity.class);
			verify(otpCodeRepository).save(captor.capture(), eq("system"));

			Instant expiration = captor.getValue().getExpiresAt();
			assertNotNull(expiration);
			assertTrue(expiration.isAfter(before), "Expiration should be after ~9 minutes from now");
			assertTrue(expiration.isBefore(after), "Expiration should be before ~11 minutes from now");
		}

	}

}
