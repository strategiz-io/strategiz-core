package io.strategiz.client.sendgridbilling.config;

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
 * Configuration for SendGrid billing API client.
 * Loads API credentials from Vault for secure access.
 *
 * Enable with: gcp.billing.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class SendGridBillingConfig {

    private static final Logger log = LoggerFactory.getLogger(SendGridBillingConfig.class);
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    @Value("${sendgrid.api.url:https://api.sendgrid.com/v3}")
    private String apiUrl;

    private final VaultSecretService vaultSecretService;

    public SendGridBillingConfig(VaultSecretService vaultSecretService) {
        this.vaultSecretService = vaultSecretService;
    }

    @Bean(name = "sendgridBillingRestTemplate")
    public RestTemplate sendgridBillingRestTemplate() {
        log.info("Initializing SendGrid Billing RestTemplate");

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
    public SendGridBillingProperties sendgridBillingProperties() {
        // Load API key from Vault
        String apiKey = vaultSecretService.readSecret("sendgrid.api-key", "");

        return new SendGridBillingProperties(apiUrl, apiKey);
    }

    /**
     * Properties holder for SendGrid billing configuration
     */
    public record SendGridBillingProperties(
            String apiUrl,
            String apiKey
    ) {
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isEmpty();
        }
    }
}
