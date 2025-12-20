package io.strategiz.service.marketdata.controller;

import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepositoryImpl;
import io.strategiz.data.marketdata.timescale.entity.MarketDataTimescaleEntity;
import io.strategiz.data.marketdata.timescale.repository.MarketDataTimescaleRepository;
import io.strategiz.data.marketdata.timescale.service.MarketDataConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin endpoint for migrating market data from Firestore to TimescaleDB.
 * Only available when TimescaleDB is enabled.
 */
@RestController
@RequestMapping("/v1/admin/marketdata")
@Tag(name = "Market Data Migration", description = "Admin endpoints for market data migration")
@ConditionalOnProperty(name = "strategiz.timescale.enabled", havingValue = "true")
public class MarketDataMigrationController {

	private static final Logger log = LoggerFactory.getLogger(MarketDataMigrationController.class);

	private static final int BATCH_SIZE = 1000;

	private final MarketDataRepositoryImpl firestoreRepo;

	private final MarketDataTimescaleRepository timescaleRepo;

	private final MarketDataConverter converter;

	@Autowired
	public MarketDataMigrationController(MarketDataRepositoryImpl firestoreRepo,
			MarketDataTimescaleRepository timescaleRepo, MarketDataConverter converter) {
		this.firestoreRepo = firestoreRepo;
		this.timescaleRepo = timescaleRepo;
		this.converter = converter;
	}

	@PostMapping("/migrate-to-timescale")
	@Operation(summary = "Migrate market data from Firestore to TimescaleDB",
			description = "Reads all market data from Firestore and writes to TimescaleDB. "
					+ "Requires X-Admin-Api-Key header.")
	public ResponseEntity<MigrationResult> migrateToTimescale(
			@RequestHeader(value = "X-Admin-Api-Key", required = true) String apiKey) {

		log.info("Starting market data migration from Firestore to TimescaleDB");

		MigrationResult result = new MigrationResult();
		result.startTime = System.currentTimeMillis();

		try {
			// Get all distinct symbols from Firestore
			List<String> symbols = firestoreRepo.findDistinctSymbols();
			log.info("Found {} distinct symbols to migrate", symbols.size());
			result.totalSymbols = symbols.size();

			int totalRecords = 0;
			int migratedRecords = 0;
			int skippedRecords = 0;
			List<String> errors = new ArrayList<>();

			for (String symbol : symbols) {
				try {
					// Read all data for this symbol from Firestore
					List<MarketDataEntity> firestoreData = firestoreRepo.findBySymbol(symbol);
					totalRecords += firestoreData.size();

					// Convert and save to TimescaleDB in batches
					List<MarketDataTimescaleEntity> timescaleData = firestoreData.stream()
						.map(converter::toTimescale)
						.filter(Objects::nonNull)
						.collect(Collectors.toList());

					for (int i = 0; i < timescaleData.size(); i += BATCH_SIZE) {
						int endIndex = Math.min(i + BATCH_SIZE, timescaleData.size());
						List<MarketDataTimescaleEntity> batch = timescaleData.subList(i, endIndex);

						try {
							timescaleRepo.saveAll(batch);
							migratedRecords += batch.size();
						}
						catch (Exception e) {
							// Some records might fail due to duplicates - that's ok
							log.warn("Batch save failed for {}, trying individual saves: {}", symbol, e.getMessage());

							for (MarketDataTimescaleEntity entity : batch) {
								try {
									timescaleRepo.save(entity);
									migratedRecords++;
								}
								catch (Exception ex) {
									skippedRecords++;
								}
							}
						}
					}

					log.info("Migrated {} records for symbol {}", firestoreData.size(), symbol);
				}
				catch (Exception e) {
					errors.add(symbol + ": " + e.getMessage());
					log.error("Error migrating symbol {}: {}", symbol, e.getMessage());
				}
			}

			result.totalRecords = totalRecords;
			result.migratedRecords = migratedRecords;
			result.skippedRecords = skippedRecords;
			result.errors = errors;
			result.success = errors.isEmpty();
			result.endTime = System.currentTimeMillis();
			result.durationMs = result.endTime - result.startTime;

			log.info("Migration completed: {} records migrated, {} skipped, {} errors in {}ms", migratedRecords,
					skippedRecords, errors.size(), result.durationMs);

			return ResponseEntity.ok(result);
		}
		catch (Exception e) {
			log.error("Migration failed", e);
			result.success = false;
			result.errors = Collections.singletonList(e.getMessage());
			result.endTime = System.currentTimeMillis();
			result.durationMs = result.endTime - result.startTime;
			return ResponseEntity.internalServerError().body(result);
		}
	}

	@GetMapping("/migration-status")
	@Operation(summary = "Check current data counts in both databases",
			description = "Returns record counts in Firestore and TimescaleDB for comparison.")
	public ResponseEntity<MigrationStatus> getMigrationStatus(
			@RequestHeader(value = "X-Admin-Api-Key", required = true) String apiKey) {

		MigrationStatus status = new MigrationStatus();

		try {
			// Count Firestore records
			List<String> firestoreSymbols = firestoreRepo.findDistinctSymbols();
			status.firestoreSymbols = firestoreSymbols.size();

			long firestoreCount = 0;
			for (String symbol : firestoreSymbols) {
				firestoreCount += firestoreRepo.countBySymbol(symbol);
			}
			status.firestoreRecords = firestoreCount;

			// Count TimescaleDB records
			List<String> timescaleSymbols = timescaleRepo.findDistinctSymbols();
			status.timescaleSymbols = timescaleSymbols.size();

			long timescaleCount = 0;
			for (String symbol : timescaleSymbols) {
				timescaleCount += timescaleRepo.countBySymbol(symbol);
			}
			status.timescaleRecords = timescaleCount;

			status.migrationComplete = status.firestoreRecords == status.timescaleRecords;

			return ResponseEntity.ok(status);
		}
		catch (Exception e) {
			log.error("Error checking migration status", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * Migration result DTO
	 */
	public static class MigrationResult {

		public boolean success;

		public int totalSymbols;

		public int totalRecords;

		public int migratedRecords;

		public int skippedRecords;

		public List<String> errors;

		public long startTime;

		public long endTime;

		public long durationMs;

	}

	/**
	 * Migration status DTO
	 */
	public static class MigrationStatus {

		public int firestoreSymbols;

		public long firestoreRecords;

		public int timescaleSymbols;

		public long timescaleRecords;

		public boolean migrationComplete;

	}

}
