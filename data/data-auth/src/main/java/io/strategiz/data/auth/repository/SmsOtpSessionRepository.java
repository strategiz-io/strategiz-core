package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.SmsOtpSessionEntity;

import java.util.Optional;

/**
 * Repository for managing SMS OTP sessions in Firestore.
 * SMS OTP sessions are stored in the sms_otp_sessions top-level collection.
 *
 * <p>Key operations:</p>
 * <ul>
 *   <li>Save OTP sessions with hashed codes</li>
 *   <li>Find active session by phone number</li>
 *   <li>Find session by ID for verification</li>
 *   <li>Update attempts on verification failure</li>
 *   <li>Delete session after successful verification</li>
 *   <li>Cleanup of expired sessions</li>
 * </ul>
 *
 * <p>Industry standard configuration:</p>
 * <ul>
 *   <li>Expiration: 5 minutes</li>
 *   <li>Max attempts: 5</li>
 *   <li>Rate limiting: 60 seconds between requests</li>
 * </ul>
 */
public interface SmsOtpSessionRepository {

	/**
	 * Save an SMS OTP session.
	 * @param entity the SMS OTP session entity
	 * @param systemUserId system user ID for audit (typically "system")
	 * @return the saved entity with generated ID
	 */
	SmsOtpSessionEntity save(SmsOtpSessionEntity entity, String systemUserId);

	/**
	 * Find an SMS OTP session by ID.
	 * @param id the session ID
	 * @return the session if found and active
	 */
	Optional<SmsOtpSessionEntity> findById(String id);

	/**
	 * Find the most recent active (non-expired, non-verified) session for a phone number.
	 * @param phoneNumber the phone number in E.164 format
	 * @return the active session if found
	 */
	Optional<SmsOtpSessionEntity> findActiveByPhoneNumber(String phoneNumber);

	/**
	 * Find the most recent active session for a phone number and purpose.
	 * @param phoneNumber the phone number in E.164 format
	 * @param purpose the session purpose ("registration" or "authentication")
	 * @return the active session if found
	 */
	Optional<SmsOtpSessionEntity> findActiveByPhoneNumberAndPurpose(String phoneNumber, String purpose);

	/**
	 * Find the most recent session for a phone number (regardless of status).
	 * Used for rate limiting checks.
	 * @param phoneNumber the phone number in E.164 format
	 * @return the most recent session if found
	 */
	Optional<SmsOtpSessionEntity> findMostRecentByPhoneNumber(String phoneNumber);

	/**
	 * Update an SMS OTP session (e.g., increment attempts).
	 * @param entity the session entity
	 * @param systemUserId system user ID for audit
	 * @return the updated entity
	 */
	SmsOtpSessionEntity update(SmsOtpSessionEntity entity, String systemUserId);

	/**
	 * Delete an SMS OTP session by ID.
	 * Called after successful verification.
	 * @param id the session ID
	 */
	void deleteById(String id);

	/**
	 * Delete all sessions for a phone number.
	 * Called after successful verification to cleanup old sessions.
	 * @param phoneNumber the phone number
	 */
	void deleteByPhoneNumber(String phoneNumber);

	/**
	 * Delete all expired SMS OTP sessions.
	 * Called periodically to clean up old sessions.
	 * @return number of deleted sessions
	 */
	int deleteExpired();

	/**
	 * Count sessions for a phone number in the last N minutes.
	 * Used for rate limiting.
	 * @param phoneNumber the phone number
	 * @param minutes the time window in minutes
	 * @return number of sessions created
	 */
	long countByPhoneNumberInLastMinutes(String phoneNumber, int minutes);

	/**
	 * Check if a phone number can send a new OTP (rate limit check).
	 * Returns true if no session exists or the last session was created
	 * more than RATE_LIMIT_SECONDS ago.
	 * @param phoneNumber the phone number
	 * @return true if OTP can be sent
	 */
	boolean canSendOtp(String phoneNumber);

}
