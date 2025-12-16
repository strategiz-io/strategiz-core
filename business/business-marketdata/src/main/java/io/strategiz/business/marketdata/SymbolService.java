package io.strategiz.business.marketdata;

import com.google.cloud.Timestamp;
import io.strategiz.data.symbol.entity.SymbolEntity;
import io.strategiz.data.symbol.repository.SymbolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Business service for symbol management and cross-exchange mapping.
 * Provides a caching layer over the SymbolRepository for frequently accessed symbols.
 */
@Service
public class SymbolService {

    private static final Logger log = LoggerFactory.getLogger(SymbolService.class);

    private final SymbolRepository symbolRepository;

    // In-memory cache for frequently accessed symbols (canonical symbol -> entity)
    private final Map<String, SymbolEntity> symbolCache = new ConcurrentHashMap<>();

    // Reverse lookup cache: provider + providerSymbol -> canonical symbol
    private final Map<String, String> providerSymbolCache = new ConcurrentHashMap<>();

    @Autowired
    public SymbolService(SymbolRepository symbolRepository) {
        this.symbolRepository = symbolRepository;
    }

    // === Cross-exchange mapping ===

    /**
     * Get the provider-specific symbol for a canonical symbol
     * @param canonicalSymbol The canonical symbol (e.g., "BTC")
     * @param provider The provider name (e.g., "YAHOO", "ALPACA")
     * @return The provider-specific symbol (e.g., "BTC-USD") or canonical if no mapping
     */
    public String getProviderSymbol(String canonicalSymbol, String provider) {
        SymbolEntity symbol = getSymbolMetadata(canonicalSymbol);
        if (symbol != null) {
            return symbol.getProviderSymbol(provider);
        }
        // Fallback: return canonical symbol as-is
        return canonicalSymbol;
    }

    /**
     * Get the canonical symbol for a provider-specific symbol
     * @param providerSymbol The provider-specific symbol (e.g., "BTC-USD")
     * @param provider The provider name (e.g., "YAHOO")
     * @return The canonical symbol (e.g., "BTC") or the provider symbol if not found
     */
    public String getCanonicalSymbol(String providerSymbol, String provider) {
        String cacheKey = provider.toUpperCase() + ":" + providerSymbol;

        // Check cache first
        if (providerSymbolCache.containsKey(cacheKey)) {
            return providerSymbolCache.get(cacheKey);
        }

        // Query repository
        Optional<SymbolEntity> symbol = symbolRepository.findByProviderSymbol(provider, providerSymbol);
        if (symbol.isPresent()) {
            String canonicalSymbol = symbol.get().getId();
            providerSymbolCache.put(cacheKey, canonicalSymbol);
            symbolCache.put(canonicalSymbol, symbol.get());
            return canonicalSymbol;
        }

        // Fallback: return provider symbol as-is (may be canonical already)
        return providerSymbol;
    }

    // === Metadata access ===

    /**
     * Get symbol metadata by canonical symbol
     * @param canonicalSymbol The canonical symbol
     * @return SymbolEntity or null if not found
     */
    public SymbolEntity getSymbolMetadata(String canonicalSymbol) {
        if (canonicalSymbol == null) {
            return null;
        }

        String upperSymbol = canonicalSymbol.toUpperCase();

        // Check cache first
        if (symbolCache.containsKey(upperSymbol)) {
            return symbolCache.get(upperSymbol);
        }

        // Query repository
        Optional<SymbolEntity> symbol = symbolRepository.findById(upperSymbol);
        if (symbol.isPresent()) {
            symbolCache.put(upperSymbol, symbol.get());
            return symbol.get();
        }

        return null;
    }

    /**
     * Get display name for a symbol
     * @param canonicalSymbol The canonical symbol
     * @return Display name or the symbol itself if not found
     */
    public String getDisplayName(String canonicalSymbol) {
        SymbolEntity symbol = getSymbolMetadata(canonicalSymbol);
        if (symbol != null && symbol.getDisplayName() != null) {
            return symbol.getDisplayName();
        }
        return canonicalSymbol;
    }

    /**
     * Get full name for a symbol
     * @param canonicalSymbol The canonical symbol
     * @return Full name or the symbol itself if not found
     */
    public String getName(String canonicalSymbol) {
        SymbolEntity symbol = getSymbolMetadata(canonicalSymbol);
        if (symbol != null && symbol.getName() != null) {
            return symbol.getName();
        }
        return canonicalSymbol;
    }

    /**
     * Check if a symbol is known
     * @param canonicalSymbol The canonical symbol
     * @return true if the symbol exists in the repository
     */
    public boolean isKnownSymbol(String canonicalSymbol) {
        return getSymbolMetadata(canonicalSymbol) != null;
    }

    // === Collection management ===

    /**
     * Get list of canonical symbols configured for a specific data source
     * @param dataSource The data source (e.g., "YAHOO", "ALPACA")
     * @return List of canonical symbols
     */
    public List<String> getSymbolsForCollection(String dataSource) {
        List<SymbolEntity> symbols = symbolRepository.findActiveForCollectionByDataSource(dataSource);
        return symbols.stream()
            .map(SymbolEntity::getId)
            .collect(Collectors.toList());
    }

    /**
     * Get list of provider-formatted symbols for a specific data source
     * @param dataSource The data source (e.g., "YAHOO", "ALPACA")
     * @return List of provider-specific symbols (e.g., ["BTC-USD", "ETH-USD"])
     */
    public List<String> getProviderSymbolsForCollection(String dataSource) {
        List<SymbolEntity> symbols = symbolRepository.findActiveForCollectionByDataSource(dataSource);
        return symbols.stream()
            .map(s -> s.getProviderSymbol(dataSource))
            .collect(Collectors.toList());
    }

    /**
     * Get all symbols active for collection
     * @return List of SymbolEntity objects
     */
    public List<SymbolEntity> getActiveCollectionSymbols() {
        return symbolRepository.findActiveForCollection();
    }

    /**
     * Mark a symbol as collected (update lastCollectedAt timestamp)
     * @param canonicalSymbol The canonical symbol
     * @param timestamp When it was collected
     */
    public void markCollected(String canonicalSymbol, Instant timestamp) {
        Optional<SymbolEntity> optSymbol = symbolRepository.findById(canonicalSymbol.toUpperCase());
        if (optSymbol.isPresent()) {
            SymbolEntity symbol = optSymbol.get();
            symbol.setLastCollectedAt(Timestamp.ofTimeSecondsAndNanos(
                timestamp.getEpochSecond(),
                timestamp.getNano()
            ));
            symbolRepository.save(symbol);

            // Update cache
            symbolCache.put(canonicalSymbol.toUpperCase(), symbol);

            log.debug("Marked {} as collected at {}", canonicalSymbol, timestamp);
        }
    }

    // === Bulk operations ===

    /**
     * Import symbols in bulk (for migration or seeding)
     * @param symbols List of symbols to import
     * @return Number of symbols imported
     */
    public int importSymbols(List<SymbolEntity> symbols) {
        List<SymbolEntity> saved = symbolRepository.saveAll(symbols);

        // Update cache
        for (SymbolEntity symbol : saved) {
            symbolCache.put(symbol.getId(), symbol);
        }

        log.info("Imported {} symbols", saved.size());
        return saved.size();
    }

    /**
     * Get all symbols by asset type
     * @param assetType The asset type (e.g., "STOCK", "CRYPTO")
     * @return List of symbols
     */
    public List<SymbolEntity> getSymbolsByAssetType(String assetType) {
        return symbolRepository.findByAssetType(assetType);
    }

    /**
     * Get all symbols by category
     * @param category The category (e.g., "defi", "payment", "tech")
     * @return List of symbols
     */
    public List<SymbolEntity> getSymbolsByCategory(String category) {
        return symbolRepository.findByCategory(category);
    }

    // === Cache management ===

    /**
     * Clear the in-memory cache
     */
    public void clearCache() {
        symbolCache.clear();
        providerSymbolCache.clear();
        log.info("Symbol cache cleared");
    }

    /**
     * Refresh a specific symbol in the cache
     * @param canonicalSymbol The symbol to refresh
     */
    public void refreshSymbol(String canonicalSymbol) {
        String upperSymbol = canonicalSymbol.toUpperCase();
        symbolCache.remove(upperSymbol);

        // Remove any provider symbol cache entries for this symbol
        providerSymbolCache.entrySet().removeIf(entry -> entry.getValue().equals(upperSymbol));

        // Reload from repository
        getSymbolMetadata(upperSymbol);
    }
}
