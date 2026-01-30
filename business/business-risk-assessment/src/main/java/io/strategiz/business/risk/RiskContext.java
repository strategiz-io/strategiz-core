package io.strategiz.business.risk;

import io.strategiz.data.device.model.DeviceIdentity;

import java.time.Instant;

/**
 * Context object passed to each {@link RiskSignalProvider} for evaluation.
 *
 * @param userId the user being authenticated
 * @param deviceId the device being used
 * @param ipAddress the client IP address
 * @param ipLocation the geo-location derived from the IP (e.g. "US/New York")
 * @param requestTime the time of the authentication request
 * @param deviceIdentity the full device identity record (may be null)
 */
public record RiskContext(String userId, String deviceId, String ipAddress, String ipLocation, Instant requestTime,
		DeviceIdentity deviceIdentity) {
}
