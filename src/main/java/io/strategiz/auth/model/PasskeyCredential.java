package io.strategiz.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Model representing a passkey credential
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyCredential {
    private String id;
    private String userId;
    private String credentialId;
    private String publicKey;
    private String attestationObject;
    private String clientDataJSON;
    private long createdAt;
    private long lastUsedAt;
    private String userAgent;
    private String deviceName;

    /**
     * Updates the last used timestamp to the current time
     */
    public void updateLastUsedTime() {
        this.lastUsedAt = Instant.now().getEpochSecond();
    }
}
