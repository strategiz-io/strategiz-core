package io.strategiz.business.portfolio.enhancer.model;

import java.math.BigDecimal;

/**
 * Represents an enhanced asset with full metadata, pricing, and user-friendly
 * information.
 */
public class EnhancedAsset {

	private String rawSymbol; // Original symbol from provider (e.g., "XXBT", "ADA.F")

	private String symbol; // Normalized symbol (e.g., "BTC", "ADA")

	private String name; // Human-readable name (e.g., "Bitcoin", "Cardano")

	private String assetType; // Type: "crypto", "stock", "fiat", "commodity"

	private BigDecimal quantity; // Amount held

	private BigDecimal currentPrice; // Current market price in USD

	private BigDecimal value; // Total value (quantity * price)

	private BigDecimal costBasis; // Original purchase cost (if available)

	private BigDecimal profitLoss; // P&L in USD

	private BigDecimal profitLossPercent; // P&L percentage

	private boolean isStaked; // Whether asset is staked/earning

	private String stakingAPR; // Annual percentage rate if staked

	private String provider; // Source provider (e.g., "kraken")

	private Long lastUpdated; // Timestamp of last update

	public EnhancedAsset() {
		this.isStaked = false;
		this.lastUpdated = System.currentTimeMillis();
	}

	// Builder pattern for cleaner construction
	public static class Builder {

		private final EnhancedAsset asset = new EnhancedAsset();

		public Builder rawSymbol(String rawSymbol) {
			asset.rawSymbol = rawSymbol;
			return this;
		}

		public Builder symbol(String symbol) {
			asset.symbol = symbol;
			return this;
		}

		public Builder name(String name) {
			asset.name = name;
			return this;
		}

		public Builder assetType(String assetType) {
			asset.assetType = assetType;
			return this;
		}

		public Builder quantity(BigDecimal quantity) {
			asset.quantity = quantity;
			return this;
		}

		public Builder currentPrice(BigDecimal currentPrice) {
			asset.currentPrice = currentPrice;
			return this;
		}

		public Builder value(BigDecimal value) {
			asset.value = value;
			return this;
		}

		public Builder costBasis(BigDecimal costBasis) {
			asset.costBasis = costBasis;
			return this;
		}

		public Builder profitLoss(BigDecimal profitLoss) {
			asset.profitLoss = profitLoss;
			return this;
		}

		public Builder profitLossPercent(BigDecimal profitLossPercent) {
			asset.profitLossPercent = profitLossPercent;
			return this;
		}

		public Builder isStaked(boolean isStaked) {
			asset.isStaked = isStaked;
			return this;
		}

		public Builder stakingAPR(String stakingAPR) {
			asset.stakingAPR = stakingAPR;
			return this;
		}

		public Builder provider(String provider) {
			asset.provider = provider;
			return this;
		}

		public EnhancedAsset build() {
			// Calculate value if not set
			if (asset.value == null && asset.quantity != null && asset.currentPrice != null) {
				asset.value = asset.quantity.multiply(asset.currentPrice);
			}

			// Calculate P&L if cost basis is available
			if (asset.costBasis != null && asset.value != null) {
				asset.profitLoss = asset.value.subtract(asset.costBasis);
				if (asset.costBasis.compareTo(BigDecimal.ZERO) > 0) {
					asset.profitLossPercent = asset.profitLoss.divide(asset.costBasis, 4, BigDecimal.ROUND_HALF_UP)
						.multiply(new BigDecimal("100"));
				}
			}

			return asset;
		}

	}

	// Getters and Setters
	public String getRawSymbol() {
		return rawSymbol;
	}

	public void setRawSymbol(String rawSymbol) {
		this.rawSymbol = rawSymbol;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAssetType() {
		return assetType;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getCurrentPrice() {
		return currentPrice;
	}

	public void setCurrentPrice(BigDecimal currentPrice) {
		this.currentPrice = currentPrice;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public BigDecimal getCostBasis() {
		return costBasis;
	}

	public void setCostBasis(BigDecimal costBasis) {
		this.costBasis = costBasis;
	}

	public BigDecimal getProfitLoss() {
		return profitLoss;
	}

	public void setProfitLoss(BigDecimal profitLoss) {
		this.profitLoss = profitLoss;
	}

	public BigDecimal getProfitLossPercent() {
		return profitLossPercent;
	}

	public void setProfitLossPercent(BigDecimal profitLossPercent) {
		this.profitLossPercent = profitLossPercent;
	}

	public boolean isStaked() {
		return isStaked;
	}

	public void setStaked(boolean staked) {
		isStaked = staked;
	}

	public String getStakingAPR() {
		return stakingAPR;
	}

	public void setStakingAPR(String stakingAPR) {
		this.stakingAPR = stakingAPR;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

}