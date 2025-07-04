package io.strategiz.service.auth.model.oauth;

/**
 * Model for Google user information
 */
public class GoogleUserInfo {
    private final String googleId;
    private final String email;
    private final String name;
    private final String pictureUrl;

    public GoogleUserInfo(String googleId, String email, String name) {
        this(googleId, email, name, null);
    }

    public GoogleUserInfo(String googleId, String email, String name, String pictureUrl) {
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
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
} 