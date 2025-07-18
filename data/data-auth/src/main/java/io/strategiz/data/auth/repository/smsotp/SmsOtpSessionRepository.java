package io.strategiz.data.auth.repository.smsotp;

import io.strategiz.data.auth.entity.smsotp.SmsOtpSessionEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SMS OTP sessions
 * 
 * Provides CRUD operations for temporary SMS OTP sessions stored in 
 * sms_otp_sessions collection in Firestore
 */
@Repository
public interface SmsOtpSessionRepository extends CrudRepository<SmsOtpSessionEntity, String> {
    
    /**
     * Find active SMS OTP session by phone number
     * Used to check for existing OTP before sending a new one
     */
    Optional<SmsOtpSessionEntity> findByPhoneNumberAndVerifiedFalse(String phoneNumber);
    
    /**
     * Find SMS OTP session by phone number and OTP code
     * Used for verification
     */
    Optional<SmsOtpSessionEntity> findByPhoneNumberAndOtpCode(String phoneNumber, String otpCode);
    
    /**
     * Find all expired OTP sessions
     * Used for cleanup operations
     */
    List<SmsOtpSessionEntity> findByExpiresAtBefore(Instant timestamp);
    
    /**
     * Find all OTP sessions created after a specific time
     * Used for rate limiting and analytics
     */
    List<SmsOtpSessionEntity> findByCreatedAtAfter(Instant timestamp);
    
    /**
     * Find OTP sessions by IP address
     * Used for abuse prevention and rate limiting
     */
    List<SmsOtpSessionEntity> findByIpAddress(String ipAddress);
    
    /**
     * Find OTP sessions by IP address created after a specific time
     * Used for IP-based rate limiting
     */
    List<SmsOtpSessionEntity> findByIpAddressAndCreatedAtAfter(String ipAddress, Instant timestamp);
    
    /**
     * Find verified OTP sessions
     * Used for analytics and cleanup
     */
    List<SmsOtpSessionEntity> findByVerifiedTrue();
    
    /**
     * Find OTP sessions by user ID
     * Used when user is identified
     */
    List<SmsOtpSessionEntity> findByUserId(String userId);
    
    /**
     * Find OTP sessions by country code
     * Used for analytics and regional operations
     */
    List<SmsOtpSessionEntity> findByCountryCode(String countryCode);
    
    /**
     * Check if phone number has recent OTP session
     * Used for rate limiting
     */
    boolean existsByPhoneNumberAndCreatedAtAfter(String phoneNumber, Instant timestamp);
    
    /**
     * Check if IP address has recent OTP session
     * Used for IP-based rate limiting
     */
    boolean existsByIpAddressAndCreatedAtAfter(String ipAddress, Instant timestamp);
    
    /**
     * Count total OTP sessions by verification status
     */
    long countByVerified(boolean verified);
    
    /**
     * Count OTP sessions created after a specific time
     */
    long countByCreatedAtAfter(Instant timestamp);
    
    /**
     * Count OTP sessions by country code
     */
    long countByCountryCode(String countryCode);
    
    /**
     * Delete expired OTP sessions
     * Used for cleanup operations
     */
    void deleteByExpiresAtBefore(Instant timestamp);
    
    /**
     * Delete verified OTP sessions older than a specific time
     * Used for cleanup operations
     */
    void deleteByVerifiedTrueAndCreatedAtBefore(Instant timestamp);
    
    // Custom query methods for business logic
    
    /**
     * Find latest OTP session for phone number
     */
    default Optional<SmsOtpSessionEntity> findLatestByPhoneNumber(String phoneNumber) {
        return findByPhoneNumberAndVerifiedFalse(phoneNumber);
    }
    
    /**
     * Count recent OTP attempts by phone number (within time window)
     */
    default long countRecentAttemptsByPhoneNumber(String phoneNumber, Instant since) {
        return findByPhoneNumberAndCreatedAtAfter(phoneNumber, since).size();
    }
    
    /**
     * Count recent OTP attempts by IP address (within time window)
     */
    default long countRecentAttemptsByIpAddress(String ipAddress, Instant since) {
        return findByIpAddressAndCreatedAtAfter(ipAddress, since).size();
    }
    
    /**
     * Find OTP sessions by phone number and creation time range
     */
    List<SmsOtpSessionEntity> findByPhoneNumberAndCreatedAtBetween(String phoneNumber, Instant startTime, Instant endTime);
    
    /**
     * Find OTP sessions by phone number created after a specific time
     */
    List<SmsOtpSessionEntity> findByPhoneNumberAndCreatedAtAfter(String phoneNumber, Instant timestamp);
}