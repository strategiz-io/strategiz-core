package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Implementation of CreatePortfolioSummaryRepository using BaseRepository
 */
@Repository
public class CreatePortfolioSummaryRepositoryImpl implements CreatePortfolioSummaryRepository {

	private final PortfolioSummaryBaseRepository baseRepository;

	@Autowired
	public CreatePortfolioSummaryRepositoryImpl(PortfolioSummaryBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public PortfolioSummaryEntity createPortfolioSummary(String userId, PortfolioSummaryEntity summary) {
		// Always save as "current" document
		return baseRepository.save(summary, userId);
	}

	@Override
	public PortfolioSummaryEntity createOrReplacePortfolioSummary(String userId, PortfolioSummaryEntity summary) {
		// This will overwrite if exists or create if not (same behavior as create)
		return baseRepository.save(summary, userId);
	}

}