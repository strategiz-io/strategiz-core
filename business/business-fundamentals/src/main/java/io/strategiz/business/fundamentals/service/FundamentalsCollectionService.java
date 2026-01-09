package io.strategiz.business.fundamentals.service;

import io.strategiz.business.fundamentals.exception.FundamentalsErrorDetails;
import io.strategiz.business.fundamentals.model.CollectionResult;
import io.strategiz.business.fundamentals.model.SymbolResult;
import io.strategiz.client.yahoofinance.client.YahooFundamentalsClient;
import io.strategiz.business.fundamentals.converter.YahooFundamentalsConverter;
import io.strategiz.client.yahoofinance.model.YahooFundamentals;
import io.strategiz.data.fundamentals.timescale.entity.FundamentalsTimescaleEntity;
import io.strategiz.data.marketdata.clickhouse.repository.FundamentalsClickHouseRepository;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Service for collecting company fundamentals data from Yahoo Finance.
 *
 * <p>
 * Responsibilities:
 * - Fetch fundamentals for multiple symbols in batches
 * - Convert Yahoo Finance DTOs to TimescaleDB entities
 * - Save to TimescaleDB with batch operations
 * - Track success/failure for each symbol
 * - Support multi-threaded processing with rate limiting
 * </p>
 *
 * <p>
 * Configuration (application.properties):
 * <pre>
 * fundamentals.batch.thread-pool-size=1
 * fundamentals.batch.batch-size=500
 * fundamentals.batch.delay-ms=150
 * </pre>
 * </p>
 */
@Service
public class FundamentalsCollectionService {

	private static final Logger log = LoggerFactory.getLogger(FundamentalsCollectionService.class);

	private final YahooFundamentalsClient yahooClient;

	private final YahooFundamentalsConverter converter;

	private final FundamentalsClickHouseRepository repository;

	@Value("${fundamentals.batch.thread-pool-size:1}")
	private int threadPoolSize;

	@Value("${fundamentals.batch.batch-size:500}")
	private int batchSize;

	@Value("${fundamentals.batch.delay-ms:150}")
	private long delayMs;

	private CollectionResult currentJobStatus;

	public FundamentalsCollectionService(YahooFundamentalsClient yahooClient, YahooFundamentalsConverter converter,
			FundamentalsClickHouseRepository repository) {
		this.yahooClient = yahooClient;
		this.converter = converter;
		this.repository = repository;
	}

	/**
	 * Update fundamentals for all provided symbols.
	 *
	 * @param symbols List of stock symbols to fetch
	 * @return CollectionResult with statistics
	 */
	public CollectionResult updateFundamentals(List<String> symbols) {
		log.info("Starting fundamentals collection for {} symbols", symbols.size());

		CollectionResult result = new CollectionResult();
		result.setTotalSymbols(symbols.size());
		this.currentJobStatus = result;

		try {
			// Process symbols (single-threaded for politeness to Yahoo Finance)
			for (int i = 0; i < symbols.size(); i++) {
				String symbol = symbols.get(i);

				try {
					SymbolResult symbolResult = processSymbol(symbol);
					result.addSymbolResult(symbolResult);

					if ((i + 1) % 10 == 0) {
						log.info("Progress: {}/{} symbols processed ({} success, {} errors)", i + 1, symbols.size(),
								result.getSuccessCount(), result.getErrorCount());
					}

					// Rate limiting: delay between requests
					if (i < symbols.size() - 1) {
						Thread.sleep(delayMs);
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					log.error("Collection interrupted", ex);
					break;
				}
				catch (Exception ex) {
					log.error("Unexpected error processing symbol {}", symbol, ex);
					result.addSymbolResult(new SymbolResult(symbol, false, ex.getMessage()));
				}
			}

			result.complete();
			log.info("Fundamentals collection complete: {}", result);

			return result;
		}
		catch (Exception ex) {
			result.complete();
			log.error("Fundamentals collection failed", ex);
			throw new StrategizException(FundamentalsErrorDetails.COLLECTION_FAILED,
					"Failed to collect fundamentals", ex);
		}
		finally {
			this.currentJobStatus = null;
		}
	}

	/**
	 * Process a single symbol: fetch, convert, save.
	 *
	 * @param symbol Stock symbol
	 * @return SymbolResult indicating success or failure
	 */
	private SymbolResult processSymbol(String symbol) {
		try {
			log.debug("Processing fundamentals for {}", symbol);

			// 1. Fetch from Yahoo Finance
			YahooFundamentals yahooData = yahooClient.getFundamentals(symbol);

			// 2. Convert to entity
			FundamentalsTimescaleEntity entity = converter.toEntity(yahooData);

			// 3. Save to TimescaleDB
			repository.save(entity);

			log.debug("Successfully saved fundamentals for {}", symbol);
			return new SymbolResult(symbol, true);
		}
		catch (StrategizException ex) {
			log.warn("Failed to process fundamentals for {}: {}", symbol, ex.getErrorDetails().getPropertyKey());
			return new SymbolResult(symbol, false, ex.getMessage());
		}
		catch (Exception ex) {
			log.error("Unexpected error processing fundamentals for {}", symbol, ex);
			return new SymbolResult(symbol, false, ex.getMessage());
		}
	}

	/**
	 * Get current job status (for real-time monitoring).
	 *
	 * @return Current CollectionResult or null if no job running
	 */
	public CollectionResult getCurrentJobStatus() {
		return currentJobStatus;
	}

	/**
	 * Batch save entities (used for bulk operations).
	 *
	 * @param entities List of entities to save
	 */
	public void batchSave(List<FundamentalsTimescaleEntity> entities) {
		if (entities.isEmpty()) {
			return;
		}

		log.info("Batch saving {} fundamentals entities", entities.size());

		try {
			repository.saveAll(entities);
			log.info("Successfully saved {} entities", entities.size());
		}
		catch (Exception ex) {
			log.error("Failed to batch save {} entities", entities.size(), ex);
			throw new StrategizException(FundamentalsErrorDetails.SAVE_FAILED, "Failed to batch save entities", ex);
		}
	}

}
