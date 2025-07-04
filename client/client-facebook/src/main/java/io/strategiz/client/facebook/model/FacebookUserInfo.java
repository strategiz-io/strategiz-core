package io.strategiz.client.facebook.model;

import java.io.Serializable;

/**
 * Model for Facebook user information
 */
public class FacebookUserInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
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

    @Override
    public String toString() {
        return "FacebookUserInfo{" +
               "facebookId='" + facebookId + '\'' +
               ", email='" + email + '\'' +
               ", name='" + name + '\'' +
               ", pictureUrl='" + pictureUrl + '\'' +
               '}';
    }
} 