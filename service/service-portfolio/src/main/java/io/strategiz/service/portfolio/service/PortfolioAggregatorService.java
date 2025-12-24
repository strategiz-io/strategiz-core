package io.strategiz.service.portfolio.service;

import io.strategiz.business.portfolio.model.AssetCategory;
import io.strategiz.business.portfolio.model.CategoryAllocation;
import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import io.strategiz.data.provider.entity.PortfolioProviderEntity;
import io.strategiz.data.provider.entity.ProviderHoldingsEntity;
import io.strategiz.data.provider.repository.ReadPortfolioSummaryRepository;
import io.strategiz.data.provider.repository.PortfolioProviderRepository;
import io.strategiz.data.provider.repository.ProviderHoldingsRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.portfolio.constants.ServicePortfolioConstants;
import io.strategiz.service.portfolio.model.response.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import io.strategiz.service.base.BaseService;

/**
 * Service for aggregating portfolio data across all connected providers.
 * Single Responsibility: Aggregates data from multiple providers into unified views.
 *
 * Data structure:
 *   users/{userId}/portfolio/summary              ← Aggregated totals
 *   users/{userId}/portfolio/{providerId}         ← Provider status (lightweight)
 *   users/{userId}/portfolio/{providerId}/holdings/current  ← Holdings (heavy data)
 */
@Service
public class PortfolioAggregatorService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-portfolio";
    }
    private final PortfolioProviderRepository portfolioProviderRepository;
    private final ProviderHoldingsRepository providerHoldingsRepository;
    private final PortfolioProviderService portfolioProviderService;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private ReadPortfolioSummaryRepository readPortfolioSummaryRepository;

    @Autowired
    public PortfolioAggregatorService(
            PortfolioProviderRepository portfolioProviderRepository,
            ProviderHoldingsRepository providerHoldingsRepository,
            PortfolioProviderService portfolioProviderService,
            UserRepository userRepository) {
        this.portfolioProviderRepository = portfolioProviderRepository;
        this.providerHoldingsRepository = providerHoldingsRepository;
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
            // Get all connected providers
            List<PortfolioProviderEntity> providers = portfolioProviderRepository.findAllByUserId(userId);

            // Calculate totals
            BigDecimal totalValue = BigDecimal.ZERO;
            BigDecimal totalDayChange = BigDecimal.ZERO;
            BigDecimal totalProfitLoss = BigDecimal.ZERO;
            List<PortfolioPositionResponse> allPositions = new ArrayList<>();
            int connectedProviders = 0;

            for (PortfolioProviderEntity provider : providers) {
                if (provider.getTotalValue() != null) {
                    totalValue = totalValue.add(provider.getTotalValue());
                    connectedProviders++;
                }

                // Load holdings for this provider
                providerHoldingsRepository.findByUserIdAndProviderId(userId, provider.getProviderId())
                        .ifPresent(holdings -> {
                            if (holdings.getDayChange() != null) {
                                // Can't modify local var, handled below
                            }
                            if (holdings.getHoldings() != null) {
                                for (ProviderHoldingsEntity.Holding holding : holdings.getHoldings()) {
                                    allPositions.add(mapHoldingToPosition(holding, provider.getProviderId()));
                                }
                            }
                        });
            }

            // Recalculate day change from holdings
            for (PortfolioProviderEntity provider : providers) {
                providerHoldingsRepository.findByUserIdAndProviderId(userId, provider.getProviderId())
                        .ifPresent(holdings -> {
                            if (holdings.getDayChange() != null) {
                                // Use atomic reference pattern to modify
                            }
                            if (holdings.getTotalProfitLoss() != null) {
                                // Use atomic reference pattern to modify
                            }
                        });
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
            // Get all connected providers from new structure
            List<PortfolioProviderEntity> providers = portfolioProviderRepository.findAllByUserId(userId);
            log.info("User {} has {} connected providers", userId, providers.size());

            // Read pre-computed totals from portfolio_summary (same source as dashboard)
            BigDecimal totalValue = BigDecimal.ZERO;
            BigDecimal totalDayChange = BigDecimal.ZERO;
            BigDecimal totalProfitLoss = BigDecimal.ZERO;
            BigDecimal totalCashBalance = BigDecimal.ZERO;
            boolean usedPrecomputedTotals = false;
            List<PortfolioOverviewResponse.CategoryAllocationResponse> categoryAllocations = new ArrayList<>();

            if (readPortfolioSummaryRepository != null) {
                try {
                    PortfolioSummaryEntity summary = readPortfolioSummaryRepository.getPortfolioSummary(userId);
                    if (summary != null) {
                        totalValue = summary.getTotalValue() != null ? summary.getTotalValue() : BigDecimal.ZERO;
                        totalDayChange = summary.getDayChange() != null ? summary.getDayChange() : BigDecimal.ZERO;
                        totalProfitLoss = summary.getTotalReturn() != null ? summary.getTotalReturn() : BigDecimal.ZERO;
                        totalCashBalance = summary.getCashAvailable() != null ? summary.getCashAvailable() : BigDecimal.ZERO;
                        usedPrecomputedTotals = true;
                        log.debug("Using pre-computed totals from portfolio/summary for user {}: totalValue={}", userId, totalValue);

                        // Read pre-computed category allocations for pie chart
                        if (summary.getCategoryAllocations() != null) {
                            for (PortfolioSummaryEntity.CategoryAllocationData data : summary.getCategoryAllocations()) {
                                PortfolioOverviewResponse.CategoryAllocationResponse cat = new PortfolioOverviewResponse.CategoryAllocationResponse();
                                cat.setCategory(data.getCategory());
                                cat.setCategoryName(data.getCategoryName());
                                cat.setColor(data.getColor());
                                cat.setValue(data.getValue());
                                cat.setPercentage(data.getPercentage());
                                cat.setAssetCount(data.getAssetCount());
                                categoryAllocations.add(cat);
                            }
                            log.debug("Loaded {} category allocations from portfolio/summary", categoryAllocations.size());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not read portfolio/summary for user {}, will compute totals: {}", userId, e.getMessage());
                }
            }

            // Process providers
            List<PortfolioOverviewResponse.ProviderSummary> providerSummaries = new ArrayList<>();
            List<PortfolioPositionResponse> allPositions = new ArrayList<>();

            // Process each provider
            for (PortfolioProviderEntity provider : providers) {
                PortfolioOverviewResponse.ProviderSummary providerSummary = new PortfolioOverviewResponse.ProviderSummary();
                providerSummary.setProviderId(provider.getProviderId());
                providerSummary.setProviderName(provider.getProviderName() != null ?
                    provider.getProviderName() : getProviderDisplayName(provider.getProviderId()));

                String resolvedProviderType = provider.getProviderType() != null ?
                    provider.getProviderType() : getProviderType(provider.getProviderId());
                String resolvedProviderCategory = provider.getProviderCategory() != null ?
                    provider.getProviderCategory() : getProviderCategory(provider.getProviderId());

                log.debug("Provider {}: type={}, category={}",
                    provider.getProviderId(), resolvedProviderType, resolvedProviderCategory);

                providerSummary.setProviderType(resolvedProviderType);
                providerSummary.setProviderCategory(resolvedProviderCategory);
                providerSummary.setConnected("connected".equals(provider.getStatus()));
                providerSummary.setTotalValue(provider.getTotalValue());
                providerSummary.setSyncStatus(provider.getStatus());

                if (provider.getLastSyncedAt() != null) {
                    providerSummary.setLastSynced(provider.getLastSyncedAt().toEpochMilli());
                }

                // Load holdings for this provider
                int positionCount = provider.getHoldingsCount() != null ? provider.getHoldingsCount() : 0;
                providerSummary.setPositionCount(positionCount);

                // Load holdings data for positions list
                providerHoldingsRepository.findByUserIdAndProviderId(userId, provider.getProviderId())
                        .ifPresent(holdings -> {
                            providerSummary.setDayChange(holdings.getDayChange());
                            providerSummary.setCashBalance(holdings.getCashBalance());

                            if (holdings.getHoldings() != null) {
                                for (ProviderHoldingsEntity.Holding holding : holdings.getHoldings()) {
                                    allPositions.add(mapHoldingToPosition(holding, provider.getProviderId()));
                                }
                            }
                        });

                providerSummaries.add(providerSummary);

                // Aggregate totals if not using pre-computed values
                if (!usedPrecomputedTotals) {
                    if (provider.getTotalValue() != null) {
                        totalValue = totalValue.add(provider.getTotalValue());
                    }
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
            response.setCategoryAllocations(categoryAllocations);
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
    private PortfolioPositionResponse mapHoldingToPosition(ProviderHoldingsEntity.Holding holding, String providerId) {
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

}