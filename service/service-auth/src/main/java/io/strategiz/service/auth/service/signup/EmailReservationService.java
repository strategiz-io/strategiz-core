package io.strategiz.service.auth.service.signup;

import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.user.entity.EmailReservationEntity;
import io.strategiz.data.user.entity.EmailReservationStatus;
import io.strategiz.data.user.repository.EmailReservationRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.base.BaseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing email reservations during signup.
 *
 * This service provides database-level email uniqueness guarantees by creating
 * reservations in the userEmails collection before user creation. The document ID
 * is the normalized email, leveraging Firestore's native uniqueness guarantee.
 *
 * Flow:
 * 1. initiateSignup: reserveEmail() creates PENDING reservation, returns pre-generated userId
 * 2. User completes verification (OTP, OAuth callback, etc.)
 * 3. verifySignup: confirmReservation() + create user in single transaction
 * 4. On failure: releaseReservation() cleans up
 * 5. Scheduled job: cleans up expired PENDING reservations
 *
 * This prevents race conditions where two users could sign up with the same email
 * simultaneously, with both passing the initial email check but only one succeeding
 * in user creation.
 */
@Service
public class EmailReservationService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-auth";
    }

    private final EmailReservationRepository emailReservationRepository;
    private final UserRepository userRepository;

    public EmailReservationService(
            EmailReservationRepository emailReservationRepository,
            UserRepository userRepository) {
        this.emailReservationRepository = emailReservationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Reserve an email address for signup.
     * This should be called at the start of the signup flow before sending OTP.
     *
     * The method generates a userId that should be used throughout the signup
     * process to ensure consistency.
     *
     * @param email The email address to reserve
     * @param signupType Type of signup (e.g., "email_otp", "oauth_google")
     * @param sessionId Session ID linking to the pending signup
     * @return The pre-generated userId to use for account creation
     * @throws StrategizException if email is already taken or reserved
     */
    public String reserveEmail(String email, String signupType, String sessionId) {
        String normalizedEmail = email.toLowerCase().trim();
        log.info("Reserving email for signup: {} (type: {})", normalizedEmail, signupType);

        // First check if a user with this email already exists (for lazy migration support)
        if (userRepository.getUserByEmail(normalizedEmail).isPresent()) {
            log.warn("Email already registered in users collection: {}", normalizedEmail);
            throw new StrategizException(AuthErrors.EMAIL_ALREADY_EXISTS, "Email address is already registered");
        }

        // Generate userId upfront
        String userId = UUID.randomUUID().toString();

        try {
            EmailReservationEntity reservation = new EmailReservationEntity(
                normalizedEmail,
                userId,
                signupType,
                sessionId
            );

            emailReservationRepository.reserve(reservation);
            log.info("Email reserved successfully: {} with userId: {}", normalizedEmail, userId);

            return userId;

        } catch (DataRepositoryException e) {
            // Convert data layer exception to auth exception
            // Check if it's a duplicate entity error (email already reserved)
            if (DataRepositoryErrorDetails.DUPLICATE_ENTITY.name().equals(e.getErrorCode())) {
                log.warn("Email reservation failed - already reserved: {}", normalizedEmail);
                throw new StrategizException(AuthErrors.EMAIL_ALREADY_EXISTS, "Email address is already in use");
            }
            log.error("Failed to reserve email {}: {}", normalizedEmail, e.getMessage(), e);
            throw new StrategizException(AuthErrors.SIGNUP_FAILED, "Failed to reserve email for signup");
        }
    }

    /**
     * Reserve an email with a pre-specified userId.
     * Useful for OAuth flows where the userId might be determined by the provider.
     *
     * @param email The email address to reserve
     * @param userId The userId to associate with this reservation
     * @param signupType Type of signup (e.g., "email_otp", "oauth_google")
     * @param sessionId Session ID linking to the pending signup
     * @throws StrategizException if email is already taken or reserved
     */
    public void reserveEmailWithUserId(String email, String userId, String signupType, String sessionId) {
        String normalizedEmail = email.toLowerCase().trim();
        log.info("Reserving email with userId for signup: {} (type: {})", normalizedEmail, signupType);

        // First check if a user with this email already exists
        if (userRepository.getUserByEmail(normalizedEmail).isPresent()) {
            log.warn("Email already registered in users collection: {}", normalizedEmail);
            throw new StrategizException(AuthErrors.EMAIL_ALREADY_EXISTS, "Email address is already registered");
        }

        try {
            EmailReservationEntity reservation = new EmailReservationEntity(
                normalizedEmail,
                userId,
                signupType,
                sessionId
            );

            emailReservationRepository.reserve(reservation);
            log.info("Email reserved successfully: {} with provided userId: {}", normalizedEmail, userId);

        } catch (DataRepositoryException e) {
            if (e.getMessage() != null && e.getMessage().contains("duplicate")) {
                log.warn("Email reservation failed - already reserved: {}", normalizedEmail);
                throw new StrategizException(AuthErrors.EMAIL_ALREADY_EXISTS, "Email address is already in use");
            }
            log.error("Failed to reserve email {}: {}", normalizedEmail, e.getMessage(), e);
            throw new StrategizException(AuthErrors.SIGNUP_FAILED, "Failed to reserve email for signup");
        }
    }

    /**
     * Confirm an email reservation during signup completion.
     * This should be called within the same transaction as user creation.
     *
     * @param email The email address to confirm
     * @return The confirmed reservation entity
     * @throws StrategizException if reservation not found or invalid
     */
    public EmailReservationEntity confirmReservation(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        log.info("Confirming email reservation: {}", normalizedEmail);

        try {
            EmailReservationEntity confirmed = emailReservationRepository.confirm(normalizedEmail);
            log.info("Email reservation confirmed: {}", normalizedEmail);
            return confirmed;

        } catch (DataRepositoryException e) {
            if (e.getMessage() != null && e.getMessage().contains("not-found")) {
                log.error("Cannot confirm - no reservation found for email: {}", normalizedEmail);
                throw new StrategizException(AuthErrors.SIGNUP_FAILED, "Email reservation not found");
            }
            log.error("Failed to confirm email reservation {}: {}", normalizedEmail, e.getMessage(), e);
            throw new StrategizException(AuthErrors.SIGNUP_FAILED, "Failed to confirm email reservation");
        }
    }

    /**
     * Release (delete) an email reservation.
     * Call this when signup fails or is cancelled to free up the email.
     *
     * @param email The email address to release
     */
    public void releaseReservation(String email) {
        if (email == null || email.isBlank()) {
            log.warn("Cannot release reservation - email is null or blank");
            return;
        }

        String normalizedEmail = email.toLowerCase().trim();
        log.info("Releasing email reservation: {}", normalizedEmail);

        try {
            emailReservationRepository.delete(normalizedEmail);
            log.info("Email reservation released: {}", normalizedEmail);
        } catch (Exception e) {
            // Log but don't throw - this is a cleanup operation
            log.warn("Failed to release email reservation {}: {}", normalizedEmail, e.getMessage());
        }
    }

    /**
     * Get the pre-generated userId for a reserved email.
     *
     * @param email The email address
     * @return Optional containing the userId if reservation exists
     */
    public Optional<String> getReservedUserId(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        return emailReservationRepository.findByEmail(normalizedEmail)
            .filter(EmailReservationEntity::isValid)
            .map(EmailReservationEntity::getUserId);
    }

    /**
     * Check if an email is available for registration.
     * This performs both user lookup and reservation check.
     *
     * @param email The email to check
     * @return true if email is available
     */
    public boolean isEmailAvailable(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        // Check users collection first (for lazy migration support)
        if (userRepository.getUserByEmail(normalizedEmail).isPresent()) {
            return false;
        }

        // Then check reservations
        return emailReservationRepository.isEmailAvailable(normalizedEmail);
    }

    /**
     * Get existing reservation for an email.
     *
     * @param email The email to check
     * @return Optional containing the reservation if exists
     */
    public Optional<EmailReservationEntity> getReservation(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        return emailReservationRepository.findByEmail(normalizedEmail);
    }

    /**
     * Scheduled job to clean up expired PENDING reservations.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredReservations() {
        log.info("Running scheduled cleanup of expired email reservations");
        try {
            int deletedCount = emailReservationRepository.deleteExpiredPendingReservations();
            if (deletedCount > 0) {
                log.info("Cleaned up {} expired email reservations", deletedCount);
            }
        } catch (Exception e) {
            log.error("Failed to clean up expired email reservations: {}", e.getMessage(), e);
        }
    }
}
