package io.strategiz.service.labs.service;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.CreateStrategyRepository;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.model.CreateStrategyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for creating strategies
 */
@Service
public class CreateStrategyService {
    
    private static final Logger logger = LoggerFactory.getLogger(CreateStrategyService.class);
    
    private final CreateStrategyRepository createStrategyRepository;
    
    @Autowired
    public CreateStrategyService(CreateStrategyRepository createStrategyRepository) {
        this.createStrategyRepository = createStrategyRepository;
    }
    
    /**
     * Create a new strategy for a user
     * 
     * @param request The strategy creation request
     * @param userId The user ID
     * @return The created strategy
     */
    public Strategy createStrategy(CreateStrategyRequest request, String userId) {
        logger.info("Creating strategy: {} for user: {}", request.getName(), userId);
        
        // Validate request
        validateCreateRequest(request);
        
        // Convert request to entity
        Strategy strategy = new Strategy();
        strategy.setName(request.getName());
        strategy.setDescription(request.getDescription());
        strategy.setCode(request.getCode());
        strategy.setLanguage(request.getLanguage());
        strategy.setType(request.getType() != null ? request.getType() : StrategyConstants.DEFAULT_TYPE);
        strategy.setTags(request.getTags());
        strategy.setParameters(request.getParameters());
        strategy.setPublic(request.isPublic());
        
        // Save using CRUD repository
        Strategy created = createStrategyRepository.createWithUserId(strategy, userId);
        
        logger.info("Successfully created strategy: {} for user: {}", created.getId(), userId);
        return created;
    }
    
    /**
     * Validate create strategy request
     */
    private void validateCreateRequest(CreateStrategyRequest request) {
        // Validate name length
        if (request.getName() != null && request.getName().length() > StrategyConstants.MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Strategy name exceeds maximum length of " + StrategyConstants.MAX_NAME_LENGTH);
        }
        
        // Validate description length
        if (request.getDescription() != null && request.getDescription().length() > StrategyConstants.MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Strategy description exceeds maximum length of " + StrategyConstants.MAX_DESCRIPTION_LENGTH);
        }
        
        // Validate code length
        if (request.getCode() != null && request.getCode().length() > StrategyConstants.MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("Strategy code exceeds maximum length of " + StrategyConstants.MAX_CODE_LENGTH);
        }
        
        // Validate language
        if (!isValidLanguage(request.getLanguage())) {
            throw new IllegalArgumentException("Invalid language: " + request.getLanguage());
        }
        
        // Validate type if provided
        if (request.getType() != null && !isValidType(request.getType())) {
            throw new IllegalArgumentException("Invalid type: " + request.getType());
        }
        
        // Validate tags count
        if (request.getTags() != null && request.getTags().size() > StrategyConstants.MAX_TAGS) {
            throw new IllegalArgumentException("Too many tags. Maximum allowed: " + StrategyConstants.MAX_TAGS);
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