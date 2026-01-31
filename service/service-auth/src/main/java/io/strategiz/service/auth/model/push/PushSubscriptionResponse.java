package io.strategiz.service.auth.model.push;

import com.google.cloud.Timestamp;
import io.strategiz.data.auth.entity.PushSubscriptionEntity;

import java.time.Instant;

/**
 * Response for push subscription operations.
 *
 * @param id the subscription ID
 * @param deviceName the friendly name for the device
 * @param pushAuthEnabled whether push auth is enabled
 * @param createdAt when the subscription was created
 * @param endpointHint partial endpoint for identification (e.g., "...googleapis.com/...")
 */
public record PushSubscriptionResponse(String id, String deviceName, boolean pushAuthEnabled, Instant createdAt,
		String endpointHint) {
	public static PushSubscriptionResponse from(PushSubscriptionEntity entity) {
		return new PushSubscriptionResponse(entity.getId(), entity.getDeviceName(),
				entity.getPushAuthEnabled() != null && entity.getPushAuthEnabled(), toInstant(entity.getCreatedDate()),
				maskEndpoint(entity.getEndpoint()));
	}

	private static Instant toInstant(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}

	private static String maskEndpoint(String endpoint) {
		if (endpoint == null || endpoint.length() < 50) {
			return "***";
		}
		// Extract domain and show partial path
		try {
			int protocolEnd = endpoint.indexOf("://") + 3;
			int domainEnd = endpoint.indexOf("/", protocolEnd);
			if (domainEnd < 0)
				domainEnd = endpoint.length();
			String domain = endpoint.substring(protocolEnd, domainEnd);
			return "..." + domain + "/...";
		}
		catch (Exception e) {
			return "***";
		}
	}
}
