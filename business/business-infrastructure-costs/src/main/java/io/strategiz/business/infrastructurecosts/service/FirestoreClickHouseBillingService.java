package io.strategiz.business.infrastructurecosts.service;

import io.strategiz.business.infrastructurecosts.model.ClickHouseCostSummary;
import io.strategiz.business.infrastructurecosts.model.ClickHouseUsage;
import io.strategiz.data.infrastructurecosts.entity.ClickHouseCostEntity;
import io.strategiz.data.infrastructurecosts.entity.ClickHouseUsageEntity;
import io.strategiz.data.infrastructurecosts.repository.ClickHouseCostRepository;
import io.strategiz.data.infrastructurecosts.repository.ClickHouseUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for managing ClickHouseDB billing data in Firestore. Replaces
 * ClickHouseBillingClient by storing and retrieving billing data from Firestore instead
 * of querying the ClickHouseDB Cloud API.
 */
@Service
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class FirestoreClickHouseBillingService {

	private static final Logger log = LoggerFactory.getLogger(FirestoreClickHouseBillingService.class);

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final ClickHouseCostRepository costRepo;

	private final ClickHouseUsageRepository usageRepo;

	public FirestoreClickHouseBillingService(ClickHouseCostRepository costRepo, ClickHouseUsageRepository usageRepo) {
		this.costRepo = costRepo;
		this.usageRepo = usageRepo;
		log.info("FirestoreClickHouseBillingService initialized - using Firestore for ClickHouseDB billing data");
	}

	/**
	 * Get cost summary for a date range (aggregated from Firestore)
	 */
	public ClickHouseCostSummary getCostSummary(LocalDate startDate, LocalDate endDate) {
		log.debug("Getting ClickHouseDB cost summary for {} to {}", startDate, endDate);

		String startDateStr = startDate.format(DATE_FORMAT);
		String endDateStr = endDate.format(DATE_FORMAT);

		List<ClickHouseCostEntity> costs = costRepo.findByDateRange(startDateStr, endDateStr);

		if (costs.isEmpty()) {
			log.debug("No cost data found for date range, returning empty summary");
			return ClickHouseCostSummary.empty(startDate, endDate);
		}

		// Aggregate costs across all dates in range
		BigDecimal totalCost = BigDecimal.ZERO;
		BigDecimal computeCost = BigDecimal.ZERO;
		BigDecimal storageCost = BigDecimal.ZERO;
		BigDecimal storageUsageGb = BigDecimal.ZERO;
		BigDecimal computeHours = BigDecimal.ZERO;
		String currency = "USD";

		for (ClickHouseCostEntity entity : costs) {
			totalCost = totalCost.add(entity.getTotalCost() != null ? entity.getTotalCost() : BigDecimal.ZERO);
			computeCost = computeCost.add(entity.getComputeCost() != null ? entity.getComputeCost() : BigDecimal.ZERO);
			storageCost = storageCost.add(entity.getStorageCost() != null ? entity.getStorageCost() : BigDecimal.ZERO);
			storageUsageGb = storageUsageGb
				.add(entity.getStorageUsageGb() != null ? entity.getStorageUsageGb() : BigDecimal.ZERO);
			computeHours = computeHours
				.add(entity.getComputeHours() != null ? entity.getComputeHours() : BigDecimal.ZERO);

			if (entity.getCurrency() != null) {
				currency = entity.getCurrency();
			}
		}

		log.debug("Aggregated {} cost records: totalCost={}", costs.size(), totalCost);

		return new ClickHouseCostSummary(startDate, endDate, totalCost, currency, computeCost, storageCost,
				storageUsageGb, computeHours);
	}

	/**
	 * Get current usage (most recent record from Firestore)
	 */
	public ClickHouseUsage getCurrentUsage() {
		log.debug("Getting current ClickHouseDB usage");

		List<ClickHouseUsageEntity> recentUsage = usageRepo.findRecent(1);

		if (recentUsage.isEmpty()) {
			log.debug("No usage data found, returning empty usage");
			return new ClickHouseUsage(null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
					Instant.now());
		}

		ClickHouseUsageEntity entity = recentUsage.get(0);
		return new ClickHouseUsage(entity.getServiceId(), entity.getServiceName(),
				entity.getStorageUsedGb() != null ? entity.getStorageUsedGb() : BigDecimal.ZERO,
				entity.getComputeHours() != null ? entity.getComputeHours() : BigDecimal.ZERO,
				entity.getDataIngestedGb() != null ? entity.getDataIngestedGb() : BigDecimal.ZERO,
				entity.getQueriesExecuted() != null ? entity.getQueriesExecuted() : BigDecimal.ZERO,
				entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now());
	}

	/**
	 * Save cost data to Firestore (for manual entry or migration)
	 */
	public void saveCostData(LocalDate date, ClickHouseCostSummary summary) {
		log.info("Saving ClickHouseDB cost data for {}", date);

		String dateStr = date.format(DATE_FORMAT);

		ClickHouseCostEntity entity = new ClickHouseCostEntity(dateStr);
		entity.setTotalCost(summary.totalCost());
		entity.setCurrency(summary.currency());
		entity.setComputeCost(summary.computeCost());
		entity.setStorageCost(summary.storageCost());
		entity.setStorageUsageGb(summary.storageUsageGb());
		entity.setComputeHours(summary.computeHours());
		entity.setUpdatedAt(Instant.now());

		costRepo.save(entity);
		log.debug("Saved ClickHouseDB cost data for {}: totalCost={}", date, summary.totalCost());
	}

	/**
	 * Save usage data to Firestore (for manual entry or migration)
	 */
	public void saveUsageData(LocalDate date, ClickHouseUsage usage) {
		log.info("Saving ClickHouseDB usage data for {}", date);

		String dateStr = date.format(DATE_FORMAT);

		ClickHouseUsageEntity entity = new ClickHouseUsageEntity(dateStr);
		entity.setServiceId(usage.serviceId());
		entity.setServiceName(usage.serviceName());
		entity.setStorageUsedGb(usage.storageUsedGb());
		entity.setComputeHours(usage.computeHours());
		entity.setDataIngestedGb(usage.dataIngestedGb());
		entity.setQueriesExecuted(usage.queriesExecuted());
		entity.setUpdatedAt(Instant.now());

		usageRepo.save(entity);
		log.debug("Saved ClickHouseDB usage data for {}", date);
	}

}
