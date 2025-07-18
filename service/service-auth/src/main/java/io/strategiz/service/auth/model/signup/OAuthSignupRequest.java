package io.strategiz.service.auth.model.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request model for OAuth-based user signup
 * Contains profile information received from OAuth providers (Google, Facebook, etc.)
 */
public class OAuthSignupRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Authentication method is required")
    private String authMethod;

    private String photoURL;
    private String phoneNumber;
    private Map<String, String> authData;

    /**
     * Default constructor
     */
    public OAuthSignupRequest() {
    }

    /**
     * Constructor for OAuth signup
     */
    public OAuthSignupRequest(String name, String email, String authMethod, String photoURL, String phoneNumber, Map<String, String> authData) {
        this.name = name;
        this.email = email;
        this.authMethod = authMethod;
        this.photoURL = photoURL;
        this.phoneNumber = phoneNumber;
        this.authData = authData;
    }

    // Getters and setters
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

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Map<String, String> getAuthData() {
        return authData;
    }

    public void setAuthData(Map<String, String> authData) {
        this.authData = authData;
    }
}