package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.SmsOtpCodeEntity;

import java.util.Optional;

/**
 * Repository for managing SMS OTP codes in Firestore. SMS OTP codes are stored in the
 * sms_otp_codes top-level collection.
 *
 * <p>
 * Key operations:
 * </p>
 * <ul>
 * <li>Save OTP codes with hashed values</li>
 * <li>Find active code by phone number</li>
 * <li>Find code by ID for verification</li>
 * <li>Update attempts on verification failure</li>
 * <li>Delete code after successful verification</li>
 * <li>Cleanup of expired codes</li>
 * </ul>
 *
 * <p>
 * Industry standard configuration:
 * </p>
 * <ul>
 * <li>Expiration: 5 minutes</li>
 * <li>Max attempts: 5</li>
 * <li>Rate limiting: 60 seconds between requests</li>
 * </ul>
 */
public interface SmsOtpCodeRepository {

	/**
	 * Save an SMS OTP code.
	 * @param entity the SMS OTP code entity
	 * @param systemUserId system user ID for audit (typically "system")
	 * @return the saved entity with generated ID
	 */
	SmsOtpCodeEntity save(SmsOtpCodeEntity entity, String systemUserId);

	/**
	 * Find an SMS OTP code by ID.
	 * @param id the code ID
	 * @return the code if found and active
	 */
	Optional<SmsOtpCodeEntity> findById(String id);

	/**
	 * Find the most recent active (non-expired, non-verified) code for a phone number.
	 * @param phoneNumber the phone number in E.164 format
	 * @return the active code if found
	 */
	Optional<SmsOtpCodeEntity> findActiveByPhoneNumber(String phoneNumber);

	/**
	 * Find the most recent active code for a phone number and purpose.
	 * @param phoneNumber the phone number in E.164 format
	 * @param purpose the code purpose ("registration" or "authentication")
	 * @return the active code if found
	 */
	Optional<SmsOtpCodeEntity> findActiveByPhoneNumberAndPurpose(String phoneNumber, String purpose);

	/**
	 * Find the most recent code for a phone number (regardless of status). Used for rate
	 * limiting checks.
	 * @param phoneNumber the phone number in E.164 format
	 * @return the most recent code if found
	 */
	Optional<SmsOtpCodeEntity> findMostRecentByPhoneNumber(String phoneNumber);

	/**
	 * Update an SMS OTP code (e.g., increment attempts).
	 * @param entity the code entity
	 * @param systemUserId system user ID for audit
	 * @return the updated entity
	 */
	SmsOtpCodeEntity update(SmsOtpCodeEntity entity, String systemUserId);

	/**
	 * Delete an SMS OTP code by ID. Called after successful verification.
	 * @param id the code ID
	 */
	void deleteById(String id);

	/**
	 * Delete all codes for a phone number. Called after successful verification to
	 * cleanup old codes.
	 * @param phoneNumber the phone number
	 */
	void deleteByPhoneNumber(String phoneNumber);

	/**
	 * Delete all expired SMS OTP codes. Called periodically to clean up old codes.
	 * @return number of deleted codes
	 */
	int deleteExpired();

	/**
	 * Count codes for a phone number in the last N minutes. Used for rate limiting.
	 * @param phoneNumber the phone number
	 * @param minutes the time window in minutes
	 * @return number of codes created
	 */
	long countByPhoneNumberInLastMinutes(String phoneNumber, int minutes);

	/**
	 * Check if a phone number can send a new OTP (rate limit check). Returns true if no
	 * code exists or the last code was created more than RATE_LIMIT_SECONDS ago.
	 * @param phoneNumber the phone number
	 * @return true if OTP can be sent
	 */
	boolean canSendOtp(String phoneNumber);

}
