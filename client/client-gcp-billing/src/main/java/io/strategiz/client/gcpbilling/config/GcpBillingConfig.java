package io.strategiz.client.gcpbilling.config;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import io.strategiz.framework.secrets.service.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

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
    public BigQuery bigQueryClient() {
        log.info("Initializing BigQuery client for project: {}", projectId);
        return BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }

    @Bean
    public MetricServiceClient metricServiceClient() throws IOException {
        log.info("Initializing GCP Metric Service client");
        return MetricServiceClient.create();
    }

    @Bean
    public GcpBillingProperties gcpBillingProperties() {
        // Try to load from Vault first
        String vaultBillingAccountId = billingAccountId;
        Boolean vaultUseBillingApi = useBillingApi;
        Boolean vaultUseBigQuery = useBigQuery;

        try {
            if (vaultSecretService != null) {
                String vaultAccountId = vaultSecretService.readSecret("gcp-billing.billing-account-id", null);
                if (vaultAccountId != null && !vaultAccountId.isEmpty()) {
                    vaultBillingAccountId = vaultAccountId;
                }

                String useBillingApiStr = vaultSecretService.readSecret("gcp-billing.use-billing-api", null);
                if (useBillingApiStr != null) {
                    vaultUseBillingApi = Boolean.parseBoolean(useBillingApiStr);
                }

                String useBigQueryStr = vaultSecretService.readSecret("gcp-billing.use-bigquery", null);
                if (useBigQueryStr != null) {
                    vaultUseBigQuery = Boolean.parseBoolean(useBigQueryStr);
                }
            }
        } catch (Exception e) {
            log.warn("Could not load billing config from Vault, using defaults: {}", e.getMessage());
        }

        log.info("GCP Billing Config - Project: {}, Billing API: {}, BigQuery: {}, Account: {}",
                projectId, vaultUseBillingApi, vaultUseBigQuery, vaultBillingAccountId);

        return new GcpBillingProperties(
                projectId,
                billingDataset,
                billingTable,
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
