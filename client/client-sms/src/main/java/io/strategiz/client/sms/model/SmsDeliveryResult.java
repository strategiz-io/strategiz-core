package io.strategiz.client.sms.model;

import java.time.Instant;

/**
 * Result of an SMS delivery attempt.
 */
public class SmsDeliveryResult {

    public enum Status {
        /** Message accepted by provider, delivery pending */
        QUEUED,
        /** Message sent to carrier */
        SENT,
        /** Message delivered to device */
        DELIVERED,
        /** Message delivery failed */
        FAILED,
        /** Provider not available or configured */
        UNAVAILABLE
    }

    private final Status status;
    private final String messageId;
    private final String errorCode;
    private final String errorMessage;
    private final Instant timestamp;
    private final String providerName;

    private SmsDeliveryResult(Builder builder) {
        this.status = builder.status;
        this.messageId = builder.messageId;
        this.errorCode = builder.errorCode;
        this.errorMessage = builder.errorMessage;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.providerName = builder.providerName;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getProviderName() {
        return providerName;
    }

    public boolean isSuccess() {
        return status == Status.QUEUED || status == Status.SENT || status == Status.DELIVERED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SmsDeliveryResult success(String messageId, String providerName) {
        return builder()
                .status(Status.QUEUED)
                .messageId(messageId)
                .providerName(providerName)
                .build();
    }

    public static SmsDeliveryResult failure(String errorCode, String errorMessage, String providerName) {
        return builder()
                .status(Status.FAILED)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .providerName(providerName)
                .build();
    }

    public static SmsDeliveryResult unavailable(String providerName) {
        return builder()
                .status(Status.UNAVAILABLE)
                .errorMessage("SMS provider not available")
                .providerName(providerName)
                .build();
    }

    @Override
    public String toString() {
        return "SmsDeliveryResult{" +
                "status=" + status +
                ", messageId='" + messageId + '\'' +
                ", providerName='" + providerName + '\'' +
                (errorMessage != null ? ", error='" + errorMessage + '\'' : "") +
                '}';
    }

    public static class Builder {
        private Status status;
        private String messageId;
        private String errorCode;
        private String errorMessage;
        private Instant timestamp;
        private String providerName;

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public SmsDeliveryResult build() {
            return new SmsDeliveryResult(this);
        }
    }
}
