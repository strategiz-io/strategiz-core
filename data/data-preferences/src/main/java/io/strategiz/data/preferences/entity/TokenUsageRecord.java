package io.strategiz.data.preferences.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.time.Instant;
import java.util.UUID;

/**
 * Token usage record for audit trail and analytics. Stored at
 * users/{userId}/token_usage/{recordId}.
 *
 * Tracks individual AI API calls with: - Model used and token counts - Credits consumed
 * (weighted by model cost) - Request type for analytics
 */
@Collection("token_usage")
public class TokenUsageRecord extends BaseEntity {

	@DocumentId
	@PropertyName("recordId")
	@JsonProperty("recordId")
	private String recordId;

	@PropertyName("userId")
	@JsonProperty("userId")
	private String userId;

	@PropertyName("timestamp")
	@JsonProperty("timestamp")
	private Instant timestamp;

	@PropertyName("modelId")
	@JsonProperty("modelId")
	private String modelId;

	@PropertyName("promptTokens")
	@JsonProperty("promptTokens")
	private Integer promptTokens;

	@PropertyName("completionTokens")
	@JsonProperty("completionTokens")
	private Integer completionTokens;

	@PropertyName("totalTokens")
	@JsonProperty("totalTokens")
	private Integer totalTokens;

	@PropertyName("creditsConsumed")
	@JsonProperty("creditsConsumed")
	private Integer creditsConsumed;

	@PropertyName("requestType")
	@JsonProperty("requestType")
	private String requestType; // chat, strategy, historical_insights, etc.

	@PropertyName("sessionId")
	@JsonProperty("sessionId")
	private String sessionId;

	@PropertyName("billingPeriodStart")
	@JsonProperty("billingPeriodStart")
	private Instant billingPeriodStart;

	// Constructors
	public TokenUsageRecord() {
		super();
		this.recordId = UUID.randomUUID().toString();
		this.timestamp = Instant.now();
	}

	public TokenUsageRecord(String userId, String modelId, int promptTokens, int completionTokens, String requestType) {
		super();
		this.recordId = UUID.randomUUID().toString();
		this.userId = userId;
		this.modelId = modelId;
		this.promptTokens = promptTokens;
		this.completionTokens = completionTokens;
		this.totalTokens = promptTokens + completionTokens;
		this.creditsConsumed = SubscriptionTier.calculateCredits(modelId, promptTokens, completionTokens);
		this.requestType = requestType;
		this.timestamp = Instant.now();
	}

	/**
	 * Create a usage record with all details.
	 */
	public static TokenUsageRecord create(String userId, String modelId, int promptTokens, int completionTokens,
			String requestType, String sessionId, Instant billingPeriodStart) {
		TokenUsageRecord record = new TokenUsageRecord(userId, modelId, promptTokens, completionTokens, requestType);
		record.setSessionId(sessionId);
		record.setBillingPeriodStart(billingPeriodStart);
		return record;
	}

	// Getters and Setters
	public String getRecordId() {
		return recordId;
	}

	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public Integer getPromptTokens() {
		return promptTokens;
	}

	public void setPromptTokens(Integer promptTokens) {
		this.promptTokens = promptTokens;
	}

	public Integer getCompletionTokens() {
		return completionTokens;
	}

	public void setCompletionTokens(Integer completionTokens) {
		this.completionTokens = completionTokens;
	}

	public Integer getTotalTokens() {
		return totalTokens;
	}

	public void setTotalTokens(Integer totalTokens) {
		this.totalTokens = totalTokens;
	}

	public Integer getCreditsConsumed() {
		return creditsConsumed;
	}

	public void setCreditsConsumed(Integer creditsConsumed) {
		this.creditsConsumed = creditsConsumed;
	}

	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Instant getBillingPeriodStart() {
		return billingPeriodStart;
	}

	public void setBillingPeriodStart(Instant billingPeriodStart) {
		this.billingPeriodStart = billingPeriodStart;
	}

	@Override
	public String getId() {
		return recordId;
	}

	@Override
	public void setId(String id) {
		this.recordId = id;
	}

}
