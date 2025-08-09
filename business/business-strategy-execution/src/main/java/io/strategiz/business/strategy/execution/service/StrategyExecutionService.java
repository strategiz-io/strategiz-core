package io.strategiz.business.strategy.execution.service;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.repository.ReadProviderIntegrationRepository;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.business.strategy.execution.model.ExecutionRequest;
import io.strategiz.business.strategy.execution.model.ExecutionResult;
import io.strategiz.business.strategy.execution.executor.PythonExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Core strategy execution service that orchestrates strategy execution
 * using connected provider integrations
 */
@Service
public class StrategyExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(StrategyExecutionService.class);
    
    private final ReadStrategyRepository strategyRepository;
    private final ReadProviderIntegrationRepository providerRepository;
    private final ExecutionEngineService executionEngine;
    private final PythonExecutor pythonExecutor;
    
    @Autowired
    public StrategyExecutionService(
            ReadStrategyRepository strategyRepository,
            ReadProviderIntegrationRepository providerRepository,
            ExecutionEngineService executionEngine,
            PythonExecutor pythonExecutor) {
        this.strategyRepository = strategyRepository;
        this.providerRepository = providerRepository;
        this.executionEngine = executionEngine;
        this.pythonExecutor = pythonExecutor;
    }
    
    /**
     * Execute a strategy with the specified provider
     * 
     * @param strategyId The strategy ID
     * @param providerId The provider ID to use for execution
     * @param userId The user ID
     * @return The execution result
     */
    public ExecutionResult executeStrategy(String strategyId, String providerId, String userId) {
        logger.info("Executing strategy {} with provider {} for user {}", strategyId, providerId, userId);
        
        try {
            // Get the strategy
            Optional<Strategy> strategyOpt = strategyRepository.findById(strategyId);
            if (strategyOpt.isEmpty()) {
                return createErrorResult(strategyId, "Strategy not found");
            }
            
            Strategy strategy = strategyOpt.get();
            
            // Verify user owns the strategy
            if (!userId.equals(strategy.getUserId()) && !strategy.isPublic()) {
                return createErrorResult(strategyId, "Access denied to strategy");
            }
            
            // Get the provider integration
            Optional<ProviderIntegrationEntity> providerOpt = providerRepository.findById(providerId);
            if (providerOpt.isEmpty()) {
                return createErrorResult(strategyId, "Provider integration not found");
            }
            
            ProviderIntegrationEntity provider = providerOpt.get();
            
            // Verify provider is connected and active
            if (!"CONNECTED".equals(provider.getStatus())) {
                return createErrorResult(strategyId, "Provider is not connected: " + provider.getStatus());
            }
            
            // Create execution request
            ExecutionRequest request = new ExecutionRequest();
            request.setStrategyId(strategyId);
            request.setUserId(userId);
            request.setProviderId(providerId);
            request.setStrategyCode(strategy.getCode());
            request.setLanguage(strategy.getLanguage());
            request.setParameters(strategy.getParameters());
            
            // Execute based on language
            ExecutionResult result;
            switch (strategy.getLanguage().toLowerCase()) {
                case "python":
                    // PythonExecutor expects just the code string
                    String pythonResult = pythonExecutor.execute(request.getStrategyCode());
                    result = new ExecutionResult();
                    result.setStrategyId(strategyId);
                    result.setStatus("SUCCESS");
                    result.setMessage(pythonResult);
                    break;
                case "java":
                    // TODO: Implement Java executor
                    result = executionEngine.execute(request);
                    break;
                default:
                    result = createErrorResult(strategyId, "Unsupported language: " + strategy.getLanguage());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing strategy", e);
            return createErrorResult(strategyId, "Execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Get available provider integrations for a user
     * 
     * @param userId The user ID
     * @return List of connected providers
     */
    public List<ProviderIntegrationEntity> getUserProviders(String userId) {
        return providerRepository.findByUserIdAndStatus(userId, "CONNECTED");
    }
    
    /**
     * Get trading providers (exchanges and brokers) for a user
     * 
     * @param userId The user ID
     * @return List of trading providers
     */
    public List<ProviderIntegrationEntity> getTradingProviders(String userId) {
        return getUserProviders(userId).stream()
                .filter(p -> "TRADING".equals(p.getProviderType()) || 
                           "CRYPTO".equals(p.getProviderType()) ||
                           "BROKER".equals(p.getProviderType()))
                .collect(Collectors.toList());
    }
    
    /**
     * Validate if a provider can execute a strategy
     * 
     * @param providerId The provider ID
     * @param strategyType The strategy type (crypto, stocks, etc.)
     * @return true if provider can execute the strategy type
     */
    public boolean canProviderExecuteStrategy(String providerId, String strategyType) {
        Optional<ProviderIntegrationEntity> providerOpt = providerRepository.findById(providerId);
        if (providerOpt.isEmpty()) {
            return false;
        }
        
        ProviderIntegrationEntity provider = providerOpt.get();
        
        // Check if provider is connected
        if (!"CONNECTED".equals(provider.getStatus())) {
            return false;
        }
        
        // Match provider type with strategy type
        switch (strategyType.toLowerCase()) {
            case "crypto":
                return "CRYPTO".equals(provider.getProviderType());
            case "stocks":
            case "equities":
                return "TRADING".equals(provider.getProviderType()) || 
                       "BROKER".equals(provider.getProviderType());
            default:
                return false;
        }
    }
    
    private ExecutionResult createErrorResult(String strategyId, String errorMessage) {
        ExecutionResult result = new ExecutionResult();
        result.setStrategyId(strategyId);
        result.setStatus("ERROR");
        result.setMessage(errorMessage);
        return result;
    }
}