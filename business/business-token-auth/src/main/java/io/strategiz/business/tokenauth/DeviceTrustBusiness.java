package io.strategiz.business.tokenauth;

import io.strategiz.business.risk.RiskAssessmentBusiness;
import io.strategiz.business.risk.RiskAssessmentResult;
import io.strategiz.business.risk.RiskContext;
import io.strategiz.business.risk.RiskLevel;
import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.DeviceIdentityRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business logic for device trust verification and authentication.
 *
 * <p>
 * Device trust acts as a silent "something you have" factor following NIST SP 800-63B
 * guidelines. When a user returns on a recognized/trusted device:
 * </p>
 * <ul>
 * <li>With MFA configured: device trust replaces one MFA factor (one-click sign-in)</li>
 * <li>Without MFA: device trust enables one-click sign-in (ACR 1)</li>
 * <li>New/unknown device: full authentication required as normal</li>
 * </ul>
 *
 * <p>
 * Trust levels based on score and last verification:
 * </p>
 * <ul>
 * <li>HIGH (score 90+): 90-day trust duration</li>
 * <li>TRUSTED (score 80-89): 30-day trust duration</li>
 * <li>RECOGNIZED (score 70-79): 7-day trust duration</li>
 * <li>UNKNOWN (score &lt; 70): no trust granted</li>
 * </ul>
 */
@Component
public class DeviceTrustBusiness {

	private static final Logger log = LoggerFactory.getLogger(DeviceTrustBusiness.class);

	private static final int TRUST_SCORE_HIGH = 90;

	private static final int TRUST_SCORE_TRUSTED = 80;

	private static final int TRUST_SCORE_RECOGNIZED = 70;

	private static final Duration TRUST_DURATION_HIGH = Duration.ofDays(90);

	private static final Duration TRUST_DURATION_TRUSTED = Duration.ofDays(30);

	private static final Duration TRUST_DURATION_RECOGNIZED = Duration.ofDays(7);

	private static final double FINGERPRINT_DRIFT_THRESHOLD = 0.7;

	// In-memory challenge store with TTL (production should use Redis/Firestore)
	private final Map<String, ChallengeEntry> pendingChallenges = new ConcurrentHashMap<>();

	private final DeviceIdentityRepository deviceRepository;

	private final UserRepository userRepository;

	private final SessionAuthBusiness sessionAuthBusiness;

	private final RiskAssessmentBusiness riskAssessmentBusiness;

	private final SecureRandom secureRandom = new SecureRandom();

	@Autowired
	public DeviceTrustBusiness(DeviceIdentityRepository deviceRepository, UserRepository userRepository,
			@Lazy SessionAuthBusiness sessionAuthBusiness, RiskAssessmentBusiness riskAssessmentBusiness) {
		this.deviceRepository = deviceRepository;
		this.userRepository = userRepository;
		this.sessionAuthBusiness = sessionAuthBusiness;
		this.riskAssessmentBusiness = riskAssessmentBusiness;
	}

	/**
	 * Verify if a device is trusted for a given fingerprint. Looks up the device by
	 * fingerprint/visitorId across all authenticated devices and checks trust validity.
	 * @param fingerprint the device fingerprint or visitor ID
	 * @param ipAddress the client IP address for context
	 * @return DeviceTrustResult with trust status and user info if trusted
	 */
	public DeviceTrustResult verifyDeviceTrust(String fingerprint, String ipAddress) {
		log.info("Verifying device trust for fingerprint: {}", fingerprint);

		if (fingerprint == null || fingerprint.isEmpty()) {
			return DeviceTrustResult.untrusted("No fingerprint provided");
		}

		// Look up device by visitor ID (fingerprint)
		Optional<DeviceIdentity> deviceOpt = deviceRepository.findByVisitorId(fingerprint);

		if (deviceOpt.isEmpty()) {
			log.debug("No device found for fingerprint: {}", fingerprint);
			return DeviceTrustResult.untrusted("Device not recognized");
		}

		DeviceIdentity device = deviceOpt.get();

		// Device must be authenticated (linked to a user)
		if (!device.isAuthenticated()) {
			log.debug("Device {} is not authenticated", device.getDeviceId());
			return DeviceTrustResult.untrusted("Device not linked to user");
		}

		// Check trust validity
		if (!device.isTrustValid()) {
			log.debug("Device {} trust is not valid (score: {}, expires: {})", device.getDeviceId(),
					device.getTrustScore(), device.getTrustExpiresAt());
			return DeviceTrustResult.untrusted("Device trust expired or insufficient");
		}

		// Check fingerprint drift if baseline exists
		if (device.getBaselineFingerprint() != null) {
			double drift = checkFingerprintDrift(device.getBaselineFingerprint(), fingerprint);
			if (drift < FINGERPRINT_DRIFT_THRESHOLD) {
				log.warn("Fingerprint drift detected for device {}: confidence {}", device.getDeviceId(), drift);
				return DeviceTrustResult.untrusted("Device fingerprint changed significantly");
			}
		}

		// Look up user info
		String userId = device.getUserId();
		String userName = resolveUserName(userId);
		TrustLevel trustLevel = calculateTrustLevel(device.getTrustScore(), device.getLastTrustVerification());

		log.info("Device {} is trusted for user {} with level {}", device.getDeviceId(), userId, trustLevel);

		return DeviceTrustResult.trusted(device.getDeviceId(), userId, userName, trustLevel);
	}

	/**
	 * Generate a cryptographic challenge for device authentication. The challenge must be
	 * signed by the device's private key to prove possession.
	 * @param deviceId the device ID to generate challenge for
	 * @return ChallengeResult with challenge nonce and ID
	 */
	public ChallengeResult generateChallenge(String deviceId) {
		log.info("Generating challenge for device: {}", deviceId);

		// Clean up expired challenges
		cleanupExpiredChallenges();

		byte[] nonceBytes = new byte[32];
		secureRandom.nextBytes(nonceBytes);
		String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
		String challengeId = UUID.randomUUID().toString();

		pendingChallenges.put(challengeId, new ChallengeEntry(deviceId, challenge, Instant.now().plusSeconds(300)));

		return new ChallengeResult(challengeId, challenge);
	}

	/**
	 * Authenticate a user via device trust by verifying the signed challenge.
	 * @param deviceId the device ID
	 * @param challengeId the challenge ID from generateChallenge
	 * @param signedChallenge the challenge signed with the device's private key
	 * (Base64-encoded)
	 * @param ipAddress the client IP address
	 * @param userAgent the client user agent
	 * @return AuthResult with tokens if verification succeeds
	 */
	public Optional<SessionAuthBusiness.AuthResult> authenticateWithDeviceTrust(String deviceId, String challengeId,
			String signedChallenge, String ipAddress, String userAgent) {
		log.info("Authenticating with device trust: deviceId={}, challengeId={}", deviceId, challengeId);

		// Retrieve and validate challenge
		ChallengeEntry entry = pendingChallenges.remove(challengeId);
		if (entry == null) {
			log.warn("Challenge not found or expired: {}", challengeId);
			return Optional.empty();
		}

		if (Instant.now().isAfter(entry.expiresAt())) {
			log.warn("Challenge expired: {}", challengeId);
			return Optional.empty();
		}

		if (!deviceId.equals(entry.deviceId())) {
			log.warn("Device ID mismatch: expected {}, got {}", entry.deviceId(), deviceId);
			return Optional.empty();
		}

		// Find the device
		List<DeviceIdentity> devices = deviceRepository.findByDeviceId(deviceId);
		if (devices.isEmpty()) {
			log.warn("Device not found: {}", deviceId);
			return Optional.empty();
		}

		DeviceIdentity device = devices.get(0);

		if (!device.isAuthenticated() || !device.isTrustValid()) {
			log.warn("Device {} is not authenticated or trust is invalid", deviceId);
			return Optional.empty();
		}

		// Verify the cryptographic signature
		String publicKeyStr = device.getPublicKey();
		if (publicKeyStr == null || publicKeyStr.isEmpty()) {
			log.warn("Device {} has no public key for crypto verification", deviceId);
			return Optional.empty();
		}

		boolean signatureValid = verifyChallengeSignature(entry.challenge(), signedChallenge, publicKeyStr);
		if (!signatureValid) {
			log.warn("Challenge signature verification failed for device {}", deviceId);
			return Optional.empty();
		}

		// Run risk assessment before granting device trust
		RiskContext riskContext = new RiskContext(device.getUserId(), deviceId, ipAddress, device.getIpLocation(),
				Instant.now(), device);
		RiskAssessmentResult riskResult = riskAssessmentBusiness.assess(riskContext);

		if (riskResult.riskLevel() == RiskLevel.CRITICAL) {
			log.warn("CRITICAL risk detected for device trust auth: user={}, score={}", device.getUserId(),
					riskResult.totalScore());
			return Optional.empty();
		}
		if (riskResult.riskLevel() == RiskLevel.HIGH || riskResult.riskLevel() == RiskLevel.MEDIUM) {
			log.warn("Risk level {} overrides device trust for user={}, score={}", riskResult.riskLevel(),
					device.getUserId(), riskResult.totalScore());
			return Optional.empty();
		}

		// Update device trust metadata
		device.setLastTrustVerification(Instant.now());
		device.setCryptoVerified(true);
		device.setLastSeen(Instant.now());
		deviceRepository.saveAuthenticatedDevice(device, device.getUserId());

		// Determine auth methods - device_trust counts as a factor
		List<String> authMethods = new ArrayList<>();
		authMethods.add("device_trust");

		// Create authentication via SessionAuthBusiness
		SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(device.getUserId(), null,
				authMethods, false, deviceId, device.getVisitorId(), ipAddress, userAgent, null);

		SessionAuthBusiness.AuthResult result = sessionAuthBusiness.createAuthentication(authRequest);

		log.info("Device trust authentication successful for user {} via device {}", device.getUserId(), deviceId);

		return Optional.of(result);
	}

	/**
	 * Establish trust for a device after successful authentication. Called when a user
	 * completes normal authentication to grant trust to their device.
	 * @param deviceId the device ID to trust
	 * @param userId the user who owns the device
	 * @param fingerprint the current device fingerprint for baseline
	 */
	public void establishTrust(String deviceId, String userId, String fingerprint) {
		log.info("Establishing trust for device {} of user {}", deviceId, userId);

		Optional<DeviceIdentity> deviceOpt = deviceRepository.findAuthenticatedDevice(userId, deviceId);
		if (deviceOpt.isEmpty()) {
			log.warn("Cannot establish trust: device {} not found for user {}", deviceId, userId);
			return;
		}

		DeviceIdentity device = deviceOpt.get();
		int trustScore = device.getTrustScore();
		TrustLevel level = calculateTrustLevel(trustScore, null);

		Duration trustDuration = switch (level) {
			case HIGH -> TRUST_DURATION_HIGH;
			case TRUSTED -> TRUST_DURATION_TRUSTED;
			case RECOGNIZED -> TRUST_DURATION_RECOGNIZED;
			default -> Duration.ZERO;
		};

		if (trustDuration.isZero()) {
			log.info("Device {} trust score {} too low for trust establishment", deviceId, trustScore);
			return;
		}

		device.setTrusted(true);
		device.setTrustLevel(level.name().toLowerCase());
		device.setTrustExpiresAt(Instant.now().plus(trustDuration));
		device.setLastTrustVerification(Instant.now());
		device.setBaselineFingerprint(fingerprint);

		deviceRepository.saveAuthenticatedDevice(device, userId);
		log.info("Trust established for device {} at level {} (expires in {} days)", deviceId, level,
				trustDuration.toDays());
	}

	/**
	 * Revoke trust for a specific device.
	 * @param userId the user ID
	 * @param deviceId the device ID to revoke
	 * @return true if trust was revoked
	 */
	public boolean revokeTrust(String userId, String deviceId) {
		log.info("Revoking trust for device {} of user {}", deviceId, userId);

		Optional<DeviceIdentity> deviceOpt = deviceRepository.findAuthenticatedDevice(userId, deviceId);
		if (deviceOpt.isEmpty()) {
			return false;
		}

		DeviceIdentity device = deviceOpt.get();
		device.setTrusted(false);
		device.setTrustLevel("revoked");
		device.setTrustExpiresAt(null);
		device.setCryptoVerified(false);

		deviceRepository.saveAuthenticatedDevice(device, userId);
		log.info("Trust revoked for device {}", deviceId);
		return true;
	}

	/**
	 * Get all trusted devices for a user.
	 * @param userId the user ID
	 * @return list of trusted devices
	 */
	public List<DeviceIdentity> getTrustedDevices(String userId) {
		return deviceRepository.findByUserIdAndTrustedTrue(userId);
	}

	/**
	 * Calculate trust level based on trust score and last verification.
	 */
	public TrustLevel calculateTrustLevel(int trustScore, Instant lastVerified) {
		if (trustScore >= TRUST_SCORE_HIGH) {
			return TrustLevel.HIGH;
		}
		if (trustScore >= TRUST_SCORE_TRUSTED) {
			return TrustLevel.TRUSTED;
		}
		if (trustScore >= TRUST_SCORE_RECOGNIZED) {
			return TrustLevel.RECOGNIZED;
		}
		return TrustLevel.UNKNOWN;
	}

	/**
	 * Check fingerprint drift between stored baseline and current fingerprint. Returns a
	 * confidence score between 0.0 (completely different) and 1.0 (identical).
	 */
	public double checkFingerprintDrift(String storedFingerprint, String currentFingerprint) {
		if (storedFingerprint == null || currentFingerprint == null) {
			return 0.0;
		}
		if (storedFingerprint.equals(currentFingerprint)) {
			return 1.0;
		}
		// Visitor IDs from FingerprintJS are opaque hashes.
		// Different hash = different fingerprint. For now, binary comparison.
		// Future: could compare individual fingerprint components for partial matches.
		return 0.0;
	}

	/**
	 * Verify RSA-PSS signature of the challenge using the device's public key.
	 */
	boolean verifyChallengeSignature(String challenge, String signedChallengeBase64, String publicKeyBase64) {
		try {
			byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey publicKey = keyFactory.generatePublic(keySpec);

			Signature signature = Signature.getInstance("SHA256withRSA/PSS");
			signature.initVerify(publicKey);
			signature.update(challenge.getBytes());

			byte[] signatureBytes = Base64.getUrlDecoder().decode(signedChallengeBase64);
			return signature.verify(signatureBytes);
		}
		catch (Exception e) {
			log.error("Challenge signature verification error: {}", e.getMessage());
			return false;
		}
	}

	private String resolveUserName(String userId) {
		try {
			Optional<UserEntity> user = userRepository.findById(userId);
			if (user.isPresent()) {
				UserEntity u = user.get();
				if (u.getProfile() != null && u.getProfile().getName() != null) {
					return u.getProfile().getName();
				}
			}
		}
		catch (Exception e) {
			log.debug("Could not resolve user name for {}: {}", userId, e.getMessage());
		}
		return null;
	}

	private void cleanupExpiredChallenges() {
		Instant now = Instant.now();
		pendingChallenges.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
	}

	// Records and enums

	public enum TrustLevel {

		HIGH, TRUSTED, RECOGNIZED, UNKNOWN

	}

	public record DeviceTrustResult(boolean trusted, String deviceId, String userId, String userName,
			TrustLevel trustLevel, String reason) {
		public static DeviceTrustResult trusted(String deviceId, String userId, String userName,
				TrustLevel trustLevel) {
			return new DeviceTrustResult(true, deviceId, userId, userName, trustLevel, null);
		}

		public static DeviceTrustResult untrusted(String reason) {
			return new DeviceTrustResult(false, null, null, null, TrustLevel.UNKNOWN, reason);
		}
	}

	public record ChallengeResult(String challengeId, String challenge) {
	}

	private record ChallengeEntry(String deviceId, String challenge, Instant expiresAt) {
	}

}
