package io.strategiz.client.webpush;

/**
 * Result of a Web Push notification attempt.
 */
public record WebPushResult(
        boolean succeeded,
        Status status,
        String error
) {
    public enum Status {
        SUCCESS,
        UNAVAILABLE,
        SUBSCRIPTION_GONE,
        FAILED
    }

    public static WebPushResult success() {
        return new WebPushResult(true, Status.SUCCESS, null);
    }

    public static WebPushResult unavailable() {
        return new WebPushResult(false, Status.UNAVAILABLE, "WebPush client not available");
    }

    public static WebPushResult subscriptionGone() {
        return new WebPushResult(false, Status.SUBSCRIPTION_GONE, "Subscription no longer valid");
    }

    public static WebPushResult failure(String error) {
        return new WebPushResult(false, Status.FAILED, error);
    }

    public boolean isSubscriptionGone() {
        return status == Status.SUBSCRIPTION_GONE;
    }
}
