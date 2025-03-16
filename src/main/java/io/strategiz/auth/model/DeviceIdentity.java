package io.strategiz.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Model representing a device identity for web crypto authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceIdentity {
    private String id;
    private String userId;
    private String deviceId;
    private String publicKey;
    private String name;
    private Map<String, Object> deviceInfo;
    private long createdAt;
    private long lastUsedAt;
    private boolean trusted;

    /**
     * Updates the last used timestamp to the current time
     */
    public void updateLastUsedTime() {
        this.lastUsedAt = Instant.now().getEpochSecond();
    }
}
