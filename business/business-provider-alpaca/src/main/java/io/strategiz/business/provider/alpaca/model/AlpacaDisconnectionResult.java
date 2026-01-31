package io.strategiz.business.provider.alpaca.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Result object for Alpaca OAuth disconnection
 */
public class AlpacaDisconnectionResult {

	private String userId;

	private String providerId;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant disconnectedAt;

	private String status;

	// Constructors
	public AlpacaDisconnectionResult() {
	}

	// Getters and Setters
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public Instant getDisconnectedAt() {
		return disconnectedAt;
	}

	public void setDisconnectedAt(Instant disconnectedAt) {
		this.disconnectedAt = disconnectedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}