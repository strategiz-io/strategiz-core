package io.strategiz.service.profile.model;

import java.time.Instant;

/**
 * Response model for Step 1 of signup: Profile Creation
 *
 * Contains user ID and temporary session token to proceed to Step 2 (authentication
 * setup). This token has limited permissions - only allows authentication method setup.
 */
public class CreateProfileResponse {

	private String userId;

	private String name;

	private String email;

	private Boolean demoMode;

	private String identityToken; // Identity token for auth setup only

	private Long expiresIn; // Token expiration (typically 1 hour)

	private String tokenType;

	private Instant createdAt;

	// Default constructor
	public CreateProfileResponse() {
		this.tokenType = "bearer";
		this.createdAt = Instant.now();
		this.expiresIn = 3600L; // 1 hour default
	}

	// Constructor
	public CreateProfileResponse(String userId, String name, String email, Boolean demoMode, String identityToken,
			Long expiresIn) {
		this.userId = userId;
		this.name = name;
		this.email = email;
		this.demoMode = demoMode;
		this.identityToken = identityToken;
		this.expiresIn = expiresIn;
		this.tokenType = "bearer";
		this.createdAt = Instant.now();
	}

	// Getters and setters
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getDemoMode() {
		return demoMode;
	}

	public void setDemoMode(Boolean demoMode) {
		this.demoMode = demoMode;
	}

	public String getIdentityToken() {
		return identityToken;
	}

	public void setIdentityToken(String identityToken) {
		this.identityToken = identityToken;
	}

	public Long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Long expiresIn) {
		this.expiresIn = expiresIn;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		return "CreateProfileResponse{" + "userId='" + userId + '\'' + ", name='" + name + '\'' + ", email='" + email
				+ '\'' + ", demoMode=" + demoMode + ", tokenType='" + tokenType + '\'' + ", expiresIn=" + expiresIn
				+ ", createdAt=" + createdAt + '}';
	}

}