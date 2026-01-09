package io.strategiz.business.marketdata;

import com.google.cloud.Timestamp;
import io.strategiz.business.marketdata.exception.MarketDataErrorDetails;
import io.strategiz.data.marketdata.entity.MarketDataCoverageEntity;
import io.strategiz.data.marketdata.repository.MarketDataCoverageRepository;
import io.strategiz.data.marketdata.clickhouse.repository.SymbolDataStatusClickHouseRepository;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for calculating and managing market data coverage snapshots.
 *
 * Calculates aggregate statistics about data completeness:
 * - Coverage percentage by timeframe (how many symbols have data)
 * - Storage metrics (TimescaleDB size, row counts)
 * - Data quality distribution (good, partial, poor quality symbols)
 * - Identified gaps in data coverage
 *
 * Snapshots are stored in Firestore and calculated:
 * - Daily (via scheduled job at 2 AM)
 * - On-demand (via admin API)
 *
 * Heavy operation - aggregates data across all symbols and timeframes.
 */
@Service
public class MarketDataCoverageService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataCoverageService.class);

    private final MarketDataCoverageRepository coverageRepository;
    private final SymbolDataStatusClickHouseRepository symbolStatusRepository;
    private final SymbolService symbolService;

    @Autowired
    public MarketDataCoverageService(
            MarketDataCoverageRepository coverageRepository,
            SymbolDataStatusClickHouseRepository symbolStatusRepository,
            SymbolService symbolService) {
        this.coverageRepository = coverageRepository;
        this.symbolStatusRepository = symbolStatusRepository;
        this.symbolService = symbolService;
    }

    /**
     * Calculate current coverage statistics and save snapshot to Firestore.
     * This is a heavy operation that queries TimescaleDB for aggregate stats.
     *
     * @param userId User ID triggering the calculation (typically "system" for scheduled jobs)
     * @return The saved coverage snapshot
     */
    public MarketDataCoverageEntity calculateAndSaveCoverage(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            userId = "system";  // Default for scheduled jobs
        }

        long startTime = System.currentTimeMillis();
        log.info("Starting coverage calculation triggered by: {}", userId);

        try {
            // Create snapshot entity
            MarketDataCoverageEntity snapshot = new MarketDataCoverageEntity();
            Instant now = Instant.now();
            snapshot.setSnapshotId("coverage_" + now.getEpochSecond());
            snapshot.setCalculatedAt(Timestamp.now());

            // Get all symbols that should have data (from ALPACA data source)
            int totalSymbols = symbolService.getSymbolsForCollection("ALPACA").size();
            snapshot.setTotalSymbols(totalSymbols);

            // Standard timeframes to check
            String[] timeframes = {"1Min", "5Min", "15Min", "1Hour", "1Day", "1Week", "1Month"};
            snapshot.setTotalTimeframes(timeframes.length);

            // Calculate coverage for each timeframe
            Map<String, MarketDataCoverageEntity.TimeframeCoverage> byTimeframe = new HashMap<>();
            for (String timeframe : timeframes) {
                MarketDataCoverageEntity.TimeframeCoverage coverage = calculateTimeframeCoverage(timeframe, totalSymbols);
                byTimeframe.put(timeframe, coverage);
            }
            snapshot.setByTimeframe(byTimeframe);

            // Calculate storage metrics (placeholder - would query TimescaleDB)
            MarketDataCoverageEntity.StorageStats storage = new MarketDataCoverageEntity.StorageStats();
            storage.setTimescaleDbRowCount(0L);  // TODO: Query from TimescaleDB
            storage.setTimescaleDbSizeBytes(0L);  // TODO: Query from TimescaleDB
            storage.setFirestoreDocCount(0L);
            storage.setEstimatedCostPerMonth(0.0);
            snapshot.setStorage(storage);

            // Calculate data quality distribution
            MarketDataCoverageEntity.QualityStats quality = calculateQualityStats();
            snapshot.setDataQuality(quality);

            // Detect data gaps (placeholder - would analyze TimescaleDB data)
            List<MarketDataCoverageEntity.DataGap> gaps = detectDataGaps();
            snapshot.setGaps(gaps);

            // Save snapshot to Firestore
            MarketDataCoverageEntity saved = coverageRepository.save(snapshot, userId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Coverage calculation completed in {}ms - snapshot: {}, {} symbols, {} timeframes",
                duration, saved.getSnapshotId(), totalSymbols, timeframes.length);

            return saved;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Coverage calculation failed after {}ms", duration, e);
            throw new StrategizException(
                MarketDataErrorDetails.DATA_FETCH_FAILED,
                "business-marketdata",
                e,
                "Coverage calculation failed"
            );
        }
    }

    /**
     * Calculate coverage statistics for a single timeframe.
     *
     * @param timeframe The timeframe to analyze
     * @param totalSymbols Total number of symbols that should have data
     * @return Coverage statistics for this timeframe
     */
    private MarketDataCoverageEntity.TimeframeCoverage calculateTimeframeCoverage(String timeframe, int totalSymbols) {
        MarketDataCoverageEntity.TimeframeCoverage coverage = new MarketDataCoverageEntity.TimeframeCoverage();

        // Count ACTIVE symbols for this timeframe
        Long activeCount = symbolStatusRepository.countByTimeframeAndStatus(timeframe, "ACTIVE");
        int symbolsWithData = (activeCount != null) ? activeCount.intValue() : 0;

        coverage.setSymbolsWithData(symbolsWithData);

        // Calculate coverage percentage
        double coveragePercent = (totalSymbols > 0)
            ? (double) symbolsWithData / totalSymbols * 100.0
            : 0.0;
        coverage.setCoveragePercent(Math.round(coveragePercent * 100.0) / 100.0);

        // Get freshness stats for this timeframe
        List<Object[]> freshnessStats = symbolStatusRepository.getFreshnessStats();
        for (Object[] stat : freshnessStats) {
            String tf = (String) stat[0];
            if (timeframe.equals(tf)) {
                Long count = (Long) stat[1];
                Double avgAge = (Double) stat[2];

                // Use count as proxy for total bars (would need actual query)
                coverage.setTotalBars(count != null ? count : 0L);

                if (symbolsWithData > 0 && count != null) {
                    coverage.setAvgBarsPerSymbol(count / symbolsWithData);
                } else {
                    coverage.setAvgBarsPerSymbol(0L);
                }
                break;
            }
        }

        // Set date range (placeholder - would query TimescaleDB for actual dates)
        coverage.setDateRangeStart(Instant.now().minus(365, java.time.temporal.ChronoUnit.DAYS).toString());
        coverage.setDateRangeEnd(Instant.now().toString());

        // Find missing symbols (total - active)
        List<String> missingSymbols = new ArrayList<>();
        if (symbolsWithData < totalSymbols) {
            // Would need to query to find exact missing symbols
            missingSymbols.add("(List would be populated from query)");
        }
        coverage.setMissingSymbols(missingSymbols);

        log.debug("Timeframe {} coverage: {}/{} symbols ({:.2f}%)",
            timeframe, symbolsWithData, totalSymbols, coveragePercent);

        return coverage;
    }

    /**
     * Calculate data quality distribution across all symbols.
     *
     * @return Quality statistics
     */
    private MarketDataCoverageEntity.QualityStats calculateQualityStats() {
        MarketDataCoverageEntity.QualityStats quality = new MarketDataCoverageEntity.QualityStats();

        List<Object[]> distribution = symbolStatusRepository.getStatusDistribution();

        int goodQuality = 0;
        int partialQuality = 0;
        int poorQuality = 0;

        for (Object[] statusCount : distribution) {
            String status = (String) statusCount[0];
            Long count = (Long) statusCount[1];

            if ("ACTIVE".equals(status)) {
                goodQuality = count.intValue();
            } else if ("STALE".equals(status)) {
                partialQuality = count.intValue();
            } else if ("FAILED".equals(status)) {
                poorQuality = count.intValue();
            }
        }

        quality.setGoodQuality(goodQuality);
        quality.setPartialQuality(partialQuality);
        quality.setPoorQuality(poorQuality);

        log.debug("Quality distribution - Good: {}, Partial: {}, Poor: {}",
            goodQuality, partialQuality, poorQuality);

        return quality;
    }

    /**
     * Detect gaps in market data coverage.
     * This is a placeholder - would implement proper gap detection logic.
     *
     * @return List of detected gaps
     */
    private List<MarketDataCoverageEntity.DataGap> detectDataGaps() {
        List<MarketDataCoverageEntity.DataGap> gaps = new ArrayList<>();

        // Placeholder - would query TimescaleDB to find actual gaps
        // For now, return empty list
        // Future implementation would:
        // 1. Query for each symbol/timeframe
        // 2. Check for missing date ranges
        // 3. Calculate expected vs actual bar counts
        // 4. Identify trading days with no data

        log.debug("Gap detection: {} gaps found", gaps.size());

        return gaps;
    }

    /**
     * Get the most recent coverage snapshot from Firestore.
     *
     * @return Optional containing the latest snapshot, or empty if none exist
     */
    public Optional<MarketDataCoverageEntity> getLatestSnapshot() {
        long startTime = System.currentTimeMillis();

        Optional<MarketDataCoverageEntity> latest = coverageRepository.findLatest();

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved latest coverage snapshot in {}ms, found: {}",
            duration, latest.isPresent());

        return latest;
    }

    /**
     * Get coverage snapshots within a date range.
     *
     * @param start Start of date range (inclusive)
     * @param end End of date range (exclusive)
     * @return List of snapshots within the range
     */
    public List<MarketDataCoverageEntity> getSnapshotsByDateRange(Timestamp start, Timestamp end) {
        if (start == null || end == null) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "start and end timestamps cannot be null"
            );
        }

        long startTime = System.currentTimeMillis();

        List<MarketDataCoverageEntity> snapshots = coverageRepository.findByDateRange(start, end);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved {} snapshots in date range in {}ms",
            snapshots.size(), duration);

        return snapshots;
    }

    /**
     * Get the N most recent coverage snapshots.
     *
     * @param limit Maximum number of snapshots to return
     * @return List of recent snapshots
     */
    public List<MarketDataCoverageEntity> getRecentSnapshots(int limit) {
        if (limit <= 0) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "limit must be greater than 0"
            );
        }

        long startTime = System.currentTimeMillis();

        List<MarketDataCoverageEntity> snapshots = coverageRepository.findRecent(limit);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved {} recent snapshots in {}ms",
            snapshots.size(), duration);

        return snapshots;
    }

    /**
     * Delete old coverage snapshots based on retention policy.
     *
     * @param retentionDays Number of days to retain snapshots
     * @return Number of snapshots deleted
     */
    public int cleanupOldSnapshots(int retentionDays) {
        if (retentionDays <= 0) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "retentionDays must be greater than 0"
            );
        }

        long startTime = System.currentTimeMillis();

        Instant cutoff = Instant.now().minus(retentionDays, java.time.temporal.ChronoUnit.DAYS);
        Timestamp cutoffTimestamp = Timestamp.ofTimeSecondsAndNanos(cutoff.getEpochSecond(), cutoff.getNano());

        int deleted = coverageRepository.deleteOlderThan(cutoffTimestamp);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Cleaned up {} old coverage snapshots (older than {} days) in {}ms",
            deleted, retentionDays, duration);

        return deleted;
    }

    /**
     * Get snapshot by ID.
     *
     * @param snapshotId The snapshot ID
     * @return Optional containing the snapshot if found
     */
    public Optional<MarketDataCoverageEntity> getSnapshotById(String snapshotId) {
        if (snapshotId == null || snapshotId.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "snapshotId cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        Optional<MarketDataCoverageEntity> snapshot = coverageRepository.findById(snapshotId);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved snapshot {} in {}ms, found: {}",
            snapshotId, duration, snapshot.isPresent());

        return snapshot;
    }
}
