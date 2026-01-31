package io.strategiz.service.waitlist.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.waitlist.model.request.WaitlistJoinRequest;
import io.strategiz.service.waitlist.model.response.WaitlistJoinResponse;
import io.strategiz.service.waitlist.service.WaitlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public controller for waitlist signups. No authentication required - these are public
 * endpoints.
 *
 * Endpoints: - POST /v1/waitlist/join - Add email to waitlist - GET /v1/waitlist/count -
 * Get total waitlist count
 */
@RestController
@RequestMapping("/v1/waitlist")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io",
		"https://www.strategiz.io" }, allowedHeaders = "*")
public class WaitlistController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(WaitlistController.class);

	@Override
	protected String getModuleName() {
		return "service-waitlist";
	}

	@Autowired
	private WaitlistService waitlistService;

	/**
	 * Join waitlist - add email to pre-launch signup list
	 */
	@PostMapping("/join")
	public ResponseEntity<WaitlistJoinResponse> join(@Valid @RequestBody WaitlistJoinRequest request,
			HttpServletRequest httpRequest) {
		try {
			String ipAddress = getClientIpAddress(httpRequest);
			String userAgent = httpRequest.getHeader("User-Agent");

			log.info("Waitlist join request from IP: {}, email: {}", ipAddress, maskEmail(request.getEmail()));

			WaitlistJoinResponse response = waitlistService.joinWaitlist(request.getEmail(), ipAddress, userAgent,
					request.getReferralSource());

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Error processing waitlist join request", e);
			return ResponseEntity.ok(WaitlistJoinResponse.error("An error occurred. Please try again later."));
		}
	}

	/**
	 * Get waitlist count - public endpoint to show signup count
	 */
	@GetMapping("/count")
	public ResponseEntity<Map<String, Object>> getCount() {
		try {
			long count = waitlistService.getWaitlistCount();
			return ResponseEntity.ok(Map.of("count", count));
		}
		catch (Exception e) {
			log.error("Error getting waitlist count", e);
			throw handleException(e, "getWaitlistCount");
		}
	}

	/**
	 * Extract client IP address from request, handling proxies
	 */
	private String getClientIpAddress(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	/**
	 * Mask email for logging
	 */
	private String maskEmail(String email) {
		if (email == null)
			return null;
		int atIndex = email.indexOf("@");
		if (atIndex > 2) {
			return email.substring(0, 2) + "***" + email.substring(atIndex);
		}
		return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
	}

}
