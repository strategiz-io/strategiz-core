package io.strategiz.service.console.service;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.strategiz.service.console.model.response.ProviderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for monitoring provider integrations and status.
 */
@Service
public class ProviderStatusService {

    private static final Logger logger = LoggerFactory.getLogger(ProviderStatusService.class);

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
        logger.info("Listing all provider statuses");

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
            throw new StrategizException(ServiceConsoleErrorDetails.PROVIDER_NOT_FOUND, "service-console",
                    providerName);
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
        logger.info("Triggering sync for provider: {}", providerName);

        ProviderConfig config = KNOWN_PROVIDERS.get(providerName.toLowerCase());
        if (config == null) {
            throw new StrategizException(ServiceConsoleErrorDetails.PROVIDER_NOT_FOUND, "service-console",
                    providerName);
        }

        // TODO: Trigger actual provider sync via message queue or scheduler
        logger.info("Provider sync triggered for: {}", providerName);

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
