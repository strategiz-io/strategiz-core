package io.strategiz.data.strategy.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Strategy ownership transfer entity - tracks ownership changes of strategies.
 *
 * Collection: strategy_ownership_transfers (top-level)
 *
 * Tracks: - Which strategy was transferred (strategyId) - Who owned it before
 * (fromOwnerId) - Who owns it now (toOwnerId) - Purchase price - When the transfer
 * occurred - How many subscribers were transferred - Monthly revenue transferred
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("strategy_ownership_transfers")
public class StrategyOwnershipTransfer extends BaseEntity {

	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	private String id;

	@PropertyName("strategyId")
	@JsonProperty("strategyId")
	@NotBlank(message = "Strategy ID is required")
	private String strategyId;

	@PropertyName("fromOwnerId")
	@JsonProperty("fromOwnerId")
	@NotBlank(message = "From owner ID is required")
	private String fromOwnerId;

	@PropertyName("toOwnerId")
	@JsonProperty("toOwnerId")
	@NotBlank(message = "To owner ID is required")
	private String toOwnerId;

	@PropertyName("purchasePrice")
	@JsonProperty("purchasePrice")
	@NotNull(message = "Purchase price is required")
	private BigDecimal purchasePrice;

	@PropertyName("currency")
	@JsonProperty("currency")
	private String currency = "USD";

	@PropertyName("transferredAt")
	@JsonProperty("transferredAt")
	@NotNull(message = "Transferred timestamp is required")
	private Timestamp transferredAt;

	@PropertyName("subscribersTransferred")
	@JsonProperty("subscribersTransferred")
	private Integer subscribersTransferred = 0;

	@PropertyName("monthlyRevenueTransferred")
	@JsonProperty("monthlyRevenueTransferred")
	private BigDecimal monthlyRevenueTransferred;

	@PropertyName("transactionId")
	@JsonProperty("transactionId")
	private String transactionId; // Stripe transaction ID

	@PropertyName("transferType")
	@JsonProperty("transferType")
	private String transferType; // PURCHASE, GIFT, TRANSFER

	// Denormalized fields for display
	@PropertyName("strategyName")
	@JsonProperty("strategyName")
	private String strategyName;

	@PropertyName("fromOwnerName")
	@JsonProperty("fromOwnerName")
	private String fromOwnerName;

	@PropertyName("toOwnerName")
	@JsonProperty("toOwnerName")
	private String toOwnerName;

	// Constructors
	public StrategyOwnershipTransfer() {
		super();
	}

	public StrategyOwnershipTransfer(String strategyId, String fromOwnerId, String toOwnerId, BigDecimal purchasePrice,
			Integer subscribersTransferred, BigDecimal monthlyRevenueTransferred) {
		super();
		this.strategyId = strategyId;
		this.fromOwnerId = fromOwnerId;
		this.toOwnerId = toOwnerId;
		this.purchasePrice = purchasePrice;
		this.subscribersTransferred = subscribersTransferred;
		this.monthlyRevenueTransferred = monthlyRevenueTransferred;
		this.transferredAt = Timestamp.now();
		this.transferType = "PURCHASE";
	}

	// Getters and Setters
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getFromOwnerId() {
		return fromOwnerId;
	}

	public void setFromOwnerId(String fromOwnerId) {
		this.fromOwnerId = fromOwnerId;
	}

	public String getToOwnerId() {
		return toOwnerId;
	}

	public void setToOwnerId(String toOwnerId) {
		this.toOwnerId = toOwnerId;
	}

	public BigDecimal getPurchasePrice() {
		return purchasePrice;
	}

	public void setPurchasePrice(BigDecimal purchasePrice) {
		this.purchasePrice = purchasePrice;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Timestamp getTransferredAt() {
		return transferredAt;
	}

	public void setTransferredAt(Timestamp transferredAt) {
		this.transferredAt = transferredAt;
	}

	public Integer getSubscribersTransferred() {
		return subscribersTransferred;
	}

	public void setSubscribersTransferred(Integer subscribersTransferred) {
		this.subscribersTransferred = subscribersTransferred;
	}

	public BigDecimal getMonthlyRevenueTransferred() {
		return monthlyRevenueTransferred;
	}

	public void setMonthlyRevenueTransferred(BigDecimal monthlyRevenueTransferred) {
		this.monthlyRevenueTransferred = monthlyRevenueTransferred;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getTransferType() {
		return transferType;
	}

	public void setTransferType(String transferType) {
		this.transferType = transferType;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}

	public String getFromOwnerName() {
		return fromOwnerName;
	}

	public void setFromOwnerName(String fromOwnerName) {
		this.fromOwnerName = fromOwnerName;
	}

	public String getToOwnerName() {
		return toOwnerName;
	}

	public void setToOwnerName(String toOwnerName) {
		this.toOwnerName = toOwnerName;
	}

	// Helper methods

	/**
	 * Check if transfer was a purchase (paid transfer).
	 */
	public boolean isPurchase() {
		return "PURCHASE".equals(this.transferType) && purchasePrice != null
				&& purchasePrice.compareTo(BigDecimal.ZERO) > 0;
	}

	/**
	 * Check if transfer was a gift (free transfer).
	 */
	public boolean isGift() {
		return "GIFT".equals(this.transferType);
	}

	/**
	 * Get formatted purchase price with currency.
	 */
	public String getFormattedPrice() {
		if (purchasePrice == null) {
			return "Free";
		}
		return String.format("%s %.2f", currency, purchasePrice);
	}

	@Override
	public String toString() {
		return "StrategyOwnershipTransfer{" + "id='" + id + '\'' + ", strategyId='" + strategyId + '\''
				+ ", fromOwnerId='" + fromOwnerId + '\'' + ", toOwnerId='" + toOwnerId + '\'' + ", purchasePrice="
				+ purchasePrice + ", transferredAt=" + transferredAt + ", subscribersTransferred="
				+ subscribersTransferred + ", monthlyRevenueTransferred=" + monthlyRevenueTransferred + '}';
	}

}
