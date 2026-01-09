package io.strategiz.client.webpush;

import io.strategiz.data.auth.entity.PushSubscriptionEntity;
import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Web Push API client for sending browser push notifications.
 *
 * <p>Uses VAPID (Voluntary Application Server Identification) keys
 * for authenticating with push services.</p>
 *
 * <p>VAPID keys should be stored in Vault at:</p>
 * <ul>
 *   <li>push.{env}.vapid-public-key - Base64 URL-safe encoded public key</li>
 *   <li>push.{env}.vapid-private-key - Base64 URL-safe encoded private key</li>
 * </ul>
 */
@Component
public class WebPushClient {

    private static final Logger log = LoggerFactory.getLogger(WebPushClient.class);

    @Value("${app.env:dev}")
    private String env;

    @Value("${vapid.subject:mailto:support@strategiz.io}")
    private String vapidSubject;

    @Autowired
    private SecretManager secretManager;

    private PushService pushService;
    private String vapidPublicKey;
    private boolean initialized = false;

    static {
        // Add Bouncy Castle provider if not already added
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @PostConstruct
    public void init() {
        try {
            // Load VAPID keys from Vault
            String secretPath = "push." + env;
            java.util.Map<String, Object> secrets = secretManager.readSecretAsMap(secretPath);

            if (secrets == null || secrets.isEmpty()) {
                log.warn("VAPID keys not configured in Vault at path: {}. Web Push will be unavailable.", secretPath);
                return;
            }

            vapidPublicKey = (String) secrets.get("vapid-public-key");
            String vapidPrivateKey = (String) secrets.get("vapid-private-key");

            if (vapidPublicKey == null || vapidPublicKey.isEmpty() ||
                vapidPrivateKey == null || vapidPrivateKey.isEmpty()) {
                log.warn("VAPID keys not configured in Vault at path: {}. Web Push will be unavailable.", secretPath);
                return;
            }

            // Initialize push service
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            initialized = true;

            log.info("WebPushClient initialized successfully with subject: {}", vapidSubject);

        } catch (GeneralSecurityException e) {
            log.error("Failed to initialize WebPushClient: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error initializing WebPushClient: {}", e.getMessage());
        }
    }

    /**
     * Check if the client is available for sending notifications.
     */
    public boolean isAvailable() {
        return initialized && pushService != null;
    }

    /**
     * Get the VAPID public key for client subscription.
     */
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    /**
     * Send a push notification to a subscription.
     *
     * @param subscription the push subscription entity
     * @param payload the notification payload (JSON)
     * @return WebPushResult with success/failure info
     */
    public WebPushResult sendNotification(PushSubscriptionEntity subscription, String payload) {
        if (!isAvailable()) {
            log.warn("WebPushClient not available, cannot send notification");
            return WebPushResult.unavailable();
        }

        try {
            // Create subscription object for the library
            Subscription.Keys keys = new Subscription.Keys(subscription.getP256dh(), subscription.getAuth());
            Subscription sub = new Subscription(subscription.getEndpoint(), keys);

            // Create and send notification
            Notification notification = new Notification(sub, payload);

            org.apache.http.HttpResponse response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                log.debug("Push notification sent successfully to subscription {}", subscription.getId());
                return WebPushResult.success();
            } else if (statusCode == 404 || statusCode == 410) {
                // Subscription is no longer valid
                log.warn("Push subscription {} is no longer valid (status {})", subscription.getId(), statusCode);
                return WebPushResult.subscriptionGone();
            } else {
                log.warn("Push notification failed for subscription {} with status {}",
                        subscription.getId(), statusCode);
                return WebPushResult.failure("HTTP " + statusCode);
            }

        } catch (Exception e) {
            log.error("Error sending push notification to subscription {}: {}",
                    subscription.getId(), e.getMessage());
            return WebPushResult.failure(e.getMessage());
        }
    }

    /**
     * Send a push notification to multiple subscriptions.
     *
     * @param subscriptions list of push subscriptions
     * @param payload the notification payload (JSON)
     * @return list of results for each subscription
     */
    public List<WebPushResult> sendNotifications(List<PushSubscriptionEntity> subscriptions, String payload) {
        List<WebPushResult> results = new ArrayList<>();
        for (PushSubscriptionEntity subscription : subscriptions) {
            results.add(sendNotification(subscription, payload));
        }
        return results;
    }

    /**
     * Send a push auth notification.
     * Creates a standardized payload for push authentication.
     *
     * @param subscription the push subscription
     * @param challenge the challenge token
     * @param purpose the auth purpose (signin, mfa, recovery)
     * @param ipAddress the IP address of the auth attempt
     * @param location the location of the auth attempt
     * @param userAgent the user agent of the auth attempt
     * @return WebPushResult with success/failure info
     */
    public WebPushResult sendPushAuthNotification(PushSubscriptionEntity subscription,
                                                   String challenge,
                                                   String purpose,
                                                   String ipAddress,
                                                   String location,
                                                   String userAgent) {
        // Build the notification payload
        String payload = buildPushAuthPayload(challenge, purpose, ipAddress, location, userAgent);
        return sendNotification(subscription, payload);
    }

    /**
     * Build the JSON payload for a push auth notification.
     */
    private String buildPushAuthPayload(String challenge, String purpose, String ipAddress,
                                         String location, String userAgent) {
        String title = getPushAuthTitle(purpose);
        String body = buildPushAuthBody(ipAddress, location);

        // Build JSON manually to avoid adding Jackson dependency
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"push_auth\",");
        json.append("\"challenge\":\"").append(escapeJson(challenge)).append("\",");
        json.append("\"purpose\":\"").append(escapeJson(purpose)).append("\",");
        json.append("\"title\":\"").append(escapeJson(title)).append("\",");
        json.append("\"body\":\"").append(escapeJson(body)).append("\",");
        json.append("\"data\":{");
        json.append("\"ipAddress\":\"").append(escapeJson(ipAddress != null ? ipAddress : "unknown")).append("\",");
        json.append("\"location\":\"").append(escapeJson(location != null ? location : "")).append("\",");
        json.append("\"userAgent\":\"").append(escapeJson(userAgent != null ? userAgent : "")).append("\"");
        json.append("}");
        json.append("}");

        return json.toString();
    }

    private String getPushAuthTitle(String purpose) {
        switch (purpose) {
            case "signin":
                return "Sign-in Request";
            case "mfa":
                return "Verification Required";
            case "recovery":
                return "Account Recovery Request";
            default:
                return "Authentication Request";
        }
    }

    private String buildPushAuthBody(String ipAddress, String location) {
        StringBuilder body = new StringBuilder("Someone is trying to access your account");
        if (location != null && !location.isEmpty()) {
            body.append(" from ").append(location);
        } else if (ipAddress != null && !ipAddress.isEmpty()) {
            body.append(" (IP: ").append(ipAddress).append(")");
        }
        body.append(". Tap to approve or deny.");
        return body.toString();
    }

    /**
     * Escape special characters for JSON strings.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
