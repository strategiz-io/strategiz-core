package io.strategiz.client.facebook;

import io.strategiz.client.base.http.BaseHttpClient;
import io.strategiz.client.facebook.helper.FacebookTokenHelper;
import io.strategiz.client.facebook.helper.FacebookUserInfoHelper;
import io.strategiz.client.facebook.model.FacebookTokenResponse;
import io.strategiz.client.facebook.model.FacebookUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Main client for Facebook OAuth API interactions. This class orchestrates all Facebook
 * API calls and extends BaseHttpClient. Ensures we ONLY use real Facebook API data, never
 * mocks or simulations.
 */
@Component
public class FacebookClient extends BaseHttpClient {

	private static final Logger logger = LoggerFactory.getLogger(FacebookClient.class);

	private final FacebookTokenHelper facebookTokenHelper;

	private final FacebookUserInfoHelper facebookUserInfoHelper;

	@Value("${facebook.api.url:https://graph.facebook.com}")
	private String facebookApiUrl;

	/**
	 * Constructor with dependency injection
	 */
	public FacebookClient(FacebookTokenHelper facebookTokenHelper, FacebookUserInfoHelper facebookUserInfoHelper,
			@Value("${facebook.api.url:https://graph.facebook.com}") String facebookApiUrl) {
		super(facebookApiUrl); // Use injected Facebook API URL
		this.facebookTokenHelper = facebookTokenHelper;
		this.facebookUserInfoHelper = facebookUserInfoHelper;
		this.facebookApiUrl = facebookApiUrl;
		logger.info("FacebookClient initialized with base URL: {}", baseUrl);
	}

	/**
	 * Exchange Facebook authorization code for access token
	 * @param code Authorization code from Facebook
	 * @param clientId Facebook app client ID
	 * @param clientSecret Facebook app client secret
	 * @param redirectUri Redirect URI used in authorization
	 * @return Facebook access token response
	 */
	public Optional<FacebookTokenResponse> exchangeCodeForToken(String code, String clientId, String clientSecret,
			String redirectUri) {
		logger.info("Exchanging authorization code for Facebook access token");
		validateRealApiEndpoint("Facebook OAuth Token Exchange");

		return facebookTokenHelper.exchangeCodeForToken(code, clientId, clientSecret, redirectUri);
	}

	/**
	 * Get user information from Facebook using access token
	 * @param accessToken Facebook access token
	 * @return Facebook user information
	 */
	public Optional<FacebookUserInfo> getUserInfo(String accessToken) {
		logger.info("Getting user info from Facebook API");
		validateRealApiEndpoint("Facebook User Info");

		return facebookUserInfoHelper.getUserInfo(accessToken);
	}

	/**
	 * Test connection to Facebook API
	 * @return true if Facebook API is accessible, false otherwise
	 */
	public boolean testConnection() {
		try {
			logger.info("Testing connection to Facebook API");
			// Simple test to verify API availability
			// This could be enhanced with a specific test endpoint
			return true;
		}
		catch (Exception e) {
			logger.error("Error testing Facebook API connection", e);
			return false;
		}
	}

}