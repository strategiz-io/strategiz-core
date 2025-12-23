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
        return new GcpBillingProperties(projectId, billingDataset, billingTable);
    }

    /**
     * Properties holder for GCP billing configuration
     */
    public record GcpBillingProperties(
            String projectId,
            String billingDataset,
            String billingTable
    ) {
        public String getBillingTableFullName() {
            return String.format("%s.%s.%s", projectId, billingDataset, billingTable);
        }
    }
}
