package io.strategiz.service.auth.service.signup;

import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.user.entity.EmailReservationEntity;
import io.strategiz.data.user.entity.EmailReservationStatus;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.EmailReservationRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailReservationService.
 *
 * Tests email reservation logic for signup uniqueness enforcement. Uses mocks to isolate
 * the service from repository implementations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailReservationService Tests")
class EmailReservationServiceTest {

	@Mock
	private EmailReservationRepository emailReservationRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private EmailReservationService emailReservationService;

	private static final String TEST_EMAIL = "test@example.com";

	private static final String TEST_SESSION_ID = "session-123";

	private static final String TEST_SIGNUP_TYPE = "email_otp";

	@Nested
	@DisplayName("reserveEmail() Tests")
	class ReserveEmailTests {

		@Test
		@DisplayName("Should successfully reserve a new email")
		void reserveEmail_NewEmail_Success() {
			// Given
			when(userRepository.getUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
			when(emailReservationRepository.reserve(any(EmailReservationEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

			// When
			String userId = emailReservationService.reserveEmail(TEST_EMAIL, TEST_SIGNUP_TYPE, TEST_SESSION_ID);

			// Then
			assertNotNull(userId);
			assertTrue(userId.matches("[a-f0-9-]{36}"), "Should return valid UUID");

			// Verify reservation was created with correct data
			ArgumentCaptor<EmailReservationEntity> captor = ArgumentCaptor.forClass(EmailReservationEntity.class);
			verify(emailReservationRepository).reserve(captor.capture());

			EmailReservationEntity captured = captor.getValue();
			assertEquals(TEST_EMAIL.toLowerCase(), captured.getEmail());
			assertEquals(userId, captured.getUserId());
			assertEquals(TEST_SIGNUP_TYPE, captured.getSignupType());
			assertEquals(TEST_SESSION_ID, captured.getSessionId());
			assertEquals(EmailReservationStatus.PENDING, captured.getStatus());
		}

		@Test
		@DisplayName("Should normalize email to lowercase")
		void reserveEmail_MixedCaseEmail_NormalizesToLowercase() {
			// Given
			String mixedCaseEmail = "Test.User@EXAMPLE.COM";
			when(userRepository.getUserByEmail(mixedCaseEmail.toLowerCase())).thenReturn(Optional.empty());
			when(emailReservationRepository.reserve(any(EmailReservationEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

			// When
			emailReservationService.reserveEmail(mixedCaseEmail, TEST_SIGNUP_TYPE, TEST_SESSION_ID);

			// Then
			ArgumentCaptor<EmailReservationEntity> captor = ArgumentCaptor.forClass(EmailReservationEntity.class);
			verify(emailReservationRepository).reserve(captor.capture());
			assertEquals("test.user@example.com", captor.getValue().getEmail());
		}

		@Test
		@DisplayName("Should reject email that already exists in users collection")
		void reserveEmail_ExistingUser_ThrowsException() {
			// Given
			UserEntity existingUser = new UserEntity();
			existingUser.setUserId("existing-user-id");
			when(userRepository.getUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(TEST_EMAIL, TEST_SIGNUP_TYPE, TEST_SESSION_ID);
			});

			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception.getErrorCode());
			verify(emailReservationRepository, never()).reserve(any());
		}

		@Test
		@DisplayName("Should reject email that is already reserved (duplicate entity)")
		void reserveEmail_AlreadyReserved_ThrowsException() {
			// Given
			when(userRepository.getUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
			when(emailReservationRepository.reserve(any(EmailReservationEntity.class)))
				.thenThrow(new DataRepositoryException(DataRepositoryErrorDetails.DUPLICATE_ENTITY, "EmailReservation",
						TEST_EMAIL));

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(TEST_EMAIL, TEST_SIGNUP_TYPE, TEST_SESSION_ID);
			});

			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception.getErrorCode());
		}

		@Test
		@DisplayName("Should handle repository save failure")
		void reserveEmail_RepositoryFailure_ThrowsException() {
			// Given
			when(userRepository.getUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
			when(emailReservationRepository.reserve(any(EmailReservationEntity.class)))
				.thenThrow(new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_SAVE_FAILED,
						"EmailReservation", TEST_EMAIL));

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(TEST_EMAIL, TEST_SIGNUP_TYPE, TEST_SESSION_ID);
			});

			assertEquals(AuthErrors.SIGNUP_FAILED.name(), exception.getErrorCode());
		}

	}

	@Nested
	@DisplayName("Duplicate Email Signup Tests")
	class DuplicateEmailSignupTests {

		@Test
		@DisplayName("Should reject second signup attempt with same email")
		void reserveEmail_SecondAttemptSameEmail_Fails() {
			// Given - First signup succeeds
			when(userRepository.getUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
			when(emailReservationRepository.reserve(any(EmailReservationEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0)) // First call
																		// succeeds
				.thenThrow(new DataRepositoryException(DataRepositoryErrorDetails.DUPLICATE_ENTITY, "EmailReservation",
						TEST_EMAIL)); // Second call fails

			// First signup
			String userId1 = emailReservationService.reserveEmail(TEST_EMAIL, TEST_SIGNUP_TYPE, "session-1");
			assertNotNull(userId1);

			// When & Then - Second signup should fail
			StrategizException exception = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(TEST_EMAIL, TEST_SIGNUP_TYPE, "session-2");
			});

			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception.getErrorCode());
		}

		@Test
		@DisplayName("Should reject email_otp signup after oauth_google reservation")
		void reserveEmail_DifferentSignupTypes_StillRejectsForSameEmail() {
			// Given
			when(userRepository.getUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
			when(emailReservationRepository.reserve(any(EmailReservationEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0)) // First call
																		// succeeds
				.thenThrow(new DataRepositoryException(DataRepositoryErrorDetails.DUPLICATE_ENTITY, "EmailReservation",
						TEST_EMAIL)); // Second call fails

			// First signup via OAuth
			emailReservationService.reserveEmail(TEST_EMAIL, "oauth_google", "oauth-session");

			// When & Then - Email OTP signup should fail
			StrategizException exception = assertThrows(StrategizException.class, () -> {
				emailReservationService.reserveEmail(TEST_EMAIL, "email_otp", "otp-session");
			});

			assertEquals(AuthErrors.EMAIL_ALREADY_EXISTS.name(), exception.getErrorCode());
		}

	}

	@Nested
	@DisplayName("confirmReservation() Tests")
	class ConfirmReservationTests {

		@Test
		@DisplayName("Should successfully confirm a pending reservation")
		void confirmReservation_Success() {
			// Given
			EmailReservationEntity reservation = new EmailReservationEntity(TEST_EMAIL, "user-123", TEST_SIGNUP_TYPE,
					TEST_SESSION_ID);
			EmailReservationEntity confirmedReservation = new EmailReservationEntity(TEST_EMAIL, "user-123",
					TEST_SIGNUP_TYPE, TEST_SESSION_ID);
			confirmedReservation.confirm();

			when(emailReservationRepository.confirm(TEST_EMAIL)).thenReturn(confirmedReservation);

			// When
			EmailReservationEntity result = emailReservationService.confirmReservation(TEST_EMAIL);

			// Then
			assertNotNull(result);
			assertEquals(EmailReservationStatus.CONFIRMED, result.getStatus());
			assertNotNull(result.getConfirmedAtEpochSecond());
			verify(emailReservationRepository).confirm(TEST_EMAIL.toLowerCase());
		}

		@Test
		@DisplayName("Should throw exception when reservation not found")
		void confirmReservation_NotFound_ThrowsException() {
			// Given
			when(emailReservationRepository.confirm(anyString())).thenThrow(new DataRepositoryException(
					DataRepositoryErrorDetails.ENTITY_NOT_FOUND, "EmailReservation", TEST_EMAIL));

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class, () -> {
				emailReservationService.confirmReservation(TEST_EMAIL);
			});

			assertEquals(AuthErrors.SIGNUP_FAILED.name(), exception.getErrorCode());
		}

	}

	@Nested
	@DisplayName("releaseReservation() Tests")
	class ReleaseReservationTests {

		@Test
		@DisplayName("Should successfully release a reservation")
		void releaseReservation_Success() {
			// Given
			doNothing().when(emailReservationRepository).delete(TEST_EMAIL.toLowerCase());

			// When
			emailReservationService.releaseReservation(TEST_EMAIL);

			// Then
			verify(emailReservationRepository).delete(TEST_EMAIL.toLowerCase());
		}

		@Test
		@DisplayName("Should not throw exception on release failure")
		void releaseReservation_FailureSilent() {
			// Given
			doThrow(new RuntimeException("Delete failed")).when(emailReservationRepository).delete(anyString());

			// When & Then - Should not throw
			assertDoesNotThrow(() -> emailReservationService.releaseReservation(TEST_EMAIL));
		}

		@Test
		@DisplayName("Should handle null email gracefully")
		void releaseReservation_NullEmail_NoOperation() {
			// When & Then
			assertDoesNotThrow(() -> emailReservationService.releaseReservation(null));
			verify(emailReservationRepository, never()).delete(anyString());
		}

		@Test
		@DisplayName("Should handle blank email gracefully")
		void releaseReservation_BlankEmail_NoOperation() {
			// When & Then
			assertDoesNotThrow(() -> emailReservationService.releaseReservation("   "));
			verify(emailReservationRepository, never()).delete(anyString());
		}

	}

	@Nested
	@DisplayName("isEmailAvailable() Tests")
	class IsEmailAvailableTests {

		@Test
		@DisplayName("Should return true for available email")
		void isEmailAvailable_NoUserNoReservation_ReturnsTrue() {
			// Given
			when(userRepository.getUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
			when(emailReservationRepository.isEmailAvailable(TEST_EMAIL.toLowerCase())).thenReturn(true);

			// When
			boolean available = emailReservationService.isEmailAvailable(TEST_EMAIL);

			// Then
			assertTrue(available);
		}

		@Test
		@DisplayName("Should return false when user exists")
		void isEmailAvailable_UserExists_ReturnsFalse() {
			// Given
			UserEntity existingUser = new UserEntity();
			when(userRepository.getUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

			// When
			boolean available = emailReservationService.isEmailAvailable(TEST_EMAIL);

			// Then
			assertFalse(available);
			verify(emailReservationRepository, never()).isEmailAvailable(anyString());
		}

		@Test
		@DisplayName("Should return false when reservation exists")
		void isEmailAvailable_ReservationExists_ReturnsFalse() {
			// Given
			when(userRepository.getUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
			when(emailReservationRepository.isEmailAvailable(TEST_EMAIL.toLowerCase())).thenReturn(false);

			// When
			boolean available = emailReservationService.isEmailAvailable(TEST_EMAIL);

			// Then
			assertFalse(available);
		}

	}

	@Nested
	@DisplayName("getReservedUserId() Tests")
	class GetReservedUserIdTests {

		@Test
		@DisplayName("Should return userId for valid reservation")
		void getReservedUserId_ValidReservation_ReturnsUserId() {
			// Given
			String expectedUserId = UUID.randomUUID().toString();
			EmailReservationEntity reservation = new EmailReservationEntity(TEST_EMAIL, expectedUserId,
					TEST_SIGNUP_TYPE, TEST_SESSION_ID);

			when(emailReservationRepository.findByEmail(TEST_EMAIL.toLowerCase())).thenReturn(Optional.of(reservation));

			// When
			Optional<String> userId = emailReservationService.getReservedUserId(TEST_EMAIL);

			// Then
			assertTrue(userId.isPresent());
			assertEquals(expectedUserId, userId.get());
		}

		@Test
		@DisplayName("Should return empty for non-existent reservation")
		void getReservedUserId_NoReservation_ReturnsEmpty() {
			// Given
			when(emailReservationRepository.findByEmail(TEST_EMAIL.toLowerCase())).thenReturn(Optional.empty());

			// When
			Optional<String> userId = emailReservationService.getReservedUserId(TEST_EMAIL);

			// Then
			assertFalse(userId.isPresent());
		}

	}

	@Nested
	@DisplayName("Email Normalization Tests")
	class EmailNormalizationTests {

		@Test
		@DisplayName("Should normalize email with leading/trailing whitespace")
		void reserveEmail_EmailWithWhitespace_NormalizesCorrectly() {
			// Given
			String emailWithSpaces = "  Test@Example.COM  ";
			when(userRepository.getUserByEmail("test@example.com")).thenReturn(Optional.empty());
			when(emailReservationRepository.reserve(any(EmailReservationEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

			// When
			emailReservationService.reserveEmail(emailWithSpaces, TEST_SIGNUP_TYPE, TEST_SESSION_ID);

			// Then
			ArgumentCaptor<EmailReservationEntity> captor = ArgumentCaptor.forClass(EmailReservationEntity.class);
			verify(emailReservationRepository).reserve(captor.capture());
			assertEquals("test@example.com", captor.getValue().getEmail());
		}

	}

}
