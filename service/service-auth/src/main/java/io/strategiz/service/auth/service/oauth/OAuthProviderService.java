package io.strategiz.service.auth.service.oauth;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.config.AuthOAuthConfig;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.base.BaseService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service that orchestrates OAuth operations across different providers. This service
 * acts as a factory/router for specific OAuth provider services.
 */
@Service
public class OAuthProviderService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	private final Map<String, OAuthProviderHandler> providerHandlers;

	private final AuthOAuthConfig oauthConfig;

	public OAuthProviderService(GoogleOAuthService googleOAuthService, FacebookOAuthService facebookOAuthService,
			AuthOAuthConfig oauthConfig) {
		this.oauthConfig = oauthConfig;

		// Initialize provider handlers map
		this.providerHandlers = new HashMap<>();

		// Register Google handler
		this.providerHandlers.put("google", new OAuthProviderHandler() {
			@Override
			public Map<String, String> getAuthorizationUrl(boolean isSignup) {
				return googleOAuthService.getAuthorizationUrl(isSignup);
			}

			@Override
			public Map<String, Object> handleOAuthCallback(String code, String state, String deviceId) {
				return googleOAuthService.handleOAuthCallback(code, state, deviceId);
			}
		});

		// Register Facebook handler
		this.providerHandlers.put("facebook", new OAuthProviderHandler() {
			@Override
			public Map<String, String> getAuthorizationUrl(boolean isSignup) {
				return facebookOAuthService.getAuthorizationUrl(isSignup);
			}

			@Override
			public Map<String, Object> handleOAuthCallback(String code, String state, String deviceId) {
				return facebookOAuthService.handleOAuthCallback(code, state, deviceId);
			}
		});

		log.info("OAuthProviderService initialized with providers: {}", providerHandlers.keySet());
	}

	/**
	 * Get authorization URL for the specified provider
	 * @param provider Provider name (google, facebook, coinbase, etc.)
	 * @param isSignup Whether this is for signup flow
	 * @return Authorization URL and state
	 */
	public Map<String, String> getAuthorizationUrl(String provider, boolean isSignup) {
		OAuthProviderHandler handler = getProviderHandler(provider);
		return handler.getAuthorizationUrl(isSignup);
	}

	/**
	 * Handle OAuth callback from the specified provider
	 * @param provider Provider name
	 * @param code Authorization code
	 * @param state State parameter
	 * @param deviceId Optional device ID
	 * @return Response containing user data or error
	 */
	public Map<String, Object> handleOAuthCallback(String provider, String code, String state, String deviceId) {
		OAuthProviderHandler handler = getProviderHandler(provider);
		return handler.handleOAuthCallback(code, state, deviceId);
	}

	/**
	 * Get frontend URL for redirects
	 * @return Frontend URL
	 */
	public String getFrontendUrl() {
		return oauthConfig.getFrontendUrl();
	}

	/**
	 * Get the handler for the specified provider
	 * @param provider Provider name
	 * @return Provider handler
	 * @throws StrategizException if provider is not supported
	 */
	private OAuthProviderHandler getProviderHandler(String provider) {
		if (provider == null || provider.trim().isEmpty()) {
			throw new StrategizException(AuthErrors.INVALID_TOKEN, "OAuth provider name is required");
		}

		String normalizedProvider = provider.toLowerCase();
		OAuthProviderHandler handler = providerHandlers.get(normalizedProvider);

		if (handler == null) {
			log.error("Unsupported OAuth provider: {}", provider);
			throw new StrategizException(AuthErrors.INVALID_TOKEN, "OAuth provider not supported: " + provider);
		}

		return handler;
	}

	/**
	 * Internal interface for OAuth provider handlers
	 */
	private interface OAuthProviderHandler {

		Map<String, String> getAuthorizationUrl(boolean isSignup);

		Map<String, Object> handleOAuthCallback(String code, String state, String deviceId);

	}

}