package io.strategiz.data.auth.repository.smsotp;

import io.strategiz.data.auth.entity.smsotp.SmsOtpAuthenticationMethodEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SMS OTP authentication methods
 * 
 * Provides CRUD operations for SMS OTP authentication methods stored in 
 * users/{userId}/authentication_methods subcollection in Firestore
 */
@Repository
public interface SmsOtpAuthenticationMethodRepository extends CrudRepository<SmsOtpAuthenticationMethodEntity, String> {
    
    /**
     * Find SMS OTP authentication method by phone number
     * Used to check if a phone number is already registered
     */
    Optional<SmsOtpAuthenticationMethodEntity> findByPhoneNumber(String phoneNumber);
    
    /**
     * Find all verified SMS OTP authentication methods for a user
     */
    List<SmsOtpAuthenticationMethodEntity> findByUserIdAndVerified(String userId, boolean verified);
    
    /**
     * Find all enabled SMS OTP authentication methods for a user
     */
    List<SmsOtpAuthenticationMethodEntity> findByUserIdAndEnabled(String userId, boolean enabled);
    
    /**
     * Find SMS OTP authentication methods by country code
     * Useful for analytics and region-specific operations
     */
    List<SmsOtpAuthenticationMethodEntity> findByCountryCode(String countryCode);
    
    /**
     * Find SMS OTP authentication methods that sent OTP after a specific time
     * Useful for rate limiting and cleanup operations
     */
    List<SmsOtpAuthenticationMethodEntity> findByLastOtpSentAtAfter(Instant timestamp);
    
    /**
     * Find SMS OTP authentication methods with high daily SMS count
     * Useful for monitoring and abuse prevention
     */
    List<SmsOtpAuthenticationMethodEntity> findByDailySmsCountGreaterThan(int count);
    
    /**
     * Check if phone number exists and is verified
     */
    boolean existsByPhoneNumberAndVerified(String phoneNumber, boolean verified);
    
    /**
     * Check if phone number exists for any user
     */
    boolean existsByPhoneNumber(String phoneNumber);
    
    /**
     * Count total SMS OTP methods by verification status
     */
    long countByVerified(boolean verified);
    
    /**
     * Count SMS OTP methods by country code
     */
    long countByCountryCode(String countryCode);
    
    // Custom query methods for business logic
    
    /**
     * Find active (enabled and verified) SMS OTP method for user
     */
    default Optional<SmsOtpAuthenticationMethodEntity> findActiveByUserId(String userId) {
        return findByUserIdAndVerified(userId, true).stream()
                .filter(SmsOtpAuthenticationMethodEntity::isEnabled)
                .findFirst();
    }
    
    /**
     * Find SMS OTP method by user ID and phone number
     */
    Optional<SmsOtpAuthenticationMethodEntity> findByUserIdAndPhoneNumber(String userId, String phoneNumber);
}