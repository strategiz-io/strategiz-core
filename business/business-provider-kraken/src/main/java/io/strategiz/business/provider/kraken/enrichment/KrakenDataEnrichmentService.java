package io.strategiz.business.provider.kraken.enrichment;

import io.strategiz.business.provider.kraken.constants.KrakenConstants;
import io.strategiz.business.provider.kraken.enrichment.enricher.*;
import io.strategiz.business.provider.kraken.enrichment.model.EnrichedKrakenData;
import io.strategiz.business.provider.kraken.exception.KrakenProviderErrorDetails;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Main orchestrator service for Kraken data enrichment. Coordinates all enrichers to
 * transform raw Kraken data into enriched format before storage to Firestore.
 *
 * This service is called during provider integration/connection to ensure all data stored
 * is already enriched and normalized.
 *
 * @author Strategiz Platform
 * @since 1.0
 */
@Service
public class KrakenDataEnrichmentService {

	private static final Logger log = LoggerFactory.getLogger(KrakenDataEnrichmentService.class);

	private final KrakenSymbolEnricher symbolEnricher;

	private final KrakenAssetMetadataEnricher metadataEnricher;

	private final KrakenStakingEnricher stakingEnricher;

	private final KrakenPriceEnricher priceEnricher;

	@Autowired
	public KrakenDataEnrichmentService(KrakenSymbolEnricher symbolEnricher,
			KrakenAssetMetadataEnricher metadataEnricher, KrakenStakingEnricher stakingEnricher,
			KrakenPriceEnricher priceEnricher) {
		this.symbolEnricher = symbolEnricher;
		this.metadataEnricher = metadataEnricher;
		this.stakingEnricher = stakingEnricher;
		this.priceEnricher = priceEnricher;
	}

	/**
	 * Enrich raw Kraken data with normalized symbols, metadata, and pricing. This is the
	 * main entry point for the enrichment pipeline.
	 * @param userId User ID for context
	 * @param rawBalances Raw balance data from Kraken API (symbol -> quantity)
	 * @param currentPrices Current market prices (may be partial, will be enhanced)
	 * @return Enriched data ready for storage
	 */
	public EnrichedKrakenData enrich(String userId, Map<String, Object> rawBalances,
			Map<String, BigDecimal> currentPrices) {

		log.info("Starting Kraken data enrichment for user: {}", userId);

		EnrichedKrakenData enrichedData = new EnrichedKrakenData();
		enrichedData.setUserId(userId);
		enrichedData.setProviderId("kraken");

		try {
			// Step 1: Normalize symbols (XXBT -> BTC, handle .S and .F suffixes)
			log.debug("Step 1: Normalizing symbols");
			Map<String, Object> normalizedBalances = symbolEnricher.normalizeSymbols(rawBalances);
			enrichedData.setNormalizedBalances(normalizedBalances);

			// Step 2: Add asset metadata (full names, types, categories)
			log.debug("Step 2: Adding asset metadata");
			Map<String, EnrichedKrakenData.AssetInfo> assetInfoMap = metadataEnricher
				.enrichWithMetadata(normalizedBalances);
			enrichedData.setAssetInfo(assetInfoMap);

			// Step 3: Detect and enrich staking information
			log.debug("Step 3: Processing staking information");
			stakingEnricher.enrichStakingData(enrichedData, rawBalances);

			// Step 4: Enrich with current prices and calculate values
			log.debug("Step 4: Adding price data and calculating values");
			priceEnricher.enrichWithPrices(enrichedData, currentPrices);

			// Calculate total portfolio metrics
			calculatePortfolioMetrics(enrichedData);

			log.info("Enrichment complete for user: {} - {} assets enriched, total value: ${}", userId,
					enrichedData.getAssetInfo().size(), enrichedData.getTotalValue());

			return enrichedData;

		}
		catch (Exception e) {
			log.error("Error during Kraken data enrichment for user: {}", userId, e);
			throw new StrategizException(KrakenProviderErrorDetails.DATA_TRANSFORMATION_FAILED,
					"business-provider-kraken", e, userId);
		}
	}

	/**
	 * Transform enriched data into ProviderDataEntity for storage. This creates the final
	 * structure that will be stored in Firestore.
	 * @param enrichedData The enriched Kraken data
	 * @return ProviderDataEntity ready for storage
	 */
	public ProviderDataEntity transformToEntity(EnrichedKrakenData enrichedData) {
		log.debug("Transforming enriched data to ProviderDataEntity");

		ProviderDataEntity entity = new ProviderDataEntity();
		entity.setProviderId(KrakenConstants.PROVIDER_ID);
		entity.setProviderName(KrakenConstants.PROVIDER_NAME);
		entity.setProviderType(KrakenConstants.PROVIDER_TYPE);
		entity.setProviderCategory(KrakenConstants.PROVIDER_CATEGORY);
		entity.setDocumentId(KrakenConstants.PROVIDER_ID);

		// Convert enriched holdings
		List<ProviderDataEntity.Holding> holdings = enrichedData.getAssetInfo()
			.entrySet()
			.stream()
			.filter(entry -> !entry.getValue().isCash()) // Exclude cash from holdings
			.map(entry -> {
				String symbol = entry.getKey();
				EnrichedKrakenData.AssetInfo info = entry.getValue();

				ProviderDataEntity.Holding holding = new ProviderDataEntity.Holding();
				holding.setAsset(info.getNormalizedSymbol());
				holding.setName(info.getFullName());
				holding.setQuantity(info.getQuantity());
				holding.setCurrentPrice(info.getCurrentPrice());
				holding.setCurrentValue(info.getCurrentValue());

				// Add enriched metadata
				holding.setAssetType(info.getAssetType());
				holding.setCategory(info.getCategory());
				holding.setMarketCapRank(info.getMarketCapRank());
				holding.setIsStaked(info.isStaked());
				holding.setStakingAPR(info.getStakingAPR());
				holding.setOriginalSymbol(info.getOriginalSymbol());

				return holding;
			})
			.toList();

		entity.setHoldings(holdings);
		entity.setTotalValue(enrichedData.getTotalValue());
		entity.setCashBalance(enrichedData.getCashBalance());
		entity.setBalances(enrichedData.getNormalizedBalances());

		return entity;
	}

	/**
	 * Calculate portfolio-wide metrics from enriched data
	 */
	private void calculatePortfolioMetrics(EnrichedKrakenData data) {
		BigDecimal totalValue = BigDecimal.ZERO;
		BigDecimal cashBalance = BigDecimal.ZERO;

		for (EnrichedKrakenData.AssetInfo info : data.getAssetInfo().values()) {
			if (info.isCash()) {
				cashBalance = cashBalance.add(info.getCurrentValue());
			}
			totalValue = totalValue.add(info.getCurrentValue());
		}

		data.setTotalValue(totalValue);
		data.setCashBalance(cashBalance);

		log.debug("Portfolio metrics - Total: ${}, Cash: ${}", totalValue, cashBalance);
	}

}