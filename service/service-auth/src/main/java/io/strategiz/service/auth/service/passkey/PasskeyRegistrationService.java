package io.strategiz.service.auth.service.passkey;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.model.passkey.PasskeyChallengeType;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.auth.exception.ServiceAuthErrorDetails;
import io.strategiz.data.user.repository.UserRepository;

// Import WebAuthn4J libraries for proper attestation parsing
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.converter.AuthenticatorDataConverter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service handling passkey registration flows
 */
@Service
public class PasskeyRegistrationService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	@Value("${passkey.rpId:localhost}")
	private String rpId;

	@Value("${passkey.rpName:Strategiz}")
	private String rpName;

	@Value("${passkey.challengeTimeoutMs:60000}")
	private int challengeTimeoutMs;

	private final PasskeyChallengeService challengeService;

	private final AuthenticationMethodRepository authMethodRepository;

	private final SessionAuthBusiness sessionAuthBusiness;

	private final UserRepository userRepository;

	private final FeatureFlagService featureFlagService;

	public PasskeyRegistrationService(PasskeyChallengeService challengeService,
			AuthenticationMethodRepository authMethodRepository, SessionAuthBusiness sessionAuthBusiness,
			UserRepository userRepository, FeatureFlagService featureFlagService) {
		this.challengeService = challengeService;
		this.authMethodRepository = authMethodRepository;
		this.sessionAuthBusiness = sessionAuthBusiness;
		this.userRepository = userRepository;
		this.featureFlagService = featureFlagService;

		// Ensure we're using real passkey registration, not mock data
		ensureRealApiData("PasskeyRegistrationService");
	}

	/**
	 * Registration request data
	 */
	public record RegistrationRequest(String userId, String username) {
	}

	/**
	 * Registration completion data
	 */
	public record RegistrationCompletion(String userId, String credentialId, String attestationObject,
			String clientDataJSON, String deviceId) {
	}

	/**
	 * Authenticator selection criteria
	 */
	public record AuthenticatorSelectionCriteria(String authenticatorAttachment, String residentKey,
			boolean requireResidentKey, String userVerification) {
	}

	/**
	 * Registration challenge data
	 */
	public record RegistrationChallenge(String rpId, String rpName, String username, String userId, String challenge,
			int timeout, AuthenticatorSelectionCriteria authenticatorSelection, String attestation,
			boolean excludeCredentials) {
	}

	/**
	 * Registration result
	 */
	public record RegistrationResult(boolean success, String credentialId, Object result) {
		public AuthTokens tokens() {
			return (AuthTokens) result;
		}
	}

	/**
	 * Authentication tokens
	 */
	public record AuthTokens(String accessToken, String refreshToken) {
	}

	/**
	 * Begin passkey registration process by generating a challenge
	 * @param request Registration request with user details
	 * @return Registration challenge for the client
	 */
	public RegistrationChallenge beginRegistration(RegistrationRequest request) {
		String userId = request.userId();
		String username = request.username();

		log.info("Beginning passkey registration for user: {}", userId);

		// Check if passkey signup is enabled
		if (!featureFlagService.isPasskeySignupEnabled()) {
			log.warn("Passkey signup is disabled - rejecting registration for user: {}", userId);
			throw new StrategizException(AuthErrors.AUTH_METHOD_DISABLED, "Passkey signup is currently disabled");
		}

		// Validate real API connection
		if (!validateRealApiConnection("PasskeyRegistrationService")) {
			throwModuleException(ServiceAuthErrorDetails.EXTERNAL_SERVICE_ERROR, "PasskeyRegistrationService",
					"validateRealApiConnection");
		}

		// Generate a challenge for this registration
		String challenge = challengeService.createChallenge(userId, PasskeyChallengeType.REGISTRATION);

		// Configure authenticator selection criteria to support both platform and
		// cross-platform authenticators
		AuthenticatorSelectionCriteria authenticatorSelection = new AuthenticatorSelectionCriteria(null, // null
																											// =
																											// support
																											// both
																											// platform
																											// and
																											// cross-platform
																											// authenticators
				"preferred", // Prefer resident keys (discoverable credentials)
				false, // Legacy property, using residentKey instead
				"preferred" // Prefer user verification
		);

		// Create registration options
		return new RegistrationChallenge(rpId, rpName, username, userId, challenge, challengeTimeoutMs,
				authenticatorSelection, "none", // Attestation conveyance preference -
												// "none", "indirect", "direct", or
												// "enterprise"
				false // Whether to exclude existing credentials
		);
	}

	/**
	 * Complete passkey registration by verifying and storing the credential
	 * @param completion Registration completion data
	 * @return Registration result
	 */
	@Transactional
	public RegistrationResult completeRegistration(RegistrationCompletion completion) {
		String userId = completion.userId();
		String credentialId = completion.credentialId();
		String attestationObject = completion.attestationObject();
		String clientDataJSON = completion.clientDataJSON();
		String deviceId = completion.deviceId();

		log.info("Completing passkey registration for user: {}", userId);

		// Validate real API connection
		if (!validateRealApiConnection("PasskeyRegistrationService")) {
			return new RegistrationResult(false, credentialId, "Real API connection validation failed");
		}

		try {
			// Extract and verify challenge
			String challenge = challengeService.extractChallengeFromClientData(clientDataJSON);
			boolean challengeValid = challengeService.verifyChallenge(challenge, userId,
					PasskeyChallengeType.REGISTRATION);

			if (!challengeValid) {
				log.warn("Invalid challenge for passkey registration: {}", challenge);
				return new RegistrationResult(false, credentialId, "Invalid challenge");
			}

			// Check if credential already exists for this user
			List<AuthenticationMethodEntity> existingMethods = authMethodRepository.findByUserIdAndType(userId,
					AuthenticationMethodType.PASSKEY);
			boolean credentialExists = existingMethods.stream()
				.anyMatch(method -> credentialId
					.equals(method.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.CREDENTIAL_ID)));

			if (credentialExists) {
				log.warn("Credential already exists: {}", credentialId);
				return new RegistrationResult(false, credentialId, "Credential already exists");
			}

			// Parse attestation to extract public key and authenticator info
			ParsedAttestation attestation = parseAttestation(attestationObject);
			if (attestation == null || attestation.publicKey() == null) {
				log.warn("Could not parse attestation object");
				return new RegistrationResult(false, credentialId, "Could not parse attestation");
			}

			// Get authenticator info from AAGUID
			AuthenticatorRegistry.AuthenticatorInfo authInfo = AuthenticatorRegistry
				.getAuthenticator(attestation.aaguid());

			// Create and save authentication method with authenticator name as default
			AuthenticationMethodEntity authMethod = new AuthenticationMethodEntity(AuthenticationMethodType.PASSKEY,
					authInfo.name() // Default name from authenticator (e.g., "iCloud
									// Keychain")
			);

			// Store essential passkey data in metadata
			// Note: Registration time comes from BaseEntity.createdDate
			authMethod.putMetadata(AuthenticationMethodMetadata.PasskeyMetadata.CREDENTIAL_ID, credentialId);
			authMethod.putMetadata(AuthenticationMethodMetadata.PasskeyMetadata.PUBLIC_KEY_BASE64,
					java.util.Base64.getEncoder().encodeToString(attestation.publicKey()));
			authMethod.putMetadata(AuthenticationMethodMetadata.PasskeyMetadata.SIGNATURE_COUNT, 0);
			authMethod.putMetadata(AuthenticationMethodMetadata.PasskeyMetadata.VERIFIED, true);

			// Store authenticator identification
			authMethod.putMetadata(AuthenticationMethodMetadata.PasskeyMetadata.AAGUID, attestation.aaguid());
			authMethod.putMetadata(AuthenticationMethodMetadata.PasskeyMetadata.AUTHENTICATOR_NAME, authInfo.name());

			// Store backup/sync status
			authMethod.putMetadata(AuthenticationMethodMetadata.PasskeyMetadata.BACKUP_ELIGIBLE,
					attestation.backupEligible());
			authMethod.putMetadata(AuthenticationMethodMetadata.PasskeyMetadata.BACKUP_STATE,
					attestation.backupState());

			log.info("Registering passkey from {} (AAGUID: {}, synced: {})", authInfo.name(), attestation.aaguid(),
					attestation.backupState());

			// Mark passkey as active and used - passkeys are verified during registration
			authMethod.setIsActive(true);
			authMethod.markAsUsed();

			authMethodRepository.saveForUser(userId, authMethod);

			// Get user's demo mode from profile
			Boolean demoMode = userRepository.findById(userId)
				.map(user -> user.getProfile() != null ? user.getProfile().getDemoMode() : true)
				.orElse(true);

			// Generate authentication tokens using unified approach
			SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(userId, null, // userEmail
																											// -
																											// could
																											// be
																											// retrieved
																											// if
																											// needed
					List.of("passkeys"), // Authentication method used
					false, // Not partial auth - passkey provides full authentication
					deviceId, deviceId, // Use deviceId as fingerprint
					null, // IP address not available in registration
					"Passkey Registration", demoMode);

			SessionAuthBusiness.AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);

			AuthTokens tokens = new AuthTokens(authResult.accessToken(), authResult.refreshToken());

			log.info("Successfully registered passkey for user: {} with credential: {}", userId, credentialId);
			return new RegistrationResult(true, credentialId, tokens);

		}
		catch (Exception e) {
			log.error("Error completing passkey registration", e);
			return new RegistrationResult(false, credentialId, e.getMessage());
		}
	}

	/**
	 * Parsed attestation data containing public key and authenticator info
	 */
	public record ParsedAttestation(byte[] publicKey, String aaguid, boolean backupEligible, boolean backupState) {
	}

	/**
	 * Extract public key and authenticator info from attestation object
	 */
	private ParsedAttestation parseAttestation(String attestationObject) {
		try {
			// Use WebAuthn4J to parse attestation
			ObjectConverter objectConverter = new ObjectConverter();
			AuthenticatorDataConverter converter = new AuthenticatorDataConverter(objectConverter);

			// Decode base64url attestation object
			byte[] attestationBytes = java.util.Base64.getUrlDecoder().decode(attestationObject);

			// Parse CBOR attestation object
			Map<String, Object> attestationMap = objectConverter.getCborConverter()
				.readValue(attestationBytes, Map.class);

			// Extract authData
			byte[] authDataBytes = (byte[]) attestationMap.get("authData");

			// Parse authenticator data
			AuthenticatorData authData = converter.convert(authDataBytes);

			// Extract attested credential data
			AttestedCredentialData attestedCredentialData = authData.getAttestedCredentialData();
			if (attestedCredentialData == null) {
				return null;
			}

			// Extract COSE key
			COSEKey coseKey = attestedCredentialData.getCOSEKey();
			if (coseKey == null) {
				return null;
			}

			// Extract AAGUID (Authenticator Attestation GUID)
			String aaguid = null;
			if (attestedCredentialData.getAaguid() != null) {
				aaguid = attestedCredentialData.getAaguid().toString();
				log.info("Extracted AAGUID: {}", aaguid);
			}

			// Extract backup flags from authenticator data flags byte
			// BE (Backup Eligible) - bit 3 (0x08), BS (Backup State) - bit 4 (0x10)
			byte flagsByte = authData.getFlags();
			boolean backupEligible = (flagsByte & 0x08) != 0; // BE flag - bit 3
			boolean backupState = (flagsByte & 0x10) != 0; // BS flag - bit 4
			log.info("Backup flags - eligible: {}, state: {} (raw flags: {})", backupEligible, backupState,
					String.format("0x%02X", flagsByte));

			// Convert public key to bytes
			byte[] publicKeyBytes = objectConverter.getCborConverter().writeValueAsBytes(coseKey);

			return new ParsedAttestation(publicKeyBytes, aaguid, backupEligible, backupState);

		}
		catch (Exception e) {
			log.warn("Error parsing attestation", e);
			return null;
		}
	}

}
