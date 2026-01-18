package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.social.entity.UserSubscription;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for a user subscription.
 */
public class UserSubscriptionResponse {

	@JsonProperty("id")
	private String id;

	@JsonProperty("subscriberId")
	private String subscriberId;

	@JsonProperty("ownerId")
	private String ownerId;

	@JsonProperty("ownerUsername")
	private String ownerUsername;

	@JsonProperty("ownerDisplayName")
	private String ownerDisplayName;

	@JsonProperty("ownerAvatarUrl")
	private String ownerAvatarUrl;

	@JsonProperty("tier")
	private String tier;

	@JsonProperty("monthlyPrice")
	private BigDecimal monthlyPrice;

	@JsonProperty("status")
	private String status;

	@JsonProperty("subscribedAt")
	private Instant subscribedAt;

	@JsonProperty("expiresAt")
	private Instant expiresAt;

	@JsonProperty("cancelledAt")
	private Instant cancelledAt;

	@JsonProperty("cancellationReason")
	private String cancellationReason;

	@JsonProperty("stripeSubscriptionId")
	private String stripeSubscriptionId;

	@JsonProperty("publicStrategyCount")
	private int publicStrategyCount;

	// Default constructor
	public UserSubscriptionResponse() {
	}

	/**
	 * Create a response from an entity.
	 */
	public static UserSubscriptionResponse fromEntity(UserSubscription entity) {
		UserSubscriptionResponse response = new UserSubscriptionResponse();
		response.setId(entity.getId());
		response.setSubscriberId(entity.getSubscriberId());
		response.setOwnerId(entity.getOwnerId());
		response.setTier(entity.getTier());
		response.setMonthlyPrice(entity.getMonthlyPrice());
		response.setStatus(entity.getStatus());
		response.setStripeSubscriptionId(entity.getStripeSubscriptionId());
		response.setCancellationReason(entity.getCancellationReason());

		if (entity.getSubscribedAt() != null) {
			response.setSubscribedAt(
					Instant.ofEpochSecond(entity.getSubscribedAt().getSeconds(), entity.getSubscribedAt().getNanos()));
		}

		if (entity.getExpiresAt() != null) {
			response.setExpiresAt(
					Instant.ofEpochSecond(entity.getExpiresAt().getSeconds(), entity.getExpiresAt().getNanos()));
		}

		if (entity.getCancelledAt() != null) {
			response.setCancelledAt(
					Instant.ofEpochSecond(entity.getCancelledAt().getSeconds(), entity.getCancelledAt().getNanos()));
		}

		return response;
	}

	// Getters and setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
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

	public String getOwnerDisplayName() {
		return ownerDisplayName;
	}

	public void setOwnerDisplayName(String ownerDisplayName) {
		this.ownerDisplayName = ownerDisplayName;
	}

	public String getOwnerAvatarUrl() {
		return ownerAvatarUrl;
	}

	public void setOwnerAvatarUrl(String ownerAvatarUrl) {
		this.ownerAvatarUrl = ownerAvatarUrl;
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

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

	public void setCancelledAt(Instant cancelledAt) {
		this.cancelledAt = cancelledAt;
	}

	public String getCancellationReason() {
		return cancellationReason;
	}

	public void setCancellationReason(String cancellationReason) {
		this.cancellationReason = cancellationReason;
	}

	public String getStripeSubscriptionId() {
		return stripeSubscriptionId;
	}

	public void setStripeSubscriptionId(String stripeSubscriptionId) {
		this.stripeSubscriptionId = stripeSubscriptionId;
	}

	public int getPublicStrategyCount() {
		return publicStrategyCount;
	}

	public void setPublicStrategyCount(int publicStrategyCount) {
		this.publicStrategyCount = publicStrategyCount;
	}

}
