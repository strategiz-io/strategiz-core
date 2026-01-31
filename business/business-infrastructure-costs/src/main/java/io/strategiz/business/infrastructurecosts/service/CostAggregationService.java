package io.strategiz.business.infrastructurecosts.service;

import io.strategiz.business.infrastructurecosts.model.CostSummary;
import io.strategiz.business.infrastructurecosts.model.DailyCost;
import io.strategiz.business.infrastructurecosts.model.FirestoreUsage;
import io.strategiz.client.gcpbilling.ClickHouseBillingClient;
import io.strategiz.client.gcpbilling.GcpBillingClient;
import io.strategiz.client.gcpbilling.GcpMonitoringClient;
import io.strategiz.client.gcpbilling.model.ClickHouseCloudCost;
import io.strategiz.client.gcpbilling.model.GcpCostSummary;
import io.strategiz.client.gcpbilling.model.GcpDailyCost;
import io.strategiz.client.gcpbilling.model.GcpServiceUsage;
import io.strategiz.business.infrastructurecosts.model.ClickHouseCostSummary;
import io.strategiz.client.gcpbilling.StripeBillingClient;
import io.strategiz.client.sendgridbilling.SendGridBillingClient;
import io.strategiz.client.sendgridbilling.model.SendGridCostSummary;
import io.strategiz.data.infrastructurecosts.entity.DailyCostEntity;
import io.strategiz.data.infrastructurecosts.entity.FirestoreUsageEntity;
import io.strategiz.data.infrastructurecosts.repository.DailyCostRepository;
import io.strategiz.data.infrastructurecosts.repository.FirestoreUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Service for aggregating infrastructure costs from GCP and ClickHouse. Combines data
 * from multiple sources and persists to Firestore for historical tracking.
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

	private final ClickHouseBillingClient clickHouseBillingClient;

	private final FirestoreClickHouseBillingService firestoreClickHouseBillingService;

	private final SendGridBillingClient sendgridBillingClient;

	private final StripeBillingClient stripeBillingClient;

	private final DailyCostRepository dailyCostRepository;

	private final FirestoreUsageRepository firestoreUsageRepository;

	@Value("${subscriptions.claude.monthly-cost:0}")
	private BigDecimal claudeMonthlyCost;

	@Value("${subscriptions.claude.name:Claude Pro}")
	private String claudeName;

	@Value("${subscriptions.claude.enabled:false}")
	private boolean claudeEnabled;

	@Value("${subscriptions.fmp.monthly-cost:0}")
	private BigDecimal fmpMonthlyCost;

	@Value("${subscriptions.fmp.name:FMP (Financial Modeling Prep)}")
	private String fmpName;

	@Value("${subscriptions.fmp.enabled:false}")
	private boolean fmpEnabled;

	public CostAggregationService(@Autowired(required = false) GcpBillingClient gcpBillingClient,
			@Autowired(required = false) GcpMonitoringClient gcpMonitoringClient,
			@Autowired(required = false) ClickHouseBillingClient clickHouseBillingClient,
			@Autowired(required = false) FirestoreClickHouseBillingService firestoreClickHouseBillingService,
			@Autowired(required = false) SendGridBillingClient sendgridBillingClient,
			@Autowired(required = false) StripeBillingClient stripeBillingClient,
			@Autowired(required = false) DailyCostRepository dailyCostRepository,
			@Autowired(required = false) FirestoreUsageRepository firestoreUsageRepository) {
		this.gcpBillingClient = gcpBillingClient;
		this.gcpMonitoringClient = gcpMonitoringClient;
		this.clickHouseBillingClient = clickHouseBillingClient;
		this.firestoreClickHouseBillingService = firestoreClickHouseBillingService;
		this.sendgridBillingClient = sendgridBillingClient;
		this.stripeBillingClient = stripeBillingClient;
		this.dailyCostRepository = dailyCostRepository;
		this.firestoreUsageRepository = firestoreUsageRepository;

		log.info(
				"CostAggregationService initialized - gcpBilling={}, gcpMonitoring={}, clickhouseCloud={}, clickhouseFirestore={}, sendgrid={}, stripe={}, dailyCostRepo={}, firestoreUsageRepo={}",
				gcpBillingClient != null, gcpMonitoringClient != null, clickHouseBillingClient != null,
				firestoreClickHouseBillingService != null, sendgridBillingClient != null, stripeBillingClient != null,
				dailyCostRepository != null, firestoreUsageRepository != null);
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
			// Get GCP costs (optional)
			GcpCostSummary gcpSummary = gcpBillingClient != null ? gcpBillingClient.getCostSummary(startOfMonth, today)
					: GcpCostSummary.empty(startOfMonth, today);

			// Get ClickHouse costs - prefer Cloud API, fall back to Firestore
			ClickHouseCostSummary clickhouseSummary = getClickHouseCosts(startOfMonth, today);

			// Get SendGrid costs (optional)
			SendGridCostSummary sendgridSummary = sendgridBillingClient != null
					? sendgridBillingClient.getCostSummary(startOfMonth, today)
					: SendGridCostSummary.empty(startOfMonth, today);

			// Get Stripe fees (optional)
			BigDecimal stripeFees = BigDecimal.ZERO;
			if (stripeBillingClient != null && stripeBillingClient.isConfigured()) {
				try {
					Map<String, BigDecimal> stripeData = stripeBillingClient.getFeeSummary(startOfMonth, today);
					stripeFees = stripeData.getOrDefault("totalFees", BigDecimal.ZERO);
				}
				catch (Exception e) {
					log.warn("Could not fetch Stripe fees: {}", e.getMessage());
				}
			}

			// Calculate totals
			BigDecimal gcpCost = gcpSummary.totalCost();
			BigDecimal clickhouseCost = clickhouseSummary.totalCost();
			BigDecimal sendgridCost = sendgridSummary.totalCost();

			// Calculate subscription costs (prorated for current month)
			BigDecimal subscriptionCosts = calculateSubscriptionCosts(today);

			BigDecimal totalCost = gcpCost.add(clickhouseCost).add(sendgridCost).add(stripeFees)
					.add(subscriptionCosts);

			// Calculate days so far
			int daysSoFar = today.getDayOfMonth();

			// Calculate average daily cost
			BigDecimal avgDailyCost = daysSoFar > 0
					? totalCost.divide(BigDecimal.valueOf(daysSoFar), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

			// Combine service breakdown
			Map<String, BigDecimal> costByService = new HashMap<>(gcpSummary.costByService());
			costByService.put("ClickHouse", clickhouseCost);
			costByService.put("SendGrid", sendgridCost);
			if (stripeFees.compareTo(BigDecimal.ZERO) > 0) {
				costByService.put("Stripe", stripeFees);
			}

			// Add subscription costs if enabled
			if (claudeEnabled && claudeMonthlyCost != null && claudeMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
				costByService.put(claudeName, prorateSubscription(claudeMonthlyCost, today));
			}
			if (fmpEnabled && fmpMonthlyCost != null && fmpMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
				costByService.put(fmpName, prorateSubscription(fmpMonthlyCost, today));
			}

			// Get last month for comparison
			String vsLastMonth = getVsLastMonth(totalCost, month);

			return new CostSummary(month, totalCost.setScale(2, RoundingMode.HALF_UP),
					gcpCost.setScale(2, RoundingMode.HALF_UP), clickhouseCost.setScale(2, RoundingMode.HALF_UP),
					sendgridCost.setScale(2, RoundingMode.HALF_UP), subscriptionCosts.setScale(2, RoundingMode.HALF_UP),
					"USD", daysSoFar, avgDailyCost, vsLastMonth, costByService);
		}
		catch (Exception e) {
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
			// Get GCP daily costs (optional)
			List<GcpDailyCost> gcpDailyCosts = gcpBillingClient != null ? gcpBillingClient.getDailyCosts(days)
					: Collections.emptyList();

			if (gcpDailyCosts.isEmpty()) {
				log.warn("No GCP billing client available or no data returned");
				return Collections.emptyList();
			}

			// Get ClickHouse estimated daily cost (prefer Cloud API, fall back to
			// Firestore)
			BigDecimal dailyClickHouseCost = BigDecimal.ZERO;
			if (days > 0) {
				ClickHouseCostSummary clickhouse = getClickHouseCosts(LocalDate.now().minusDays(days), LocalDate.now());
				if (clickhouse.totalCost().compareTo(BigDecimal.ZERO) > 0) {
					dailyClickHouseCost = clickhouse.totalCost()
						.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);
				}
			}

			// Get SendGrid estimated daily cost (optional)
			BigDecimal dailySendGridCost = BigDecimal.ZERO;
			if (sendgridBillingClient != null && days > 0) {
				SendGridCostSummary sendgrid = sendgridBillingClient.getCostSummary(LocalDate.now().minusDays(days),
						LocalDate.now());
				dailySendGridCost = sendgrid.totalCost().divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);
			}

			// Combine into daily costs
			List<DailyCost> result = new ArrayList<>();

			for (GcpDailyCost gcpDaily : gcpDailyCosts) {
				Map<String, BigDecimal> breakdown = new HashMap<>(gcpDaily.costByService());
				breakdown.put("ClickHouse", dailyClickHouseCost);
				breakdown.put("SendGrid", dailySendGridCost);

				// Add daily subscription costs
				BigDecimal dailySubscriptionCost = getDailySubscriptionCost(gcpDaily.date());
				if (claudeEnabled && claudeMonthlyCost != null && claudeMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
					breakdown.put(claudeName,
							claudeMonthlyCost.divide(BigDecimal.valueOf(gcpDaily.date().lengthOfMonth()), 4,
									RoundingMode.HALF_UP));
				}
				if (fmpEnabled && fmpMonthlyCost != null && fmpMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
					breakdown.put(fmpName, fmpMonthlyCost.divide(BigDecimal.valueOf(gcpDaily.date().lengthOfMonth()), 4,
							RoundingMode.HALF_UP));
				}

				BigDecimal totalCost = gcpDaily.totalCost()
					.add(dailyClickHouseCost)
					.add(dailySendGridCost)
					.add(dailySubscriptionCost);

				result.add(new DailyCost(gcpDaily.date().format(DATE_FORMAT),
						totalCost.setScale(2, RoundingMode.HALF_UP), breakdown));
			}

			return result;
		}
		catch (Exception e) {
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
			// Get GCP costs (optional)
			GcpCostSummary gcpSummary = gcpBillingClient != null ? gcpBillingClient.getCurrentMonthCosts()
					: GcpCostSummary.empty(LocalDate.now().withDayOfMonth(1), LocalDate.now());

			Map<String, BigDecimal> costByService = new HashMap<>(gcpSummary.costByService());

			// Add ClickHouse costs (prefer Cloud API, fall back to Firestore)
			ClickHouseCostSummary clickhouseSummary = getClickHouseCosts(LocalDate.now().withDayOfMonth(1),
					LocalDate.now());
			if (clickhouseSummary.totalCost().compareTo(BigDecimal.ZERO) > 0) {
				costByService.put("ClickHouse", clickhouseSummary.totalCost());
			}

			// Add SendGrid costs if available
			if (sendgridBillingClient != null) {
				SendGridCostSummary sendgridSummary = sendgridBillingClient
					.getCostSummary(LocalDate.now().withDayOfMonth(1), LocalDate.now());
				costByService.put("SendGrid", sendgridSummary.totalCost());
			}

			// Add Stripe fees if available
			if (stripeBillingClient != null && stripeBillingClient.isConfigured()) {
				try {
					Map<String, BigDecimal> stripeData = stripeBillingClient
						.getFeeSummary(LocalDate.now().withDayOfMonth(1), LocalDate.now());
					BigDecimal stripeFees = stripeData.getOrDefault("totalFees", BigDecimal.ZERO);
					if (stripeFees.compareTo(BigDecimal.ZERO) > 0) {
						costByService.put("Stripe", stripeFees);
					}
				}
				catch (Exception e) {
					log.warn("Could not fetch Stripe fees: {}", e.getMessage());
				}
			}

			// Add subscription costs
			LocalDate now = LocalDate.now();
			if (claudeEnabled && claudeMonthlyCost != null && claudeMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
				costByService.put(claudeName, prorateSubscription(claudeMonthlyCost, now));
			}
			if (fmpEnabled && fmpMonthlyCost != null && fmpMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
				costByService.put(fmpName, prorateSubscription(fmpMonthlyCost, now));
			}

			return costByService;
		}
		catch (Exception e) {
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
			// Get from Cloud Monitoring (optional)
			List<GcpServiceUsage> metrics = gcpMonitoringClient != null
					? gcpMonitoringClient.getDailyFirestoreMetrics(days) : Collections.emptyList();

			// Also check our tracked data (optional)
			String endDate = LocalDate.now().format(DATE_FORMAT);
			String startDate = LocalDate.now().minusDays(days).format(DATE_FORMAT);
			List<FirestoreUsageEntity> trackedUsage = firestoreUsageRepository != null
					? firestoreUsageRepository.findByDateRange(startDate, endDate) : Collections.emptyList();

			// Convert to response model
			List<FirestoreUsage> result = new ArrayList<>();

			for (FirestoreUsageEntity entity : trackedUsage) {
				List<FirestoreUsage.CollectionUsage> collections = new ArrayList<>();

				if (entity.getReadsByCollection() != null) {
					for (Map.Entry<String, Long> entry : entity.getReadsByCollection().entrySet()) {
						String collName = entry.getKey();
						long reads = entry.getValue();
						long writes = entity.getWritesByCollection() != null
								? entity.getWritesByCollection().getOrDefault(collName, 0L) : 0L;

						// Estimate cost
						BigDecimal estimatedCost = estimateFirestoreCost(reads, writes);

						collections.add(new FirestoreUsage.CollectionUsage(collName, reads, writes, estimatedCost));
					}
				}

				result.add(new FirestoreUsage(entity.getDate(), collections,
						entity.getTotalReads() != null ? entity.getTotalReads() : 0L,
						entity.getTotalWrites() != null ? entity.getTotalWrites() : 0L,
						entity.getEstimatedCost() != null ? entity.getEstimatedCost() : BigDecimal.ZERO));
			}

			return result;
		}
		catch (Exception e) {
			log.error("Error getting Firestore usage: {}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	/**
	 * Scheduled job to aggregate and persist daily costs Runs at 1 AM every day
	 */
	@Scheduled(cron = "0 0 1 * * *")
	public void aggregateDailyCosts() {
		LocalDate yesterday = LocalDate.now().minusDays(1);
		String date = yesterday.format(DATE_FORMAT);

		log.info("Aggregating daily costs for {}", date);

		// Skip if repository is not available
		if (dailyCostRepository == null) {
			log.warn("DailyCostRepository not available, skipping cost aggregation");
			return;
		}

		try {
			// Get GCP costs for yesterday (optional)
			GcpCostSummary gcpSummary = gcpBillingClient != null ? gcpBillingClient.getCostSummary(yesterday, yesterday)
					: GcpCostSummary.empty(yesterday, yesterday);

			// Get ClickHouse costs (prefer Cloud API, fall back to Firestore)
			ClickHouseCostSummary clickhouseSummary = getClickHouseCosts(yesterday, yesterday);

			// Get SendGrid costs (optional)
			SendGridCostSummary sendgridSummary = sendgridBillingClient != null
					? sendgridBillingClient.getCostSummary(yesterday, yesterday)
					: SendGridCostSummary.empty(yesterday, yesterday);

			// Get subscription costs
			BigDecimal subscriptionCost = getDailySubscriptionCost(yesterday);

			// Get Firestore metrics (optional)
			List<GcpServiceUsage> firestoreMetrics = gcpMonitoringClient != null
					? gcpMonitoringClient.getFirestoreMetrics() : Collections.emptyList();

			// Create and save daily cost entity
			DailyCostEntity entity = new DailyCostEntity(date);
			entity.setGcpCost(gcpSummary.totalCost());
			entity.setClickhouseCost(clickhouseSummary.totalCost());
			entity.setTotalCost(gcpSummary.totalCost()
				.add(clickhouseSummary.totalCost())
				.add(sendgridSummary.totalCost())
				.add(subscriptionCost));
			entity.setCurrency("USD");

			Map<String, BigDecimal> costByService = new HashMap<>(gcpSummary.costByService());
			if (clickhouseSummary.totalCost().compareTo(BigDecimal.ZERO) > 0) {
				costByService.put("ClickHouse", clickhouseSummary.totalCost());
			}
			if (sendgridBillingClient != null) {
				costByService.put("SendGrid", sendgridSummary.totalCost());
			}

			// Add subscription costs
			if (claudeEnabled && claudeMonthlyCost != null && claudeMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
				costByService.put(claudeName,
						claudeMonthlyCost.divide(BigDecimal.valueOf(yesterday.lengthOfMonth()), 4,
								RoundingMode.HALF_UP));
			}
			if (fmpEnabled && fmpMonthlyCost != null && fmpMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
				costByService.put(fmpName, fmpMonthlyCost.divide(BigDecimal.valueOf(yesterday.lengthOfMonth()), 4,
						RoundingMode.HALF_UP));
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

		}
		catch (Exception e) {
			log.error("Error aggregating daily costs: {}", e.getMessage(), e);
		}
	}

	/**
	 * Get ClickHouse costs - prefers Cloud API, falls back to Firestore data
	 */
	private ClickHouseCostSummary getClickHouseCosts(LocalDate startDate, LocalDate endDate) {
		// Prefer ClickHouse Cloud API for real-time data
		if (clickHouseBillingClient != null && clickHouseBillingClient.isConfigured()) {
			try {
				ClickHouseCloudCost cloudCost = clickHouseBillingClient.getCostSummary(startDate, endDate);
				// Convert ClickHouseCloudCost to ClickHouseCostSummary
				return new ClickHouseCostSummary(startDate, endDate, cloudCost.totalCostUsd(), "USD",
						cloudCost.computeCostChc(), cloudCost.storageCostChc(), BigDecimal.ZERO, // storageUsageGb
																									// not
																									// available
																									// from
																									// API
						BigDecimal.ZERO // computeHours not available from API
				);
			}
			catch (Exception e) {
				log.warn("Failed to get ClickHouse costs from Cloud API, falling back to Firestore: {}",
						e.getMessage());
			}
		}

		// Fall back to Firestore data
		if (firestoreClickHouseBillingService != null) {
			return firestoreClickHouseBillingService.getCostSummary(startDate, endDate);
		}

		return ClickHouseCostSummary.empty(startDate, endDate);
	}

	private String getVsLastMonth(BigDecimal currentTotal, String currentMonth) {
		// Skip comparison if GCP billing client is not available
		if (gcpBillingClient == null) {
			return "N/A";
		}

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

			// Get ClickHouse costs (prefer Cloud API, fall back to Firestore)
			ClickHouseCostSummary lastMonthCh = getClickHouseCosts(lastMonthStart, lastMonthSameDay);

			SendGridCostSummary lastMonthSg = sendgridBillingClient != null
					? sendgridBillingClient.getCostSummary(lastMonthStart, lastMonthSameDay)
					: SendGridCostSummary.empty(lastMonthStart, lastMonthSameDay);

			// Calculate last month subscription costs (prorated for same number of days)
			BigDecimal lastMonthSubscription = calculateSubscriptionCosts(lastMonthSameDay);

			BigDecimal lastMonthTotal = lastMonthGcp.totalCost()
				.add(lastMonthCh.totalCost())
				.add(lastMonthSg.totalCost())
				.add(lastMonthSubscription);

			if (lastMonthTotal.compareTo(BigDecimal.ZERO) == 0) {
				return "N/A";
			}

			BigDecimal percentChange = currentTotal.subtract(lastMonthTotal)
				.divide(lastMonthTotal, 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100));

			String sign = percentChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
			return sign + percentChange.setScale(1, RoundingMode.HALF_UP) + "%";

		}
		catch (Exception e) {
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
		BigDecimal total = BigDecimal.ZERO;
		int daysInMonth = currentDate.lengthOfMonth();
		int daysSoFar = currentDate.getDayOfMonth();

		if (claudeEnabled && claudeMonthlyCost != null && claudeMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
			total = total.add(claudeMonthlyCost.divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(daysSoFar)));
		}
		if (fmpEnabled && fmpMonthlyCost != null && fmpMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
			total = total.add(fmpMonthlyCost.divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(daysSoFar)));
		}

		return total.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * Calculate daily subscription costs
	 */
	private BigDecimal getDailySubscriptionCost(LocalDate date) {
		BigDecimal total = BigDecimal.ZERO;
		int daysInMonth = date.lengthOfMonth();

		if (claudeEnabled && claudeMonthlyCost != null && claudeMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
			total = total.add(claudeMonthlyCost.divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP));
		}
		if (fmpEnabled && fmpMonthlyCost != null && fmpMonthlyCost.compareTo(BigDecimal.ZERO) > 0) {
			total = total.add(fmpMonthlyCost.divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP));
		}

		return total.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal prorateSubscription(BigDecimal monthlyCost, LocalDate currentDate) {
		int daysInMonth = currentDate.lengthOfMonth();
		int daysSoFar = currentDate.getDayOfMonth();
		return monthlyCost.divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP)
			.multiply(BigDecimal.valueOf(daysSoFar))
			.setScale(2, RoundingMode.HALF_UP);
	}

}
