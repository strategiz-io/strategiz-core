package io.strategiz.service.console.controller;

import com.google.cloud.Timestamp;
import io.strategiz.business.marketdata.MarketDataCoverageService;
import io.strategiz.data.marketdata.entity.MarketDataCoverageEntity;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.model.response.MarketDataCoverageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Console admin controller for market data coverage monitoring. Provides endpoints for
 * viewing coverage snapshots and triggering calculations.
 *
 * Endpoints: - GET /v1/console/marketdata/coverage - Get latest coverage snapshot - POST
 * /v1/console/marketdata/coverage/calculate - Trigger new calculation - GET
 * /v1/console/marketdata/coverage/history - Get historical snapshots
 */
@RestController
@RequestMapping("/v1/console/marketdata/coverage")
@Tag(name = "Console Market Data Coverage", description = "Admin endpoints for market data coverage monitoring")
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class ConsoleMarketDataCoverageController extends BaseController {

	private static final String MODULE_NAME = "CONSOLE";

	private final MarketDataCoverageService coverageService;

	private final io.strategiz.business.marketdata.SymbolDataStatusService symbolDataStatusService;

	@Autowired
	public ConsoleMarketDataCoverageController(MarketDataCoverageService coverageService,
			io.strategiz.business.marketdata.SymbolDataStatusService symbolDataStatusService) {
		this.coverageService = coverageService;
		this.symbolDataStatusService = symbolDataStatusService;
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	/**
	 * Get the latest coverage snapshot.
	 * @param request HTTP request containing adminUserId attribute
	 * @return Latest coverage snapshot or 404 if none exists
	 */
	@GetMapping
	@Operation(summary = "Get latest coverage snapshot",
			description = "Retrieves the most recent market data coverage snapshot")
	public ResponseEntity<MarketDataCoverageResponse> getLatestCoverage(HttpServletRequest request) {

		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getLatestCoverage", adminUserId);

		Optional<MarketDataCoverageEntity> latest = coverageService.getLatestSnapshot();

		if (latest.isEmpty()) {
			log.warn("No coverage snapshots found");
			return ResponseEntity.notFound().build();
		}

		MarketDataCoverageResponse response = convertToResponse(latest.get());

		log.debug("Returning coverage snapshot: {}", response.getSnapshotId());
		return ResponseEntity.ok(response);
	}

	/**
	 * Trigger a new coverage calculation. This is a heavy operation that aggregates data
	 * across all symbols and timeframes.
	 * @param request HTTP request containing adminUserId attribute
	 * @return The newly calculated coverage snapshot
	 */
	@PostMapping("/calculate")
	@Operation(summary = "Calculate coverage snapshot",
			description = "Triggers a new coverage calculation (heavy operation)")
	public ResponseEntity<MarketDataCoverageResponse> calculateCoverage(HttpServletRequest request) {

		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("calculateCoverage", adminUserId);
		log.info("Admin user {} triggering coverage calculation", adminUserId);

		MarketDataCoverageEntity snapshot = coverageService.calculateAndSaveCoverage(adminUserId);

		MarketDataCoverageResponse response = convertToResponse(snapshot);

		log.info("Coverage calculation completed: {}", response.getSnapshotId());
		return ResponseEntity.ok(response);
	}

	/**
	 * Get historical coverage snapshots.
	 * @param request HTTP request containing adminUserId attribute
	 * @param limit Maximum number of snapshots to return (default 30)
	 * @return List of recent coverage snapshots
	 */
	@GetMapping("/history")
	@Operation(summary = "Get coverage history", description = "Retrieves recent coverage snapshots for trend analysis")
	public ResponseEntity<List<MarketDataCoverageResponse>> getCoverageHistory(HttpServletRequest request,
			@RequestParam(defaultValue = "30") int limit) {

		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getCoverageHistory", adminUserId, "limit=" + limit);

		List<MarketDataCoverageEntity> snapshots = coverageService.getRecentSnapshots(limit);

		List<MarketDataCoverageResponse> responses = snapshots.stream()
			.map(this::convertToResponse)
			.collect(Collectors.toList());

		log.debug("Returning {} coverage snapshots", responses.size());
		return ResponseEntity.ok(responses);
	}

	/**
	 * Get data gaps from the latest snapshot.
	 * @param request HTTP request containing adminUserId attribute
	 * @return List of data gaps
	 */
	@GetMapping("/gaps")
	@Operation(summary = "Get data gaps", description = "Retrieves identified gaps in market data coverage")
	public ResponseEntity<List<MarketDataCoverageResponse.DataGap>> getDataGaps(HttpServletRequest request) {

		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getDataGaps", adminUserId);

		Optional<MarketDataCoverageEntity> latest = coverageService.getLatestSnapshot();

		if (latest.isEmpty()) {
			log.warn("No coverage snapshots found");
			return ResponseEntity.notFound().build();
		}

		List<MarketDataCoverageResponse.DataGap> gaps = latest.get()
			.getGaps()
			.stream()
			.map(this::convertDataGap)
			.collect(Collectors.toList());

		log.debug("Returning {} data gaps", gaps.size());
		return ResponseEntity.ok(gaps);
	}

	// Converter methods

	/**
	 * Convert entity to response DTO.
	 */
	private MarketDataCoverageResponse convertToResponse(MarketDataCoverageEntity entity) {
		MarketDataCoverageResponse response = new MarketDataCoverageResponse();
		response.setSnapshotId(entity.getSnapshotId());
		response.setCalculatedAt(formatTimestamp(entity.getCalculatedAt()));
		response.setTotalSymbols(entity.getTotalSymbols());
		response.setTotalTimeframes(entity.getTotalTimeframes());

		// Convert timeframe coverage map
		Map<String, MarketDataCoverageResponse.TimeframeCoverage> byTimeframe = new HashMap<>();
		if (entity.getByTimeframe() != null) {
			entity.getByTimeframe().forEach((timeframe, coverage) -> {
				byTimeframe.put(timeframe, convertTimeframeCoverage(coverage));
			});
		}
		response.setByTimeframe(byTimeframe);

		// Convert storage stats
		if (entity.getStorage() != null) {
			response.setStorage(convertStorageStats(entity.getStorage()));
		}

		// Convert quality stats
		if (entity.getDataQuality() != null) {
			response.setDataQuality(convertQualityStats(entity.getDataQuality()));
		}

		// Convert gaps
		if (entity.getGaps() != null) {
			List<MarketDataCoverageResponse.DataGap> gaps = entity.getGaps()
				.stream()
				.map(this::convertDataGap)
				.collect(Collectors.toList());
			response.setGaps(gaps);
		}

		return response;
	}

	private MarketDataCoverageResponse.TimeframeCoverage convertTimeframeCoverage(
			MarketDataCoverageEntity.TimeframeCoverage entity) {
		MarketDataCoverageResponse.TimeframeCoverage response = new MarketDataCoverageResponse.TimeframeCoverage();
		response.setSymbolsWithData(entity.getSymbolsWithData());
		response.setCoveragePercent(entity.getCoveragePercent());
		response.setTotalBars(entity.getTotalBars());
		response.setAvgBarsPerSymbol(entity.getAvgBarsPerSymbol());
		response.setDateRangeStart(entity.getDateRangeStart());
		response.setDateRangeEnd(entity.getDateRangeEnd());
		response.setMissingSymbols(entity.getMissingSymbols());
		return response;
	}

	private MarketDataCoverageResponse.StorageStats convertStorageStats(MarketDataCoverageEntity.StorageStats entity) {
		MarketDataCoverageResponse.StorageStats response = new MarketDataCoverageResponse.StorageStats();
		response.setTimescaleDbRowCount(entity.getTimescaleDbRowCount());
		response.setTimescaleDbSizeBytes(entity.getTimescaleDbSizeBytes());
		response.setFirestoreDocCount(entity.getFirestoreDocCount());
		response.setEstimatedCostPerMonth(entity.getEstimatedCostPerMonth());
		return response;
	}

	private MarketDataCoverageResponse.QualityStats convertQualityStats(MarketDataCoverageEntity.QualityStats entity) {
		MarketDataCoverageResponse.QualityStats response = new MarketDataCoverageResponse.QualityStats();
		response.setGoodQuality(entity.getGoodQuality());
		response.setPartialQuality(entity.getPartialQuality());
		response.setPoorQuality(entity.getPoorQuality());
		return response;
	}

	private MarketDataCoverageResponse.DataGap convertDataGap(MarketDataCoverageEntity.DataGap entity) {
		MarketDataCoverageResponse.DataGap response = new MarketDataCoverageResponse.DataGap();
		response.setSymbol(entity.getSymbol());
		response.setTimeframe(entity.getTimeframe());
		response.setGapStart(entity.getGapStart());
		response.setGapEnd(entity.getGapEnd());
		response.setMissingBars(entity.getMissingBars());
		return response;
	}

	/**
	 * Get consolidated freshness metrics for the target timeframes. This is the main
	 * endpoint for the consolidated coverage/freshness view.
	 * @param request HTTP request containing adminUserId attribute
	 * @param freshnessThresholdMinutes Freshness threshold in minutes (default 15)
	 * @return Consolidated freshness metrics including overall percentage and
	 * per-timeframe breakdown
	 */
	@GetMapping("/freshness")
	@Operation(summary = "Get freshness metrics",
			description = "Retrieves consolidated freshness metrics for target timeframes")
	public ResponseEntity<Map<String, Object>> getFreshnessMetrics(HttpServletRequest request,
			@RequestParam(defaultValue = "15") int freshnessThresholdMinutes) {

		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getFreshnessMetrics", adminUserId, "threshold=" + freshnessThresholdMinutes);

		try {
			// Target timeframes for S&P 500 coverage tracking
			List<String> targetTimeframes = List.of("1h", "1D", "1W", "1M");
			int totalSymbols = 547; // S&P 500 symbols

			// Calculate freshness metrics per timeframe
			List<Map<String, Object>> timeframeMetrics = symbolDataStatusService
				.calculateFreshnessMetrics(targetTimeframes, freshnessThresholdMinutes);

			// Calculate overall freshness
			long totalPairs = (long) totalSymbols * targetTimeframes.size(); // 547 * 4 =
																				// 2,188
			long totalFreshPairs = timeframeMetrics.stream().mapToLong(m -> (Long) m.get("freshSymbols")).sum();
			double overallFreshnessPercent = (totalFreshPairs * 100.0) / totalPairs;

			// Build response
			Map<String, Object> response = new HashMap<>();
			response.put("overallFreshnessPercent", Math.round(overallFreshnessPercent * 100.0) / 100.0);
			response.put("totalFreshPairs", totalFreshPairs);
			response.put("totalPairs", totalPairs);
			response.put("totalSymbols", totalSymbols);
			response.put("totalTimeframes", targetTimeframes.size());
			response.put("freshnessThresholdMinutes", freshnessThresholdMinutes);
			response.put("timeframeMetrics", timeframeMetrics);
			response.put("calculatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

			log.info("Freshness metrics: {}/{} pairs fresh ({}%)", totalFreshPairs, totalPairs,
					Math.round(overallFreshnessPercent));

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to calculate freshness metrics: {}", e.getMessage(), e);
			throw handleException(e, "freshness-metrics-calculation-failed");
		}
	}

	/**
	 * Format Firestore Timestamp to ISO 8601 string.
	 */
	private String formatTimestamp(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
		return DateTimeFormatter.ISO_INSTANT.format(instant);
	}

}
