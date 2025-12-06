package io.strategiz.service.portfolio.service;

import io.strategiz.business.portfolio.model.AssetCategory;
import io.strategiz.business.portfolio.model.CategoryAllocation;
import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.entity.ProviderStatus;
import io.strategiz.data.provider.repository.ReadPortfolioSummaryRepository;
import io.strategiz.data.provider.repository.ReadProviderDataRepository;
import io.strategiz.data.provider.repository.ReadProviderIntegrationRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.portfolio.constants.ServicePortfolioConstants;
import io.strategiz.service.portfolio.model.response.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for aggregating portfolio data across all connected providers.
 * Single Responsibility: Aggregates data from multiple providers into unified views.
 */
@Service
public class PortfolioAggregatorService {
    
    private static final Logger log = LoggerFactory.getLogger(PortfolioAggregatorService.class);

    private final ReadProviderDataRepository providerDataRepository;
    private final ReadProviderIntegrationRepository providerIntegrationRepository;
    private final PortfolioProviderService portfolioProviderService;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private ReadPortfolioSummaryRepository readPortfolioSummaryRepository;

    @Autowired
    public PortfolioAggregatorService(
            ReadProviderDataRepository providerDataRepository,
            ReadProviderIntegrationRepository providerIntegrationRepository,
            PortfolioProviderService portfolioProviderService,
            UserRepository userRepository) {
        this.providerDataRepository = providerDataRepository;
        this.providerIntegrationRepository = providerIntegrationRepository;
        this.portfolioProviderService = portfolioProviderService;
        this.userRepository = userRepository;
    }
    
    /**
     * Get lightweight portfolio summary for dashboard
     * 
     * @param userId User ID
     * @return Summary with total value, day change, and top holdings
     */
    public PortfolioSummaryResponse getPortfolioSummary(String userId) {
        log.debug("Fetching portfolio summary for user: {}", userId);
        
        PortfolioSummaryResponse response = new PortfolioSummaryResponse();
        
        try {
            // Get all provider data
            List<ProviderDataEntity> allProviderData = providerDataRepository.getAllProviderData(userId);
            
            // Calculate totals
            BigDecimal totalValue = BigDecimal.ZERO;
            BigDecimal totalDayChange = BigDecimal.ZERO;
            BigDecimal totalProfitLoss = BigDecimal.ZERO;
            List<PortfolioPositionResponse> allPositions = new ArrayList<>();
            int connectedProviders = 0;
            
            for (ProviderDataEntity providerData : allProviderData) {
                if (providerData.getTotalValue() != null) {
                    totalValue = totalValue.add(providerData.getTotalValue());
                    connectedProviders++;
                }
                if (providerData.getDayChange() != null) {
                    totalDayChange = totalDayChange.add(providerData.getDayChange());
                }
                if (providerData.getTotalProfitLoss() != null) {
                    totalProfitLoss = totalProfitLoss.add(providerData.getTotalProfitLoss());
                }
                
                // Collect all positions
                if (providerData.getHoldings() != null) {
                    for (ProviderDataEntity.Holding holding : providerData.getHoldings()) {
                        allPositions.add(mapHoldingToPosition(holding, providerData.getProviderId()));
                    }
                }
            }
            
            // Sort and limit top positions
            allPositions.sort((a, b) -> {
                BigDecimal valueA = a.getCurrentValue() != null ? a.getCurrentValue() : BigDecimal.ZERO;
                BigDecimal valueB = b.getCurrentValue() != null ? b.getCurrentValue() : BigDecimal.ZERO;
                return valueB.compareTo(valueA);
            });
            
            List<PortfolioPositionResponse> topPositions = allPositions.stream()
                .limit(ServicePortfolioConstants.DEFAULT_TOP_HOLDINGS_LIMIT)
                .collect(Collectors.toList());
            
            // Calculate percentages
            BigDecimal dayChangePercent = calculatePercentChange(totalDayChange, totalValue);
            BigDecimal totalProfitLossPercent = calculateProfitLossPercent(totalProfitLoss, totalValue);
            
            // Build response
            response.setTotalValue(totalValue);
            response.setDayChange(totalDayChange);
            response.setDayChangePercent(dayChangePercent);
            response.setTotalProfitLoss(totalProfitLoss);
            response.setTotalProfitLossPercent(totalProfitLossPercent);
            response.setConnectedProviders(connectedProviders);
            response.setTotalPositions(allPositions.size());
            response.setTopPositions(topPositions);
            response.setLastUpdated(System.currentTimeMillis());
            
            log.info("Portfolio summary for user {}: total value: {}, providers: {}", 
                    userId, totalValue, connectedProviders);
            
        } catch (Exception e) {
            log.error("Error creating portfolio summary for user {}: {}", userId, e.getMessage(), e);
            // Return empty summary on error
            response.setTotalValue(BigDecimal.ZERO);
            response.setDayChange(BigDecimal.ZERO);
            response.setConnectedProviders(0);
            response.setTopPositions(new ArrayList<>());
        }
        
        return response;
    }
    
    /**
     * Get complete portfolio overview for portfolio page
     * 
     * @param userId User ID
     * @return Full portfolio data including all providers and holdings
     */
    public PortfolioOverviewResponse getPortfolioOverview(String userId) {
        log.debug("Fetching portfolio overview for user: {}", userId);
        
        PortfolioOverviewResponse response = new PortfolioOverviewResponse();
        
        try {
            // Get all provider integrations to see what's connected
            List<ProviderIntegrationEntity> integrations = providerIntegrationRepository.findByUserId(userId);
            log.info("User {} has {} provider integrations", userId, integrations.size());

            // Read stored provider data for all connected providers
            // Data is synced via service-provider's sync endpoint, not fetched real-time here
            List<ProviderDataEntity> allProviderData = new ArrayList<>();

            // Get connected provider IDs
            List<String> connectedProviderIds = integrations.stream()
                .filter(i -> ProviderStatus.CONNECTED.getValue().equals(i.getStatus()))
                .map(ProviderIntegrationEntity::getProviderId)
                .collect(Collectors.toList());

            log.info("Connected providers for user {}: {}", userId, connectedProviderIds);

            // Fetch stored data for each connected provider
            for (String providerId : connectedProviderIds) {
                try {
                    ProviderDataEntity providerData = providerDataRepository.getProviderData(userId, providerId);
                    if (providerData != null && providerData.getTotalValue() != null) {
                        allProviderData.add(providerData);
                        log.info("Added {} data with total value: {}", providerId, providerData.getTotalValue());
                    } else {
                        log.debug("No stored data found for provider {} user {}", providerId, userId);
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch {} data for user {}: {}", providerId, userId, e.getMessage());
                }
            }

            // Read pre-computed totals from portfolio_summary (same source as dashboard)
            // This ensures consistency between dashboard and portfolio views
            BigDecimal totalValue = BigDecimal.ZERO;
            BigDecimal totalDayChange = BigDecimal.ZERO;
            BigDecimal totalProfitLoss = BigDecimal.ZERO;
            BigDecimal totalCashBalance = BigDecimal.ZERO;
            boolean usedPrecomputedTotals = false;

            if (readPortfolioSummaryRepository != null) {
                try {
                    PortfolioSummaryEntity summary = readPortfolioSummaryRepository.getPortfolioSummary(userId);
                    if (summary != null) {
                        totalValue = summary.getTotalValue() != null ? summary.getTotalValue() : BigDecimal.ZERO;
                        totalDayChange = summary.getDayChange() != null ? summary.getDayChange() : BigDecimal.ZERO;
                        totalProfitLoss = summary.getTotalReturn() != null ? summary.getTotalReturn() : BigDecimal.ZERO;
                        // Note: totalCashBalance is not in portfolio_summary, will be computed from provider data
                        usedPrecomputedTotals = true;
                        log.debug("Using pre-computed totals from portfolio_summary for user {}: totalValue={}", userId, totalValue);
                    }
                } catch (Exception e) {
                    log.warn("Could not read portfolio_summary for user {}, will compute totals: {}", userId, e.getMessage());
                }
            }

            // Process providers
            List<PortfolioOverviewResponse.ProviderSummary> providerSummaries = new ArrayList<>();
            List<PortfolioPositionResponse> allPositions = new ArrayList<>();
            
            // Process each provider's data
            for (ProviderDataEntity providerData : allProviderData) {
                PortfolioOverviewResponse.ProviderSummary summary = new PortfolioOverviewResponse.ProviderSummary();
                summary.setProviderId(providerData.getProviderId());
                summary.setProviderName(providerData.getProviderName());

                // Use entity values with fallback to helper methods for legacy data
                String providerId = providerData.getProviderId();
                String entityProviderType = providerData.getProviderType();
                String entityProviderCategory = providerData.getProviderCategory();
                String resolvedProviderType = entityProviderType != null ? entityProviderType : getProviderType(providerId);
                String resolvedProviderCategory = entityProviderCategory != null ? entityProviderCategory : getProviderCategory(providerId);

                log.info("Provider {}: entityType={}, entityCategory={}, resolvedType={}, resolvedCategory={}",
                    providerId, entityProviderType, entityProviderCategory, resolvedProviderType, resolvedProviderCategory);

                summary.setProviderType(resolvedProviderType);
                summary.setProviderCategory(resolvedProviderCategory);
                summary.setConnected(true);
                summary.setTotalValue(providerData.getTotalValue());
                summary.setDayChange(providerData.getDayChange());
                summary.setCashBalance(providerData.getCashBalance());
                summary.setSyncStatus(providerData.getSyncStatus());
                
                if (providerData.getLastUpdatedAt() != null) {
                    summary.setLastSynced(providerData.getLastUpdatedAt().toEpochMilli());
                }
                
                // Count positions
                int positionCount = providerData.getHoldings() != null ? providerData.getHoldings().size() : 0;
                summary.setPositionCount(positionCount);
                
                // Add to positions list
                if (providerData.getHoldings() != null) {
                    for (ProviderDataEntity.Holding holding : providerData.getHoldings()) {
                        allPositions.add(mapHoldingToPosition(holding, providerData.getProviderId()));
                    }
                }
                
                providerSummaries.add(summary);

                // Aggregate totals only if we didn't get pre-computed values from portfolio_summary
                // This ensures consistency with dashboard which reads from portfolio_summary
                if (!usedPrecomputedTotals) {
                    if (providerData.getTotalValue() != null) {
                        totalValue = totalValue.add(providerData.getTotalValue());
                    }
                    if (providerData.getDayChange() != null) {
                        totalDayChange = totalDayChange.add(providerData.getDayChange());
                    }
                    if (providerData.getTotalProfitLoss() != null) {
                        totalProfitLoss = totalProfitLoss.add(providerData.getTotalProfitLoss());
                    }
                }
                // Always compute cash balance from provider data (not in portfolio_summary)
                if (providerData.getCashBalance() != null) {
                    totalCashBalance = totalCashBalance.add(providerData.getCashBalance());
                }
            }
            
            // Add disconnected providers
            for (ProviderIntegrationEntity integration : integrations) {
                boolean hasData = providerSummaries.stream()
                    .anyMatch(p -> p.getProviderId().equals(integration.getProviderId()));

                if (!hasData && ProviderStatus.CONNECTED.getValue().equals(integration.getStatus())) {
                    PortfolioOverviewResponse.ProviderSummary summary = new PortfolioOverviewResponse.ProviderSummary();
                    summary.setProviderId(integration.getProviderId());
                    summary.setProviderName(getProviderDisplayName(integration.getProviderId()));
                    summary.setProviderType(getProviderType(integration.getProviderId()));
                    summary.setProviderCategory(getProviderCategory(integration.getProviderId()));
                    summary.setConnected(false);
                    summary.setTotalValue(BigDecimal.ZERO);
                    summary.setPositionCount(0);
                    summary.setSyncStatus("disconnected");
                    providerSummaries.add(summary);
                }
            }
            
            // Sort positions by value
            allPositions.sort((a, b) -> {
                BigDecimal valueA = a.getCurrentValue() != null ? a.getCurrentValue() : BigDecimal.ZERO;
                BigDecimal valueB = b.getCurrentValue() != null ? b.getCurrentValue() : BigDecimal.ZERO;
                return valueB.compareTo(valueA);
            });
            
            // Calculate percentages
            BigDecimal dayChangePercent = calculatePercentChange(totalDayChange, totalValue);
            BigDecimal totalProfitLossPercent = calculateProfitLossPercent(totalProfitLoss, totalValue);
            
            // Calculate asset allocation
            PortfolioOverviewResponse.AssetAllocation allocation = calculateAssetAllocation(allPositions, totalValue, totalCashBalance);
            
            // Build response
            response.setTotalValue(totalValue);
            response.setDayChange(totalDayChange);
            response.setDayChangePercent(dayChangePercent);
            response.setTotalProfitLoss(totalProfitLoss);
            response.setTotalProfitLossPercent(totalProfitLossPercent);
            response.setTotalCashBalance(totalCashBalance);
            response.setProviders(providerSummaries);
            response.setAllPositions(allPositions);
            response.setAssetAllocation(allocation);
            response.setLastUpdated(System.currentTimeMillis());
            
            log.info("Portfolio overview for user {}: {} providers, {} positions, total value: {}", 
                    userId, providerSummaries.size(), allPositions.size(), totalValue);
            
        } catch (Exception e) {
            log.error("Error fetching portfolio overview for user {}: {}", userId, e.getMessage(), e);
            // Return empty overview on error
            response.setTotalValue(BigDecimal.ZERO);
            response.setProviders(new ArrayList<>());
            response.setAllPositions(new ArrayList<>());
        }
        
        return response;
    }

    /**
     * Map a Holding entity to PortfolioPositionResponse
     */
    private PortfolioPositionResponse mapHoldingToPosition(ProviderDataEntity.Holding holding, String providerId) {
        PortfolioPositionResponse position = new PortfolioPositionResponse();
        
        position.setSymbol(holding.getAsset());
        position.setName(holding.getName());
        position.setQuantity(holding.getQuantity());
        position.setCurrentPrice(holding.getCurrentPrice());
        position.setCurrentValue(holding.getCurrentValue());
        position.setCostBasis(holding.getCostBasis());
        position.setProfitLoss(holding.getProfitLoss());
        position.setProfitLossPercent(holding.getProfitLossPercent());
        position.setProvider(providerId);
        position.setAssetType(determineAssetType(providerId, holding.getAsset()));
        
        return position;
    }
    
    /**
     * Calculate asset allocation percentages
     */
    private PortfolioOverviewResponse.AssetAllocation calculateAssetAllocation(
            List<PortfolioPositionResponse> positions, BigDecimal totalValue, BigDecimal cashBalance) {
        
        PortfolioOverviewResponse.AssetAllocation allocation = new PortfolioOverviewResponse.AssetAllocation();
        
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            allocation.setCryptoPercent(BigDecimal.ZERO);
            allocation.setStockPercent(BigDecimal.ZERO);
            allocation.setForexPercent(BigDecimal.ZERO);
            allocation.setCashPercent(BigDecimal.ZERO);
            allocation.setOtherPercent(BigDecimal.ZERO);
            return allocation;
        }
        
        BigDecimal cryptoValue = BigDecimal.ZERO;
        BigDecimal stockValue = BigDecimal.ZERO;
        BigDecimal forexValue = BigDecimal.ZERO;
        
        for (PortfolioPositionResponse position : positions) {
            BigDecimal value = position.getCurrentValue() != null ? position.getCurrentValue() : BigDecimal.ZERO;
            
            if (ServicePortfolioConstants.ASSET_TYPE_CRYPTO.equals(position.getAssetType())) {
                cryptoValue = cryptoValue.add(value);
            } else if (ServicePortfolioConstants.ASSET_TYPE_STOCK.equals(position.getAssetType())) {
                stockValue = stockValue.add(value);
            } else if (ServicePortfolioConstants.ASSET_TYPE_FOREX.equals(position.getAssetType())) {
                forexValue = forexValue.add(value);
            }
        }
        
        allocation.setCryptoPercent(calculatePercent(cryptoValue, totalValue));
        allocation.setStockPercent(calculatePercent(stockValue, totalValue));
        allocation.setForexPercent(calculatePercent(forexValue, totalValue));
        allocation.setCashPercent(calculatePercent(cashBalance, totalValue));
        
        // Calculate "other" as remainder
        BigDecimal totalPercent = allocation.getCryptoPercent()
            .add(allocation.getStockPercent())
            .add(allocation.getForexPercent())
            .add(allocation.getCashPercent());
        
        allocation.setOtherPercent(new BigDecimal("100").subtract(totalPercent));
        
        return allocation;
    }
    
    /**
     * Calculate percentage change
     */
    private BigDecimal calculatePercentChange(BigDecimal change, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0 || change == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal previousValue = total.subtract(change);
        if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return change.divide(previousValue, 4, RoundingMode.HALF_UP)
                     .multiply(new BigDecimal("100"));
    }
    
    /**
     * Calculate profit/loss percentage
     */
    private BigDecimal calculateProfitLossPercent(BigDecimal profitLoss, BigDecimal totalValue) {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0 || profitLoss == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal costBasis = totalValue.subtract(profitLoss);
        if (costBasis.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return profitLoss.divide(costBasis, 4, RoundingMode.HALF_UP)
                         .multiply(new BigDecimal("100"));
    }
    
    /**
     * Calculate percentage of value vs total
     */
    private BigDecimal calculatePercent(BigDecimal value, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0 || value == null) {
            return BigDecimal.ZERO;
        }
        
        return value.divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
    }
    
    /**
     * Determine asset type based on provider and symbol
     */
    private String determineAssetType(String providerId, String symbol) {
        // Crypto providers
        if (ServicePortfolioConstants.PROVIDER_KRAKEN.equals(providerId) ||
            ServicePortfolioConstants.PROVIDER_COINBASE.equals(providerId) ||
            ServicePortfolioConstants.PROVIDER_BINANCE.equals(providerId)) {
            return ServicePortfolioConstants.ASSET_TYPE_CRYPTO;
        }
        
        // Stock providers
        if (ServicePortfolioConstants.PROVIDER_ALPACA.equals(providerId) ||
            ServicePortfolioConstants.PROVIDER_SCHWAB.equals(providerId) ||
            ServicePortfolioConstants.PROVIDER_ROBINHOOD.equals(providerId)) {
            return ServicePortfolioConstants.ASSET_TYPE_STOCK;
        }
        
        return ServicePortfolioConstants.ASSET_TYPE_STOCK;
    }
    
    /**
     * Get display name for provider
     */
    private String getProviderDisplayName(String providerId) {
        Map<String, String> providerNames = Map.of(
            ServicePortfolioConstants.PROVIDER_KRAKEN, "Kraken",
            ServicePortfolioConstants.PROVIDER_COINBASE, "Coinbase",
            ServicePortfolioConstants.PROVIDER_BINANCE, "Binance US",
            ServicePortfolioConstants.PROVIDER_ALPACA, "Alpaca",
            ServicePortfolioConstants.PROVIDER_SCHWAB, "Charles Schwab",
            ServicePortfolioConstants.PROVIDER_ROBINHOOD, "Robinhood"
        );
        return providerNames.getOrDefault(providerId, providerId);
    }

    /**
     * Get provider type (crypto, equity, forex) based on provider ID
     */
    private String getProviderType(String providerId) {
        Map<String, String> providerTypes = Map.of(
            ServicePortfolioConstants.PROVIDER_KRAKEN, "crypto",
            ServicePortfolioConstants.PROVIDER_COINBASE, "crypto",
            ServicePortfolioConstants.PROVIDER_BINANCE, "crypto",
            ServicePortfolioConstants.PROVIDER_ALPACA, "equity",
            ServicePortfolioConstants.PROVIDER_SCHWAB, "equity",
            ServicePortfolioConstants.PROVIDER_ROBINHOOD, "equity"
        );
        return providerTypes.getOrDefault(providerId, "crypto");
    }

    /**
     * Get provider category (exchange, brokerage) based on provider ID
     */
    private String getProviderCategory(String providerId) {
        Map<String, String> providerCategories = Map.of(
            ServicePortfolioConstants.PROVIDER_KRAKEN, "exchange",
            ServicePortfolioConstants.PROVIDER_COINBASE, "exchange",
            ServicePortfolioConstants.PROVIDER_BINANCE, "exchange",
            ServicePortfolioConstants.PROVIDER_ALPACA, "brokerage",
            ServicePortfolioConstants.PROVIDER_SCHWAB, "brokerage",
            ServicePortfolioConstants.PROVIDER_ROBINHOOD, "brokerage"
        );
        return providerCategories.getOrDefault(providerId, "exchange");
    }

    /**
     * Calculate category-based asset allocation for pie chart visualization.
     * Groups positions by asset category (crypto, stocks, bonds, cash, etc.)
     *
     * @param positions All portfolio positions
     * @param totalValue Total portfolio value
     * @param cashBalance Cash balance to include
     * @return List of CategoryAllocation objects grouped by category
     */
    public List<CategoryAllocation> calculateCategoryAllocation(
            List<PortfolioPositionResponse> positions, BigDecimal totalValue, BigDecimal cashBalance) {

        log.debug("Calculating category allocation for {} positions, total value: {}",
                positions.size(), totalValue);

        // Group positions by category
        Map<AssetCategory, List<PortfolioPositionResponse>> positionsByCategory = new HashMap<>();

        for (PortfolioPositionResponse position : positions) {
            String assetType = position.getAssetType();
            AssetCategory category = AssetCategory.fromAssetType(assetType);
            positionsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(position);
        }

        // Calculate allocation for each category
        List<CategoryAllocation> allocations = new ArrayList<>();

        for (Map.Entry<AssetCategory, List<PortfolioPositionResponse>> entry : positionsByCategory.entrySet()) {
            AssetCategory category = entry.getKey();
            List<PortfolioPositionResponse> categoryPositions = entry.getValue();

            BigDecimal categoryValue = BigDecimal.ZERO;
            BigDecimal categoryCostBasis = BigDecimal.ZERO;

            for (PortfolioPositionResponse position : categoryPositions) {
                if (position.getCurrentValue() != null) {
                    categoryValue = categoryValue.add(position.getCurrentValue());
                }
                if (position.getCostBasis() != null) {
                    categoryCostBasis = categoryCostBasis.add(position.getCostBasis());
                }
            }

            BigDecimal categoryProfitLoss = categoryValue.subtract(categoryCostBasis);

            CategoryAllocation allocation = CategoryAllocation.builder()
                    .category(category)
                    .value(categoryValue)
                    .costBasis(categoryCostBasis)
                    .profitLoss(categoryProfitLoss)
                    .assetCount(categoryPositions.size())
                    .calculatePercentage(totalValue)
                    .calculateProfitLossPercent()
                    .build();

            allocations.add(allocation);
        }

        // Add cash category if there's cash balance
        if (cashBalance != null && cashBalance.compareTo(BigDecimal.ZERO) > 0) {
            CategoryAllocation cashAllocation = CategoryAllocation.builder()
                    .category(AssetCategory.CASH)
                    .value(cashBalance)
                    .costBasis(cashBalance)  // Cash has no profit/loss
                    .profitLoss(BigDecimal.ZERO)
                    .profitLossPercent(BigDecimal.ZERO)
                    .assetCount(1)
                    .calculatePercentage(totalValue)
                    .build();

            allocations.add(cashAllocation);
        }

        // Sort by value descending
        allocations.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        log.info("Category allocation calculated: {} categories", allocations.size());
        for (CategoryAllocation allocation : allocations) {
            log.debug("  {}: ${} ({}%)",
                    allocation.getCategoryName(),
                    allocation.getValue(),
                    allocation.getPercentage());
        }

        return allocations;
    }

    /**
     * Convert ProviderPortfolioResponse to ProviderDataEntity
     */
    private ProviderDataEntity convertToProviderDataEntity(ProviderPortfolioResponse response) {
        ProviderDataEntity entity = new ProviderDataEntity();
        
        entity.setProviderId(response.getProviderId());
        entity.setProviderName(response.getProviderName());
        entity.setProviderType(response.getProviderType());
        entity.setProviderCategory(response.getProviderCategory());
        entity.setTotalValue(response.getTotalValue());
        entity.setDayChange(response.getDayChange());
        entity.setDayChangePercent(response.getDayChangePercent());
        entity.setTotalProfitLoss(response.getTotalProfitLoss());
        entity.setTotalProfitLossPercent(response.getTotalProfitLossPercent());
        entity.setCashBalance(response.getCashBalance());
        entity.setSyncStatus(response.getSyncStatus());
        entity.setErrorMessage(response.getErrorMessage());
        entity.setLastUpdatedAt(Instant.now());
        
        // Convert positions to holdings
        if (response.getPositions() != null) {
            List<ProviderDataEntity.Holding> holdings = new ArrayList<>();
            for (PortfolioPositionResponse position : response.getPositions()) {
                ProviderDataEntity.Holding holding = new ProviderDataEntity.Holding();
                holding.setAsset(position.getSymbol());
                holding.setName(position.getName());
                holding.setQuantity(position.getQuantity());
                holding.setCurrentPrice(position.getCurrentPrice());
                holding.setCurrentValue(position.getCurrentValue());
                holding.setCostBasis(position.getCostBasis());
                holding.setProfitLoss(position.getProfitLoss());
                holding.setProfitLossPercent(position.getProfitLossPercent());
                holdings.add(holding);
            }
            entity.setHoldings(holdings);
        }
        
        // Convert balances
        if (response.getBalances() != null) {
            Map<String, Object> balances = new HashMap<>();
            response.getBalances().forEach((key, value) -> balances.put(key, value.toString()));
            entity.setBalances(balances);
        }
        
        return entity;
    }
}