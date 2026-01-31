package io.strategiz.data.symbol.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.Exclude;
import com.google.cloud.firestore.annotation.PropertyName;
import io.strategiz.data.base.annotation.Collection;
import io.strategiz.data.base.entity.BaseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore entity for symbol reference data and cross-exchange mapping.
 *
 * Collection structure in Firestore: - Collection: "symbols" - Document ID: canonical
 * symbol (e.g., "BTC", "AAPL", "ETH")
 *
 * This stores: - Symbol metadata (name, category, description) - Cross-exchange symbol
 * mappings (e.g., BTC -> BTC-USD on Yahoo, BTCUSDT on Binance) - Data collection tracking
 * (which symbols are being collected, last collected timestamp)
 */
@Collection("symbols")
public class SymbolEntity extends BaseEntity {

	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	private String id; // Canonical symbol (e.g., "BTC", "AAPL")

	// Display information
	@PropertyName("name")
	@JsonProperty("name")
	private String name; // Full name (e.g., "Bitcoin", "Apple Inc.")

	@PropertyName("displayName")
	@JsonProperty("displayName")
	private String displayName; // Display format (e.g., "Bitcoin (BTC)")

	@PropertyName("assetType")
	@JsonProperty("assetType")
	private String assetType; // STOCK, CRYPTO, ETF, FOREX, FIAT

	@PropertyName("category")
	@JsonProperty("category")
	private String category; // defi, payment, platform, tech, etc.

	@PropertyName("description")
	@JsonProperty("description")
	private String description;

	@PropertyName("decimals")
	@JsonProperty("decimals")
	private Integer decimals;

	// Cross-exchange mapping (CRITICAL)
	// Maps provider name to provider-specific symbol format
	// e.g., {"YAHOO": "BTC-USD", "ALPACA": "BTC/USD", "COINBASE": "BTC-USD", "BINANCE":
	// "BTCUSDT"}
	@PropertyName("providerSymbols")
	@JsonProperty("providerSymbols")
	private Map<String, String> providerSymbols;

	// Data collection tracking
	@PropertyName("collectionActive")
	@JsonProperty("collectionActive")
	private Boolean collectionActive; // Is this symbol being collected?

	@PropertyName("primaryDataSource")
	@JsonProperty("primaryDataSource")
	private String primaryDataSource; // YAHOO, ALPACA, etc.

	@PropertyName("timeframes")
	@JsonProperty("timeframes")
	private List<String> timeframes; // ["1D", "1h"] - which timeframes collected

	@PropertyName("lastCollectedAt")
	@JsonProperty("lastCollectedAt")
	private Timestamp lastCollectedAt; // When was data last collected

	// Additional metadata
	@PropertyName("iconUrl")
	@JsonProperty("iconUrl")
	private String iconUrl;

	@PropertyName("status")
	@JsonProperty("status")
	private String status; // ACTIVE, DELISTED, HALTED

	// Constructor
	public SymbolEntity() {
		this.providerSymbols = new HashMap<>();
		this.timeframes = new ArrayList<>();
		this.collectionActive = false;
		this.status = "ACTIVE";
	}

	/**
	 * Create a SymbolEntity with basic info
	 */
	public SymbolEntity(String symbol, String name, String assetType) {
		this();
		this.id = symbol.toUpperCase();
		this.name = name;
		this.assetType = assetType;
		this.displayName = name + " (" + symbol.toUpperCase() + ")";
	}

	// === Helper methods ===

	/**
	 * Get the provider-specific symbol for a given provider
	 */
	@Exclude
	@JsonIgnore
	public String getProviderSymbol(String provider) {
		if (providerSymbols == null) {
			return id; // Return canonical symbol as fallback
		}
		return providerSymbols.getOrDefault(provider.toUpperCase(), id);
	}

	/**
	 * Set the provider-specific symbol for a given provider
	 */
	public void setProviderSymbol(String provider, String symbol) {
		if (providerSymbols == null) {
			providerSymbols = new HashMap<>();
		}
		providerSymbols.put(provider.toUpperCase(), symbol);
	}

	/**
	 * Check if this symbol has a mapping for a specific provider
	 */
	@Exclude
	@JsonIgnore
	public boolean hasProviderMapping(String provider) {
		return providerSymbols != null && providerSymbols.containsKey(provider.toUpperCase());
	}

	/**
	 * Check if this symbol is active for data collection
	 */
	@Exclude
	@JsonIgnore
	public boolean isActiveForCollection() {
		return Boolean.TRUE.equals(collectionActive) && "ACTIVE".equals(status);
	}

	// === Getters and Setters ===

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id != null ? id.toUpperCase() : null;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getDecimals() {
		return decimals;
	}

	public void setDecimals(Integer decimals) {
		this.decimals = decimals;
	}

	public Map<String, String> getProviderSymbols() {
		return providerSymbols;
	}

	public void setProviderSymbols(Map<String, String> providerSymbols) {
		this.providerSymbols = providerSymbols;
	}

	public Boolean getCollectionActive() {
		return collectionActive;
	}

	public void setCollectionActive(Boolean collectionActive) {
		this.collectionActive = collectionActive;
	}

	public String getPrimaryDataSource() {
		return primaryDataSource;
	}

	public void setPrimaryDataSource(String primaryDataSource) {
		this.primaryDataSource = primaryDataSource;
	}

	public List<String> getTimeframes() {
		return timeframes;
	}

	public void setTimeframes(List<String> timeframes) {
		this.timeframes = timeframes;
	}

	public Timestamp getLastCollectedAt() {
		return lastCollectedAt;
	}

	public void setLastCollectedAt(Timestamp lastCollectedAt) {
		this.lastCollectedAt = lastCollectedAt;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return String.format("Symbol[%s: %s (%s) - %s]", id, name, assetType, status);
	}

}
