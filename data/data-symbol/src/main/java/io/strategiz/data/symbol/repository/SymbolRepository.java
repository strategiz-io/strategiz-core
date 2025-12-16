package io.strategiz.data.symbol.repository;

import io.strategiz.data.symbol.entity.SymbolEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for symbol reference data operations.
 * Symbols are system-wide (not user-scoped).
 */
public interface SymbolRepository {

    /**
     * Find symbol by canonical symbol (document ID)
     * @param canonicalSymbol The canonical symbol (e.g., "BTC", "AAPL")
     * @return Optional containing the symbol if found
     */
    Optional<SymbolEntity> findById(String canonicalSymbol);

    /**
     * Find symbol by provider-specific symbol
     * @param provider The provider name (e.g., "YAHOO", "ALPACA", "COINBASE")
     * @param providerSymbol The provider-specific symbol (e.g., "BTC-USD", "BTCUSDT")
     * @return Optional containing the symbol if found
     */
    Optional<SymbolEntity> findByProviderSymbol(String provider, String providerSymbol);

    /**
     * Find all symbols of a specific asset type
     * @param assetType The asset type (e.g., "STOCK", "CRYPTO", "ETF")
     * @return List of symbols matching the asset type
     */
    List<SymbolEntity> findByAssetType(String assetType);

    /**
     * Find all symbols that are active for data collection
     * @return List of symbols with collectionActive=true and status=ACTIVE
     */
    List<SymbolEntity> findActiveForCollection();

    /**
     * Find all symbols active for collection with a specific data source
     * @param dataSource The primary data source (e.g., "YAHOO", "ALPACA")
     * @return List of symbols configured for that data source
     */
    List<SymbolEntity> findActiveForCollectionByDataSource(String dataSource);

    /**
     * Find all symbols
     * @return List of all active symbols
     */
    List<SymbolEntity> findAll();

    /**
     * Save a symbol
     * @param symbol The symbol entity to save
     * @return The saved symbol
     */
    SymbolEntity save(SymbolEntity symbol);

    /**
     * Save multiple symbols in batch
     * @param symbols List of symbols to save
     * @return List of saved symbols
     */
    List<SymbolEntity> saveAll(List<SymbolEntity> symbols);

    /**
     * Delete a symbol (soft delete)
     * @param canonicalSymbol The canonical symbol to delete
     * @return true if deleted, false if not found
     */
    boolean delete(String canonicalSymbol);

    /**
     * Check if a symbol exists
     * @param canonicalSymbol The canonical symbol to check
     * @return true if exists and active
     */
    boolean exists(String canonicalSymbol);

    /**
     * Count all active symbols
     * @return Count of active symbols
     */
    long count();

    /**
     * Find symbols by category
     * @param category The category (e.g., "defi", "payment", "tech")
     * @return List of symbols in that category
     */
    List<SymbolEntity> findByCategory(String category);
}
