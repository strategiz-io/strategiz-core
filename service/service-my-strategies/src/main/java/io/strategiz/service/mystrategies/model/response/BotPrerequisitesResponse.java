package io.strategiz.service.mystrategies.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for bot deployment prerequisites check. Returns information about the
 * user's trading provider connection status.
 */
public class BotPrerequisitesResponse {

	@JsonProperty("alpacaConnected")
	private boolean alpacaConnected;

	@JsonProperty("alpacaEnvironment")
	private String alpacaEnvironment; // "paper" or "live", null if not connected

	@JsonProperty("canDeployBot")
	private boolean canDeployBot;

	@JsonProperty("message")
	private String message; // User-friendly message about the status

	public BotPrerequisitesResponse() {
	}

	public BotPrerequisitesResponse(boolean alpacaConnected, String alpacaEnvironment, boolean canDeployBot,
			String message) {
		this.alpacaConnected = alpacaConnected;
		this.alpacaEnvironment = alpacaEnvironment;
		this.canDeployBot = canDeployBot;
		this.message = message;
	}

	// Static factory methods for common responses
	public static BotPrerequisitesResponse notConnected() {
		return new BotPrerequisitesResponse(false, null, false,
				"Please connect your Alpaca account in Settings to deploy trading bots.");
	}

	public static BotPrerequisitesResponse connected(String environment) {
		String envLabel = "paper".equalsIgnoreCase(environment) ? "Paper Trading" : "Live Trading";
		return new BotPrerequisitesResponse(true, environment, true,
				"Alpaca account connected (" + envLabel + "). Ready to deploy bots.");
	}

	public static BotPrerequisitesResponse error(String errorMessage) {
		return new BotPrerequisitesResponse(false, null, false, "Alpaca connection error: " + errorMessage);
	}

	// Getters and Setters
	public boolean isAlpacaConnected() {
		return alpacaConnected;
	}

	public void setAlpacaConnected(boolean alpacaConnected) {
		this.alpacaConnected = alpacaConnected;
	}

	public String getAlpacaEnvironment() {
		return alpacaEnvironment;
	}

	public void setAlpacaEnvironment(String alpacaEnvironment) {
		this.alpacaEnvironment = alpacaEnvironment;
	}

	public boolean isCanDeployBot() {
		return canDeployBot;
	}

	public void setCanDeployBot(boolean canDeployBot) {
		this.canDeployBot = canDeployBot;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
