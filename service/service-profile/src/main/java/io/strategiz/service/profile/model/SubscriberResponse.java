package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.social.entity.UserSubscription;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for a subscriber (from the owner's perspective).
 */
public class SubscriberResponse {

	@JsonProperty("subscriptionId")
	private String subscriptionId;

	@JsonProperty("subscriberId")
	private String subscriberId;

	@JsonProperty("subscriberUsername")
	private String subscriberUsername;

	@JsonProperty("subscriberDisplayName")
	private String subscriberDisplayName;

	@JsonProperty("subscriberAvatarUrl")
	private String subscriberAvatarUrl;

	@JsonProperty("tier")
	private String tier;

	@JsonProperty("monthlyPrice")
	private BigDecimal monthlyPrice;

	@JsonProperty("status")
	private String status;

	@JsonProperty("subscribedAt")
	private Instant subscribedAt;

	@JsonProperty("cancelledAt")
	private Instant cancelledAt;

	// Default constructor
	public SubscriberResponse() {
	}

	/**
	 * Create a response from an entity.
	 */
	public static SubscriberResponse fromEntity(UserSubscription entity) {
		SubscriberResponse response = new SubscriberResponse();
		response.setSubscriptionId(entity.getId());
		response.setSubscriberId(entity.getSubscriberId());
		response.setTier(entity.getTier());
		response.setMonthlyPrice(entity.getMonthlyPrice());
		response.setStatus(entity.getStatus());

		if (entity.getSubscribedAt() != null) {
			response.setSubscribedAt(
					Instant.ofEpochSecond(entity.getSubscribedAt().getSeconds(), entity.getSubscribedAt().getNanos()));
		}

		if (entity.getCancelledAt() != null) {
			response.setCancelledAt(
					Instant.ofEpochSecond(entity.getCancelledAt().getSeconds(), entity.getCancelledAt().getNanos()));
		}

		return response;
	}

	// Getters and setters
	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	public String getSubscriberUsername() {
		return subscriberUsername;
	}

	public void setSubscriberUsername(String subscriberUsername) {
		this.subscriberUsername = subscriberUsername;
	}

	public String getSubscriberDisplayName() {
		return subscriberDisplayName;
	}

	public void setSubscriberDisplayName(String subscriberDisplayName) {
		this.subscriberDisplayName = subscriberDisplayName;
	}

	public String getSubscriberAvatarUrl() {
		return subscriberAvatarUrl;
	}

	public void setSubscriberAvatarUrl(String subscriberAvatarUrl) {
		this.subscriberAvatarUrl = subscriberAvatarUrl;
	}

	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public BigDecimal getMonthlyPrice() {
		return monthlyPrice;
	}

	public void setMonthlyPrice(BigDecimal monthlyPrice) {
		this.monthlyPrice = monthlyPrice;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getSubscribedAt() {
		return subscribedAt;
	}

	public void setSubscribedAt(Instant subscribedAt) {
		this.subscribedAt = subscribedAt;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

	public void setCancelledAt(Instant cancelledAt) {
		this.cancelledAt = cancelledAt;
	}

}
