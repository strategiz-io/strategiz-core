package io.strategiz.data.cryptotoken.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

/**
 * Entity representing a user's STRAT crypto token wallet.
 *
 * <p>Collection path: cryptoWallets/{walletId}</p>
 *
 * <p>Token Economics:</p>
 * <ul>
 *   <li>$1 USD = 100 STRAT tokens</li>
 *   <li>Platform fee: Configurable (see PlatformConfig entity)</li>
 *   <li>Uses: Pay owner subscriptions, tip creators, earn rewards</li>
 *   <li>NOTE: STRAT is NOT for AI credits (those come from platform tier only)</li>
 * </ul>
 *
 * <p>Balance is stored as Long with 6 decimal precision (micro-units).
 * Example: 1,000,000 = 1.000000 STRAT tokens</p>
 *
 * @see io.strategiz.data.cryptotoken.entity.CryptoTransaction
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("cryptoWallets")
public class CryptoWallet extends BaseEntity {

	@JsonProperty("id")
	private String id;

	@JsonProperty("userId")
	private String userId;

	@JsonProperty("balance")
	private Long balance;

	@JsonProperty("lockedBalance")
	private Long lockedBalance;

	@JsonProperty("totalEarned")
	private Long totalEarned;

	@JsonProperty("totalSpent")
	private Long totalSpent;

	@JsonProperty("totalPurchased")
	private Long totalPurchased;

	@JsonProperty("externalWalletAddress")
	private String externalWalletAddress;

	@JsonProperty("status")
	private String status;

	@JsonProperty("createdAt")
	private Timestamp createdAt;

	@JsonProperty("updatedAt")
	private Timestamp updatedAt;

	public static final long MICRO_UNITS = 1_000_000L;

	public static final long TOKENS_PER_USD = 100L;

	public CryptoWallet() {
		super();
		this.balance = 0L;
		this.lockedBalance = 0L;
		this.totalEarned = 0L;
		this.totalSpent = 0L;
		this.totalPurchased = 0L;
		this.status = "active";
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

	public Long getBalance() {
		return balance;
	}

	public void setBalance(Long balance) {
		this.balance = balance;
	}

	public Long getLockedBalance() {
		return lockedBalance;
	}

	public void setLockedBalance(Long lockedBalance) {
		this.lockedBalance = lockedBalance;
	}

	public Long getTotalEarned() {
		return totalEarned;
	}

	public void setTotalEarned(Long totalEarned) {
		this.totalEarned = totalEarned;
	}

	public Long getTotalSpent() {
		return totalSpent;
	}

	public void setTotalSpent(Long totalSpent) {
		this.totalSpent = totalSpent;
	}

	public Long getTotalPurchased() {
		return totalPurchased;
	}

	public void setTotalPurchased(Long totalPurchased) {
		this.totalPurchased = totalPurchased;
	}

	public String getExternalWalletAddress() {
		return externalWalletAddress;
	}

	public void setExternalWalletAddress(String externalWalletAddress) {
		this.externalWalletAddress = externalWalletAddress;
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

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Long getAvailableBalance() {
		return balance - (lockedBalance != null ? lockedBalance : 0L);
	}

	public boolean isActive() {
		return "active".equals(status);
	}

	public boolean isSuspended() {
		return "suspended".equals(status);
	}

	public boolean hasSufficientBalance(long amount) {
		return getAvailableBalance() >= amount;
	}

	public static double toDisplayValue(long microUnits) {
		return (double) microUnits / MICRO_UNITS;
	}

	public static long toMicroUnits(double displayValue) {
		return (long) (displayValue * MICRO_UNITS);
	}

	public static long usdCentsToMicroUnits(long usdCents) {
		return usdCents * MICRO_UNITS;
	}

	public static long microUnitsToUsdCents(long microUnits) {
		return microUnits / MICRO_UNITS;
	}

}
