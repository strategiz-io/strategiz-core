package io.strategiz.business.provider.kraken.helper;

import io.strategiz.business.provider.kraken.constants.KrakenConstants;
import io.strategiz.business.provider.kraken.exception.KrakenProviderErrorDetails;
import io.strategiz.business.provider.kraken.enrichment.model.EnrichedKrakenData;
import io.strategiz.data.provider.entity.ProviderHoldingsEntity;
import io.strategiz.framework.exception.StrategizException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Component responsible for transforming Kraken API responses into ProviderHoldingsEntity.
 * Handles data normalization, calculation of portfolio metrics, and data structure conversion.
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
@Component
public class KrakenDataTransformer {
    
    private static final Logger log = LoggerFactory.getLogger(KrakenDataTransformer.class);
    private static final String MODULE_NAME = "business-provider-kraken";
    
    /**
     * Transform Kraken API data into ProviderHoldingsEntity
     * 
     * @param userId User ID for audit fields
     * @param balanceResponse Raw balance response from Kraken API
     * @param tradesResponse Raw trades response from Kraken API
     * @param currentPrices Current market prices for assets
     * @return Transformed ProviderHoldingsEntity ready for storage
     */
    public ProviderHoldingsEntity transformKrakenData(String userId,
                                                  Map<String, Object> balanceResponse,
                                                  Map<String, Object> tradesResponse,
                                                  Map<String, BigDecimal> currentPrices) {
        
        log.debug("Transforming Kraken data for user: {}", userId);
        
        try {
            // Create entity - ProviderHoldingsEntity only contains holdings data
            // Provider metadata (name, type, category) is stored in PortfolioProviderEntity
            ProviderHoldingsEntity data = new ProviderHoldingsEntity(KrakenConstants.PROVIDER_ID, userId);
            
            // Extract and process balances
            Map<String, Object> balances = extractBalances(balanceResponse);
            data.setBalances(balances);
            
            // Transform balances into holdings with valuation
            List<ProviderHoldingsEntity.Holding> holdings = createHoldings(balances, currentPrices, tradesResponse);
            data.setHoldings(holdings);
            
            // Calculate portfolio totals
            BigDecimal totalValue = calculateTotalValue(holdings, balances);
            data.setTotalValue(totalValue);
            
            // Extract cash balance
            BigDecimal cashBalance = extractCashBalance(balances);
            data.setCashBalance(cashBalance);
            
            // Calculate profit/loss if we have trade history
            calculateProfitLoss(data, tradesResponse);
            
            // Set sync metadata
            data.setSyncStatus("success");
            data.setLastUpdatedAt(Instant.now());
            
            log.debug("Transformed data: {} holdings, total value: {}, cash: {}", 
                     holdings.size(), totalValue, cashBalance);
            
            return data;
            
        } catch (Exception e) {
            log.error("Error transforming Kraken data for user: {}", userId, e);
            throw new StrategizException(
                KrakenProviderErrorDetails.DATA_TRANSFORMATION_FAILED,
                MODULE_NAME,
                userId,
                "transformation",
                e.getMessage()
            );
        }
    }
    
    /**
     * Extract balances from Kraken API response
     */
    private Map<String, Object> extractBalances(Map<String, Object> response) {
        if (response != null && response.containsKey("result")) {
            Object result = response.get("result");
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
        }
        return new HashMap<>();
    }
    
    /**
     * Create holdings from balance data and current prices
     */
    private List<ProviderHoldingsEntity.Holding> createHoldings(Map<String, Object> balances,
                                                            Map<String, BigDecimal> currentPrices,
                                                            Map<String, Object> tradesResponse) {
        
        List<ProviderHoldingsEntity.Holding> holdings = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : balances.entrySet()) {
            String assetCode = entry.getKey();
            
            // Skip cash assets in holdings (they're tracked separately)
            if (KrakenConstants.CASH_ASSETS.contains(assetCode)) {
                continue;
            }
            
            try {
                BigDecimal quantity = new BigDecimal(entry.getValue().toString());
                
                // Only include assets with non-zero balance
                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    ProviderHoldingsEntity.Holding holding = new ProviderHoldingsEntity.Holding();
                    
                    // Normalize asset name at storage time for consistency
                    String normalizedAsset = normalizeAssetName(assetCode);
                    holding.setAsset(normalizedAsset);
                    holding.setName(getAssetFullName(normalizedAsset));
                    holding.setQuantity(quantity);
                    
                    // Set current price and value
                    BigDecimal currentPrice = currentPrices.getOrDefault(assetCode, BigDecimal.ZERO);
                    holding.setCurrentPrice(currentPrice);
                    
                    BigDecimal currentValue = quantity.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
                    holding.setCurrentValue(currentValue);
                    
                    // Calculate cost basis from trade history if available
                    BigDecimal costBasis = calculateCostBasis(assetCode, quantity, tradesResponse);
                    holding.setCostBasis(costBasis);
                    
                    // Calculate profit/loss
                    if (costBasis != null && costBasis.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal profitLoss = currentValue.subtract(costBasis);
                        holding.setProfitLoss(profitLoss);
                        
                        BigDecimal profitLossPercent = profitLoss
                            .divide(costBasis, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                        holding.setProfitLossPercent(profitLossPercent);
                    }
                    
                    holdings.add(holding);
                    
                    log.debug("Created holding: {} qty={} price={} value={}", 
                             normalizedAsset, quantity, currentPrice, currentValue);
                }
                
            } catch (Exception e) {
                log.warn("Error processing holding for asset {}: {}", assetCode, e.getMessage());
            }
        }
        
        // Sort holdings by value descending
        holdings.sort((h1, h2) -> h2.getCurrentValue().compareTo(h1.getCurrentValue()));
        
        return holdings;
    }
    
    /**
     * Normalize Kraken asset code to standard symbol
     */
    private String normalizeAssetName(String krakenAsset) {
        // Check if we have a mapping for this asset
        String mapped = KrakenConstants.ASSET_MAPPING.get(krakenAsset);
        if (mapped != null) {
            return mapped;
        }
        
        // Apply general normalization rules
        if (krakenAsset.startsWith("X") && krakenAsset.length() == 4) {
            return krakenAsset.substring(1); // XETH -> ETH
        }
        if (krakenAsset.startsWith("Z") && krakenAsset.length() == 4) {
            return krakenAsset.substring(1); // ZUSD -> USD
        }
        
        return krakenAsset;
    }
    
    /**
     * Get full name for asset (for display purposes)
     */
    private String getAssetFullName(String asset) {
        // Common crypto asset names
        Map<String, String> assetNames = Map.of(
            "BTC", "Bitcoin",
            "ETH", "Ethereum",
            "XRP", "Ripple",
            "LTC", "Litecoin",
            "ADA", "Cardano",
            "DOT", "Polkadot",
            "LINK", "Chainlink",
            "USD", "US Dollar",
            "EUR", "Euro"
        );
        
        return assetNames.getOrDefault(asset, asset);
    }
    
    /**
     * Calculate total portfolio value including cash
     */
    private BigDecimal calculateTotalValue(List<ProviderHoldingsEntity.Holding> holdings,
                                          Map<String, Object> balances) {
        
        // Sum up holding values
        BigDecimal holdingsValue = holdings.stream()
            .map(ProviderHoldingsEntity.Holding::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Add cash balances
        BigDecimal cashValue = extractCashBalance(balances);
        
        return holdingsValue.add(cashValue).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Extract total cash balance from all fiat currencies
     */
    private BigDecimal extractCashBalance(Map<String, Object> balances) {
        BigDecimal totalCash = BigDecimal.ZERO;
        
        for (String cashAsset : KrakenConstants.CASH_ASSETS) {
            if (balances.containsKey(cashAsset)) {
                try {
                    BigDecimal amount = new BigDecimal(balances.get(cashAsset).toString());
                    
                    // Convert non-USD to USD (simplified - would need exchange rates)
                    if (cashAsset.equals("ZUSD") || cashAsset.equals("USD")) {
                        totalCash = totalCash.add(amount);
                    } else if (cashAsset.equals("ZEUR") || cashAsset.equals("EUR")) {
                        // Approximate EUR to USD conversion
                        totalCash = totalCash.add(amount.multiply(new BigDecimal("1.10")));
                    }
                    // Add other currency conversions as needed
                    
                } catch (Exception e) {
                    log.debug("Error parsing cash balance for {}: {}", cashAsset, e.getMessage());
                }
            }
        }
        
        return totalCash.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate cost basis from trade history
     */
    private BigDecimal calculateCostBasis(String asset, BigDecimal currentQuantity,
                                         Map<String, Object> tradesResponse) {
        
        if (tradesResponse == null || !tradesResponse.containsKey("result")) {
            return null;
        }
        
        try {
            Map<String, Object> result = (Map<String, Object>) tradesResponse.get("result");
            Map<String, Object> trades = (Map<String, Object>) result.get("trades");
            
            if (trades == null || trades.isEmpty()) {
                return null;
            }
            
            // Calculate weighted average cost
            BigDecimal totalCost = BigDecimal.ZERO;
            BigDecimal totalQuantity = BigDecimal.ZERO;
            
            for (Object tradeObj : trades.values()) {
                if (tradeObj instanceof Map) {
                    Map<String, Object> trade = (Map<String, Object>) tradeObj;
                    
                    // Check if trade is for this asset
                    String pair = (String) trade.get("pair");
                    if (pair != null && pair.contains(normalizeAssetName(asset))) {
                        String type = (String) trade.get("type");
                        BigDecimal volume = new BigDecimal(trade.get("vol").toString());
                        BigDecimal price = new BigDecimal(trade.get("price").toString());
                        BigDecimal cost = new BigDecimal(trade.get("cost").toString());
                        
                        if ("buy".equals(type)) {
                            totalQuantity = totalQuantity.add(volume);
                            totalCost = totalCost.add(cost);
                        } else if ("sell".equals(type)) {
                            // Reduce position
                            totalQuantity = totalQuantity.subtract(volume);
                        }
                    }
                }
            }
            
            // Calculate weighted average if we have quantity
            if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                // Use current quantity for cost basis calculation
                BigDecimal avgPrice = totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP);
                return currentQuantity.multiply(avgPrice).setScale(2, RoundingMode.HALF_UP);
            }
            
        } catch (Exception e) {
            log.debug("Error calculating cost basis for {}: {}", asset, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Transform enriched Kraken data into ProviderHoldingsEntity.
     * This method uses pre-enriched data from the enrichment service.
     * 
     * @param userId User ID for audit fields
     * @param enrichedData Enriched data from the enrichment service
     * @param tradesResponse Raw trades response for transaction history
     * @return Transformed ProviderHoldingsEntity ready for storage
     */
    public ProviderHoldingsEntity transformEnrichedData(String userId,
                                                    EnrichedKrakenData enrichedData,
                                                    Map<String, Object> tradesResponse) {
        
        log.debug("Transforming enriched Kraken data for user: {}", userId);
        
        try {
            // Create entity - ProviderHoldingsEntity only contains holdings data
            // Provider metadata (name, type, category) is stored in PortfolioProviderEntity
            ProviderHoldingsEntity data = new ProviderHoldingsEntity(KrakenConstants.PROVIDER_ID, userId);

            // Store raw balances for reference
            Map<String, Object> rawBalances = new HashMap<>();
            for (Map.Entry<String, EnrichedKrakenData.AssetInfo> entry : enrichedData.getAssetInfo().entrySet()) {
                EnrichedKrakenData.AssetInfo info = entry.getValue();
                if (info.getOriginalSymbol() != null) {
                    rawBalances.put(info.getOriginalSymbol(), info.getQuantity());
                } else {
                    rawBalances.put(info.getNormalizedSymbol(), info.getQuantity());
                }
            }
            data.setBalances(rawBalances);
            
            // Transform enriched asset info into holdings
            List<ProviderHoldingsEntity.Holding> holdings = createEnrichedHoldings(enrichedData, tradesResponse);
            data.setHoldings(holdings);
            
            // Calculate portfolio totals
            BigDecimal totalValue = enrichedData.getTotalValue();
            if (totalValue == null) {
                totalValue = calculateTotalValueFromHoldings(holdings, enrichedData);
            }
            data.setTotalValue(totalValue);
            
            // Extract cash balance
            BigDecimal cashBalance = enrichedData.getCashBalance();
            if (cashBalance == null) {
                cashBalance = calculateCashBalance(enrichedData);
            }
            data.setCashBalance(cashBalance);
            
            // Calculate profit/loss
            calculateProfitLoss(data, tradesResponse);
            
            // Set sync metadata
            data.setSyncStatus("success");
            data.setLastUpdatedAt(Instant.now());
            
            log.debug("Transformed enriched data: {} holdings, total value: {}, cash: {}", 
                     holdings.size(), totalValue, cashBalance);
            
            return data;
            
        } catch (Exception e) {
            log.error("Error transforming enriched Kraken data for user: {}", userId, e);
            throw new StrategizException(
                KrakenProviderErrorDetails.DATA_TRANSFORMATION_FAILED,
                MODULE_NAME,
                userId,
                "transformation",
                e.getMessage()
            );
        }
    }
    
    /**
     * Create holdings from enriched asset information
     */
    private List<ProviderHoldingsEntity.Holding> createEnrichedHoldings(EnrichedKrakenData enrichedData,
                                                                    Map<String, Object> tradesResponse) {
        
        List<ProviderHoldingsEntity.Holding> holdings = new ArrayList<>();
        
        for (EnrichedKrakenData.AssetInfo assetInfo : enrichedData.getAssetInfo().values()) {
            // Skip cash assets in holdings
            if (assetInfo.isCash()) {
                continue;
            }
            
            // Skip zero balances
            if (assetInfo.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            
            ProviderHoldingsEntity.Holding holding = new ProviderHoldingsEntity.Holding();
            
            // Basic asset information
            holding.setAsset(assetInfo.getNormalizedSymbol());
            holding.setName(assetInfo.getFullName());
            holding.setQuantity(assetInfo.getQuantity());
            
            // Pricing information
            holding.setCurrentPrice(assetInfo.getCurrentPrice());
            holding.setCurrentValue(assetInfo.getCurrentValue());
            holding.setPriceChange24h(assetInfo.getPriceChange24h());
            
            // Enrichment fields
            holding.setAssetType(assetInfo.getAssetType());
            holding.setCategory(assetInfo.getCategory());
            holding.setMarketCapRank(assetInfo.getMarketCapRank());
            holding.setIsStaked(assetInfo.isStaked());
            holding.setStakingAPR(assetInfo.getStakingAPR());
            holding.setOriginalSymbol(assetInfo.getOriginalSymbol());
            
            // Calculate cost basis and average buy price from trade history if available
            String assetForCostBasis = assetInfo.getOriginalSymbol() != null ? 
                assetInfo.getOriginalSymbol() : assetInfo.getNormalizedSymbol();
            BigDecimal costBasis = calculateCostBasis(assetForCostBasis, assetInfo.getQuantity(), tradesResponse);
            holding.setCostBasis(costBasis);
            
            // Calculate average buy price (cost basis / quantity)
            if (costBasis != null && costBasis.compareTo(BigDecimal.ZERO) > 0 && 
                assetInfo.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal avgBuyPrice = costBasis.divide(assetInfo.getQuantity(), 4, RoundingMode.HALF_UP);
                assetInfo.setAverageBuyPrice(avgBuyPrice);
                holding.setAverageBuyPrice(avgBuyPrice);
            }
            
            // Calculate profit/loss
            if (costBasis != null && costBasis.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal profitLoss = assetInfo.getCurrentValue().subtract(costBasis);
                holding.setProfitLoss(profitLoss);
                
                BigDecimal profitLossPercent = profitLoss
                    .divide(costBasis, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                holding.setProfitLossPercent(profitLossPercent);
            }
            
            holdings.add(holding);
            
            log.debug("Created enriched holding: {} ({}) qty={} value={} staked={}", 
                     holding.getAsset(), holding.getAssetType(), 
                     holding.getQuantity(), holding.getCurrentValue(), 
                     holding.getIsStaked());
        }
        
        // Sort holdings by value descending
        holdings.sort((h1, h2) -> h2.getCurrentValue().compareTo(h1.getCurrentValue()));
        
        return holdings;
    }
    
    /**
     * Calculate total value from holdings and cash
     */
    private BigDecimal calculateTotalValueFromHoldings(List<ProviderHoldingsEntity.Holding> holdings,
                                                       EnrichedKrakenData enrichedData) {
        // Sum up holding values
        BigDecimal holdingsValue = holdings.stream()
            .map(ProviderHoldingsEntity.Holding::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Add cash balance
        BigDecimal cashValue = calculateCashBalance(enrichedData);
        
        return holdingsValue.add(cashValue).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate cash balance from enriched data
     */
    private BigDecimal calculateCashBalance(EnrichedKrakenData enrichedData) {
        BigDecimal totalCash = BigDecimal.ZERO;
        
        for (EnrichedKrakenData.AssetInfo assetInfo : enrichedData.getAssetInfo().values()) {
            if (assetInfo.isCash()) {
                // Convert to USD if needed (using price as exchange rate)
                BigDecimal usdValue = assetInfo.getQuantity().multiply(
                    assetInfo.getCurrentPrice() != null ? assetInfo.getCurrentPrice() : BigDecimal.ONE
                );
                totalCash = totalCash.add(usdValue);
            }
        }
        
        return totalCash.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate overall portfolio profit/loss
     */
    private void calculateProfitLoss(ProviderHoldingsEntity data, Map<String, Object> tradesResponse) {
        
        // Calculate total P&L from holdings
        BigDecimal totalProfitLoss = BigDecimal.ZERO;
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        
        if (data.getHoldings() != null) {
            for (ProviderHoldingsEntity.Holding holding : data.getHoldings()) {
                if (holding.getProfitLoss() != null) {
                    totalProfitLoss = totalProfitLoss.add(holding.getProfitLoss());
                }
                if (holding.getCostBasis() != null) {
                    totalCostBasis = totalCostBasis.add(holding.getCostBasis());
                }
            }
        }
        
        data.setTotalProfitLoss(totalProfitLoss);
        
        // Calculate percentage if we have cost basis
        if (totalCostBasis.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profitLossPercent = totalProfitLoss
                .divide(totalCostBasis, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            data.setTotalProfitLossPercent(profitLossPercent);
        }
        
        // Day change would require historical data - set to zero for now
        data.setDayChange(BigDecimal.ZERO);
        data.setDayChangePercent(BigDecimal.ZERO);
    }
}