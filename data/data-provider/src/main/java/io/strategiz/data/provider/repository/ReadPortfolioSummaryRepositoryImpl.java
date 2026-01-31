package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of ReadPortfolioSummaryRepository using BaseRepository
 */
@Repository
public class ReadPortfolioSummaryRepositoryImpl implements ReadPortfolioSummaryRepository {

	private final PortfolioSummaryBaseRepository baseRepository;

	@Autowired
	public ReadPortfolioSummaryRepositoryImpl(PortfolioSummaryBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public PortfolioSummaryEntity getPortfolioSummary(String userId) {
		return baseRepository.findCurrent(userId).orElse(null);
	}

	@Override
	public List<PortfolioSummaryEntity> getPortfolioSummaryHistory(String userId, Instant from, Instant to) {
		return baseRepository.findByDateRange(userId, from, to);
	}

	@Override
	public boolean portfolioSummaryExists(String userId) {
		return baseRepository.currentExists(userId);
	}

	@Override
	public Instant getLastSyncTime(String userId) {
		return baseRepository.getLastSyncTime(userId);
	}

}