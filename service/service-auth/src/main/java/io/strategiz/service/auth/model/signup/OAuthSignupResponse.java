package io.strategiz.service.auth.model.signup;

/**
 * Response model for OAuth-based user signup Contains user information and authentication
 * tokens after successful OAuth signup (Google, Facebook, etc.)
 */
public class OAuthSignupResponse {

	private boolean success;

	private String message;

	private String userId;

	private String email;

	private String name;

	private String accessToken;

	private String refreshToken;

	private String photoURL;

	/**
	 * Default constructor
	 */
	public OAuthSignupResponse() {
	}

	/**
	 * Constructor for successful signup
	 */
	public OAuthSignupResponse(boolean success, String message, String userId, String email, String name,
			String accessToken, String refreshToken, String photoURL) {
		this.success = success;
		this.message = message;
		this.userId = userId;
		this.email = email;
		this.name = name;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.photoURL = photoURL;
	}

	/**
	 * Constructor for failed signup
	 */
	public OAuthSignupResponse(boolean success, String message) {
		this.success = success;
		this.message = message;
	}

	// Getters and setters
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getPhotoURL() {
		return photoURL;
	}

	public void setPhotoURL(String photoURL) {
		this.photoURL = photoURL;
	}

}