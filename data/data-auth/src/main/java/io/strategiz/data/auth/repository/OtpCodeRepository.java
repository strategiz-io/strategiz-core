package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.OtpCodeEntity;

import java.util.Optional;

/**
 * Repository for managing OTP codes.
 * OTP codes are stored in the otp_codes top-level collection.
 *
 * <p>Key operations:</p>
 * <ul>
 *   <li>Save OTP codes with hashed values</li>
 *   <li>Find OTP by email and purpose</li>
 *   <li>Delete OTP after successful verification</li>
 *   <li>Cleanup of expired OTPs</li>
 * </ul>
 */
public interface OtpCodeRepository {

	/**
	 * Save an OTP code.
	 * @param entity the OTP code entity
	 * @param userId system user ID for audit (typically "system")
	 * @return the saved entity with generated ID
	 */
	OtpCodeEntity save(OtpCodeEntity entity, String userId);

	/**
	 * Find an OTP code by ID.
	 * @param id the OTP code ID
	 * @return the OTP code if found
	 */
	Optional<OtpCodeEntity> findById(String id);

	/**
	 * Find an OTP code by email and purpose.
	 * @param email the email address
	 * @param purpose the OTP purpose (e.g., "signup", "reset-password")
	 * @return the OTP code if found
	 */
	Optional<OtpCodeEntity> findByEmailAndPurpose(String email, String purpose);

	/**
	 * Update an OTP code (e.g., increment attempts).
	 * @param entity the OTP code entity
	 * @param userId system user ID for audit
	 * @return the updated entity
	 */
	OtpCodeEntity update(OtpCodeEntity entity, String userId);

	/**
	 * Delete an OTP code by ID.
	 * @param id the OTP code ID
	 */
	void deleteById(String id);

	/**
	 * Delete an OTP code by email and purpose.
	 * Called after successful verification.
	 * @param email the email address
	 * @param purpose the OTP purpose
	 */
	void deleteByEmailAndPurpose(String email, String purpose);

	/**
	 * Delete all expired OTP codes.
	 * Called periodically to clean up old codes.
	 * @return number of deleted codes
	 */
	int deleteExpired();

	/**
	 * Count OTP codes for an email in the last N hours.
	 * Used for rate limiting.
	 * @param email the email address
	 * @param hours the time window in hours
	 * @return number of codes sent
	 */
	long countByEmailInLastHours(String email, int hours);

}
