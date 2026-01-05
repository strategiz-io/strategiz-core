package io.strategiz.client.gcpbilling.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import io.strategiz.framework.secrets.service.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for GCP Billing and Monitoring API clients.
 * Loads credentials from Vault for secure access.
 *
 * Enable with: gcp.billing.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class GcpBillingConfig {

    private static final Logger log = LoggerFactory.getLogger(GcpBillingConfig.class);

    @Value("${gcp.project.id:strategiz-io}")
    private String projectId;

    @Value("${gcp.billing.dataset:billing_export}")
    private String billingDataset;

    @Value("${gcp.billing.table:gcp_billing_export_v1}")
    private String billingTable;

    @Value("${gcp.billing.use-billing-api:true}")
    private boolean useBillingApi;

    @Value("${gcp.billing.use-bigquery:false}")
    private boolean useBigQuery;

    @Value("${gcp.billing.account-id:}")
    private String billingAccountId;

    private final VaultSecretService vaultSecretService;

    public GcpBillingConfig(VaultSecretService vaultSecretService) {
        this.vaultSecretService = vaultSecretService;
    }

    @Bean
    @ConditionalOnProperty(name = "gcp.billing.use-bigquery", havingValue = "true", matchIfMissing = false)
    public BigQuery bigQueryClient() {
        log.info("Initializing BigQuery client for project: {}", projectId);
        return BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }

    @Bean
    @ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnProperty(name = "gcp.billing.demo-mode", havingValue = "false", matchIfMissing = false)
    public MetricServiceClient metricServiceClient() throws IOException {
        log.info("Initializing GCP Metric Service client");

        // Load service account JSON from Vault at secret/strategiz/gcp-billing
        // Path format: "gcp-billing.credentials" -> path: secret/strategiz/gcp-billing, field: credentials
        String serviceAccountBase64 = vaultSecretService.readSecret("gcp-billing.credentials");

        if (serviceAccountBase64 == null || serviceAccountBase64.isEmpty()) {
            log.warn("No service account credentials found in Vault at secret/strategiz/gcp-billing, using Application Default Credentials");
            return MetricServiceClient.create();
        }

        log.info("Using service account credentials from Vault");
        log.debug("Credentials length: {} characters, first 50 chars: '{}'",
                serviceAccountBase64.length(),
                serviceAccountBase64.substring(0, Math.min(50, serviceAccountBase64.length())));

        // Try to use credentials - could be raw JSON or base64-encoded
        String serviceAccountJson;
        String trimmed = serviceAccountBase64.trim();

        if (trimmed.startsWith("{")) {
            // Already decoded JSON
            log.info("Credentials are raw JSON (VaultSecretService auto-decoded)");
            serviceAccountJson = trimmed;
        } else {
            // Try base64 decoding
            log.info("Credentials do not start with '{', attempting base64 decode...");
            try {
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(trimmed);
                serviceAccountJson = new String(decodedBytes, StandardCharsets.UTF_8);
                log.info("Successfully decoded base64 credentials, JSON length: {} chars", serviceAccountJson.length());
            } catch (IllegalArgumentException e) {
                log.error("FAILED to decode base64. First chars of value: '{}'",
                        trimmed.substring(0, Math.min(100, trimmed.length())));
                log.error("Base64 decode error: {}", e.getMessage());

                // Last resort: maybe it's already JSON but with leading/trailing whitespace or BOM
                if (trimmed.contains("\"type\"") && trimmed.contains("\"private_key\"")) {
                    log.warn("Credentials contain JSON fields but don't start with '{', using as-is after trim");
                    serviceAccountJson = trimmed;
                } else {
                    throw new IOException("Credentials are not valid JSON or base64: " + e.getMessage(), e);
                }
            }
        }

        log.debug("Final service account JSON length: {} characters", serviceAccountJson.length());

        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))
        );

        MetricServiceSettings settings = MetricServiceSettings.newBuilder()
            .setCredentialsProvider(() -> credentials)
            .build();

        return MetricServiceClient.create(settings);
    }

    @Bean
    public GcpBillingProperties gcpBillingProperties() {
        // Try to load from Vault first
        String vaultBillingAccountId = billingAccountId;
        Boolean vaultUseBillingApi = useBillingApi;
        Boolean vaultUseBigQuery = useBigQuery;
        String vaultBillingTable = billingTable;

        try {
            if (vaultSecretService != null) {
                // Read from Vault path: secret/strategiz/gcp-billing
                String vaultAccountId = vaultSecretService.readSecret("gcp-billing.billing-account-id");
                if (vaultAccountId != null && !vaultAccountId.isEmpty()) {
                    vaultBillingAccountId = vaultAccountId;
                }

                String useBillingApiStr = vaultSecretService.readSecret("gcp-billing.use-billing-api");
                if (useBillingApiStr != null) {
                    vaultUseBillingApi = Boolean.parseBoolean(useBillingApiStr);
                }

                String useBigQueryStr = vaultSecretService.readSecret("gcp-billing.use-bigquery");
                if (useBigQueryStr != null) {
                    vaultUseBigQuery = Boolean.parseBoolean(useBigQueryStr);
                }

                // Read billing table name from Vault (important: actual table has billing account suffix)
                String vaultTable = vaultSecretService.readSecret("gcp-billing.billing-table");
                if (vaultTable != null && !vaultTable.isEmpty()) {
                    vaultBillingTable = vaultTable;
                    log.info("Using billing table from Vault: {}", vaultBillingTable);
                }
            }
        } catch (Exception e) {
            log.warn("Could not load billing config from Vault, using defaults: {}", e.getMessage());
        }

        log.info("GCP Billing Config - Project: {}, Dataset: {}, Table: {}, Billing API: {}, BigQuery: {}, Account: {}",
                projectId, billingDataset, vaultBillingTable, vaultUseBillingApi, vaultUseBigQuery, vaultBillingAccountId);

        return new GcpBillingProperties(
                projectId,
                billingDataset,
                vaultBillingTable,
                vaultUseBillingApi,
                vaultUseBigQuery,
                vaultBillingAccountId
        );
    }

    /**
     * Properties holder for GCP billing configuration
     */
    public record GcpBillingProperties(
            String projectId,
            String billingDataset,
            String billingTable,
            boolean useBillingApi,
            boolean useBigQuery,
            String billingAccountId
    ) {
        public String getBillingTableFullName() {
            return String.format("%s.%s.%s", projectId, billingDataset, billingTable);
        }
    }
}
