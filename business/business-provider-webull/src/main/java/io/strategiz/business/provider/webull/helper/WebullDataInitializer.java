package io.strategiz.business.provider.webull.helper;

import io.strategiz.business.provider.webull.constants.WebullConstants;
import io.strategiz.business.provider.webull.exception.WebullProviderErrorDetails;
import io.strategiz.client.webull.auth.WebullApiAuthClient;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.repository.CreateProviderDataRepository;
import io.strategiz.framework.exception.StrategizException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Component responsible for initializing and storing Webull portfolio data.
 * Handles fetching data from Webull API and persisting to Firestore.
 *
 * @author Strategiz Platform
 * @since 1.0
 */
@Component
public class WebullDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(WebullDataInitializer.class);
    private static final String MODULE_NAME = "business-provider-webull";

    private final WebullApiAuthClient webullClient;
    private final CreateProviderDataRepository createProviderDataRepo;

    @Autowired
    public WebullDataInitializer(WebullApiAuthClient webullClient,
                                  CreateProviderDataRepository createProviderDataRepo) {
        this.webullClient = webullClient;
        this.createProviderDataRepo = createProviderDataRepo;
    }

    /**
     * Initialize and store provider data for a user.
     * Fetches account positions and balance from Webull API.
     *
     * @param userId User ID
     * @param appKey Webull App key
     * @param appSecret Webull App secret
     * @param accountId Webull account ID
     * @return Stored ProviderDataEntity
     */
    public ProviderDataEntity initializeAndStoreData(String userId, String appKey, String appSecret, String accountId) {
        log.info("Initializing Webull portfolio data for user: {}", userId);

        try {
            // 1. Fetch account positions
            Map<String, Object> positionsResponse = fetchAccountPositions(appKey, appSecret, accountId);
            log.debug("Fetched positions data for user: {}", userId);

            // 2. Transform positions to holdings
            List<ProviderDataEntity.Holding> holdings = transformPositionsToHoldings(positionsResponse);
            log.debug("Transformed {} positions to holdings", holdings.size());

            // 3. Calculate totals
            BigDecimal totalValue = BigDecimal.ZERO;
            BigDecimal cashBalance = BigDecimal.ZERO;

            for (ProviderDataEntity.Holding holding : holdings) {
                if (holding.getCurrentValue() != null) {
                    totalValue = totalValue.add(holding.getCurrentValue());
                }
            }

            // Try to get cash balance from account balance API
            try {
                Map<String, Object> balanceResponse = webullClient.getAccountBalance(appKey, appSecret, accountId).block();
                if (balanceResponse != null && balanceResponse.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> balanceData = (Map<String, Object>) balanceResponse.get("data");
                    if (balanceData.containsKey("cash_balance")) {
                        cashBalance = new BigDecimal(balanceData.get("cash_balance").toString());
                        totalValue = totalValue.add(cashBalance);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch account balance: {}", e.getMessage());
            }

            // 4. Create ProviderDataEntity
            ProviderDataEntity data = new ProviderDataEntity();
            data.setProviderId(WebullConstants.PROVIDER_ID);
            data.setProviderName(WebullConstants.PROVIDER_NAME);
            data.setProviderType("equity");
            data.setProviderCategory("brokerage");
            data.setHoldings(holdings);
            data.setTotalValue(totalValue);
            data.setCashBalance(cashBalance);
            data.setLastUpdatedAt(Instant.now());
            data.setSyncStatus("success");

            // 5. Store in Firestore
            ProviderDataEntity savedData = createProviderDataRepo.createOrReplaceProviderData(
                    userId, WebullConstants.PROVIDER_ID, data
            );

            log.info("Successfully initialized and stored Webull data for user: {}, total value: {}",
                    userId, savedData.getTotalValue());

            return savedData;

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to initialize Webull data for user: {}", userId, e);
            throw new StrategizException(
                    WebullProviderErrorDetails.DATA_INITIALIZATION_FAILED,
                    MODULE_NAME,
                    userId,
                    e.getMessage()
            );
        }
    }

    /**
     * Fetch account positions from Webull API
     */
    private Map<String, Object> fetchAccountPositions(String appKey, String appSecret, String accountId) {
        log.debug("Fetching account positions from Webull");

        try {
            Map<String, Object> response = webullClient.getAccountPositions(appKey, appSecret, accountId).block();

            if (response == null) {
                throw new StrategizException(
                        WebullProviderErrorDetails.POSITIONS_FETCH_FAILED,
                        MODULE_NAME,
                        "null response"
                );
            }

            // Check for API errors
            if (response.containsKey("code")) {
                Object code = response.get("code");
                if (code != null && !"0".equals(code.toString())) {
                    String errorMsg = response.containsKey("msg") ? response.get("msg").toString() : "Unknown error";
                    log.error("Webull API returned error: {}", errorMsg);
                    throw new StrategizException(
                            WebullProviderErrorDetails.POSITIONS_FETCH_FAILED,
                            MODULE_NAME,
                            errorMsg
                    );
                }
            }

            return response;

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching positions from Webull", e);
            throw new StrategizException(
                    WebullProviderErrorDetails.POSITIONS_FETCH_FAILED,
                    MODULE_NAME,
                    e.getMessage()
            );
        }
    }

    /**
     * Transform Webull positions response to Holding list
     */
    @SuppressWarnings("unchecked")
    private List<ProviderDataEntity.Holding> transformPositionsToHoldings(Map<String, Object> positionsResponse) {
        List<ProviderDataEntity.Holding> holdings = new ArrayList<>();

        if (positionsResponse == null || !positionsResponse.containsKey("holdings")) {
            // Try alternate data structure
            if (positionsResponse != null && positionsResponse.containsKey("data")) {
                Object data = positionsResponse.get("data");
                if (data instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    if (dataMap.containsKey("holdings")) {
                        List<Map<String, Object>> positions = (List<Map<String, Object>>) dataMap.get("holdings");
                        return transformPositionsList(positions);
                    }
                }
            }
            return holdings;
        }

        List<Map<String, Object>> positions = (List<Map<String, Object>>) positionsResponse.get("holdings");
        return transformPositionsList(positions);
    }

    private List<ProviderDataEntity.Holding> transformPositionsList(List<Map<String, Object>> positions) {
        List<ProviderDataEntity.Holding> holdings = new ArrayList<>();

        if (positions == null) {
            return holdings;
        }

        for (Map<String, Object> position : positions) {
            try {
                ProviderDataEntity.Holding holding = new ProviderDataEntity.Holding();

                // Extract basic info - use setAsset for symbol
                String symbol = getStringValue(position, "symbol");
                holding.setAsset(symbol);
                holding.setName(symbol); // Webull might not provide name

                // Extract quantities
                String quantityStr = getStringValue(position, "quantity");
                if (quantityStr != null) {
                    holding.setQuantity(new BigDecimal(quantityStr));
                }

                // Extract prices
                String lastPriceStr = getStringValue(position, "last_price");
                if (lastPriceStr != null) {
                    holding.setCurrentPrice(new BigDecimal(lastPriceStr));
                }

                String unitCostStr = getStringValue(position, "unit_cost");
                if (unitCostStr != null) {
                    holding.setCostBasis(new BigDecimal(unitCostStr));
                    holding.setAverageBuyPrice(new BigDecimal(unitCostStr));
                }

                // Extract/calculate values
                String marketValueStr = getStringValue(position, "market_value");
                if (marketValueStr != null) {
                    holding.setCurrentValue(new BigDecimal(marketValueStr));
                } else if (holding.getQuantity() != null && holding.getCurrentPrice() != null) {
                    holding.setCurrentValue(holding.getQuantity().multiply(holding.getCurrentPrice()));
                }

                // Extract P/L
                String unrealizedPLStr = getStringValue(position, "unrealized_profit_loss");
                if (unrealizedPLStr != null) {
                    holding.setProfitLoss(new BigDecimal(unrealizedPLStr));
                }

                String unrealizedPLRateStr = getStringValue(position, "unrealized_profit_loss_rate");
                if (unrealizedPLRateStr != null) {
                    holding.setProfitLossPercent(new BigDecimal(unrealizedPLRateStr));
                }

                // Set instrument type as asset type
                String instrumentType = getStringValue(position, "instrument_type");
                if (instrumentType != null) {
                    holding.setAssetType(instrumentType.toLowerCase());
                } else {
                    holding.setAssetType("stock"); // Default to stock
                }

                holdings.add(holding);

            } catch (Exception e) {
                log.warn("Error transforming position: {}", e.getMessage());
            }
        }

        return holdings;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
