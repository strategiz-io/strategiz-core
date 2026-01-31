package io.strategiz.client.binanceus.auth.model;

/**
 * Binance US API credentials model
 */
public class BinanceUSApiCredentials {

	private String apiKey;

	private String apiSecret;

	private String userId;

	public BinanceUSApiCredentials() {
	}

	public BinanceUSApiCredentials(String apiKey, String apiSecret) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}

	public BinanceUSApiCredentials(String apiKey, String apiSecret, String userId) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.userId = userId;
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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public String toString() {
		return "BinanceUSApiCredentials{" + "apiKey='"
				+ (apiKey != null ? apiKey.substring(0, Math.min(apiKey.length(), 4)) + "****" : null) + '\''
				+ ", userId='" + userId + '\'' + '}';
	}

}