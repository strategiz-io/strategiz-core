package io.strategiz.client.fcm.model;

import java.util.List;

/**
 * Result of push notification delivery attempt.
 */
public class PushDeliveryResult {

	private Status status;

	private String messageId;

	private String errorCode;

	private String errorMessage;

	private String providerName;

	// For multicast results
	private int successCount;

	private int failureCount;

	private List<String> failedTokens;

	public enum Status {

		/** Push notification sent successfully. */
		SENT,
		/** Push partially delivered (some tokens failed). */
		PARTIAL,
		/** Push delivery failed. */
		FAILED,
		/** Provider unavailable. */
		UNAVAILABLE

	}

	private PushDeliveryResult() {
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

	public int getSuccessCount() {
		return successCount;
	}

	public int getFailureCount() {
		return failureCount;
	}

	public List<String> getFailedTokens() {
		return failedTokens;
	}

	public boolean isSuccess() {
		return status == Status.SENT;
	}

	public static PushDeliveryResult success(String messageId, String providerName) {
		PushDeliveryResult result = new PushDeliveryResult();
		result.status = Status.SENT;
		result.messageId = messageId;
		result.providerName = providerName;
		result.successCount = 1;
		result.failureCount = 0;
		return result;
	}

	public static PushDeliveryResult multicastSuccess(int successCount, int failureCount, List<String> failedTokens,
			String providerName) {
		PushDeliveryResult result = new PushDeliveryResult();
		result.status = failureCount == 0 ? Status.SENT : Status.PARTIAL;
		result.providerName = providerName;
		result.successCount = successCount;
		result.failureCount = failureCount;
		result.failedTokens = failedTokens;
		return result;
	}

	public static PushDeliveryResult failure(String errorCode, String errorMessage, String providerName) {
		PushDeliveryResult result = new PushDeliveryResult();
		result.status = Status.FAILED;
		result.errorCode = errorCode;
		result.errorMessage = errorMessage;
		result.providerName = providerName;
		return result;
	}

	public static PushDeliveryResult unavailable(String providerName) {
		PushDeliveryResult result = new PushDeliveryResult();
		result.status = Status.UNAVAILABLE;
		result.providerName = providerName;
		result.errorMessage = "Provider not configured or unavailable";
		return result;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final PushDeliveryResult result = new PushDeliveryResult();

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

		public Builder successCount(int count) {
			result.successCount = count;
			return this;
		}

		public Builder failureCount(int count) {
			result.failureCount = count;
			return this;
		}

		public Builder failedTokens(List<String> tokens) {
			result.failedTokens = tokens;
			return this;
		}

		public PushDeliveryResult build() {
			return result;
		}

	}

}
