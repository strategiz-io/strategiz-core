package io.strategiz.service.labs.service;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import io.strategiz.service.labs.utils.StrategyNameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for validating strategy name uniqueness
 * Enforces two-tier uniqueness rules:
 * 1. Draft strategies: Names must be unique per user (case-insensitive)
 * 2. Published strategies: Names must be globally unique (case-insensitive)
 */
@Service
public class StrategyNameValidationService extends BaseService {

	private final ReadStrategyRepository readStrategyRepository;

	@Autowired
	public StrategyNameValidationService(ReadStrategyRepository readStrategyRepository) {
		this.readStrategyRepository = readStrategyRepository;
	}

	/**
	 * Validate name uniqueness for draft strategy within user's portfolio
	 *
	 * @param userId            User creating/updating the strategy
	 * @param name              Strategy name to validate
	 * @param excludeStrategyId Strategy ID to exclude from check (null for create,
	 *                          strategyId for update)
	 * @throws StrategizException if duplicate name found
	 */
	public void validateDraftNameUniqueness(String userId, String name, String excludeStrategyId) {
		String normalizedName = StrategyNameUtils.normalizeName(name);

		List<Strategy> conflicts = readStrategyRepository
			.findByOwnerIdAndNormalizedName(userId, normalizedName)
			.stream()
			.filter(s -> !Boolean.TRUE.equals(s.getIsPublished())) // DRAFT = isPublished false
			.filter(s -> excludeStrategyId == null || !s.getId().equals(excludeStrategyId))
			.collect(Collectors.toList());

		if (!conflicts.isEmpty()) {
			throwModuleException(ServiceStrategyErrorDetails.DUPLICATE_STRATEGY_NAME, String.format(
					"You already have a draft strategy named '%s'. Please choose a different name.", name));
		}
	}

	/**
	 * Validate name uniqueness globally for published strategies
	 *
	 * @param name              Strategy name to validate
	 * @param excludeStrategyId Strategy ID to exclude from check
	 * @throws StrategizException if duplicate published name found
	 */
	public void validatePublishedNameUniqueness(String name, String excludeStrategyId) {
		String normalizedName = StrategyNameUtils.normalizeName(name);

		List<Strategy> conflicts = readStrategyRepository
			.findByNormalizedNameAndPublishStatus(normalizedName, StrategyConstants.PUBLISH_STATUS_PUBLISHED)
			.stream()
			.filter(s -> excludeStrategyId == null || !s.getId().equals(excludeStrategyId))
			.collect(Collectors.toList());

		if (!conflicts.isEmpty()) {
			throwModuleException(ServiceStrategyErrorDetails.DUPLICATE_PUBLISHED_NAME, String.format(
					"A published strategy named '%s' already exists. Please choose a unique name before publishing.",
					name));
		}
	}

	@Override
	protected String getModuleName() {
		return "strategy";
	}

}
