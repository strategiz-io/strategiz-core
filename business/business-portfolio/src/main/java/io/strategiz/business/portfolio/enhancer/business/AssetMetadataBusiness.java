package io.strategiz.business.portfolio.enhancer.business;

import io.strategiz.business.portfolio.enhancer.mapper.UniversalSymbolMapper;
import io.strategiz.business.portfolio.enhancer.model.AssetMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Business component for managing asset metadata.
 * Provides human-readable names, descriptions, and metadata for assets.
 */
@Component
public class AssetMetadataBusiness {
    
    private final UniversalSymbolMapper universalMapper;
    
    @Autowired
    public AssetMetadataBusiness(UniversalSymbolMapper universalMapper) {
        this.universalMapper = universalMapper;
    }
    
    /**
     * Get metadata for a standard symbol
     * @param symbol Standard symbol (e.g., "BTC", "ETH")
     * @return Asset metadata with name, description, etc.
     */
    public AssetMetadata getMetadata(String symbol) {
        return universalMapper.getMetadata(symbol);
    }
    
    /**
     * Get human-readable name for a symbol
     * @param symbol Standard symbol
     * @return Human-readable name
     */
    public String getAssetName(String symbol) {
        return universalMapper.getName(symbol);
    }
    
    /**
     * Check if we have metadata for a symbol
     * @param symbol Symbol to check
     * @return true if metadata exists
     */
    public boolean hasMetadata(String symbol) {
        return universalMapper.isKnownSymbol(symbol);
    }
    
    /**
     * Get display name for an asset (includes symbol)
     * @param symbol Standard symbol
     * @return Display name like "Bitcoin (BTC)"
     */
    public String getDisplayName(String symbol) {
        AssetMetadata metadata = getMetadata(symbol);
        return metadata != null ? metadata.getDisplayName() : symbol;
    }
    
    /**
     * Determine if an asset can be staked
     * @param symbol Standard symbol
     * @return true if asset supports staking
     */
    public boolean isStakeable(String symbol) {
        AssetMetadata metadata = getMetadata(symbol);
        return metadata != null && metadata.isStakeable();
    }
    
    /**
     * Get asset type (crypto, fiat, stock, etc.)
     * @param symbol Standard symbol
     * @return Asset type string
     */
    public String getAssetType(String symbol) {
        AssetMetadata metadata = getMetadata(symbol);
        return metadata != null ? metadata.getAssetType() : "unknown";
    }
    
    /**
     * Get asset category (payment, platform, defi, etc.)
     * @param symbol Standard symbol
     * @return Asset category string
     */
    public String getAssetCategory(String symbol) {
        AssetMetadata metadata = getMetadata(symbol);
        return metadata != null ? metadata.getCategory() : "unknown";
    }
}