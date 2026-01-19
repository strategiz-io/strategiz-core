package io.strategiz.data.cryptotoken.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

/**
 * Entity representing a crypto token transaction in the ledger.
 *
 * <p>Collection path: cryptoTransactions/{transactionId}</p>
 *
 * <p>Transaction Types:</p>
 * <ul>
 *   <li>PURCHASE: Buy tokens with USD</li>
 *   <li>EARN: Receive tokens from rewards/engagement</li>
 *   <li>SPEND: Use tokens for owner subscriptions or tips</li>
 *   <li>TRANSFER: Send tokens to another user</li>
 * </ul>
 *
 * <p>NOTE: STRAT tokens CANNOT be converted to AI credits.
 * AI credits come from platform subscription tier only.</p>
 *
 * @see io.strategiz.data.cryptotoken.entity.CryptoWallet
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("cryptoTransactions")
public class CryptoTransaction extends BaseEntity {

	@JsonProperty("id")
	private String id;

	@JsonProperty("userId")
	private String userId;

	@JsonProperty("type")
	private String type;

	@JsonProperty("amount")
	private Long amount;

	@JsonProperty("balanceAfter")
	private Long balanceAfter;

	@JsonProperty("referenceType")
	private String referenceType;

	@JsonProperty("referenceId")
	private String referenceId;

	@JsonProperty("counterpartyId")
	private String counterpartyId;

	@JsonProperty("description")
	private String description;

	@JsonProperty("platformFee")
	private Long platformFee;

	@JsonProperty("status")
	private String status;

	@JsonProperty("createdAt")
	private Timestamp createdAt;

	@JsonProperty("completedAt")
	private Timestamp completedAt;

	public static final String TYPE_PURCHASE = "PURCHASE";

	public static final String TYPE_EARN = "EARN";

	public static final String TYPE_SPEND = "SPEND";

	public static final String TYPE_TRANSFER = "TRANSFER";

	/**
	 * @deprecated STRAT tokens cannot be converted to AI credits.
	 * AI credits come from platform subscription tier only.
	 */
	@Deprecated(forRemoval = true)
	public static final String TYPE_CONVERT = "CONVERT";

	public static final String REF_SUBSCRIPTION = "subscription";

	public static final String REF_TIP = "tip";

	/**
	 * @deprecated STRAT tokens cannot be used for AI credits.
	 * AI credits come from platform subscription tier only.
	 */
	@Deprecated(forRemoval = true)
	public static final String REF_AI_CREDITS = "ai_credits";

	public static final String REF_REWARD = "reward";

	public static final String REF_STRIPE = "stripe";

	public static final String REF_STRAT_PACK = "strat_pack";

	public static final String STATUS_PENDING = "pending";

	public static final String STATUS_COMPLETED = "completed";

	public static final String STATUS_FAILED = "failed";

	public CryptoTransaction() {
		super();
		this.status = STATUS_PENDING;
		this.platformFee = 0L;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getAmount() {
		return amount;
	}

	public void setAmount(Long amount) {
		this.amount = amount;
	}

	public Long getBalanceAfter() {
		return balanceAfter;
	}

	public void setBalanceAfter(Long balanceAfter) {
		this.balanceAfter = balanceAfter;
	}

	public String getReferenceType() {
		return referenceType;
	}

	public void setReferenceType(String referenceType) {
		this.referenceType = referenceType;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public String getCounterpartyId() {
		return counterpartyId;
	}

	public void setCounterpartyId(String counterpartyId) {
		this.counterpartyId = counterpartyId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getPlatformFee() {
		return platformFee;
	}

	public void setPlatformFee(Long platformFee) {
		this.platformFee = platformFee;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Timestamp completedAt) {
		this.completedAt = completedAt;
	}

	public boolean isPending() {
		return STATUS_PENDING.equals(status);
	}

	public boolean isCompleted() {
		return STATUS_COMPLETED.equals(status);
	}

	public boolean isFailed() {
		return STATUS_FAILED.equals(status);
	}

	public boolean isCredit() {
		return TYPE_PURCHASE.equals(type) || TYPE_EARN.equals(type)
				|| (TYPE_TRANSFER.equals(type) && amount != null && amount > 0);
	}

	public boolean isDebit() {
		return TYPE_SPEND.equals(type) || TYPE_CONVERT.equals(type)
				|| (TYPE_TRANSFER.equals(type) && amount != null && amount < 0);
	}

}
