package io.strategiz.service.console.service;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.strategiz.service.console.model.response.ProviderStatusResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for monitoring provider integrations and status.
 */
@Service
public class ProviderStatusService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-console";
    }

    // Known provider configurations
    private static final Map<String, ProviderConfig> KNOWN_PROVIDERS = Map.of(
        "alpaca", new ProviderConfig("alpaca", "Alpaca Markets", "STOCK"),
        "schwab", new ProviderConfig("schwab", "Charles Schwab", "STOCK"),
        "robinhood", new ProviderConfig("robinhood", "Robinhood", "STOCK"),
        "coinbase", new ProviderConfig("coinbase", "Coinbase", "CRYPTO"),
        "kraken", new ProviderConfig("kraken", "Kraken", "CRYPTO"),
        "binanceus", new ProviderConfig("binanceus", "Binance US", "CRYPTO"),
        "google", new ProviderConfig("google", "Google OAuth", "OAUTH"),
        "facebook", new ProviderConfig("facebook", "Facebook OAuth", "OAUTH")
    );

    public List<ProviderStatusResponse> listProviders() {
        log.info("Listing all provider statuses");

        List<ProviderStatusResponse> responses = new ArrayList<>();
        for (Map.Entry<String, ProviderConfig> entry : KNOWN_PROVIDERS.entrySet()) {
            ProviderConfig config = entry.getValue();
            ProviderStatusResponse response = new ProviderStatusResponse(
                config.name,
                config.displayName,
                config.type
            );
            response.setActiveIntegrations(0); // TODO: Add cross-user integration counting
            response.setStatus("UP"); // Default to UP, could integrate health checks
            responses.add(response);
        }

        return responses;
    }

    public ProviderStatusResponse getProvider(String providerName) {
        ProviderConfig config = KNOWN_PROVIDERS.get(providerName.toLowerCase());
        if (config == null) {
            throwModuleException(ServiceConsoleErrorDetails.PROVIDER_NOT_FOUND, providerName);
        }

        ProviderStatusResponse response = new ProviderStatusResponse(
            config.name,
            config.displayName,
            config.type
        );
        response.setActiveIntegrations(0); // TODO: Add cross-user integration counting
        response.setStatus("UP");
        return response;
    }

    public ProviderStatusResponse syncProvider(String providerName) {
        log.info("Triggering sync for provider: {}", providerName);

        ProviderConfig config = KNOWN_PROVIDERS.get(providerName.toLowerCase());
        if (config == null) {
            throwModuleException(ServiceConsoleErrorDetails.PROVIDER_NOT_FOUND, providerName);
        }

        // TODO: Trigger actual provider sync via message queue or scheduler
        log.info("Provider sync triggered for: {}", providerName);

        return getProvider(providerName);
    }

    // Internal class
    private static class ProviderConfig {
        String name;
        String displayName;
        String type;

        ProviderConfig(String name, String displayName, String type) {
            this.name = name;
            this.displayName = displayName;
            this.type = type;
        }
    }
}
