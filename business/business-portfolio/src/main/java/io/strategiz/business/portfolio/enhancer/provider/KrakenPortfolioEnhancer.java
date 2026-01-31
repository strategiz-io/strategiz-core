package io.strategiz.business.portfolio.enhancer.provider;

import io.strategiz.business.portfolio.enhancer.business.AssetMetadataBusiness;
import io.strategiz.business.portfolio.enhancer.business.MarketPriceBusiness;
import io.strategiz.business.portfolio.enhancer.business.SymbolNormalizationBusiness;
import io.strategiz.business.portfolio.enhancer.business.SymbolNormalizationBusiness.NormalizedSymbol;
import io.strategiz.business.portfolio.enhancer.model.AssetMetadata;
import io.strategiz.business.portfolio.enhancer.model.EnhancedAsset;
import io.strategiz.business.portfolio.enhancer.model.EnhancedPortfolio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Kraken-specific portfolio enhancer that transforms raw Kraken data into enhanced
 * portfolio with human-readable names and current prices.
 */
@Component
public class KrakenPortfolioEnhancer {

	private static final Logger LOGGER = Logger.getLogger(KrakenPortfolioEnhancer.class.getName());

	private final SymbolNormalizationBusiness symbolNormalization;

	private final AssetMetadataBusiness assetMetadata;

	private final MarketPriceBusiness marketPrice;

	@Autowired
	public KrakenPortfolioEnhancer(SymbolNormalizationBusiness symbolNormalization, AssetMetadataBusiness assetMetadata,
			MarketPriceBusiness marketPrice) {
		this.symbolNormalization = symbolNormalization;
		this.assetMetadata = assetMetadata;
		this.marketPrice = marketPrice;
	}

	/**
	 * Enhance Kraken portfolio data
	 * @param userId User ID
	 * @param krakenBalances Raw balances from Kraken API (symbol -> quantity)
	 * @return Enhanced portfolio with full metadata and prices
	 */
	public EnhancedPortfolio enhance(String userId, Map<String, BigDecimal> krakenBalances) {
		LOGGER.info("Enhancing Kraken portfolio for user: " + userId);

		EnhancedPortfolio portfolio = new EnhancedPortfolio();
		portfolio.setUserId(userId);
		portfolio.setProviderId("kraken");
		portfolio.setProviderName("Kraken");

		if (krakenBalances == null || krakenBalances.isEmpty()) {
			LOGGER.warning("No balances to enhance for user: " + userId);
			return portfolio;
		}

		// Step 1: Normalize all symbols and filter out zero balances
		List<AssetToEnhance> assetsToEnhance = new ArrayList<>();
		for (Map.Entry<String, BigDecimal> entry : krakenBalances.entrySet()) {
			BigDecimal quantity = entry.getValue();

			// Skip zero or negative balances
			if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}

			String krakenSymbol = entry.getKey();
			NormalizedSymbol normalized = symbolNormalization.normalize(krakenSymbol, "kraken");

			AssetToEnhance asset = new AssetToEnhance();
			asset.originalSymbol = krakenSymbol;
			asset.standardSymbol = normalized.getStandardSymbol();
			asset.quantity = quantity;
			asset.assetType = normalized.getAssetType();
			asset.isStaked = normalized.isStaked();
			asset.stakingInfo = normalized.getStakingInfo();

			assetsToEnhance.add(asset);
		}

		// Step 2: Batch fetch prices for all crypto assets
		List<String> cryptoSymbols = assetsToEnhance.stream()
			.filter(a -> "crypto".equals(a.assetType))
			.map(a -> a.standardSymbol)
			.distinct()
			.collect(Collectors.toList());

		Map<String, BigDecimal> cryptoPrices = marketPrice.getBatchPrices(cryptoSymbols, "crypto");

		// Step 3: Create enhanced assets
		for (AssetToEnhance assetInfo : assetsToEnhance) {
			EnhancedAsset enhancedAsset = createEnhancedAsset(assetInfo, cryptoPrices);
			portfolio.addAsset(enhancedAsset);
		}

		// Step 4: Calculate portfolio metrics
		portfolio.calculateMetrics();

		LOGGER.info("Enhanced Kraken portfolio with " + portfolio.getAssets().size() + " assets, total value: $"
				+ portfolio.getTotalValue());

		return portfolio;
	}

	/**
	 * Create an enhanced asset from normalized data
	 */
	private EnhancedAsset createEnhancedAsset(AssetToEnhance assetInfo, Map<String, BigDecimal> cryptoPrices) {
		// Get asset metadata
		AssetMetadata metadata = assetMetadata.getMetadata(assetInfo.standardSymbol);

		// Get current price
		BigDecimal currentPrice = BigDecimal.ZERO;
		if ("crypto".equals(assetInfo.assetType)) {
			currentPrice = cryptoPrices.getOrDefault(assetInfo.standardSymbol, BigDecimal.ZERO);
		}
		else if ("fiat".equals(assetInfo.assetType)) {
			currentPrice = marketPrice.getCurrentPrice(assetInfo.standardSymbol, "fiat");
		}

		// Build enhanced asset
		return new EnhancedAsset.Builder().rawSymbol(assetInfo.originalSymbol)
			.symbol(assetInfo.standardSymbol)
			.name(metadata.getName())
			.assetType(assetInfo.assetType)
			.quantity(assetInfo.quantity)
			.currentPrice(currentPrice)
			.isStaked(assetInfo.isStaked)
			.stakingAPR(assetInfo.stakingInfo)
			.provider("kraken")
			.build();
	}

	/**
	 * Enhance a single Kraken asset (for real-time updates)
	 */
	public EnhancedAsset enhanceAsset(String krakenSymbol, BigDecimal quantity) {
		// Normalize symbol
		NormalizedSymbol normalized = symbolNormalization.normalize(krakenSymbol, "kraken");

		// Get metadata
		AssetMetadata metadata = assetMetadata.getMetadata(normalized.getStandardSymbol());

		// Get current price
		BigDecimal currentPrice = marketPrice.getCurrentPrice(normalized.getStandardSymbol(),
				normalized.getAssetType());

		// Build enhanced asset
		return new EnhancedAsset.Builder().rawSymbol(krakenSymbol)
			.symbol(normalized.getStandardSymbol())
			.name(metadata.getName())
			.assetType(normalized.getAssetType())
			.quantity(quantity)
			.currentPrice(currentPrice)
			.isStaked(normalized.isStaked())
			.stakingAPR(normalized.getStakingInfo())
			.provider("kraken")
			.build();
	}

	/**
	 * Helper class to hold asset information during enhancement
	 */
	private static class AssetToEnhance {

		String originalSymbol;

		String standardSymbol;

		BigDecimal quantity;

		String assetType;

		boolean isStaked;

		String stakingInfo;

	}

}