package io.strategiz.data.marketdata.repository;

import io.strategiz.data.marketdata.entity.MarketDataEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for market data operations
 * Handles storage and retrieval of historical market data in Firestore
 */
public interface MarketDataRepository {
    
    /**
     * Save a single market data point
     */
    MarketDataEntity save(MarketDataEntity entity);
    
    /**
     * Save multiple market data points in batch
     * More efficient for daily batch jobs
     */
    List<MarketDataEntity> saveAll(List<MarketDataEntity> entities);
    
    /**
     * Find market data by ID
     */
    Optional<MarketDataEntity> findById(String id);
    
    /**
     * Find all market data for a specific symbol
     */
    List<MarketDataEntity> findBySymbol(String symbol);
    
    /**
     * Find market data for a symbol on a specific date
     */
    List<MarketDataEntity> findBySymbolAndDate(String symbol, LocalDate date);
    
    /**
     * Find market data for a symbol within a date range
     */
    List<MarketDataEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate);
    
    /**
     * Find market data for a symbol with specific timeframe
     */
    List<MarketDataEntity> findBySymbolAndTimeframe(String symbol, String timeframe);
    
    /**
     * Find market data for multiple symbols on a specific date
     * Useful for portfolio/watchlist analysis
     */
    List<MarketDataEntity> findBySymbolsAndDate(List<String> symbols, LocalDate date);
    
    /**
     * Get the latest market data for a symbol
     */
    Optional<MarketDataEntity> findLatestBySymbol(String symbol);
    
    /**
     * Get the latest market data for multiple symbols
     */
    List<MarketDataEntity> findLatestBySymbols(List<String> symbols);
    
    /**
     * Delete old market data beyond retention period
     */
    int deleteOlderThan(LocalDate cutoffDate);
    
    /**
     * Count total market data points for a symbol
     */
    long countBySymbol(String symbol);
    
    /**
     * Check if data exists for a symbol on a date
     */
    boolean existsBySymbolAndDate(String symbol, LocalDate date);
    
    /**
     * Get distinct symbols in the database
     */
    List<String> findDistinctSymbols();
    
    /**
     * Get date range of available data for a symbol
     */
    DateRange getDateRangeForSymbol(String symbol);
    
    /**
     * Simple date range holder
     */
    class DateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;
        
        public DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
        
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
    }
}