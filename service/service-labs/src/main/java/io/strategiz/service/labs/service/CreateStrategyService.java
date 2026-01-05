package io.strategiz.service.labs.service;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.CreateStrategyRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import io.strategiz.service.labs.model.CreateStrategyRequest;
import io.strategiz.service.labs.utils.StrategyNameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for creating strategies
 */
@Service
public class CreateStrategyService extends BaseService {

    private final CreateStrategyRepository createStrategyRepository;
    private final StrategyNameValidationService nameValidationService;

    @Override
    protected String getModuleName() {
        return "service-labs";
    }

    @Autowired
    public CreateStrategyService(CreateStrategyRepository createStrategyRepository,
                                  StrategyNameValidationService nameValidationService) {
        this.createStrategyRepository = createStrategyRepository;
        this.nameValidationService = nameValidationService;
    }
    
    /**
     * Create a new strategy for a user
     * 
     * @param request The strategy creation request
     * @param userId The user ID
     * @return The created strategy
     */
    public Strategy createStrategy(CreateStrategyRequest request, String userId) {
        log.info("Creating strategy: {} for user: {}", request.getName(), userId);

        // Validate request
        validateCreateRequest(request);

        // Validate name uniqueness
        nameValidationService.validateDraftNameUniqueness(userId, request.getName(), null);

        // Convert request to entity
        Strategy strategy = new Strategy();
        strategy.setName(request.getName());
        strategy.setNormalizedName(StrategyNameUtils.normalizeName(request.getName()));
        strategy.setDescription(request.getDescription());
        strategy.setCode(request.getCode());
        strategy.setLanguage(request.getLanguage());
        strategy.setType(request.getType() != null ? request.getType() : StrategyConstants.DEFAULT_TYPE);
        strategy.setIsPublished(request.getIsPublished());
        strategy.setIsPublic(request.getIsPublic());
        strategy.setIsListed(request.getIsListed());
        strategy.setTags(request.getTags());
        strategy.setParameters(request.getParameters());
        strategy.setPerformance(request.getPerformance());

        // Set ownership fields - creator and owner are same user at creation
        strategy.setCreatorId(userId);
        strategy.setOwnerId(userId);

        // Parse and set seedFundingDate if provided
        if (request.getSeedFundingDate() != null && !request.getSeedFundingDate().isEmpty()) {
            try {
                java.time.Instant instant = java.time.Instant.parse(request.getSeedFundingDate());
                strategy.setSeedFundingDate(com.google.cloud.Timestamp.fromProto(
                    com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(instant.getEpochSecond())
                        .setNanos(instant.getNano())
                        .build()
                ));
            } catch (Exception e) {
                log.warn("Invalid seedFundingDate format, skipping: {}", request.getSeedFundingDate());
            }
        }

        // Save using CRUD repository
        Strategy created = createStrategyRepository.createWithUserId(strategy, userId);

        log.info("Successfully created strategy: {} for user: {}", created.getId(), userId);
        return created;
    }
    
    /**
     * Validate create strategy request
     */
    private void validateCreateRequest(CreateStrategyRequest request) {
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

        // Validate performance data - strategies must be backtested before saving
        if (request.getPerformance() == null) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_MISSING_PERFORMANCE,
                    "Strategy must be run at least once to generate performance data before saving");
        }
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