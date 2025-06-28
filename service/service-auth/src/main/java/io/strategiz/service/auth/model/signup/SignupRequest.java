package io.strategiz.service.auth.model.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Consolidated request model for user signup that includes both profile information
 * and authentication method selection
 */
public class SignupRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Authentication method is required")
    private String authMethod; // "passkey", "totp", "sms", etc.
    
    private String photoURL;
    
    // Optional fields for specific auth methods
    private String phoneNumber; // For SMS auth
    
    // Default constructor
    public SignupRequest() {
    }
    
    // Full constructor
    public SignupRequest(String name, String email, String authMethod, String photoURL, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.authMethod = authMethod;
        this.photoURL = photoURL;
        this.phoneNumber = phoneNumber;
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
}
