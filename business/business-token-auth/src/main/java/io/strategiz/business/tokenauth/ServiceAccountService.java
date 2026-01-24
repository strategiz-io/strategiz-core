package io.strategiz.business.tokenauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Service for handling service account authentication (OAuth 2.0 Client Credentials flow).
 *
 * Service accounts are machine-to-machine authentication credentials that allow
 * automated systems to interact with the Strategiz API.
 */
@Service
public class ServiceAccountService {

	private static final Logger log = LoggerFactory.getLogger(ServiceAccountService.class);

	private final PasetoTokenProvider tokenProvider;

	public ServiceAccountService(PasetoTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	/**
	 * Generate an access token for a service account.
	 *
	 * @param clientId The service account client ID
	 * @param clientSecret The service account client secret
	 * @param requestedScopes Optional list of requested scopes
	 * @param requestedValidity Optional token validity duration
	 * @param clientIp The client IP address for audit logging
	 * @return ServiceAccountTokenResult containing the access token and metadata
	 * @throws ServiceAccountAuthenticationException if authentication fails
	 */
	public ServiceAccountTokenResult generateToken(String clientId, String clientSecret, List<String> requestedScopes,
			Duration requestedValidity, String clientIp) throws ServiceAccountAuthenticationException {

		log.debug("Service account token generation request: clientId={}, ip={}", clientId, clientIp);

		// TODO: Implement actual service account validation against database
		// For now, reject all requests until service accounts are properly configured
		throw new ServiceAccountAuthenticationException("Service accounts are not yet configured. "
				+ "Please contact support to set up a service account.");
	}

	/**
	 * Validate a service account's credentials.
	 *
	 * @param clientId The service account client ID
	 * @param clientSecret The service account client secret
	 * @return true if credentials are valid
	 */
	public boolean validateCredentials(String clientId, String clientSecret) {
		// TODO: Implement credential validation against database
		return false;
	}

	/**
	 * Result of a successful service account token generation.
	 */
	public record ServiceAccountTokenResult(String accessToken, String tokenType, long expiresIn,
			List<String> scopes) {

		public static ServiceAccountTokenResult bearer(String accessToken, long expiresInSeconds, List<String> scopes) {
			return new ServiceAccountTokenResult(accessToken, "Bearer", expiresInSeconds, scopes);
		}

	}

	/**
	 * Exception thrown when service account authentication fails.
	 */
	public static class ServiceAccountAuthenticationException extends Exception {

		public ServiceAccountAuthenticationException(String message) {
			super(message);
		}

		public ServiceAccountAuthenticationException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
