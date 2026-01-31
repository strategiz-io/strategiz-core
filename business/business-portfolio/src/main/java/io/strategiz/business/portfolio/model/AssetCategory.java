package io.strategiz.business.portfolio.model;

import java.util.Arrays;

/**
 * Asset category classification for portfolio grouping and visualization. Maps asset
 * types to higher-level categories for aggregation and display.
 */
public enum AssetCategory {

	CRYPTOCURRENCY("Cryptocurrencies", "crypto", "#39FF14", "Digital currencies and tokens"),
	STOCKS("Stocks & Equities", "stock", "#00BFFF", "Public company shares and equities"),
	BONDS("Bonds & Fixed Income", "bond", "#FFD700", "Government and corporate bonds"),
	CASH("Cash & Money Market", "fiat", "#FFFFFF", "Cash, savings, and money market funds"),
	COMMODITIES("Commodities", "commodity", "#FFA500", "Physical commodities and futures"),
	FOREX("Foreign Exchange", "forex", "#BF00FF", "Currency pairs and FX positions"),
	ALTERNATIVES("Alternative Investments", "alternative", "#FF1493", "REITs, collectibles, and other alternatives");

	private final String displayName;

	private final String assetType;

	private final String color;

	private final String description;

	AssetCategory(String displayName, String assetType, String color, String description) {
		this.displayName = displayName;
		this.assetType = assetType;
		this.color = color;
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getAssetType() {
		return assetType;
	}

	public String getColor() {
		return color;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Maps an asset type string to its corresponding category.
	 * @param assetType the asset type string (e.g., "crypto", "stock")
	 * @return the matching AssetCategory, or ALTERNATIVES if no match found
	 */
	public static AssetCategory fromAssetType(String assetType) {
		if (assetType == null || assetType.isEmpty()) {
			return ALTERNATIVES;
		}

		String normalizedType = assetType.toLowerCase().trim();

		return Arrays.stream(values())
			.filter(category -> category.getAssetType().equals(normalizedType))
			.findFirst()
			.orElse(ALTERNATIVES);
	}

	/**
	 * Checks if this category represents a crypto asset.
	 */
	public boolean isCrypto() {
		return this == CRYPTOCURRENCY;
	}

	/**
	 * Checks if this category represents a traditional security.
	 */
	public boolean isTraditionalSecurity() {
		return this == STOCKS || this == BONDS;
	}

	/**
	 * Checks if this category represents cash or cash equivalents.
	 */
	public boolean isCash() {
		return this == CASH;
	}

}
