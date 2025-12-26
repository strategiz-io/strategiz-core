package io.strategiz.batch.fundamentals;

import io.strategiz.business.fundamentals.model.CollectionResult;
import io.strategiz.business.fundamentals.service.FundamentalsCollectionService;
import io.strategiz.business.marketdata.SymbolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled job for daily company fundamentals data collection.
 *
 * <p>
 * This job runs daily at 2 AM (configurable) to update fundamental data
 * for all active symbols from Yahoo Finance. Unlike market data collection,
 * this runs once per day as fundamental data changes infrequently.
 * </p>
 *
 * <p>
 * Configuration:
 * <pre>
 * fundamentals.batch.incremental-enabled=true
 * fundamentals.batch.incremental-cron=0 0 2 * * *
 * </pre>
 * </p>
 *
 * <p>
 * The job can also be triggered manually via the admin console.
 * Only one instance can run at a time (enforced with AtomicBoolean).
 * </p>
 */
@Component
@Profile("scheduler")
public class FundamentalsIncrementalJob {

	private static final Logger log = LoggerFactory.getLogger(FundamentalsIncrementalJob.class);

	private final FundamentalsCollectionService collectionService;

	private final SymbolService symbolService;

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	@Value("${fundamentals.batch.incremental-enabled:false}")
	private boolean incrementalEnabled;

	@Value("${fundamentals.batch.data-source:YAHOO}")
	private String dataSource;

	@Autowired
	public FundamentalsIncrementalJob(FundamentalsCollectionService collectionService, SymbolService symbolService) {
		this.collectionService = collectionService;
		this.symbolService = symbolService;
	}

	/**
	 * Scheduled daily fundamentals update job.
	 *
	 * Runs at 2 AM daily (configurable via fundamentals.batch.incremental-cron).
	 * Can be disabled via fundamentals.batch.incremental-enabled=false.
	 */
	@Scheduled(cron = "${fundamentals.batch.incremental-cron:0 0 2 * * *}")
	public void executeScheduledIncremental() {
		if (!incrementalEnabled) {
			log.debug("Fundamentals incremental job is disabled");
			return;
		}

		log.info("Starting scheduled fundamentals incremental collection");
		executeIncremental();
	}

	/**
	 * Manually trigger fundamentals collection.
	 *
	 * This method can be called from the admin console to trigger an
	 * immediate fundamentals update outside the regular schedule.
	 *
	 * @return CollectionResult with statistics
	 */
	public CollectionResult triggerManualExecution() {
		log.info("Manual fundamentals incremental collection triggered");
		return executeIncremental();
	}

	/**
	 * Execute the fundamentals collection process.
	 *
	 * Thread-safe: Only one instance can run at a time.
	 *
	 * @return CollectionResult with execution statistics
	 */
	private CollectionResult executeIncremental() {
		// Ensure only one job runs at a time
		if (!isRunning.compareAndSet(false, true)) {
			log.warn("Fundamentals incremental job is already running, skipping execution");
			CollectionResult alreadyRunning = new CollectionResult();
			alreadyRunning.setTotalSymbols(0);
			return alreadyRunning;
		}

		try {
			log.info("=== Fundamentals Incremental Collection Started ===");

			// Get list of symbols to collect
			List<String> symbols = symbolService.getSymbolsForCollection(dataSource);
			log.info("Found {} symbols configured for {} fundamentals collection", symbols.size(), dataSource);

			if (symbols.isEmpty()) {
				log.warn("No symbols configured for fundamentals collection, exiting");
				CollectionResult emptyResult = new CollectionResult();
				emptyResult.setTotalSymbols(0);
				return emptyResult;
			}

			// Execute collection
			CollectionResult result = collectionService.updateFundamentals(symbols);

			// Log results
			log.info("=== Fundamentals Incremental Collection Completed ===");
			log.info("Total symbols: {}", result.getTotalSymbols());
			log.info("Successful: {}", result.getSuccessCount());
			log.info("Errors: {}", result.getErrorCount());
			log.info("Duration: {} seconds", result.getDurationSeconds());

			return result;
		}
		catch (Exception ex) {
			log.error("Fundamentals incremental collection failed with exception", ex);

			CollectionResult errorResult = new CollectionResult();
			errorResult.setTotalSymbols(0);
			return errorResult;
		}
		finally {
			isRunning.set(false);
		}
	}

	/**
	 * Check if the job is currently running.
	 *
	 * @return true if the job is running
	 */
	public boolean isRunning() {
		return isRunning.get();
	}

	/**
	 * Get the current job configuration status.
	 *
	 * @return true if the job is enabled
	 */
	public boolean isEnabled() {
		return incrementalEnabled;
	}

}
