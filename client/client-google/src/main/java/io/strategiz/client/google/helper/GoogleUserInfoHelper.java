package io.strategiz.client.google.helper;

import io.strategiz.client.google.model.GoogleUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Helper class for retrieving user information from Google Following Single
 * Responsibility Principle - only handles user info retrieval
 */
@Component
public class GoogleUserInfoHelper {

	private static final Logger logger = LoggerFactory.getLogger(GoogleUserInfoHelper.class);

	private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

	private final RestTemplate restTemplate;

	public GoogleUserInfoHelper(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * Retrieve user information from Google using access token
	 * @param accessToken Google OAuth access token
	 * @return Google user information
	 */
	public Optional<GoogleUserInfo> getUserInfo(String accessToken) {
		try {
			HttpEntity<Void> request = buildUserInfoRequest(accessToken);
			ResponseEntity<Map<String, Object>> response = makeUserInfoRequest(request);

			if (isValidUserData(response)) {
				return extractUserInfo(response);
			}

			logger.error("Failed to get user data from Google");
			return Optional.empty();

		}
		catch (Exception e) {
			logger.error("Error getting user info from Google", e);
			return Optional.empty();
		}
	}

	private HttpEntity<Void> buildUserInfoRequest(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.setBearerAuth(accessToken);

		return new HttpEntity<>(headers);
	}

	private ResponseEntity<Map<String, Object>> makeUserInfoRequest(HttpEntity<Void> request) {
		return restTemplate.exchange(USER_INFO_URL, HttpMethod.GET, request,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
	}

	private boolean isValidUserData(ResponseEntity<Map<String, Object>> response) {
		Map<String, Object> userData = response.getBody();
		return response.getStatusCode().is2xxSuccessful() && userData != null && userData.containsKey("sub"); // Google
																												// uses
																												// "sub"
																												// as
																												// the
																												// user
																												// ID
	}

	private Optional<GoogleUserInfo> extractUserInfo(ResponseEntity<Map<String, Object>> response) {
		Map<String, Object> userData = response.getBody();
		String googleId = (String) userData.get("sub");
		String email = (String) userData.get("email");
		String name = (String) userData.get("name");
		String pictureUrl = (String) userData.get("picture");
		String givenName = (String) userData.get("given_name");
		String familyName = (String) userData.get("family_name");

		return Optional.of(new GoogleUserInfo(googleId, email, name, pictureUrl, givenName, familyName));
	}

}