package io.strategiz.business.fundamentals.service;

import io.strategiz.business.fundamentals.exception.FundamentalsErrorDetails;
import io.strategiz.business.fundamentals.model.CollectionResult;
import io.strategiz.business.fundamentals.model.SymbolResult;
import io.strategiz.client.fmp.client.FmpFundamentalsClient;
import io.strategiz.business.fundamentals.converter.FmpFundamentalsConverter;
import io.strategiz.client.fmp.dto.FmpFundamentals;
import io.strategiz.data.fundamentals.entity.FundamentalsEntity;
import io.strategiz.data.fundamentals.repository.FundamentalsRepository;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for collecting company fundamentals data from Financial Modeling Prep (FMP).
 *
 * <p>
 * Responsibilities:
 * - Fetch fundamentals for multiple symbols in batches
 * - Convert FMP DTOs to fundamentals entities
 * - Save to data repository with batch operations
 * - Track success/failure for each symbol
 * </p>
 *
 * <p>
 * Configuration (application.properties):
 * <pre>
 * strategiz.fmp.enabled=true
 * fmp.api-key=your-api-key
 * fundamentals.batch.delay-ms=150
 * </pre>
 * </p>
 */
@Service
public class FundamentalsCollectionService {

	private static final Logger log = LoggerFactory.getLogger(FundamentalsCollectionService.class);

	private final FmpFundamentalsClient fmpClient;

	private final FmpFundamentalsConverter fmpConverter;

	private final FundamentalsRepository repository;

	@Value("${fundamentals.batch.delay-ms:150}")
	private long delayMs;

	private CollectionResult currentJobStatus;

	@Autowired
	public FundamentalsCollectionService(
			@Autowired(required = false) FmpFundamentalsClient fmpClient,
			@Autowired(required = false) FmpFundamentalsConverter fmpConverter,
			FundamentalsRepository repository) {
		this.fmpClient = fmpClient;
		this.fmpConverter = fmpConverter;
		this.repository = repository;
	}

	/**
	 * Update fundamentals for all provided symbols.
	 *
	 * @param symbols List of stock symbols to fetch
	 * @return CollectionResult with statistics
	 */
	public CollectionResult updateFundamentals(List<String> symbols) {
		log.info("Starting fundamentals collection for {} symbols using FMP", symbols.size());

		// Validate FMP is configured
		if (fmpClient == null || fmpConverter == null) {
			throw new StrategizException(FundamentalsErrorDetails.COLLECTION_FAILED,
					"FMP client not configured. Enable with strategiz.fmp.enabled=true and provide fmp.api-key");
		}

		CollectionResult result = new CollectionResult();
		result.setTotalSymbols(symbols.size());
		this.currentJobStatus = result;

		try {
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
	 * Process a single symbol: fetch from FMP, convert, save.
	 *
	 * @param symbol Stock symbol
	 * @return SymbolResult indicating success or failure
	 */
	private SymbolResult processSymbol(String symbol) {
		try {
			log.debug("Processing fundamentals for {} using FMP", symbol);

			// 1. Fetch from FMP
			FmpFundamentals fmpData = fmpClient.getFundamentals(symbol);

			// 2. Convert to entity
			FundamentalsEntity entity = fmpConverter.toEntity(fmpData);

			// 3. Save to database
			repository.save(entity);

			log.debug("Successfully saved fundamentals for {}", symbol);
			return new SymbolResult(symbol, true);
		}
		catch (StrategizException ex) {
			String errorKey = ex.getErrorDetails() != null ? ex.getErrorDetails().getPropertyKey() : "UNKNOWN";
			log.warn("Failed to process fundamentals for {}: {} - {}", symbol, errorKey, ex.getMessage());
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
	public void batchSave(List<FundamentalsEntity> entities) {
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
