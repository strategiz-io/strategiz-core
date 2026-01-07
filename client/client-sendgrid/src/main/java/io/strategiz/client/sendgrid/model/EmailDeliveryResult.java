package io.strategiz.client.sendgrid.model;

/**
 * Result of email delivery attempt.
 */
public class EmailDeliveryResult {

	private Status status;

	private String messageId;

	private String errorCode;

	private String errorMessage;

	private String providerName;

	public enum Status {

		/** Email accepted for delivery. */
		ACCEPTED,
		/** Email queued for sending. */
		QUEUED,
		/** Email sent to recipient server. */
		SENT,
		/** Email delivered to recipient. */
		DELIVERED,
		/** Email delivery failed. */
		FAILED,
		/** Provider unavailable. */
		UNAVAILABLE

	}

	private EmailDeliveryResult() {
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

	public String getProviderName() {
		return providerName;
	}

	public boolean isSuccess() {
		return status == Status.ACCEPTED || status == Status.QUEUED || status == Status.SENT
				|| status == Status.DELIVERED;
	}

	public static EmailDeliveryResult success(String messageId, String providerName) {
		EmailDeliveryResult result = new EmailDeliveryResult();
		result.status = Status.ACCEPTED;
		result.messageId = messageId;
		result.providerName = providerName;
		return result;
	}

	public static EmailDeliveryResult failure(String errorCode, String errorMessage, String providerName) {
		EmailDeliveryResult result = new EmailDeliveryResult();
		result.status = Status.FAILED;
		result.errorCode = errorCode;
		result.errorMessage = errorMessage;
		result.providerName = providerName;
		return result;
	}

	public static EmailDeliveryResult unavailable(String providerName) {
		EmailDeliveryResult result = new EmailDeliveryResult();
		result.status = Status.UNAVAILABLE;
		result.providerName = providerName;
		result.errorMessage = "Provider not configured or unavailable";
		return result;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final EmailDeliveryResult result = new EmailDeliveryResult();

		public Builder status(Status status) {
			result.status = status;
			return this;
		}

		public Builder messageId(String messageId) {
			result.messageId = messageId;
			return this;
		}

		public Builder errorCode(String errorCode) {
			result.errorCode = errorCode;
			return this;
		}

		public Builder errorMessage(String errorMessage) {
			result.errorMessage = errorMessage;
			return this;
		}

		public Builder providerName(String providerName) {
			result.providerName = providerName;
			return this;
		}

		public EmailDeliveryResult build() {
			return result;
		}

	}

}
