package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import io.strategiz.data.provider.exception.DataProviderErrorDetails;
import io.strategiz.data.provider.exception.ProviderIntegrationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Implementation of UpdatePortfolioSummaryRepository using BaseRepository
 */
@Repository
public class UpdatePortfolioSummaryRepositoryImpl implements UpdatePortfolioSummaryRepository {

	private final PortfolioSummaryBaseRepository baseRepository;

	private final ReadPortfolioSummaryRepository readRepository;

	@Autowired
	public UpdatePortfolioSummaryRepositoryImpl(PortfolioSummaryBaseRepository baseRepository,
			ReadPortfolioSummaryRepository readRepository) {
		this.baseRepository = baseRepository;
		this.readRepository = readRepository;
	}

	@Override
	public PortfolioSummaryEntity updatePortfolioSummary(String userId, PortfolioSummaryEntity summary) {
		// Update the entire summary (will replace current document)
		return baseRepository.save(summary, userId);
	}

	@Override
	public PortfolioSummaryEntity updateTotalValue(String userId, BigDecimal totalValue) {
		// Get existing summary
		PortfolioSummaryEntity existing = readRepository.getPortfolioSummary(userId);
		if (existing == null) {
			throw new ProviderIntegrationException(DataProviderErrorDetails.PROVIDER_NOT_FOUND, "PortfolioSummary",
					userId);
		}

		// Update total value and sync time
		existing.setTotalValue(totalValue);
		existing.setLastSyncedAt(Instant.now());

		return baseRepository.save(existing, userId);
	}

	@Override
	public PortfolioSummaryEntity updateDayChange(String userId, BigDecimal dayChange, BigDecimal dayChangePercent) {
		// Get existing summary
		PortfolioSummaryEntity existing = readRepository.getPortfolioSummary(userId);
		if (existing == null) {
			throw new ProviderIntegrationException(DataProviderErrorDetails.PROVIDER_NOT_FOUND, "PortfolioSummary",
					userId);
		}

		// Update day change values
		existing.setDayChange(dayChange);
		existing.setDayChangePercent(dayChangePercent);
		existing.setLastSyncedAt(Instant.now());

		return baseRepository.save(existing, userId);
	}

	@Override
	public PortfolioSummaryEntity updateAccountPerformance(String userId, Map<String, BigDecimal> accountPerformance) {
		// Get existing summary
		PortfolioSummaryEntity existing = readRepository.getPortfolioSummary(userId);
		if (existing == null) {
			throw new ProviderIntegrationException(DataProviderErrorDetails.PROVIDER_NOT_FOUND, "PortfolioSummary",
					userId);
		}

		// Update account performance
		existing.setAccountPerformance(accountPerformance);
		existing.setLastSyncedAt(Instant.now());

		return baseRepository.save(existing, userId);
	}

	@Override
	public PortfolioSummaryEntity updateAssetAllocation(String userId,
			PortfolioSummaryEntity.AssetAllocation assetAllocation) {
		// Get existing summary
		PortfolioSummaryEntity existing = readRepository.getPortfolioSummary(userId);
		if (existing == null) {
			throw new ProviderIntegrationException(DataProviderErrorDetails.PROVIDER_NOT_FOUND, "PortfolioSummary",
					userId);
		}

		// Update asset allocation
		existing.setAssetAllocation(assetAllocation);
		existing.setLastSyncedAt(Instant.now());

		return baseRepository.save(existing, userId);
	}

	@Override
	public PortfolioSummaryEntity updateLastSyncTime(String userId, Instant timestamp) {
		// Get existing summary
		PortfolioSummaryEntity existing = readRepository.getPortfolioSummary(userId);
		if (existing == null) {
			throw new ProviderIntegrationException(DataProviderErrorDetails.PROVIDER_NOT_FOUND, "PortfolioSummary",
					userId);
		}

		// Update last sync time
		existing.setLastSyncedAt(timestamp);

		return baseRepository.save(existing, userId);
	}

}