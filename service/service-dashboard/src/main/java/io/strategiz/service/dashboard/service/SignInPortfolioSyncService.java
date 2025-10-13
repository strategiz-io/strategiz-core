package io.strategiz.service.dashboard.service;

import io.strategiz.service.base.BaseService;
import io.strategiz.framework.secrets.controller.SecretManager;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.client.kraken.auth.KrakenApiAuthClient;
import io.strategiz.data.provider.repository.CreateProviderDataRepository;
import io.strategiz.data.provider.repository.ReadProviderDataRepository;
import io.strategiz.data.provider.repository.UpdateProviderDataRepository;
import io.strategiz.data.provider.repository.CreatePortfolioSummaryRepository;
import io.strategiz.data.provider.repository.ReadPortfolioSummaryRepository;
import io.strategiz.data.provider.repository.UpdatePortfolioSummaryRepository;
import io.strategiz.data.provider.repository.ReadProviderIntegrationRepository;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for handling portfolio synchronization on user sign-in
 * Fetches data from all connected providers and stores in Firestore
 */
@Service
public class SignInPortfolioSyncService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(SignInPortfolioSyncService.class);
    private static final String MODULE_NAME = "service-dashboard";

    @Autowired
    private SecretManager secretManager;

    @Autowired
    private KrakenApiAuthClient krakenClient;

    // CRUD Repositories for Provider Data
    @Autowired(required = false)
    private CreateProviderDataRepository createProviderDataRepo;

    @Autowired(required = false)
    private ReadProviderDataRepository readProviderDataRepo;

    @Autowired(required = false)
    private UpdateProviderDataRepository updateProviderDataRepo;

    // CRUD Repositories for Portfolio Summary
    @Autowired(required = false)
    private CreatePortfolioSummaryRepository createPortfolioSummaryRepo;

    @Autowired(required = false)
    private ReadPortfolioSummaryRepository readPortfolioSummaryRepo;

    @Autowired(required = false)
    private UpdatePortfolioSummaryRepository updatePortfolioSummaryRepo;

    // Repository for reading provider integrations
    @Autowired(required = false)
    private ReadProviderIntegrationRepository readProviderIntegrationRepo;

    @Override
    protected String getModuleName() {
        return "service-dashboard";
    }

    /**
     * Perform initial portfolio sync for user on sign-in
     * 
     * @param userId User ID
     * @param demoMode Whether user is in demo mode
     * @return Sync result with status and synced providers
     */
    public Map<String, Object> performInitialSync(String userId, Boolean demoMode) {
        log.info("Starting initial portfolio sync for user: {}, demoMode: {}", userId, demoMode);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("demoMode", demoMode);
        result.put("startTime", Instant.now().toString());

        // Skip sync if in demo mode
        if (Boolean.TRUE.equals(demoMode)) {
            result.put("status", "skipped");
            result.put("reason", "Demo mode active");
            return result;
        }

        try {
            // Initialize Firestore structure
            initializeFirestoreStructure(userId);

            // Get connected providers
            List<ProviderIntegrationEntity> connectedProviders = getConnectedProviders(userId);
            
            if (connectedProviders.isEmpty()) {
                result.put("status", "skipped");
                result.put("reason", "No connected providers");
                return result;
            }

            // Sync all providers asynchronously
            List<String> syncedProviders = syncAllProviders(userId, connectedProviders);

            result.put("status", "completed");
            result.put("syncedProviders", syncedProviders);
            result.put("totalProviders", connectedProviders.size());
            result.put("endTime", Instant.now().toString());

        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error during initial sync for user: {}", userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.SYNC_INITIALIZATION_FAILED,
                MODULE_NAME,
                userId,
                e.getMessage()
            );
        }

        return result;
    }

    /**
     * Refresh portfolio data for user
     * 
     * @param userId User ID
     * @return Refresh result
     */
    public Map<String, Object> refreshPortfolio(String userId) {
        log.info("Refreshing portfolio for user: {}", userId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ProviderIntegrationEntity> connectedProviders = getConnectedProviders(userId);
            List<String> syncedProviders = syncAllProviders(userId, connectedProviders);
            
            result.put("status", "completed");
            result.put("syncedProviders", syncedProviders);
            result.put("timestamp", Instant.now().toString());
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error refreshing portfolio for user: {}", userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.DASHBOARD_ERROR,
                MODULE_NAME,
                "refresh-portfolio",
                e.getMessage()
            );
        }
        
        return result;
    }

    /**
     * Get sync status for user
     * 
     * @param userId User ID
     * @return Current sync status
     */
    public Map<String, Object> getSyncStatus(String userId) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Read portfolio summary to get last sync time
            PortfolioSummaryEntity summary = readPortfolioSummaryRepo.getPortfolioSummary(userId);
            
            if (summary != null) {
                status.put("lastSyncedAt", summary.getLastSyncedAt());
                status.put("hasSyncedData", true);
            } else {
                status.put("hasSyncedData", false);
            }
            
            // Get provider sync statuses
            List<ProviderDataEntity> providerData = readProviderDataRepo.getAllProviderData(userId);
            Map<String, Object> providerStatuses = new HashMap<>();
            
            for (ProviderDataEntity data : providerData) {
                Map<String, Object> providerStatus = new HashMap<>();
                providerStatus.put("lastUpdated", data.getLastUpdatedAt());
                providerStatus.put("hasData", true);
                providerStatuses.put(data.getProviderId(), providerStatus);
            }
            
            status.put("providers", providerStatuses);
            status.put("status", "success");
            
        } catch (Exception e) {
            log.error("Error getting sync status for user: {}", userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.DASHBOARD_ERROR,
                MODULE_NAME,
                "get-sync-status",
                e.getMessage()
            );
        }
        
        return status;
    }

    /**
     * Initialize Firestore structure for user
     */
    private void initializeFirestoreStructure(String userId) {
        log.debug("Initializing Firestore structure for user: {}", userId);
        
        // Check if portfolio summary exists, create if not
        PortfolioSummaryEntity existingSummary = readPortfolioSummaryRepo.getPortfolioSummary(userId);
        
        if (existingSummary == null) {
            PortfolioSummaryEntity initialSummary = createInitialPortfolioSummary();
            createPortfolioSummaryRepo.createPortfolioSummary(userId, initialSummary);
            log.info("Created initial portfolio summary for user: {}", userId);
        }
    }

    /**
     * Get connected providers for user
     */
    private List<ProviderIntegrationEntity> getConnectedProviders(String userId) {
        try {
            return readProviderIntegrationRepo.findByUserIdAndEnabledTrue(userId);
        } catch (Exception e) {
            log.error("Error getting connected providers for user: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Sync all providers for user
     */
    @Async
    private List<String> syncAllProviders(String userId, List<ProviderIntegrationEntity> providers) {
        log.info("Syncing {} providers for user: {}", providers.size(), userId);
        
        List<CompletableFuture<ProviderDataEntity>> futures = providers.stream()
            .map(provider -> syncProviderAsync(userId, provider))
            .collect(Collectors.toList());
        
        // Wait for all with timeout
        List<ProviderDataEntity> results = futures.stream()
            .map(f -> {
                try {
                    return f.get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Provider sync timeout or error", e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        // Calculate and update portfolio summary
        if (!results.isEmpty()) {
            updatePortfolioSummary(userId, results);
        }
        
        return results.stream()
            .map(ProviderDataEntity::getProviderId)
            .collect(Collectors.toList());
    }

    /**
     * Sync individual provider asynchronously
     */
    @Async
    private CompletableFuture<ProviderDataEntity> syncProviderAsync(String userId, ProviderIntegrationEntity provider) {
        try {
            String providerId = provider.getProviderId();
            log.debug("Syncing provider {} for user: {}", providerId, userId);
            
            // Get credentials from Vault
            Map<String, String> credentials = getProviderCredentials(userId, providerId);

            if (credentials == null || credentials.isEmpty()) {
                log.warn("⚠️ Skipping provider {} for user {} - no credentials found in Vault", providerId, userId);
                // Return empty future instead of throwing exception
                // This allows other providers to sync successfully
                return CompletableFuture.completedFuture(null);
            }
            
            // Fetch data based on provider type
            ProviderDataEntity data = null;
            
            switch (providerId.toLowerCase()) {
                case "kraken":
                    data = fetchKrakenData(userId, credentials);
                    break;
                case "coinbase":
                    // TODO: Implement Coinbase client
                    log.info("Coinbase sync not yet implemented");
                    break;
                case "alpaca":
                    // TODO: Implement Alpaca client
                    log.info("Alpaca sync not yet implemented");
                    break;
                default:
                    log.warn("Unknown provider: {}", providerId);
            }
            
            if (data != null) {
                // Save or update provider data
                saveProviderData(userId, providerId, data);
                return CompletableFuture.completedFuture(data);
            }
            
        } catch (StrategizException e) {
            // Log but don't fail the entire sync for one provider
            log.warn("Provider {} sync failed for user {}: {}", provider.getProviderId(), userId, e.getMessage());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            // Log but don't fail the entire sync for one provider
            log.warn("Unexpected error syncing provider {} for user {}: {}", provider.getProviderId(), userId, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get provider credentials from Vault
     * Returns empty map if credentials not found or error occurs
     */
    private Map<String, String> getProviderCredentials(String userId, String providerId) {
        String vaultPath = String.format("secret/strategiz/users/%s/providers/%s", userId, providerId);
        try {
            Map<String, Object> secretData = secretManager.readSecretAsMap(vaultPath);
            if (secretData == null) {
                log.debug("No secret data found at vault path: {}", vaultPath);
                return new HashMap<>();
            }
            // Convert Map<String, Object> to Map<String, String>
            Map<String, String> credentials = new HashMap<>();
            secretData.forEach((key, value) -> {
                if (value != null) {
                    credentials.put(key, value.toString());
                }
            });
            return credentials;
        } catch (Exception e) {
            log.warn("Unable to retrieve credentials for provider {} user {} from {}: {}",
                     providerId, userId, vaultPath, e.getMessage());
            // Return empty map instead of throwing - allows graceful degradation
            return new HashMap<>();
        }
    }

    /**
     * Fetch data from Kraken
     */
    private ProviderDataEntity fetchKrakenData(String userId, Map<String, String> credentials) {
        try {
            String apiKey = credentials.get("apiKey");
            String apiSecret = credentials.get("apiSecret");
            String otp = credentials.get("otp");
            
            // Fetch balance from Kraken
            Map<String, Object> balanceResponse = krakenClient.getAccountBalance(apiKey, apiSecret, otp).block();
            
            if (balanceResponse == null || balanceResponse.containsKey("error")) {
                String errorMsg = balanceResponse != null ? balanceResponse.toString() : "null response";
                log.error("Error fetching Kraken balance: {}", errorMsg);
                throw new StrategizException(
                    ServiceDashboardErrorDetails.SYNC_PROVIDER_FAILED,
                    MODULE_NAME,
                    userId,
                    "kraken",
                    "Balance fetch failed: " + errorMsg
                );
            }
            
            // Transform to ProviderDataEntity
            ProviderDataEntity data = new ProviderDataEntity();
            data.setProviderId("kraken");
            data.setProviderName("Kraken");
            data.setAccountType("crypto");
            
            // Process balance data
            if (balanceResponse.containsKey("result")) {
                Map<String, Object> balances = (Map<String, Object>) balanceResponse.get("result");
                data.setBalances(balances);
                
                // Calculate total value (simplified - would need price data)
                BigDecimal totalValue = calculateTotalValue(balances);
                data.setTotalValue(totalValue);
            }
            
            data.setLastUpdatedAt(Instant.now());
            
            return data;
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching Kraken data for user: {}", userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.SYNC_PROVIDER_FAILED,
                MODULE_NAME,
                userId,
                "kraken",
                e.getMessage()
            );
        }
    }

    /**
     * Save or update provider data
     */
    private void saveProviderData(String userId, String providerId, ProviderDataEntity data) {
        try {
            // Check if data exists
            ProviderDataEntity existing = readProviderDataRepo.getProviderData(userId, providerId);
            
            if (existing != null) {
                // Update existing
                updateProviderDataRepo.updateProviderData(userId, providerId, data);
            } else {
                // Create new
                createProviderDataRepo.createProviderData(userId, providerId, data);
            }
            
        } catch (Exception e) {
            log.error("Error saving provider data for {} user {}", providerId, userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.SYNC_FIRESTORE_ERROR,
                MODULE_NAME,
                "saveProviderData",
                "provider_data",
                providerId,
                e.getMessage()
            );
        }
    }

    /**
     * Update portfolio summary with latest provider data
     */
    private void updatePortfolioSummary(String userId, List<ProviderDataEntity> providerData) {
        try {
            PortfolioSummaryEntity summary = calculatePortfolioSummary(providerData);
            
            // Check if summary exists
            PortfolioSummaryEntity existing = readPortfolioSummaryRepo.getPortfolioSummary(userId);
            
            if (existing != null) {
                updatePortfolioSummaryRepo.updatePortfolioSummary(userId, summary);
            } else {
                createPortfolioSummaryRepo.createPortfolioSummary(userId, summary);
            }
            
        } catch (Exception e) {
            log.error("Error updating portfolio summary for user: {}", userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.SYNC_FIRESTORE_ERROR,
                MODULE_NAME,
                "updatePortfolioSummary",
                "portfolio_summary",
                "current",
                e.getMessage()
            );
        }
    }

    /**
     * Calculate portfolio summary from provider data
     */
    private PortfolioSummaryEntity calculatePortfolioSummary(List<ProviderDataEntity> providerData) {
        PortfolioSummaryEntity summary = new PortfolioSummaryEntity();
        
        // Calculate totals
        BigDecimal totalValue = providerData.stream()
            .map(p -> p.getTotalValue() != null ? p.getTotalValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        summary.setTotalValue(totalValue);
        
        // Set account performance
        Map<String, BigDecimal> accountPerformance = new HashMap<>();
        for (ProviderDataEntity data : providerData) {
            accountPerformance.put(data.getProviderId(), data.getTotalValue());
        }
        summary.setAccountPerformance(accountPerformance);
        
        summary.setLastSyncedAt(Instant.now());
        
        return summary;
    }

    /**
     * Create initial portfolio summary
     */
    private PortfolioSummaryEntity createInitialPortfolioSummary() {
        PortfolioSummaryEntity summary = new PortfolioSummaryEntity();
        summary.setTotalValue(BigDecimal.ZERO);
        summary.setTotalReturn(BigDecimal.ZERO);
        summary.setTotalReturnPercent(BigDecimal.ZERO);
        summary.setCashAvailable(BigDecimal.ZERO);
        summary.setDayChange(BigDecimal.ZERO);
        summary.setDayChangePercent(BigDecimal.ZERO);
        summary.setAssetAllocation(new PortfolioSummaryEntity.AssetAllocation());
        summary.setAccountPerformance(new HashMap<>());
        summary.setLastSyncedAt(Instant.now());
        return summary;
    }

    /**
     * Calculate total value from balances
     */
    private BigDecimal calculateTotalValue(Map<String, Object> balances) {
        // Simplified calculation - in production would need current prices
        BigDecimal total = BigDecimal.ZERO;
        
        for (Map.Entry<String, Object> entry : balances.entrySet()) {
            if (entry.getKey().equals("ZUSD") || entry.getKey().equals("USD")) {
                try {
                    BigDecimal value = new BigDecimal(entry.getValue().toString());
                    total = total.add(value);
                } catch (Exception e) {
                    log.debug("Could not parse balance value: {}", entry.getValue());
                }
            }
        }
        
        return total;
    }
}