package io.strategiz.service.marketplace.model.domain;

import java.util.List;
import java.util.Map;

/**
 * Strategy model - represents a strategy in the marketplace Stored in the top-level
 * 'strategies' collection
 */
public class Strategy {

	private String id;

	private String name;

	private String description;

	private String creatorId;

	private String creatorName;

	private String creatorEmail;

	private List<String> tags;

	private double price;

	private String currency; // e.g., "USD"

	private boolean isPublic;

	private long createdAt;

	private long updatedAt;

	private Map<String, Object> metadata;

	private Map<String, Object> configuration;

	private String version;

	private List<String> supportedExchanges;

	private long purchaseCount;

	private double averageRating;

	// Getters and setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}

	public String getCreatorName() {
		return creatorName;
	}

	public void setCreatorName(String creatorName) {
		this.creatorName = creatorName;
	}

	public String getCreatorEmail() {
		return creatorEmail;
	}

	public void setCreatorEmail(String creatorEmail) {
		this.creatorEmail = creatorEmail;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
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

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public Map<String, Object> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<String> getSupportedExchanges() {
		return supportedExchanges;
	}

	public void setSupportedExchanges(List<String> supportedExchanges) {
		this.supportedExchanges = supportedExchanges;
	}

	public long getPurchaseCount() {
		return purchaseCount;
	}

	public void setPurchaseCount(long purchaseCount) {
		this.purchaseCount = purchaseCount;
	}

	public double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(double averageRating) {
		this.averageRating = averageRating;
	}

}