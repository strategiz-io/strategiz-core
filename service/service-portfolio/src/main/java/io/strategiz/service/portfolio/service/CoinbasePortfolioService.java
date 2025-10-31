package io.strategiz.service.portfolio.service;

import io.strategiz.business.portfolio.enhancer.PortfolioEnhancementOrchestrator;
import io.strategiz.business.portfolio.enhancer.business.MarketPriceBusiness;
import io.strategiz.client.coinbase.CoinbaseClient;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.repository.ReadProviderDataRepository;
import io.strategiz.service.portfolio.constants.ServicePortfolioConstants;
import io.strategiz.service.portfolio.model.response.PortfolioPositionResponse;
import io.strategiz.service.portfolio.model.response.ProviderPortfolioResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling Coinbase-specific portfolio operations.
 * Single Responsibility: Manages only Coinbase portfolio data.
 * Dependency Inversion: Depends on abstractions (repositories, clients).
 */
@Service
public class CoinbasePortfolioService {

    private static final Logger log = LoggerFactory.getLogger(CoinbasePortfolioService.class);

    private final CoinbaseClient coinbaseClient;
    private final ReadProviderDataRepository readProviderDataRepository;
    private final PortfolioEnhancementOrchestrator portfolioEnhancer;
    private final MarketPriceBusiness marketPriceBusiness;

    // Cache for crypto prices (symbol -> price)
    private final Map<String, BigDecimal> priceCache = new ConcurrentHashMap<>();
    private final Map<String, Long> priceCacheTimestamp = new ConcurrentHashMap<>();
    private static final long PRICE_CACHE_TTL = 60000; // 1 minute

    @Autowired
    public CoinbasePortfolioService(
            @Autowired(required = false) CoinbaseClient coinbaseClient,
            ReadProviderDataRepository readProviderDataRepository,
            @Autowired(required = false) PortfolioEnhancementOrchestrator portfolioEnhancer,
            @Autowired(required = false) MarketPriceBusiness marketPriceBusiness) {
        this.coinbaseClient = coinbaseClient;
        this.readProviderDataRepository = readProviderDataRepository;
        this.portfolioEnhancer = portfolioEnhancer;
        this.marketPriceBusiness = marketPriceBusiness;
    }

    /**
     * Get Coinbase portfolio data for a user.
     *
     * @param userId User ID
     * @return Coinbase portfolio response
     */
    public ProviderPortfolioResponse getCoinbasePortfolio(String userId) {
        log.info("Fetching Coinbase portfolio for user: {}", userId);

        try {
            // Fetch real data from provider_data collection
            ProviderPortfolioResponse response = fetchCoinbaseData(userId);

            // Log what we're returning
            if (response != null && response.getPositions() != null) {
                log.info("Returning Coinbase portfolio with {} positions, total value: {}",
                    response.getPositions().size(), response.getTotalValue());
            }

            return response;

        } catch (Exception e) {
            log.error("Error fetching Coinbase portfolio for user {}: {}", userId, e.getMessage(), e);
            return createErrorResponse("Failed to fetch Coinbase portfolio: " + e.getMessage());
        }
    }

    /**
     * Fetch Coinbase portfolio data from provider_data collection.
     */
    private ProviderPortfolioResponse fetchCoinbaseData(String userId) {
        log.info("Fetching Coinbase portfolio data from provider_data for user: {}", userId);

        // Read Coinbase provider data entity
        ProviderDataEntity data = readProviderDataRepository.getProviderData(
            userId, ServicePortfolioConstants.PROVIDER_COINBASE);

        if (data == null) {
            log.warn("No Coinbase data found for user: {}", userId);
            return createErrorResponse("No Coinbase data available. Please reconnect your Coinbase account.");
        }

        log.info("Found Coinbase data for user: {} with {} holdings", userId,
            data.getHoldings() != null ? data.getHoldings().size() : 0);

        // Build response from provider data
        ProviderPortfolioResponse response = new ProviderPortfolioResponse();
        response.setProviderId(ServicePortfolioConstants.PROVIDER_COINBASE);
        response.setProviderName(data.getProviderName() != null ? data.getProviderName() : "Coinbase");
        response.setAccountType(data.getAccountType() != null ? data.getAccountType() : "exchange");
        response.setConnected(true);
        response.setSyncStatus(data.getSyncStatus() != null ? data.getSyncStatus() : "synced");

        List<PortfolioPositionResponse> positions = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal cashBalance = BigDecimal.ZERO;

        if (data.getHoldings() != null) {
            for (ProviderDataEntity.Holding holding : data.getHoldings()) {
                try {
                    // Skip if no quantity
                    if (holding.getQuantity() == null || holding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }

                    // Get current price from holding or fetch new one
                    BigDecimal currentPrice = holding.getCurrentPrice() != null ?
                        holding.getCurrentPrice() : getCurrentPrice(holding.getAsset());

                    // Build position
                    PortfolioPositionResponse position = new PortfolioPositionResponse();
                    position.setSymbol(holding.getAsset());
                    position.setQuantity(holding.getQuantity());
                    position.setCurrentPrice(currentPrice);

                    // Calculate market value
                    BigDecimal marketValue = holding.getQuantity().multiply(currentPrice)
                        .setScale(2, RoundingMode.HALF_UP);
                    position.setCurrentValue(marketValue);

                    // Set name
                    position.setName(holding.getName());

                    positions.add(position);

                    // Accumulate totals
                    totalValue = totalValue.add(marketValue);

                    // Track fiat currencies as cash balance
                    if (isFiatCurrency(holding.getAsset())) {
                        cashBalance = cashBalance.add(marketValue);
                    }

                } catch (Exception e) {
                    log.error("Error processing Coinbase holding for asset: {}", holding.getAsset(), e);
                    // Continue with other positions
                }
            }
        }

        response.setPositions(positions);
        response.setTotalValue(data.getTotalValue() != null ? data.getTotalValue() : totalValue);
        response.setCashBalance(data.getCashBalance() != null ? data.getCashBalance() : cashBalance);

        // Set sync timestamp
        if (data.getLastUpdatedAt() != null) {
            response.setLastSynced(data.getLastUpdatedAt().toEpochMilli());
        } else {
            response.setLastSynced(System.currentTimeMillis());
        }

        return response;
    }

    /**
     * Get current price for a symbol with caching.
     */
    private BigDecimal getCurrentPrice(String symbol) {
        // Check cache first
        Long timestamp = priceCacheTimestamp.get(symbol);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < PRICE_CACHE_TTL) {
            BigDecimal cachedPrice = priceCache.get(symbol);
            if (cachedPrice != null) {
                log.debug("Using cached price for {}: {}", symbol, cachedPrice);
                return cachedPrice;
            }
        }

        // Fiat currencies have price of 1.0
        if (isFiatCurrency(symbol)) {
            BigDecimal price = BigDecimal.ONE;
            priceCache.put(symbol, price);
            priceCacheTimestamp.put(symbol, System.currentTimeMillis());
            return price;
        }

        // Try to get price from market data business
        if (marketPriceBusiness != null) {
            try {
                BigDecimal price = marketPriceBusiness.getCurrentPrice(symbol, "USD");
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    priceCache.put(symbol, price);
                    priceCacheTimestamp.put(symbol, System.currentTimeMillis());
                    log.debug("Fetched price for {}: {}", symbol, price);
                    return price;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch price for {}: {}", symbol, e.getMessage());
            }
        }

        // Default to 1.0 if price not available
        log.warn("No price available for {}, defaulting to 1.0", symbol);
        return BigDecimal.ONE;
    }

    /**
     * Check if symbol is a fiat currency.
     */
    private boolean isFiatCurrency(String symbol) {
        Set<String> fiatCurrencies = Set.of("USD", "EUR", "GBP", "CAD", "JPY", "AUD", "CHF", "CNY");
        return fiatCurrencies.contains(symbol.toUpperCase());
    }

    /**
     * Create error response.
     */
    private ProviderPortfolioResponse createErrorResponse(String errorMessage) {
        ProviderPortfolioResponse response = new ProviderPortfolioResponse();
        response.setProviderId(ServicePortfolioConstants.PROVIDER_COINBASE);
        response.setProviderName("Coinbase");
        response.setConnected(false);
        response.setErrorMessage(errorMessage);
        response.setPositions(Collections.emptyList());
        response.setTotalValue(BigDecimal.ZERO);
        response.setCashBalance(BigDecimal.ZERO);
        return response;
    }

    /**
     * Refresh Coinbase portfolio data.
     *
     * @param userId User ID
     * @return Success status
     */
    public boolean refreshCoinbaseData(String userId) {
        log.info("Refreshing Coinbase data for user: {}", userId);

        try {
            // For now, just fetch existing data
            // In the future, we can trigger a re-sync from Coinbase API
            ProviderPortfolioResponse freshData = fetchCoinbaseData(userId);

            return freshData != null && freshData.getTotalValue() != null;

        } catch (Exception e) {
            log.error("Error refreshing Coinbase data for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
}
