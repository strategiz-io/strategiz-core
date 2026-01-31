package io.strategiz.business.portfolio.enhancer.model;

/**
 * Metadata information for an asset including display name, type, and other properties.
 */
public class AssetMetadata {

	private String symbol; // Standard symbol (e.g., "BTC")

	private String name; // Full name (e.g., "Bitcoin")

	private String displayName; // Display name (e.g., "Bitcoin (BTC)")

	private String assetType; // Type: crypto, stock, fiat, commodity

	private String category; // Sub-category: defi, payment, platform, etc.

	private String iconUrl; // URL to asset icon/logo

	private Integer decimals; // Number of decimal places (8 for BTC)

	private String description; // Brief description

	private boolean stakeable; // Whether asset can be staked

	private String defaultAPR; // Default staking APR if applicable

	public AssetMetadata() {
	}

	public AssetMetadata(String symbol, String name, String assetType) {
		this.symbol = symbol;
		this.name = name;
		this.assetType = assetType;
		this.displayName = name + " (" + symbol + ")";
	}

	// Static factory methods for common assets
	public static AssetMetadata bitcoin() {
		AssetMetadata metadata = new AssetMetadata("BTC", "Bitcoin", "crypto");
		metadata.setCategory("payment");
		metadata.setDecimals(8);
		metadata.setDescription("The first and largest cryptocurrency");
		metadata.setStakeable(false);
		return metadata;
	}

	public static AssetMetadata ethereum() {
		AssetMetadata metadata = new AssetMetadata("ETH", "Ethereum", "crypto");
		metadata.setCategory("platform");
		metadata.setDecimals(18);
		metadata.setDescription("Smart contract platform");
		metadata.setStakeable(true);
		metadata.setDefaultAPR("4.5%");
		return metadata;
	}

	public static AssetMetadata usDollar() {
		AssetMetadata metadata = new AssetMetadata("USD", "US Dollar", "fiat");
		metadata.setCategory("currency");
		metadata.setDecimals(2);
		metadata.setDescription("United States Dollar");
		metadata.setStakeable(false);
		return metadata;
	}

	// Getters and Setters
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

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getAssetType() {
		return assetType;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public Integer getDecimals() {
		return decimals;
	}

	public void setDecimals(Integer decimals) {
		this.decimals = decimals;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isStakeable() {
		return stakeable;
	}

	public void setStakeable(boolean stakeable) {
		this.stakeable = stakeable;
	}

	public String getDefaultAPR() {
		return defaultAPR;
	}

	public void setDefaultAPR(String defaultAPR) {
		this.defaultAPR = defaultAPR;
	}

}