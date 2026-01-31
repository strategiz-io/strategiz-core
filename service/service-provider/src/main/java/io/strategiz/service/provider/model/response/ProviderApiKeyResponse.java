package io.strategiz.service.provider.model.response;

/**
 * Response model for provider API key operations
 */
public class ProviderApiKeyResponse {

	private boolean success;

	private String provider;

	private String message;

	private String connectionId;

	private String status;

	public ProviderApiKeyResponse() {
	}

	public ProviderApiKeyResponse(boolean success, String provider, String message, String connectionId,
			String status) {
		this.success = success;
		this.provider = provider;
		this.message = message;
		this.connectionId = connectionId;
		this.status = status;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private boolean success;

		private String provider;

		private String message;

		private String connectionId;

		private String status;

		public Builder success(boolean success) {
			this.success = success;
			return this;
		}

		public Builder provider(String provider) {
			this.provider = provider;
			return this;
		}

		public Builder message(String message) {
			this.message = message;
			return this;
		}

		public Builder connectionId(String connectionId) {
			this.connectionId = connectionId;
			return this;
		}

		public Builder status(String status) {
			this.status = status;
			return this;
		}

		public ProviderApiKeyResponse build() {
			return new ProviderApiKeyResponse(success, provider, message, connectionId, status);
		}

	}

}