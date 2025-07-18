package io.strategiz.data.auth.entity.passkey;

import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Passkey authentication method entity for WebAuthn/FIDO2 authentication
 * Stored in users/{userId}/authentication_methods subcollection
 */
public class PasskeyAuthenticationMethodEntity extends AuthenticationMethodEntity {

    @PropertyName("credentials")
    @JsonProperty("credentials")
    private List<Map<String, Object>> credentials; // List of passkey credentials

    @PropertyName("relyingParty")
    @JsonProperty("relyingParty")
    private String relyingParty = "strategiz.io";

    // Constructors
    public PasskeyAuthenticationMethodEntity() {
        super("PASSKEY", "Passkey");
    }

    public PasskeyAuthenticationMethodEntity(String name, List<Map<String, Object>> credentials) {
        super("PASSKEY", name);
        this.credentials = credentials;
    }

    // Getters and Setters
    public List<Map<String, Object>> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<Map<String, Object>> credentials) {
        this.credentials = credentials;
    }

    public String getRelyingParty() {
        return relyingParty;
    }

    public void setRelyingParty(String relyingParty) {
        this.relyingParty = relyingParty;
    }

    // Convenience methods
    public boolean hasCredentials() {
        return credentials != null && !credentials.isEmpty();
    }

    public int getCredentialCount() {
        return credentials != null ? credentials.size() : 0;
    }

    // Abstract method implementations
    @Override
    public String getAuthenticationMethodType() {
        return "PASSKEY";
    }

    @Override
    public boolean isConfigured() {
        return hasCredentials();
    }

    @Override
    public Map<String, Object> getTypeSpecificData() {
        Map<String, Object> data = new HashMap<>();
        data.put("relyingParty", relyingParty);
        data.put("credentialCount", getCredentialCount());
        data.put("hasCredentials", hasCredentials());
        return data;
    }
}