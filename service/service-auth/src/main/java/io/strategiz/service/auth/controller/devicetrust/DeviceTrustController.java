package io.strategiz.service.auth.controller.devicetrust;

import io.strategiz.business.tokenauth.DeviceTrustBusiness;
import io.strategiz.business.tokenauth.DeviceTrustBusiness.ChallengeResult;
import io.strategiz.business.tokenauth.DeviceTrustBusiness.DeviceTrustResult;
import io.strategiz.business.tokenauth.DeviceTrustBusiness.TrustLevel;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.authorization.context.SecurityContextHolder;
import io.strategiz.service.auth.exception.ServiceAuthErrorDetails;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for device trust authentication flow.
 *
 * <p>
 * Enables returning users on trusted devices to sign in with a single click, without
 * needing to re-enter credentials or complete MFA challenges.
 * </p>
 *
 * <p>
 * Flow:
 * </p>
 * <ol>
 * <li>POST /verify — check if current device is trusted, returns welcome-back info</li>
 * <li>POST /challenge — generate a crypto challenge for the device to sign</li>
 * <li>POST /authenticate — verify signed challenge and issue session tokens</li>
 * </ol>
 */
@RestController
@RequestMapping("/v1/auth/device-trust")
@Tag(name = "Device Trust", description = "Device trust silent authentication")
public class DeviceTrustController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(DeviceTrustController.class);

	private final DeviceTrustBusiness deviceTrustBusiness;

	private final CookieUtil cookieUtil;

	public DeviceTrustController(DeviceTrustBusiness deviceTrustBusiness, CookieUtil cookieUtil) {
		this.deviceTrustBusiness = deviceTrustBusiness;
		this.cookieUtil = cookieUtil;
	}

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	/**
	 * Verify if the current device is trusted and eligible for one-click sign-in.
	 * @param body must contain "fingerprint" (visitor ID from FingerprintJS)
	 * @param request HTTP request for IP extraction
	 * @return trust status with user info if trusted
	 */
	@PostMapping("/verify")
	@Operation(summary = "Verify device trust", description = "Check if device is trusted for one-click sign-in")
	public ResponseEntity<Map<String, Object>> verifyDeviceTrust(@RequestBody Map<String, String> body,
			HttpServletRequest request) {

		String fingerprint = body.get("fingerprint");
		if (fingerprint == null || fingerprint.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing fingerprint"));
		}

		String clientIp = getClientIp(request);
		log.info("Device trust verification for fingerprint from IP: {}", clientIp);

		DeviceTrustResult result = deviceTrustBusiness.verifyDeviceTrust(fingerprint, clientIp);

		if (!result.trusted()) {
			return ResponseEntity
				.ok(Map.of("welcomeBack", false, "reason", result.reason() != null ? result.reason() : "unknown"));
		}

		return ResponseEntity
			.ok(Map.of("welcomeBack", true, "deviceId", result.deviceId(), "userId", result.userId(), "userName",
					result.userName() != null ? result.userName() : "", "trustLevel", result.trustLevel().name()));
	}

	/**
	 * Generate a cryptographic challenge for device trust authentication. The device must
	 * sign this challenge with its stored private key.
	 * @param body must contain "deviceId"
	 * @return challenge nonce and challenge ID
	 */
	@PostMapping("/challenge")
	@Operation(summary = "Generate device trust challenge",
			description = "Generate crypto challenge for device to sign")
	public ResponseEntity<Map<String, Object>> generateChallenge(@RequestBody Map<String, String> body) {

		String deviceId = body.get("deviceId");
		if (deviceId == null || deviceId.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing deviceId"));
		}

		ChallengeResult challenge = deviceTrustBusiness.generateChallenge(deviceId);

		return ResponseEntity
			.ok(Map.of("success", true, "challengeId", challenge.challengeId(), "challenge", challenge.challenge()));
	}

	/**
	 * Authenticate using device trust by verifying the signed crypto challenge. On
	 * success, issues session tokens as HTTP-only cookies.
	 * @param body must contain "deviceId", "challengeId", "signedChallenge"
	 * @param request HTTP request for IP and user agent
	 * @param response HTTP response for setting cookies
	 * @return authentication result with user info
	 */
	@PostMapping("/authenticate")
	@Operation(summary = "Authenticate with device trust",
			description = "Verify signed challenge and issue session tokens")
	public ResponseEntity<Map<String, Object>> authenticateWithDeviceTrust(@RequestBody Map<String, String> body,
			HttpServletRequest request, HttpServletResponse response) {

		String deviceId = body.get("deviceId");
		String challengeId = body.get("challengeId");
		String signedChallenge = body.get("signedChallenge");

		if (deviceId == null || challengeId == null || signedChallenge == null) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "error", "Missing deviceId, challengeId, or signedChallenge"));
		}

		String clientIp = getClientIp(request);
		String userAgent = request.getHeader("User-Agent");

		log.info("Device trust authentication attempt: deviceId={} from IP={}", deviceId, clientIp);

		Optional<SessionAuthBusiness.AuthResult> authResult = deviceTrustBusiness.authenticateWithDeviceTrust(deviceId,
				challengeId, signedChallenge, clientIp, userAgent);

		if (authResult.isEmpty()) {
			log.warn("Device trust authentication failed for device {}", deviceId);
			return ResponseEntity.status(401)
				.body(Map.of("success", false, "error", "Device trust authentication failed"));
		}

		SessionAuthBusiness.AuthResult result = authResult.get();

		// Set session cookies
		cookieUtil.setAccessTokenCookie(response, result.accessToken());
		cookieUtil.setRefreshTokenCookie(response, result.refreshToken());

		log.info("Device trust authentication successful for device {}", deviceId);

		return ResponseEntity
			.ok(Map.of("success", true, "accessToken", result.accessToken(), "refreshToken", result.refreshToken()));
	}

	/**
	 * List all trusted devices for the authenticated user.
	 * @return list of trusted devices with metadata
	 */
	@GetMapping("/devices")
	@Operation(summary = "List trusted devices", description = "Get all trusted devices for the current user")
	public ResponseEntity<Map<String, Object>> listTrustedDevices() {
		Optional<AuthenticatedUser> userOpt = SecurityContextHolder.getAuthenticatedUser();
		if (userOpt.isEmpty()) {
			return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not authenticated"));
		}

		String userId = userOpt.get().getUserId();
		List<DeviceIdentity> devices = deviceTrustBusiness.getTrustedDevices(userId);

		List<Map<String, Object>> deviceList = devices.stream().map(d -> {
			Map<String, Object> info = new HashMap<>();
			info.put("deviceId", d.getDeviceId());
			info.put("deviceName", d.getDeviceName() != null ? d.getDeviceName() : "Unknown Device");
			info.put("browserName", d.getBrowserName());
			info.put("osName", d.getOsName());
			info.put("lastSeen", d.getLastSeen() != null ? d.getLastSeen().toString() : null);
			info.put("trustLevel", d.getTrustLevel());
			info.put("trustExpiresAt", d.getTrustExpiresAt() != null ? d.getTrustExpiresAt().toString() : null);
			info.put("trustScore", d.getTrustScore());
			return info;
		}).collect(Collectors.toList());

		return ResponseEntity.ok(Map.of("success", true, "devices", deviceList));
	}

	/**
	 * Revoke trust for a specific device.
	 * @param deviceId the device ID to revoke
	 * @return success status
	 */
	@DeleteMapping("/devices/{deviceId}")
	@Operation(summary = "Revoke device trust", description = "Revoke trust for a specific device")
	public ResponseEntity<Map<String, Object>> revokeDeviceTrust(@PathVariable String deviceId) {
		Optional<AuthenticatedUser> userOpt = SecurityContextHolder.getAuthenticatedUser();
		if (userOpt.isEmpty()) {
			return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not authenticated"));
		}

		String userId = userOpt.get().getUserId();
		boolean revoked = deviceTrustBusiness.revokeTrust(userId, deviceId);

		if (!revoked) {
			return ResponseEntity.status(404)
				.body(Map.of("success", false, "error", "Device not found or not trusted"));
		}

		log.info("Device trust revoked: userId={}, deviceId={}", userId, deviceId);
		return ResponseEntity.ok(Map.of("success", true, "message", "Device trust revoked"));
	}

	private String getClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

}
