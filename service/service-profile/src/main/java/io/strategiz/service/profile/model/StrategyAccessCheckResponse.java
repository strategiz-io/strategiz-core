package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for checking if a user has access to deploy a strategy.
 */
public class StrategyAccessCheckResponse {

	@JsonProperty("hasAccess")
	private boolean hasAccess;

	@JsonProperty("accessType")
	private String accessType; // OWNER, SUBSCRIPTION, PURCHASED, NONE

	@JsonProperty("ownerId")
	private String ownerId;

	@JsonProperty("ownerUsername")
	private String ownerUsername;

	@JsonProperty("subscriptionRequired")
	private boolean subscriptionRequired;

	@JsonProperty("subscriptionPrice")
	private java.math.BigDecimal subscriptionPrice;

	@JsonProperty("message")
	private String message;

	// Default constructor
	public StrategyAccessCheckResponse() {
	}

	// Builder-style static factory methods
	public static StrategyAccessCheckResponse ownerAccess() {
		StrategyAccessCheckResponse response = new StrategyAccessCheckResponse();
		response.setHasAccess(true);
		response.setAccessType("OWNER");
		response.setMessage("You own this strategy");
		return response;
	}

	public static StrategyAccessCheckResponse subscriptionAccess(String ownerId) {
		StrategyAccessCheckResponse response = new StrategyAccessCheckResponse();
		response.setHasAccess(true);
		response.setAccessType("SUBSCRIPTION");
		response.setOwnerId(ownerId);
		response.setMessage("You have an active subscription to the owner");
		return response;
	}

	public static StrategyAccessCheckResponse purchasedAccess() {
		StrategyAccessCheckResponse response = new StrategyAccessCheckResponse();
		response.setHasAccess(true);
		response.setAccessType("PURCHASED");
		response.setMessage("You purchased this strategy");
		return response;
	}

	public static StrategyAccessCheckResponse noAccess(String ownerId, java.math.BigDecimal subscriptionPrice) {
		StrategyAccessCheckResponse response = new StrategyAccessCheckResponse();
		response.setHasAccess(false);
		response.setAccessType("NONE");
		response.setOwnerId(ownerId);
		response.setSubscriptionRequired(true);
		response.setSubscriptionPrice(subscriptionPrice);
		response.setMessage("Subscribe to the owner to deploy this strategy");
		return response;
	}

	// Getters and setters
	public boolean isHasAccess() {
		return hasAccess;
	}

	public void setHasAccess(boolean hasAccess) {
		this.hasAccess = hasAccess;
	}

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getOwnerUsername() {
		return ownerUsername;
	}

	public void setOwnerUsername(String ownerUsername) {
		this.ownerUsername = ownerUsername;
	}

	public boolean isSubscriptionRequired() {
		return subscriptionRequired;
	}

	public void setSubscriptionRequired(boolean subscriptionRequired) {
		this.subscriptionRequired = subscriptionRequired;
	}

	public java.math.BigDecimal getSubscriptionPrice() {
		return subscriptionPrice;
	}

	public void setSubscriptionPrice(java.math.BigDecimal subscriptionPrice) {
		this.subscriptionPrice = subscriptionPrice;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
