package io.strategiz.service.marketplace.model.domain;

/**
 * Review model - represents a user's review of a strategy Stored in the 'reviews'
 * subcollection under each strategy document
 */
public class Review {

	private String id;

	private String strategyId;

	private String userId;

	private String userName;

	private int rating; // 1-5

	private String comment;

	private long createdAt;

	private long updatedAt;

	private boolean isVerifiedPurchase;

	// Getters and setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public long getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(long updatedAt) {
		this.updatedAt = updatedAt;
	}

	public boolean isVerifiedPurchase() {
		return isVerifiedPurchase;
	}

	public void setVerifiedPurchase(boolean isVerifiedPurchase) {
		this.isVerifiedPurchase = isVerifiedPurchase;
	}

}