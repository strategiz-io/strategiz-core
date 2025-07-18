package io.strategiz.data.auth.repository.emailotp;

import io.strategiz.data.auth.entity.emailotp.EmailOtpAuthenticationMethodEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Email OTP authentication methods
 * 
 * Provides CRUD operations for Email OTP authentication methods stored in 
 * users/{userId}/authentication_methods subcollection in Firestore
 */
@Repository
public interface EmailOtpAuthenticationMethodRepository extends CrudRepository<EmailOtpAuthenticationMethodEntity, String> {
    
    /**
     * Find Email OTP authentication method by email address
     * Used to check if an email is already registered
     */
    Optional<EmailOtpAuthenticationMethodEntity> findByEmail(String email);
    
    /**
     * Find all verified Email OTP authentication methods for a user
     */
    List<EmailOtpAuthenticationMethodEntity> findByUserIdAndVerified(String userId, boolean verified);
    
    /**
     * Find all enabled Email OTP authentication methods for a user
     */
    List<EmailOtpAuthenticationMethodEntity> findByUserIdAndEnabled(String userId, boolean enabled);
    
    /**
     * Find Email OTP authentication methods that sent OTP after a specific time
     * Useful for rate limiting and cleanup operations
     */
    List<EmailOtpAuthenticationMethodEntity> findByLastOtpSentAtAfter(Instant timestamp);
    
    /**
     * Find Email OTP authentication methods with high daily email count
     * Useful for monitoring and abuse prevention
     */
    List<EmailOtpAuthenticationMethodEntity> findByDailyEmailCountGreaterThan(int count);
    
    /**
     * Check if email exists and is verified
     */
    boolean existsByEmailAndVerified(String email, boolean verified);
    
    /**
     * Check if email exists for any user
     */
    boolean existsByEmail(String email);
    
    /**
     * Count total Email OTP methods by verification status
     */
    long countByVerified(boolean verified);
    
    // Custom query methods for business logic
    
    /**
     * Find active (enabled and verified) Email OTP method for user
     */
    default Optional<EmailOtpAuthenticationMethodEntity> findActiveByUserId(String userId) {
        return findByUserIdAndVerified(userId, true).stream()
                .filter(EmailOtpAuthenticationMethodEntity::isEnabled)
                .findFirst();
    }
    
    /**
     * Find Email OTP method by user ID and email address
     */
    Optional<EmailOtpAuthenticationMethodEntity> findByUserIdAndEmail(String userId, String email);
}