package io.strategiz.business.strategy.execution.service;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.entity.ProviderStatus;import io.strategiz.data.provider.repository.ReadProviderIntegrationRepository;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.business.strategy.execution.model.ExecutionRequest;
import io.strategiz.business.strategy.execution.model.ExecutionResult;
import io.strategiz.business.strategy.execution.executor.PythonExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic for live strategy execution with connected provider integrations (brokers, exchanges).
 *
 * Follows naming convention: Business modules end with "Business" suffix.
 */
@Service
public class StrategyExecutionBusiness {

    private static final Logger logger = LoggerFactory.getLogger(StrategyExecutionBusiness.class);

    private final ReadStrategyRepository strategyRepository;
    private final ReadProviderIntegrationRepository providerRepository;
    private final ExecutionEngineService executionEngine;
    private final PythonExecutor pythonExecutor;
    private final ObjectMapper objectMapper;

    @Autowired
    public StrategyExecutionBusiness(
            ReadStrategyRepository strategyRepository,
            ReadProviderIntegrationRepository providerRepository,
            ExecutionEngineService executionEngine,
            PythonExecutor pythonExecutor,
            ObjectMapper objectMapper) {
        this.strategyRepository = strategyRepository;
        this.providerRepository = providerRepository;
        this.executionEngine = executionEngine;
        this.pythonExecutor = pythonExecutor;
        this.objectMapper = objectMapper;
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

            // Verify user owns the strategy or strategy is public
            if (!userId.equals(strategy.getOwnerId()) && !Boolean.TRUE.equals(strategy.getIsPublic())) {
                return createErrorResult(strategyId, "Access denied to strategy");
            }
            
            // Get the provider integration
            Optional<ProviderIntegrationEntity> providerOpt = providerRepository.findById(providerId);
            if (providerOpt.isEmpty()) {
                return createErrorResult(strategyId, "Provider integration not found");
            }
            
            ProviderIntegrationEntity provider = providerOpt.get();

            // Verify provider is enabled
            if (!ProviderStatus.CONNECTED.getValue().equals(provider.getStatus())) {
                return createErrorResult(strategyId, "Provider is not enabled");
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
                    result = executePythonStrategy(request, strategy);
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
     * Execute Python strategy and parse signals from JSON output
     *
     * @param request The execution request
     * @param strategy The strategy entity
     * @return ExecutionResult with parsed signals
     */
    private ExecutionResult executePythonStrategy(ExecutionRequest request, Strategy strategy) {
        logger.debug("Executing Python strategy: {}", strategy.getId());

        try {
            // Execute Python code using GraalVM
            String pythonOutput = pythonExecutor.execute(request.getStrategyCode());

            logger.debug("Python execution output: {}", pythonOutput);

            // Create result object
            ExecutionResult result = new ExecutionResult();
            result.setStrategyId(strategy.getId());

            // Parse JSON output
            try {
                JsonNode jsonOutput = objectMapper.readTree(pythonOutput);

                // Check for execution errors
                if (jsonOutput.has("error") && jsonOutput.get("error").asBoolean()) {
                    result.setStatus("ERROR");
                    result.setMessage(jsonOutput.has("reason") ?
                                    jsonOutput.get("reason").asText() : "Strategy execution failed");
                    return result;
                }

                // Extract signal
                if (jsonOutput.has("signal")) {
                    ExecutionResult.Signal signal = new ExecutionResult.Signal();

                    // Signal type (BUY, SELL, HOLD)
                    signal.setType(jsonOutput.get("signal").asText());

                    // Price (optional)
                    if (jsonOutput.has("price") && !jsonOutput.get("price").isNull()) {
                        signal.setPrice(jsonOutput.get("price").asDouble());
                    }

                    // Quantity (optional)
                    if (jsonOutput.has("quantity") && !jsonOutput.get("quantity").isNull()) {
                        signal.setQuantity(jsonOutput.get("quantity").asDouble());
                    }

                    // Reason (optional)
                    if (jsonOutput.has("reason") && !jsonOutput.get("reason").isNull()) {
                        signal.setReason(jsonOutput.get("reason").asText());
                    }

                    // Add signal to result
                    List<ExecutionResult.Signal> signals = new ArrayList<>();
                    signals.add(signal);
                    result.setSignals(signals);
                }

                // Extract indicators/metrics
                if (jsonOutput.has("indicators") && jsonOutput.get("indicators").isObject()) {
                    Map<String, Object> metrics = new HashMap<>();
                    JsonNode indicators = jsonOutput.get("indicators");

                    indicators.fields().forEachRemaining(entry -> {
                        JsonNode value = entry.getValue();
                        if (value.isNumber()) {
                            metrics.put(entry.getKey(), value.asDouble());
                        } else if (value.isTextual()) {
                            metrics.put(entry.getKey(), value.asText());
                        } else if (value.isBoolean()) {
                            metrics.put(entry.getKey(), value.asBoolean());
                        }
                    });

                    result.setMetrics(metrics);
                }

                result.setStatus("SUCCESS");
                result.setMessage("Strategy executed successfully");

                logger.info("Python strategy executed successfully, signal: {}",
                          result.getSignals().isEmpty() ? "NONE" : result.getSignals().get(0).getType());

                return result;

            } catch (Exception parseError) {
                logger.error("Failed to parse Python execution output as JSON", parseError);
                result.setStatus("ERROR");
                result.setMessage("Invalid strategy output format: " + parseError.getMessage());
                return result;
            }

        } catch (Exception e) {
            logger.error("Error executing Python strategy", e);
            return createErrorResult(strategy.getId(), "Python execution failed: " + e.getMessage());
        }
    }

    /**
     * Get available provider integrations for a user
     *
     * @param userId The user ID
     * @return List of connected providers
     */
    public List<ProviderIntegrationEntity> getUserProviders(String userId) {
        // Find all enabled providers for the user
        return providerRepository.findByUserId(userId).stream()
                .filter(p -> ProviderStatus.CONNECTED.getValue().equals(p.getStatus()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get trading providers (exchanges and brokers) for a user
     * 
     * @param userId The user ID
     * @return List of trading providers
     */
    public List<ProviderIntegrationEntity> getTradingProviders(String userId) {
        // Filter by provider IDs that are known trading providers
        return getUserProviders(userId).stream()
                .filter(p -> isTradingProvider(p.getProviderId()))
                .collect(Collectors.toList());
    }
    
    private boolean isTradingProvider(String providerId) {
        // Known trading provider IDs
        return "kraken".equals(providerId) || 
               "coinbase".equals(providerId) ||
               "binanceus".equals(providerId) ||
               "alpaca".equals(providerId) ||
               "schwab".equals(providerId);
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

        // Check if provider is enabled
        if (!ProviderStatus.CONNECTED.getValue().equals(provider.getStatus())) {
            return false;
        }
        
        // Match provider ID with strategy type
        String providerIdValue = provider.getProviderId();
        switch (strategyType.toLowerCase()) {
            case "crypto":
                return "kraken".equals(providerIdValue) || 
                       "coinbase".equals(providerIdValue) ||
                       "binanceus".equals(providerIdValue);
            case "stocks":
            case "equities":
                return "alpaca".equals(providerIdValue) || 
                       "schwab".equals(providerIdValue);
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