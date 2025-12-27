package io.strategiz.service.labs.service;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.StrategyPerformance;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.data.strategy.repository.UpdateStrategyRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import io.strategiz.service.labs.model.CreateStrategyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for updating strategies
 */
@Service
public class UpdateStrategyService extends BaseService {

    private final UpdateStrategyRepository updateStrategyRepository;
    private final ReadStrategyRepository readStrategyRepository;

    @Override
    protected String getModuleName() {
        return "service-labs";
    }
    
    @Autowired
    public UpdateStrategyService(UpdateStrategyRepository updateStrategyRepository,
                               ReadStrategyRepository readStrategyRepository) {
        this.updateStrategyRepository = updateStrategyRepository;
        this.readStrategyRepository = readStrategyRepository;
    }
    
    /**
     * Update a strategy
     * 
     * @param strategyId The strategy ID
     * @param userId The user ID
     * @param request The update request
     * @return The updated strategy
     */
    public Strategy updateStrategy(String strategyId, String userId, CreateStrategyRequest request) {
        log.info("Updating strategy: {} for user: {}", strategyId, userId);
        
        // First check if strategy exists and user has access
        Strategy existing = readStrategyRepository.findById(strategyId)
                .filter(s -> userId.equals(s.getUserId()))
                .orElseThrow(() -> new StrategizException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND, getModuleName(),
                        "Strategy not found or access denied"));
        
        // Update fields from request
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setCode(request.getCode());
        existing.setLanguage(request.getLanguage());
        existing.setType(request.getType() != null ? request.getType() : StrategyConstants.DEFAULT_TYPE);
        existing.setTags(request.getTags());
        existing.setParameters(request.getParameters());
        existing.setPublic(request.isPublic());
        existing.setPerformance(request.getPerformance());

        // Parse and set seedFundingDate if provided
        if (request.getSeedFundingDate() != null && !request.getSeedFundingDate().isEmpty()) {
            try {
                java.time.Instant instant = java.time.Instant.parse(request.getSeedFundingDate());
                existing.setSeedFundingDate(com.google.cloud.Timestamp.fromProto(
                    com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(instant.getEpochSecond())
                        .setNanos(instant.getNano())
                        .build()
                ));
            } catch (Exception e) {
                log.warn("Invalid seedFundingDate format, skipping: {}", request.getSeedFundingDate());
            }
        }

        // Validate the updated strategy
        validateUpdateRequest(request);
        
        // Update the strategy
        Strategy updated = updateStrategyRepository.update(strategyId, userId, existing);

        log.info("Successfully updated strategy: {} for user: {}", strategyId, userId);
        return updated;
    }

    /**
     * Update strategy status
     *
     * @param strategyId The strategy ID
     * @param userId The user ID
     * @param status The new status
     * @return true if updated successfully
     */
    public boolean updateStrategyStatus(String strategyId, String userId, String status) {
        log.info("Updating strategy {} status to: {} for user: {}", strategyId, status, userId);

        // Validate status
        if (!isValidStatus(status)) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_INVALID_STATUS,
                    "Invalid status: " + status);
        }

        boolean updated = updateStrategyRepository.updateStatus(strategyId, userId, status);

        if (updated) {
            log.info("Successfully updated strategy {} status to: {}", strategyId, status);
        } else {
            log.warn("Failed to update strategy {} status", strategyId);
        }

        return updated;
    }

    /**
     * Update strategy code
     *
     * @param strategyId The strategy ID
     * @param userId The user ID
     * @param code The new code
     * @return The updated strategy
     */
    public Optional<Strategy> updateStrategyCode(String strategyId, String userId, String code) {
        log.info("Updating strategy {} code for user: {}", strategyId, userId);

        // Validate code length
        if (code != null && code.length() > StrategyConstants.MAX_CODE_LENGTH) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_CODE_TOO_LONG,
                    "Strategy code exceeds maximum length of " + StrategyConstants.MAX_CODE_LENGTH);
        }

        return updateStrategyRepository.updateCode(strategyId, userId, code);
    }

    /**
     * Update strategy tags
     *
     * @param strategyId The strategy ID
     * @param userId The user ID
     * @param tags The new tags
     * @return The updated strategy
     */
    public Optional<Strategy> updateStrategyTags(String strategyId, String userId, List<String> tags) {
        log.info("Updating strategy {} tags for user: {}", strategyId, userId);

        // Validate tags count
        if (tags != null && tags.size() > StrategyConstants.MAX_TAGS) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_TOO_MANY_TAGS,
                    "Too many tags. Maximum allowed: " + StrategyConstants.MAX_TAGS);
        }

        return updateStrategyRepository.updateTags(strategyId, userId, tags);
    }

    /**
     * Update strategy performance metrics
     *
     * @param strategyId The strategy ID
     * @param userId The user ID
     * @param performance The performance metrics
     * @return The updated strategy
     */
    public Optional<Strategy> updateStrategyPerformance(String strategyId, String userId, StrategyPerformance performance) {
        log.info("Updating strategy {} performance metrics for user: {}", strategyId, userId);

        // Validate performance data if provided
        if (performance != null) {
            // Basic validation - ensure critical fields are present
            if (performance.getTotalReturn() == null && performance.getTotalTrades() == null) {
                throwModuleException(ServiceStrategyErrorDetails.STRATEGY_INVALID_PERFORMANCE,
                        "Performance data must include at least totalReturn or totalTrades");
            }
        }

        return updateStrategyRepository.updatePerformance(strategyId, userId, performance);
    }

    /**
     * Update strategy backtest results
     *
     * @param strategyId The strategy ID
     * @param userId The user ID
     * @param backtestResults The backtest results
     * @return The updated strategy
     */
    public Optional<Strategy> updateStrategyBacktestResults(String strategyId, String userId, Map<String, Object> backtestResults) {
        log.info("Updating strategy {} backtest results for user: {}", strategyId, userId);
        return updateStrategyRepository.updateBacktestResults(strategyId, userId, backtestResults);
    }
    
    private void validateUpdateRequest(CreateStrategyRequest request) {
        // Validate name length
        if (request.getName() != null && request.getName().length() > StrategyConstants.MAX_NAME_LENGTH) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_NAME_TOO_LONG,
                    "Strategy name exceeds maximum length of " + StrategyConstants.MAX_NAME_LENGTH);
        }

        // Validate description length
        if (request.getDescription() != null && request.getDescription().length() > StrategyConstants.MAX_DESCRIPTION_LENGTH) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_DESCRIPTION_TOO_LONG,
                    "Strategy description exceeds maximum length of " + StrategyConstants.MAX_DESCRIPTION_LENGTH);
        }

        // Validate code length
        if (request.getCode() != null && request.getCode().length() > StrategyConstants.MAX_CODE_LENGTH) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_CODE_TOO_LONG,
                    "Strategy code exceeds maximum length of " + StrategyConstants.MAX_CODE_LENGTH);
        }

        // Validate language
        if (!isValidLanguage(request.getLanguage())) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_INVALID_LANGUAGE,
                    "Invalid language: " + request.getLanguage());
        }

        // Validate type if provided
        if (request.getType() != null && !isValidType(request.getType())) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_INVALID_TYPE,
                    "Invalid type: " + request.getType());
        }

        // Validate tags count
        if (request.getTags() != null && request.getTags().size() > StrategyConstants.MAX_TAGS) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_TOO_MANY_TAGS,
                    "Too many tags. Maximum allowed: " + StrategyConstants.MAX_TAGS);
        }
    }
    
    private boolean isValidStatus(String status) {
        return StrategyConstants.STATUS_DRAFT.equals(status) || 
               StrategyConstants.STATUS_ACTIVE.equals(status) || 
               StrategyConstants.STATUS_ARCHIVED.equals(status);
    }
    
    private boolean isValidLanguage(String language) {
        return StrategyConstants.LANGUAGE_PYTHON.equals(language) ||
               StrategyConstants.LANGUAGE_JAVA.equals(language) ||
               StrategyConstants.LANGUAGE_PINESCRIPT.equals(language);
    }
    
    private boolean isValidType(String type) {
        return StrategyConstants.TYPE_TECHNICAL.equals(type) ||
               StrategyConstants.TYPE_FUNDAMENTAL.equals(type) ||
               StrategyConstants.TYPE_HYBRID.equals(type);
    }
}