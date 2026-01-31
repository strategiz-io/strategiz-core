package io.strategiz.service.mystrategies.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic response model for success/error messages.
 */
public class MessageResponse {

	@JsonProperty("message")
	private String message;

	@JsonProperty("id")
	private String id; // Optional, for create operations

	@JsonProperty("status")
	private String status; // Optional, for status updates

	public MessageResponse() {
	}

	public MessageResponse(String message) {
		this.message = message;
	}

	public MessageResponse(String id, String message) {
		this.id = id;
		this.message = message;
	}

	// Getters and Setters
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
