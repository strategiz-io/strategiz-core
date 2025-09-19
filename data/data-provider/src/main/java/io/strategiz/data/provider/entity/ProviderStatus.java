package io.strategiz.data.provider.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum representing the status of a provider integration
 */
public enum ProviderStatus {
    CONNECTED("connected"),       // Provider is successfully connected and active
    DISCONNECTED("disconnected"); // Provider is not connected or was disconnected
    
    private final String value;
    
    ProviderStatus(String value) {
        this.value = value;
    }
    
    @JsonValue  // This tells Jackson to use this method for serialization
    public String getValue() {
        return value;
    }
    
    @JsonCreator  // This tells Jackson to use this method for deserialization
    public static ProviderStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (ProviderStatus status : values()) {
            // Handle both uppercase (legacy) and lowercase
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Unknown provider status: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}