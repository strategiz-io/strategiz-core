package io.strategiz.service.auth.model.oauth;

/**
 * Model for Facebook user information
 */
public class FacebookUserInfo {

	private final String facebookId;

	private final String email;

	private final String name;

	private final String pictureUrl;

	public FacebookUserInfo(String facebookId, String email, String name) {
		this(facebookId, email, name, null);
	}

	public FacebookUserInfo(String facebookId, String email, String name, String pictureUrl) {
		this.facebookId = facebookId;
		this.email = email;
		this.name = name;
		this.pictureUrl = pictureUrl;
	}

	public String getFacebookId() {
		return facebookId;
	}

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}

	public String getPictureUrl() {
		return pictureUrl;
	}

}