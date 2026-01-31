package io.strategiz.client.execution.model;

import java.util.List;
import java.util.Map;

public class HealthStatus {

	private String status;

	private List<String> supportedLanguages;

	private int maxTimeoutSeconds;

	private int maxMemoryMb;

	private Map<String, String> metadata;

	public static HealthStatusBuilder builder() {
		return new HealthStatusBuilder();
	}

	public static class HealthStatusBuilder {

		private String status;

		private List<String> supportedLanguages;

		private int maxTimeoutSeconds;

		private int maxMemoryMb;

		private Map<String, String> metadata;

		public HealthStatusBuilder status(String status) {
			this.status = status;
			return this;
		}

		public HealthStatusBuilder supportedLanguages(List<String> supportedLanguages) {
			this.supportedLanguages = supportedLanguages;
			return this;
		}

		public HealthStatusBuilder maxTimeoutSeconds(int maxTimeoutSeconds) {
			this.maxTimeoutSeconds = maxTimeoutSeconds;
			return this;
		}

		public HealthStatusBuilder maxMemoryMb(int maxMemoryMb) {
			this.maxMemoryMb = maxMemoryMb;
			return this;
		}

		public HealthStatusBuilder metadata(Map<String, String> metadata) {
			this.metadata = metadata;
			return this;
		}

		public HealthStatus build() {
			HealthStatus status = new HealthStatus();
			status.status = this.status;
			status.supportedLanguages = this.supportedLanguages;
			status.maxTimeoutSeconds = this.maxTimeoutSeconds;
			status.maxMemoryMb = this.maxMemoryMb;
			status.metadata = this.metadata;
			return status;
		}

	}

	public String getStatus() {
		return status;
	}

	public List<String> getSupportedLanguages() {
		return supportedLanguages;
	}

	public int getMaxTimeoutSeconds() {
		return maxTimeoutSeconds;
	}

	public int getMaxMemoryMb() {
		return maxMemoryMb;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

}
