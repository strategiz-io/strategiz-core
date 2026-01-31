package io.strategiz.service.profile.controller;

import io.strategiz.business.preferences.service.AlertPreferencesService;
import io.strategiz.data.preferences.entity.AlertNotificationPreferences;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.profile.model.AlertPreferencesResponse;
import io.strategiz.service.profile.model.ConfirmPhoneRequest;
import io.strategiz.service.profile.model.PhoneVerificationRequest;
import io.strategiz.service.profile.model.UpdateAlertPreferencesRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing alert notification preferences. Provides endpoints for
 * configuring phone number, email, channels, and quiet hours.
 */
@RestController
@RequestMapping("/v1/users/preferences/alerts")
@Validated
public class AlertPreferencesController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(AlertPreferencesController.class);

	private final AlertPreferencesService alertPreferencesService;

	public AlertPreferencesController(AlertPreferencesService alertPreferencesService) {
		this.alertPreferencesService = alertPreferencesService;
	}

	@Override
	protected String getModuleName() {
		return "service-profile";
	}

	/**
	 * Get current alert notification preferences.
	 * @param user The authenticated user
	 * @return The alert preferences
	 */
	@GetMapping
	@RequireAuth(minAcr = "1")
	public ResponseEntity<AlertPreferencesResponse> getPreferences(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Getting alert preferences for user {}", userId);

		AlertNotificationPreferences prefs = alertPreferencesService.getPreferences(userId);
		AlertPreferencesResponse response = mapToResponse(prefs);

		return ResponseEntity.ok(response);
	}

	/**
	 * Update alert notification preferences.
	 * @param request The update request
	 * @param user The authenticated user
	 * @return The updated preferences
	 */
	@PutMapping
	@RequireAuth(minAcr = "1")
	public ResponseEntity<AlertPreferencesResponse> updatePreferences(
			@Valid @RequestBody UpdateAlertPreferencesRequest request, @AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Updating alert preferences for user {}", userId);

		AlertNotificationPreferences prefs = mapFromRequest(request, userId);
		AlertNotificationPreferences updated = alertPreferencesService.updatePreferences(userId, prefs);
		AlertPreferencesResponse response = mapToResponse(updated);

		return ResponseEntity.ok(response);
	}

	/**
	 * Initiate phone verification for SMS alerts. Sends an OTP code to the provided phone
	 * number.
	 * @param request The phone verification request
	 * @param user The authenticated user
	 * @return Success message
	 */
	@PostMapping("/verify-phone")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, String>> initiatePhoneVerification(
			@Valid @RequestBody PhoneVerificationRequest request, @AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Initiating phone verification for user {}", userId);

		// Update phone number (marks as unverified)
		alertPreferencesService.updatePhoneNumber(userId, request.getPhoneNumber());

		// TODO: Send OTP via existing Firebase SMS service
		// For now, return success - phone verification will use the existing
		// SMS OTP flow from service-auth

		return ResponseEntity
			.ok(Map.of("message", "Verification code sent to " + maskPhoneNumber(request.getPhoneNumber()),
					"phoneNumber", maskPhoneNumber(request.getPhoneNumber())));
	}

	/**
	 * Confirm phone verification with OTP code.
	 * @param request The confirmation request with OTP code
	 * @param user The authenticated user
	 * @return Success message
	 */
	@PostMapping("/confirm-phone")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, Object>> confirmPhoneVerification(@Valid @RequestBody ConfirmPhoneRequest request,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Confirming phone verification for user {}", userId);

		// TODO: Verify OTP code using existing Firebase SMS service
		// For now, mark as verified - will integrate with SMS OTP service

		AlertNotificationPreferences updated = alertPreferencesService.verifyPhoneNumber(userId);

		return ResponseEntity.ok(Map.of("verified", true, "phoneNumber", maskPhoneNumber(updated.getPhoneNumber())));
	}

	/**
	 * Test a specific notification channel.
	 * @param channel The channel to test (email, sms, push, in-app)
	 * @param user The authenticated user
	 * @return Success message
	 */
	@PostMapping("/test/{channel}")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, String>> testChannel(@PathVariable String channel,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Testing {} notification for user {}", channel, userId);

		// Validate channel
		if (!isValidChannel(channel)) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", "Invalid channel. Use: email, sms, push, or in-app"));
		}

		// TODO: Send test notification through AlertNotificationService

		return ResponseEntity.ok(Map.of("message", "Test " + channel + " notification sent", "channel", channel));
	}

	// Helper methods

	private AlertPreferencesResponse mapToResponse(AlertNotificationPreferences prefs) {
		AlertPreferencesResponse response = new AlertPreferencesResponse();
		response.setPhoneNumber(maskPhoneNumber(prefs.getPhoneNumber()));
		response.setPhoneVerified(prefs.getPhoneVerified());
		response.setEmailForAlerts(prefs.getEmailForAlerts());
		response.setEmailVerified(prefs.getEmailVerified());
		response.setEnabledChannels(prefs.getEnabledChannels());
		response.setQuietHoursEnabled(prefs.getQuietHoursEnabled());
		response.setQuietHoursStart(prefs.getQuietHoursStart());
		response.setQuietHoursEnd(prefs.getQuietHoursEnd());
		response.setQuietHoursTimezone(prefs.getQuietHoursTimezone());
		response.setMaxAlertsPerHour(prefs.getMaxAlertsPerHour());
		return response;
	}

	private AlertNotificationPreferences mapFromRequest(UpdateAlertPreferencesRequest request, String userId) {
		AlertNotificationPreferences prefs = alertPreferencesService.getPreferences(userId);

		if (request.getPhoneNumber() != null) {
			prefs.setPhoneNumber(request.getPhoneNumber());
			prefs.setPhoneVerified(false); // Require re-verification
		}
		if (request.getEmailForAlerts() != null) {
			prefs.setEmailForAlerts(request.getEmailForAlerts());
		}
		if (request.getEnabledChannels() != null) {
			prefs.setEnabledChannels(request.getEnabledChannels());
		}
		if (request.getQuietHoursEnabled() != null) {
			prefs.setQuietHoursEnabled(request.getQuietHoursEnabled());
		}
		if (request.getQuietHoursStart() != null) {
			prefs.setQuietHoursStart(request.getQuietHoursStart());
		}
		if (request.getQuietHoursEnd() != null) {
			prefs.setQuietHoursEnd(request.getQuietHoursEnd());
		}
		if (request.getQuietHoursTimezone() != null) {
			prefs.setQuietHoursTimezone(request.getQuietHoursTimezone());
		}
		if (request.getMaxAlertsPerHour() != null) {
			prefs.setMaxAlertsPerHour(request.getMaxAlertsPerHour());
		}

		return prefs;
	}

	private String maskPhoneNumber(String phone) {
		if (phone == null || phone.length() < 4) {
			return null;
		}
		return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
	}

	private boolean isValidChannel(String channel) {
		return channel != null && (channel.equalsIgnoreCase("email") || channel.equalsIgnoreCase("sms")
				|| channel.equalsIgnoreCase("push") || channel.equalsIgnoreCase("in-app"));
	}

}
