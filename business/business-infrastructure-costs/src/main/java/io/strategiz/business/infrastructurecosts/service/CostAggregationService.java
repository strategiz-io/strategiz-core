package io.strategiz.business.infrastructurecosts.service;

import io.strategiz.business.infrastructurecosts.model.CostSummary;
import io.strategiz.business.infrastructurecosts.model.DailyCost;
import io.strategiz.business.infrastructurecosts.model.FirestoreUsage;
import io.strategiz.client.gcpbilling.GcpBillingClient;
import io.strategiz.client.gcpbilling.GcpMonitoringClient;
import io.strategiz.client.gcpbilling.model.GcpCostSummary;
import io.strategiz.client.gcpbilling.model.GcpDailyCost;
import io.strategiz.client.gcpbilling.model.GcpServiceUsage;
import io.strategiz.client.timescalebilling.TimescaleBillingClient;
import io.strategiz.client.timescalebilling.model.TimescaleCostSummary;
import io.strategiz.data.infrastructurecosts.entity.DailyCostEntity;
import io.strategiz.data.infrastructurecosts.entity.FirestoreUsageEntity;
import io.strategiz.data.infrastructurecosts.repository.DailyCostRepository;
import io.strategiz.data.infrastructurecosts.repository.FirestoreUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for aggregating infrastructure costs from GCP and TimescaleDB.
 * Combines data from multiple sources and persists to Firestore for historical tracking.
 *
 * Enable with: gcp.billing.enabled=true and gcp.billing.demo-mode=false
 */
@Service
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnProperty(name = "gcp.billing.demo-mode", havingValue = "false", matchIfMissing = false)
public class CostAggregationService {

    private static final Logger log = LoggerFactory.getLogger(CostAggregationService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final GcpBillingClient gcpBillingClient;
    private final GcpMonitoringClient gcpMonitoringClient;
    private final TimescaleBillingClient timescaleBillingClient;
    private final DailyCostRepository dailyCostRepository;
    private final FirestoreUsageRepository firestoreUsageRepository;

    @Value("${subscriptions.claude.monthly-cost:0}")
    private BigDecimal claudeMonthlyCost;

    @Value("${subscriptions.claude.name:Claude Pro}")
    private String claudeName;

    @Value("${subscriptions.claude.enabled:false}")
    private boolean claudeEnabled;

    public CostAggregationService(
            GcpBillingClient gcpBillingClient,
            GcpMonitoringClient gcpMonitoringClient,
            TimescaleBillingClient timescaleBillingClient,
            DailyCostRepository dailyCostRepository,
            FirestoreUsageRepository firestoreUsageRepository) {
        this.gcpBillingClient = gcpBillingClient;
        this.gcpMonitoringClient = gcpMonitoringClient;
        this.timescaleBillingClient = timescaleBillingClient;
        this.dailyCostRepository = dailyCostRepository;
        this.firestoreUsageRepository = firestoreUsageRepository;
    }

    /**
     * Get current month cost summary
     */
    public CostSummary getCurrentMonthSummary() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        String month = today.format(MONTH_FORMAT);

        log.info("Getting current month cost summary for {}", month);

        try {
            // Get GCP costs
            GcpCostSummary gcpSummary = gcpBillingClient.getCostSummary(startOfMonth, today);

            // Get TimescaleDB costs
            TimescaleCostSummary timescaleSummary = timescaleBillingClient.getCostSummary(startOfMonth, today);

            // Calculate totals
            BigDecimal gcpCost = gcpSummary.totalCost();
            BigDecimal timescaleCost = timescaleSummary.totalCost();

            // Calculate subscription costs (prorated for current month)
            BigDecimal subscriptionCosts = calculateSubscriptionCosts(today);

            BigDecimal totalCost = gcpCost.add(timescaleCost).add(subscriptionCosts);

            // Calculate days so far
            int daysSoFar = today.getDayOfMonth();

            // Calculate average daily cost
            BigDecimal avgDailyCost = daysSoFar > 0
                    ? totalCost.divide(BigDecimal.valueOf(daysSoFar), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Combine service breakdown
            Map<String, BigDecimal> costByService = new HashMap<>(gcpSummary.costByService());
            costByService.put("TimescaleDB", timescaleCost);

            // Add subscription costs if enabled
            if (claudeEnabled && subscriptionCosts.compareTo(BigDecimal.ZERO) > 0) {
                costByService.put(claudeName, subscriptionCosts);
            }

            // Get last month for comparison
            String vsLastMonth = getVsLastMonth(totalCost, month);

            return new CostSummary(
                    month,
                    totalCost.setScale(2, RoundingMode.HALF_UP),
                    gcpCost.setScale(2, RoundingMode.HALF_UP),
                    timescaleCost.setScale(2, RoundingMode.HALF_UP),
                    subscriptionCosts.setScale(2, RoundingMode.HALF_UP),
                    "USD",
                    daysSoFar,
                    avgDailyCost,
                    vsLastMonth,
                    costByService
            );
        } catch (Exception e) {
            log.error("Error getting current month summary: {}", e.getMessage(), e);
            return CostSummary.empty(month);
        }
    }

    /**
     * Get daily cost breakdown for specified number of days
     */
    public List<DailyCost> getDailyCosts(int days) {
        log.info("Getting daily costs for last {} days", days);

        try {
            // Get GCP daily costs
            List<GcpDailyCost> gcpDailyCosts = gcpBillingClient.getDailyCosts(days);

            // Get TimescaleDB estimated daily cost
            TimescaleCostSummary timescale = timescaleBillingClient.getCostSummary(
                    LocalDate.now().minusDays(days),
                    LocalDate.now()
            );
            BigDecimal dailyTimescaleCost = days > 0
                    ? timescale.totalCost().divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Combine into daily costs
            List<DailyCost> result = new ArrayList<>();

            for (GcpDailyCost gcpDaily : gcpDailyCosts) {
                Map<String, BigDecimal> breakdown = new HashMap<>(gcpDaily.costByService());
                breakdown.put("TimescaleDB", dailyTimescaleCost);

                // Add daily subscription cost
                BigDecimal dailySubscriptionCost = getDailySubscriptionCost(gcpDaily.date());
                if (claudeEnabled && dailySubscriptionCost.compareTo(BigDecimal.ZERO) > 0) {
                    breakdown.put(claudeName, dailySubscriptionCost);
                }

                BigDecimal totalCost = gcpDaily.totalCost()
                        .add(dailyTimescaleCost)
                        .add(dailySubscriptionCost);

                result.add(new DailyCost(
                        gcpDaily.date().format(DATE_FORMAT),
                        totalCost.setScale(2, RoundingMode.HALF_UP),
                        breakdown
                ));
            }

            return result;
        } catch (Exception e) {
            log.error("Error getting daily costs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get costs grouped by service
     */
    public Map<String, BigDecimal> getCostsByService() {
        log.info("Getting costs by service for current month");

        try {
            GcpCostSummary gcpSummary = gcpBillingClient.getCurrentMonthCosts();
            TimescaleCostSummary timescaleSummary = timescaleBillingClient.getCostSummary(
                    LocalDate.now().withDayOfMonth(1),
                    LocalDate.now()
            );

            Map<String, BigDecimal> costByService = new HashMap<>(gcpSummary.costByService());
            costByService.put("TimescaleDB", timescaleSummary.totalCost());

            // Add subscription costs
            BigDecimal subscriptionCosts = calculateSubscriptionCosts(LocalDate.now());
            if (claudeEnabled && subscriptionCosts.compareTo(BigDecimal.ZERO) > 0) {
                costByService.put(claudeName, subscriptionCosts);
            }

            return costByService;
        } catch (Exception e) {
            log.error("Error getting costs by service: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get Firestore usage metrics
     */
    public List<FirestoreUsage> getFirestoreUsage(int days) {
        log.info("Getting Firestore usage for last {} days", days);

        try {
            // Get from Cloud Monitoring
            List<GcpServiceUsage> metrics = gcpMonitoringClient.getDailyFirestoreMetrics(days);

            // Also check our tracked data
            String endDate = LocalDate.now().format(DATE_FORMAT);
            String startDate = LocalDate.now().minusDays(days).format(DATE_FORMAT);
            List<FirestoreUsageEntity> trackedUsage = firestoreUsageRepository.findByDateRange(startDate, endDate);

            // Convert to response model
            List<FirestoreUsage> result = new ArrayList<>();

            for (FirestoreUsageEntity entity : trackedUsage) {
                List<FirestoreUsage.CollectionUsage> collections = new ArrayList<>();

                if (entity.getReadsByCollection() != null) {
                    for (Map.Entry<String, Long> entry : entity.getReadsByCollection().entrySet()) {
                        String collName = entry.getKey();
                        long reads = entry.getValue();
                        long writes = entity.getWritesByCollection() != null
                                ? entity.getWritesByCollection().getOrDefault(collName, 0L)
                                : 0L;

                        // Estimate cost
                        BigDecimal estimatedCost = estimateFirestoreCost(reads, writes);

                        collections.add(new FirestoreUsage.CollectionUsage(
                                collName,
                                reads,
                                writes,
                                estimatedCost
                        ));
                    }
                }

                result.add(new FirestoreUsage(
                        entity.getDate(),
                        collections,
                        entity.getTotalReads() != null ? entity.getTotalReads() : 0L,
                        entity.getTotalWrites() != null ? entity.getTotalWrites() : 0L,
                        entity.getEstimatedCost() != null ? entity.getEstimatedCost() : BigDecimal.ZERO
                ));
            }

            return result;
        } catch (Exception e) {
            log.error("Error getting Firestore usage: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Scheduled job to aggregate and persist daily costs
     * Runs at 1 AM every day
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void aggregateDailyCosts() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String date = yesterday.format(DATE_FORMAT);

        log.info("Aggregating daily costs for {}", date);

        try {
            // Get GCP costs for yesterday
            GcpCostSummary gcpSummary = gcpBillingClient.getCostSummary(yesterday, yesterday);

            // Get TimescaleDB costs
            TimescaleCostSummary timescaleSummary = timescaleBillingClient.getCostSummary(yesterday, yesterday);

            // Get subscription costs
            BigDecimal subscriptionCost = getDailySubscriptionCost(yesterday);

            // Get Firestore metrics
            List<GcpServiceUsage> firestoreMetrics = gcpMonitoringClient.getFirestoreMetrics();

            // Create and save daily cost entity
            DailyCostEntity entity = new DailyCostEntity(date);
            entity.setGcpCost(gcpSummary.totalCost());
            entity.setTimescaleCost(timescaleSummary.totalCost());
            entity.setTotalCost(gcpSummary.totalCost()
                    .add(timescaleSummary.totalCost())
                    .add(subscriptionCost));
            entity.setCurrency("USD");

            Map<String, BigDecimal> costByService = new HashMap<>(gcpSummary.costByService());
            costByService.put("TimescaleDB", timescaleSummary.totalCost());

            // Add subscription costs
            if (claudeEnabled && subscriptionCost.compareTo(BigDecimal.ZERO) > 0) {
                costByService.put(claudeName, subscriptionCost);
            }

            entity.setCostByService(costByService);

            // Add Firestore metrics
            for (GcpServiceUsage metric : firestoreMetrics) {
                switch (metric.metricName()) {
                    case "document_reads" -> entity.setFirestoreReads(metric.usage().longValue());
                    case "document_writes" -> entity.setFirestoreWrites(metric.usage().longValue());
                    case "document_deletes" -> entity.setFirestoreDeletes(metric.usage().longValue());
                }
            }

            dailyCostRepository.save(entity);
            log.info("Saved daily cost aggregate for {}: ${}", date, entity.getTotalCost());

        } catch (Exception e) {
            log.error("Error aggregating daily costs: {}", e.getMessage(), e);
        }
    }

    private String getVsLastMonth(BigDecimal currentTotal, String currentMonth) {
        try {
            YearMonth lastMonth = YearMonth.parse(currentMonth).minusMonths(1);
            LocalDate lastMonthStart = lastMonth.atDay(1);
            LocalDate lastMonthEnd = lastMonth.atEndOfMonth();

            // Get same number of days from last month
            int currentDays = LocalDate.now().getDayOfMonth();
            LocalDate lastMonthSameDay = lastMonthStart.plusDays(currentDays - 1);
            if (lastMonthSameDay.isAfter(lastMonthEnd)) {
                lastMonthSameDay = lastMonthEnd;
            }

            GcpCostSummary lastMonthGcp = gcpBillingClient.getCostSummary(lastMonthStart, lastMonthSameDay);
            TimescaleCostSummary lastMonthTs = timescaleBillingClient.getCostSummary(lastMonthStart, lastMonthSameDay);

            // Calculate last month subscription costs (prorated for same number of days)
            BigDecimal lastMonthSubscription = calculateSubscriptionCosts(lastMonthSameDay);

            BigDecimal lastMonthTotal = lastMonthGcp.totalCost()
                    .add(lastMonthTs.totalCost())
                    .add(lastMonthSubscription);

            if (lastMonthTotal.compareTo(BigDecimal.ZERO) == 0) {
                return "N/A";
            }

            BigDecimal percentChange = currentTotal.subtract(lastMonthTotal)
                    .divide(lastMonthTotal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            String sign = percentChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            return sign + percentChange.setScale(1, RoundingMode.HALF_UP) + "%";

        } catch (Exception e) {
            log.debug("Could not calculate vs last month: {}", e.getMessage());
            return "N/A";
        }
    }

    private BigDecimal estimateFirestoreCost(long reads, long writes) {
        // Firestore pricing (us-east1):
        // $0.06 per 100,000 reads
        // $0.18 per 100,000 writes
        BigDecimal readCost = BigDecimal.valueOf(reads * 0.0000006);
        BigDecimal writeCost = BigDecimal.valueOf(writes * 0.0000018);
        return readCost.add(writeCost).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate prorated subscription costs for the current month
     */
    private BigDecimal calculateSubscriptionCosts(LocalDate currentDate) {
        if (!claudeEnabled || claudeMonthlyCost == null || claudeMonthlyCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Prorate based on days in month
        int daysInMonth = currentDate.lengthOfMonth();
        int daysSoFar = currentDate.getDayOfMonth();

        // Calculate prorated cost: (monthly cost / days in month) * days so far
        return claudeMonthlyCost
                .divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(daysSoFar))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate daily subscription costs
     */
    private BigDecimal getDailySubscriptionCost(LocalDate date) {
        if (!claudeEnabled || claudeMonthlyCost == null || claudeMonthlyCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        int daysInMonth = date.lengthOfMonth();
        return claudeMonthlyCost
                .divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
