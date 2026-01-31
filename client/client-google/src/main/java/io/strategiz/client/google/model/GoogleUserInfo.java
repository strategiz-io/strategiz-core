package io.strategiz.client.google.model;

import java.io.Serializable;

/**
 * Model for Google user information
 */
public class GoogleUserInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String googleId;

	private final String email;

	private final String name;

	private final String pictureUrl;

	private final String givenName;

	private final String familyName;

	public GoogleUserInfo(String googleId, String email, String name) {
		this(googleId, email, name, null, null, null);
	}

	public GoogleUserInfo(String googleId, String email, String name, String pictureUrl) {
		this(googleId, email, name, pictureUrl, null, null);
	}

	public GoogleUserInfo(String googleId, String email, String name, String pictureUrl, String givenName,
			String familyName) {
		this.googleId = googleId;
		this.email = email;
		this.name = name;
		this.pictureUrl = pictureUrl;
		this.givenName = givenName;
		this.familyName = familyName;
	}

	public String getGoogleId() {
		return googleId;
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

	public String getGivenName() {
		return givenName;
	}

	public String getFamilyName() {
		return familyName;
	}

	@Override
	public String toString() {
		return "GoogleUserInfo{" + "googleId='" + googleId + '\'' + ", email='" + email + '\'' + ", name='" + name
				+ '\'' + ", pictureUrl='" + pictureUrl + '\'' + ", givenName='" + givenName + '\'' + ", familyName='"
				+ familyName + '\'' + '}';
	}

}