package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for usage information.
 */
public class UsageResponse {

	@JsonProperty("messagesUsed")
	private int messagesUsed;

	@JsonProperty("messagesLimit")
	private int messagesLimit; // -1 for unlimited

	@JsonProperty("messagesRemaining")
	private int messagesRemaining; // -1 for unlimited

	@JsonProperty("strategiesUsed")
	private int strategiesUsed;

	@JsonProperty("strategiesLimit")
	private int strategiesLimit; // -1 for unlimited

	@JsonProperty("strategiesRemaining")
	private int strategiesRemaining; // -1 for unlimited

	@JsonProperty("resetDate")
	private String resetDate; // YYYY-MM-DD

	// Default constructor
	public UsageResponse() {
	}

	// Getters and setters
	public int getMessagesUsed() {
		return messagesUsed;
	}

	public void setMessagesUsed(int messagesUsed) {
		this.messagesUsed = messagesUsed;
	}

	public int getMessagesLimit() {
		return messagesLimit;
	}

	public void setMessagesLimit(int messagesLimit) {
		this.messagesLimit = messagesLimit;
	}

	public int getMessagesRemaining() {
		return messagesRemaining;
	}

	public void setMessagesRemaining(int messagesRemaining) {
		this.messagesRemaining = messagesRemaining;
	}

	public int getStrategiesUsed() {
		return strategiesUsed;
	}

	public void setStrategiesUsed(int strategiesUsed) {
		this.strategiesUsed = strategiesUsed;
	}

	public int getStrategiesLimit() {
		return strategiesLimit;
	}

	public void setStrategiesLimit(int strategiesLimit) {
		this.strategiesLimit = strategiesLimit;
	}

	public int getStrategiesRemaining() {
		return strategiesRemaining;
	}

	public void setStrategiesRemaining(int strategiesRemaining) {
		this.strategiesRemaining = strategiesRemaining;
	}

	public String getResetDate() {
		return resetDate;
	}

	public void setResetDate(String resetDate) {
		this.resetDate = resetDate;
	}

}
