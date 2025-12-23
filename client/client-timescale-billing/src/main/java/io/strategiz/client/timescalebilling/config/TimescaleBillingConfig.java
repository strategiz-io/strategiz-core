package io.strategiz.client.timescalebilling.config;

import io.strategiz.framework.secrets.service.VaultSecretService;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for TimescaleDB Cloud billing API client.
 * Loads API credentials from Vault for secure access.
 *
 * Enable with: gcp.billing.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class TimescaleBillingConfig {

    private static final Logger log = LoggerFactory.getLogger(TimescaleBillingConfig.class);
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    @Value("${timescale.cloud.api.url:https://console.cloud.timescale.com/api}")
    private String apiUrl;

    @Value("${timescale.cloud.project.id:}")
    private String projectId;

    private final VaultSecretService vaultSecretService;

    public TimescaleBillingConfig(VaultSecretService vaultSecretService) {
        this.vaultSecretService = vaultSecretService;
    }

    @Bean(name = "timescaleBillingRestTemplate")
    public RestTemplate timescaleBillingRestTemplate() {
        log.info("Initializing TimescaleDB Billing RestTemplate");

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplateBuilder()
                .requestFactory(() -> requestFactory)
                .setConnectTimeout(Duration.ofMillis(CONNECTION_TIMEOUT))
                .setReadTimeout(Duration.ofMillis(READ_TIMEOUT))
                .build();
    }

    @Bean
    public TimescaleBillingProperties timescaleBillingProperties() {
        // Load API token from Vault
        String apiToken = vaultSecretService.readSecret("timescale-billing.api-token", "");

        return new TimescaleBillingProperties(apiUrl, projectId, apiToken);
    }

    /**
     * Properties holder for TimescaleDB billing configuration
     */
    public record TimescaleBillingProperties(
            String apiUrl,
            String projectId,
            String apiToken
    ) {
        public boolean isConfigured() {
            return apiToken != null && !apiToken.isEmpty();
        }
    }
}
