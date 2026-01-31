package io.strategiz.data.user.entity;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;

/**
 * Entity for email reservations used to ensure email uniqueness during signup. The
 * document ID is the normalized lowercase email, which leverages Firestore's native
 * document ID uniqueness to prevent duplicate registrations.
 *
 * Stored in Firestore collection: userEmails
 *
 * Flow: 1. User initiates signup - PENDING reservation created 2. If reservation fails
 * (doc exists) - email already taken 3. User completes verification 4. Reservation
 * confirmed within transaction along with user creation 5. Scheduled job cleans up
 * expired PENDING reservations
 *
 * Note: This is a simple entity without BaseEntity lifecycle management since
 * reservations are either ephemeral (PENDING) or permanent (CONFIRMED).
 */
public class EmailReservationEntity {

	/**
	 * The normalized lowercase email address. This is also the document ID, guaranteeing
	 * uniqueness.
	 */
	@DocumentId
	private String email;

	/**
	 * Pre-generated UUID for the user account. Generated early so it can be used
	 * consistently throughout signup.
	 */
	private String userId;

	/**
	 * Current status of the reservation (PENDING or CONFIRMED).
	 */
	private EmailReservationStatus status;

	/**
	 * Epoch second when the reservation was created.
	 */
	private long createdAtEpochSecond;

	/**
	 * Epoch second when a PENDING reservation expires. After this time, the reservation
	 * can be cleaned up.
	 */
	private long expiresAtEpochSecond;

	/**
	 * Epoch second when the reservation was confirmed. Only set when status is CONFIRMED.
	 */
	private Long confirmedAtEpochSecond;

	/**
	 * Type of signup that created this reservation. Examples: "email_otp",
	 * "oauth_google", "oauth_facebook"
	 */
	private String signupType;

	/**
	 * Session ID linking to the pending signup process. Allows correlation with in-memory
	 * pending signup data.
	 */
	private String sessionId;

	/**
	 * Default TTL for PENDING reservations: 10 minutes.
	 */
	public static final int DEFAULT_TTL_SECONDS = 600;

	public EmailReservationEntity() {
		// Default constructor for Firestore deserialization
	}

	/**
	 * Create a new PENDING email reservation.
	 * @param email Normalized lowercase email address
	 * @param userId Pre-generated user ID
	 * @param signupType Type of signup (e.g., "email_otp", "oauth_google")
	 * @param sessionId Session ID for the signup process
	 */
	public EmailReservationEntity(String email, String userId, String signupType, String sessionId) {
		this(email, userId, signupType, sessionId, DEFAULT_TTL_SECONDS);
	}

	/**
	 * Create a new PENDING email reservation with custom TTL.
	 * @param email Normalized lowercase email address
	 * @param userId Pre-generated user ID
	 * @param signupType Type of signup (e.g., "email_otp", "oauth_google")
	 * @param sessionId Session ID for the signup process
	 * @param ttlSeconds Time-to-live in seconds for the PENDING reservation
	 */
	public EmailReservationEntity(String email, String userId, String signupType, String sessionId, int ttlSeconds) {
		this.email = email.toLowerCase();
		this.userId = userId;
		this.status = EmailReservationStatus.PENDING;
		this.createdAtEpochSecond = Instant.now().getEpochSecond();
		this.expiresAtEpochSecond = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
		this.signupType = signupType;
		this.sessionId = sessionId;
	}

	// Getters and Setters

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email != null ? email.toLowerCase() : null;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public EmailReservationStatus getStatus() {
		return status;
	}

	public void setStatus(EmailReservationStatus status) {
		this.status = status;
	}

	public long getCreatedAtEpochSecond() {
		return createdAtEpochSecond;
	}

	public void setCreatedAtEpochSecond(long createdAtEpochSecond) {
		this.createdAtEpochSecond = createdAtEpochSecond;
	}

	public long getExpiresAtEpochSecond() {
		return expiresAtEpochSecond;
	}

	public void setExpiresAtEpochSecond(long expiresAtEpochSecond) {
		this.expiresAtEpochSecond = expiresAtEpochSecond;
	}

	public Long getConfirmedAtEpochSecond() {
		return confirmedAtEpochSecond;
	}

	public void setConfirmedAtEpochSecond(Long confirmedAtEpochSecond) {
		this.confirmedAtEpochSecond = confirmedAtEpochSecond;
	}

	public String getSignupType() {
		return signupType;
	}

	public void setSignupType(String signupType) {
		this.signupType = signupType;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	// Utility Methods

	/**
	 * Check if this is a PENDING reservation that has expired.
	 */
	public boolean isExpired() {
		return status == EmailReservationStatus.PENDING && Instant.now().getEpochSecond() > expiresAtEpochSecond;
	}

	/**
	 * Check if this reservation is still valid (not expired if PENDING).
	 */
	public boolean isValid() {
		if (status == EmailReservationStatus.CONFIRMED) {
			return true;
		}
		return !isExpired();
	}

	/**
	 * Confirm this reservation, transitioning from PENDING to CONFIRMED.
	 */
	public void confirm() {
		this.status = EmailReservationStatus.CONFIRMED;
		this.confirmedAtEpochSecond = Instant.now().getEpochSecond();
	}

	@Override
	public String toString() {
		return "EmailReservationEntity{" + "email='" + email + '\'' + ", userId='" + userId + '\'' + ", status="
				+ status + ", signupType='" + signupType + '\'' + ", sessionId='" + sessionId + '\'' + ", createdAt="
				+ Instant.ofEpochSecond(createdAtEpochSecond) + ", expiresAt="
				+ Instant.ofEpochSecond(expiresAtEpochSecond) + (confirmedAtEpochSecond != null
						? ", confirmedAt=" + Instant.ofEpochSecond(confirmedAtEpochSecond) : "")
				+ '}';
	}

}
