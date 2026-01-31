package io.strategiz.service.auth.controller.session;

import io.strategiz.service.auth.service.session.SignOutService;
import io.strategiz.service.auth.model.session.SignOutRequest;
import io.strategiz.service.auth.model.session.SignOutResponse;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Controller for user sign-out operations. Handles user logout and session cleanup. Uses
 * clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1/auth/signout")
public class SignOutController extends BaseController {

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	private static final Logger log = LoggerFactory.getLogger(SignOutController.class);

	private final SignOutService signOutService;

	private final CookieUtil cookieUtil;

	public SignOutController(SignOutService signOutService, CookieUtil cookieUtil) {
		this.signOutService = signOutService;
		this.cookieUtil = cookieUtil;
	}

	/**
	 * Sign out a user and clean up their session
	 * @param signOutRequest Sign out request containing user session information
	 * @param httpRequest HTTP request for session context
	 * @param httpResponse HTTP response to clear cookies
	 * @return Clean sign out response - no wrapper, let GlobalExceptionHandler handle
	 * errors
	 */
	@PostMapping
	public ResponseEntity<SignOutResponse> signOut(@Valid @RequestBody SignOutRequest signOutRequest,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		log.info("Processing sign out for user: {}", signOutRequest.userId());

		// Sign out user and clear session/cookies - let exceptions bubble up
		SignOutResponse signOutResponse = signOutService.signOut(signOutRequest.userId(), signOutRequest.sessionId(),
				signOutRequest.deviceId(), signOutRequest.revokeAllSessions(), httpRequest, httpResponse);

		// Additional cookie clearing as backup (the service should handle this, but
		// double-check)
		if (signOutResponse.success()) {
			log.info("Additional cookie clearing for user: {}", signOutRequest.userId());
			cookieUtil.clearAuthCookies(httpResponse);
		}

		// Return clean response using BaseController method
		return createCleanResponse(signOutResponse);
	}

}
