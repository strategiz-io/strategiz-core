package io.strategiz.batch.fundamentals;

import io.strategiz.business.fundamentals.model.CollectionResult;
import io.strategiz.business.fundamentals.service.FundamentalsCollectionService;
import io.strategiz.business.marketdata.SymbolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manual job for backfilling company fundamentals data.
 *
 * <p>
 * This job is designed for one-time operations such as:
 * - Initial data population for new deployments
 * - Full data refresh after system maintenance
 * - Recovering from extended data collection outages
 * </p>
 *
 * <p>
 * Unlike the incremental job, this job:
 * - Does NOT run on a schedule (manual trigger only)
 * - Processes all configured symbols regardless of last update time
 * - Can be used to refresh data for specific symbols or all symbols
 * </p>
 *
 * <p>
 * Configuration:
 * <pre>
 * fundamentals.batch.data-source=YAHOO
 * </pre>
 * </p>
 *
 * <p>
 * Trigger via admin console endpoints.
 * Only one instance can run at a time (enforced with AtomicBoolean).
 * </p>
 */
@Component
public class FundamentalsBackfillJob {

	private static final Logger log = LoggerFactory.getLogger(FundamentalsBackfillJob.class);

	private final FundamentalsCollectionService collectionService;

	private final SymbolService symbolService;

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	@Value("${fundamentals.batch.data-source:YAHOO}")
	private String dataSource;

	@Autowired
	public FundamentalsBackfillJob(FundamentalsCollectionService collectionService, SymbolService symbolService) {
		this.collectionService = collectionService;
		this.symbolService = symbolService;
	}

	/**
	 * Execute full backfill for all configured symbols.
	 *
	 * This method is called from the admin console to perform a complete
	 * fundamentals data refresh.
	 *
	 * @return CollectionResult with execution statistics
	 */
	public CollectionResult executeBackfill() {
		return executeBackfill(null);
	}

	/**
	 * Execute backfill for a specific list of symbols.
	 *
	 * If symbols list is null or empty, processes all configured symbols.
	 *
	 * @param symbols List of symbols to backfill, or null for all symbols
	 * @return CollectionResult with execution statistics
	 */
	public CollectionResult executeBackfill(List<String> symbols) {
		// Ensure only one backfill job runs at a time
		if (!isRunning.compareAndSet(false, true)) {
			log.warn("Fundamentals backfill job is already running, skipping execution");
			CollectionResult alreadyRunning = new CollectionResult();
			alreadyRunning.setTotalSymbols(0);
			return alreadyRunning;
		}

		try {
			log.info("=== Fundamentals Backfill Started ===");

			// Get list of symbols to collect
			List<String> targetSymbols;
			if (symbols == null || symbols.isEmpty()) {
				targetSymbols = symbolService.getSymbolsForCollection(dataSource);
				log.info("Backfilling all {} symbols configured for {} fundamentals", targetSymbols.size(),
						dataSource);
			}
			else {
				targetSymbols = symbols;
				log.info("Backfilling {} specified symbols for {} fundamentals", targetSymbols.size(), dataSource);
			}

			if (targetSymbols.isEmpty()) {
				log.warn("No symbols to backfill, exiting");
				CollectionResult emptyResult = new CollectionResult();
				emptyResult.setTotalSymbols(0);
				return emptyResult;
			}

			// Execute collection
			CollectionResult result = collectionService.updateFundamentals(targetSymbols);

			// Log results
			log.info("=== Fundamentals Backfill Completed ===");
			log.info("Total symbols: {}", result.getTotalSymbols());
			log.info("Successful: {}", result.getSuccessCount());
			log.info("Errors: {}", result.getErrorCount());
			log.info("Duration: {} seconds", result.getDurationSeconds());

			if (result.getErrorCount() > 0) {
				log.warn("Backfill completed with {} errors - check logs for details", result.getErrorCount());
			}

			return result;
		}
		catch (Exception ex) {
			log.error("Fundamentals backfill failed with exception", ex);

			CollectionResult errorResult = new CollectionResult();
			errorResult.setTotalSymbols(0);
			return errorResult;
		}
		finally {
			isRunning.set(false);
		}
	}

	/**
	 * Check if the backfill job is currently running.
	 *
	 * @return true if the job is running
	 */
	public boolean isRunning() {
		return isRunning.get();
	}

}
