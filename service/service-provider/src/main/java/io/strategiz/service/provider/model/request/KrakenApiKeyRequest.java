package io.strategiz.service.provider.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Request model for Kraken API key configuration
 */
public class KrakenApiKeyRequest {

	@NotBlank(message = "API key is required")
	@JsonProperty("apiKey")
	private String apiKey;

	@NotBlank(message = "API secret is required")
	@JsonProperty("apiSecret")
	private String apiSecret;

	@JsonProperty("otp")
	private String otp; // Optional - only required if 2FA is enabled on the API key

	public KrakenApiKeyRequest() {
	}

	public KrakenApiKeyRequest(String apiKey, String apiSecret, String otp) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.otp = otp;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiSecret() {
		return apiSecret;
	}

	public void setApiSecret(String apiSecret) {
		this.apiSecret = apiSecret;
	}

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}

}