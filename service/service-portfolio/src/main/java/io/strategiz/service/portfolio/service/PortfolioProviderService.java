package io.strategiz.service.portfolio.service;

import io.strategiz.business.portfolio.enhancer.PortfolioEnhancementOrchestrator;
import io.strategiz.business.portfolio.enhancer.model.EnhancedAsset;
import io.strategiz.business.portfolio.enhancer.model.EnhancedPortfolio;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.repository.ReadProviderDataRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.portfolio.model.response.PortfolioPositionResponse;
import io.strategiz.service.portfolio.model.response.ProviderPortfolioResponse;
import io.strategiz.service.portfolio.constants.ServicePortfolioConstants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import io.strategiz.service.base.BaseService;

/**
 * Service for handling provider-specific portfolio operations. Single Responsibility:
 * Manages individual provider portfolio data.
 */
@Service
public class PortfolioProviderService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-portfolio";
	}

	private final ReadProviderDataRepository providerDataRepository;

	private final UserRepository userRepository;

	private final PortfolioEnhancementOrchestrator portfolioEnhancer;

	@Autowired
	public PortfolioProviderService(ReadProviderDataRepository providerDataRepository, UserRepository userRepository,
			@Autowired(required = false) PortfolioEnhancementOrchestrator portfolioEnhancer) {
		this.providerDataRepository = providerDataRepository;
		this.userRepository = userRepository;
		this.portfolioEnhancer = portfolioEnhancer;
	}

	/**
	 * Get portfolio data for a specific provider.
	 * @param userId User ID
	 * @param providerId Provider ID (kraken, coinbase, etc.)
	 * @return Provider portfolio response
	 */
	public ProviderPortfolioResponse getProviderPortfolio(String userId, String providerId) {
		log.debug("Fetching {} portfolio for user: {}", providerId, userId);

		try {
			// Read stored provider data from Firestore
			ProviderDataEntity providerData = providerDataRepository.getProviderData(userId, providerId);

			if (providerData == null) {
				log.debug("No data found for provider {} and user {}", providerId, userId);
				return createEmptyResponse(providerId);
			}

			// Apply enhancement if available and provider supports it
			if (portfolioEnhancer != null && isEnhancementSupported(providerId)) {
				log.debug("Applying enhancement to {} data for user {}", providerId, userId);
				return enhanceAndMapToResponse(providerData, userId, providerId);
			}
			else {
				return mapToProviderResponse(providerData);
			}

		}
		catch (Exception e) {
			log.error("Error fetching {} portfolio for user {}: {}", providerId, userId, e.getMessage(), e);
			return createErrorResponse(providerId, e.getMessage());
		}
	}

	/**
	 * Map ProviderDataEntity to ProviderPortfolioResponse
	 */
	private ProviderPortfolioResponse mapToProviderResponse(ProviderDataEntity entity) {
		ProviderPortfolioResponse response = new ProviderPortfolioResponse();

		response.setProviderId(entity.getProviderId());
		response.setProviderName(entity.getProviderName());
		response.setProviderType(entity.getProviderType());
		response.setProviderCategory(entity.getProviderCategory());
		response.setConnected(true);
		response.setTotalValue(entity.getTotalValue());
		response.setDayChange(entity.getDayChange());
		response.setDayChangePercent(entity.getDayChangePercent());
		response.setTotalProfitLoss(entity.getTotalProfitLoss());
		response.setTotalProfitLossPercent(entity.getTotalProfitLossPercent());
		response.setCashBalance(entity.getCashBalance());
		response.setSyncStatus(entity.getSyncStatus());
		response.setErrorMessage(entity.getErrorMessage());

		// Convert last updated Instant to timestamp
		if (entity.getLastUpdatedAt() != null) {
			response.setLastSynced(entity.getLastUpdatedAt().toEpochMilli());
		}
		else {
			response.setLastSynced(System.currentTimeMillis());
		}

		// Map holdings to positions
		if (entity.getHoldings() != null) {
			List<PortfolioPositionResponse> positions = entity.getHoldings()
				.stream()
				.map(holding -> mapHoldingToPosition(holding, entity.getProviderId()))
				.collect(Collectors.toList());
			response.setPositions(positions);
		}
		else {
			response.setPositions(new ArrayList<>());
		}

		// Map raw balances
		if (entity.getBalances() != null) {
			Map<String, BigDecimal> balances = new HashMap<>();
			entity.getBalances().forEach((key, value) -> {
				if (value instanceof Number) {
					balances.put(key, new BigDecimal(value.toString()));
				}
			});
			response.setBalances(balances);
		}

		return response;
	}

	/**
	 * Map a Holding to PortfolioPositionResponse
	 */
	private PortfolioPositionResponse mapHoldingToPosition(ProviderDataEntity.Holding holding, String providerId) {
		PortfolioPositionResponse position = new PortfolioPositionResponse();

		// Data is already normalized at storage time for Kraken
		position.setSymbol(holding.getAsset());
		position.setName(holding.getName());
		position.setQuantity(holding.getQuantity());
		position.setCurrentPrice(holding.getCurrentPrice());
		position.setCurrentValue(holding.getCurrentValue());
		position.setCostBasis(holding.getCostBasis());
		position.setProfitLoss(holding.getProfitLoss());
		position.setProfitLossPercent(holding.getProfitLossPercent());
		position.setProvider(providerId);

		// Determine asset type based on provider
		position.setAssetType(determineAssetType(providerId, holding.getAsset()));

		return position;
	}

	/**
	 * Determine asset type based on provider and symbol
	 */
	private String determineAssetType(String providerId, String symbol) {
		// Crypto providers
		if (ServicePortfolioConstants.PROVIDER_KRAKEN.equals(providerId)
				|| ServicePortfolioConstants.PROVIDER_COINBASE.equals(providerId)
				|| ServicePortfolioConstants.PROVIDER_BINANCE.equals(providerId)) {
			return ServicePortfolioConstants.ASSET_TYPE_CRYPTO;
		}

		// Stock providers
		if (ServicePortfolioConstants.PROVIDER_ALPACA.equals(providerId)
				|| ServicePortfolioConstants.PROVIDER_SCHWAB.equals(providerId)
				|| ServicePortfolioConstants.PROVIDER_ROBINHOOD.equals(providerId)) {
			return ServicePortfolioConstants.ASSET_TYPE_STOCK;
		}

		// Could be refined further based on symbol patterns
		return ServicePortfolioConstants.ASSET_TYPE_STOCK;
	}

	/**
	 * Create empty response for disconnected provider
	 */
	private ProviderPortfolioResponse createEmptyResponse(String providerId) {
		ProviderPortfolioResponse response = new ProviderPortfolioResponse();
		response.setProviderId(providerId);
		response.setProviderName(getProviderDisplayName(providerId));
		response.setConnected(false);
		response.setTotalValue(BigDecimal.ZERO);
		response.setPositions(new ArrayList<>());
		response.setSyncStatus("disconnected");
		response.setLastSynced(System.currentTimeMillis());
		return response;
	}

	/**
	 * Create error response
	 */
	private ProviderPortfolioResponse createErrorResponse(String providerId, String errorMessage) {
		ProviderPortfolioResponse response = createEmptyResponse(providerId);
		response.setSyncStatus(ServicePortfolioConstants.SYNC_STATUS_ERROR);
		response.setErrorMessage(errorMessage);
		return response;
	}

	/**
	 * Get display name for provider
	 */
	private String getProviderDisplayName(String providerId) {
		Map<String, String> providerNames = Map.of(ServicePortfolioConstants.PROVIDER_KRAKEN, "Kraken",
				ServicePortfolioConstants.PROVIDER_COINBASE, "Coinbase", ServicePortfolioConstants.PROVIDER_BINANCE,
				"Binance US", ServicePortfolioConstants.PROVIDER_ALPACA, "Alpaca",
				ServicePortfolioConstants.PROVIDER_SCHWAB, "Charles Schwab",
				ServicePortfolioConstants.PROVIDER_ROBINHOOD, "Robinhood");
		return providerNames.getOrDefault(providerId, providerId);
	}

	/**
	 * Check if user is in demo mode
	 */
	private boolean isDemoMode(String userId) {
		try {
			return userRepository.findById(userId).map(user -> {
				if (user.getProfile() != null && user.getProfile().getDemoMode() != null) {
					return user.getProfile().getDemoMode();
				}
				// Default to demo mode if not set
				return true;
			}).orElse(true);
		}
		catch (Exception e) {
			log.warn("Error checking demo mode for user {}, defaulting to true: {}", userId, e.getMessage());
			return true;
		}
	}

	/**
	 * Check if enhancement is supported for the provider
	 */
	private boolean isEnhancementSupported(String providerId) {
		// Currently support enhancement for crypto providers
		return ServicePortfolioConstants.PROVIDER_KRAKEN.equals(providerId)
				|| ServicePortfolioConstants.PROVIDER_COINBASE.equals(providerId)
				|| ServicePortfolioConstants.PROVIDER_BINANCE.equals(providerId);
	}

	/**
	 * Enhance and map ProviderDataEntity to response. Applies the enhancement layer to
	 * transform raw provider data into user-friendly format.
	 */
	private ProviderPortfolioResponse enhanceAndMapToResponse(ProviderDataEntity entity, String userId,
			String providerId) {
		try {
			// Convert entity to raw data format for enhancer
			Map<String, Object> rawData = new HashMap<>();

			// Add balances
			if (entity.getBalances() != null) {
				rawData.put("balances", entity.getBalances());
			}

			// Add holdings if available - preserve all holding data
			if (entity.getHoldings() != null && !entity.getHoldings().isEmpty()) {
				List<Map<String, Object>> holdings = new ArrayList<>();
				for (ProviderDataEntity.Holding holding : entity.getHoldings()) {
					Map<String, Object> holdingData = new HashMap<>();
					holdingData.put("asset", holding.getAsset());
					holdingData.put("name", holding.getName());
					holdingData.put("quantity", holding.getQuantity());
					holdingData.put("currentPrice", holding.getCurrentPrice());
					holdingData.put("currentValue", holding.getCurrentValue());
					holdingData.put("costBasis", holding.getCostBasis());
					holdingData.put("profitLoss", holding.getProfitLoss());
					holdingData.put("profitLossPercent", holding.getProfitLossPercent());
					holdings.add(holdingData);
				}
				if (!holdings.isEmpty()) {
					rawData.put("holdings", holdings);
				}
			}

			log.debug("Enhancing {} data with raw balances: {}", providerId, rawData.get("balances"));

			// Apply enhancement
			EnhancedPortfolio enhanced = portfolioEnhancer.enhanceProviderPortfolio(userId, providerId, rawData);

			log.debug("Enhanced {} portfolio - Total value: {}, Assets count: {}", providerId, enhanced.getTotalValue(),
					enhanced.getAssets() != null ? enhanced.getAssets().size() : 0);

			// Convert enhanced portfolio to response
			return convertEnhancedToResponse(enhanced);

		}
		catch (Exception e) {
			log.error("Error enhancing {} data for user {}, falling back to raw mapping: {}", providerId, userId,
					e.getMessage(), e);
			return mapToProviderResponse(entity);
		}
	}

	/**
	 * Convert EnhancedPortfolio to ProviderPortfolioResponse.
	 */
	private ProviderPortfolioResponse convertEnhancedToResponse(EnhancedPortfolio enhanced) {
		ProviderPortfolioResponse response = new ProviderPortfolioResponse();

		response.setProviderId(enhanced.getProviderId());
		response.setProviderName(enhanced.getProviderName());
		response.setProviderType("crypto"); // Default, will be overridden by actual
											// provider
		response.setProviderCategory("exchange"); // Default, will be overridden by actual
													// provider
		response.setConnected(true);
		response.setTotalValue(enhanced.getTotalValue());
		response.setDayChange(BigDecimal.ZERO); // TODO: Calculate from historical data
		response.setDayChangePercent(BigDecimal.ZERO); // TODO: Calculate from historical
														// data
		response.setTotalProfitLoss(enhanced.getTotalProfitLoss());
		response.setTotalProfitLossPercent(enhanced.getTotalProfitLossPercent());
		response.setCashBalance(enhanced.getCashBalance());
		response.setSyncStatus("synced");
		response
			.setLastSynced(enhanced.getLastUpdated() != null ? enhanced.getLastUpdated() : System.currentTimeMillis());

		// Convert enhanced assets to positions
		if (enhanced.getAssets() != null) {
			List<PortfolioPositionResponse> positions = new ArrayList<>();
			for (EnhancedAsset asset : enhanced.getAssets()) {
				PortfolioPositionResponse position = new PortfolioPositionResponse();
				position.setSymbol(asset.getSymbol());
				position.setName(asset.getName());
				position.setQuantity(asset.getQuantity());
				position.setCurrentPrice(asset.getCurrentPrice());
				position.setCurrentValue(asset.getValue());
				position.setCostBasis(asset.getCostBasis());
				position.setProfitLoss(asset.getProfitLoss());
				position.setProfitLossPercent(asset.getProfitLossPercent());
				position.setAssetType(asset.getAssetType());
				position.setProvider(enhanced.getProviderId());

				// Add any additional metadata
				if (asset.isStaked()) {
					if (position.getMetadata() == null) {
						position.setMetadata(new HashMap<>());
					}
					position.getMetadata().put("staked", true);
				}

				positions.add(position);
			}
			response.setPositions(positions);
		}
		else {
			response.setPositions(new ArrayList<>());
		}

		return response;
	}

}