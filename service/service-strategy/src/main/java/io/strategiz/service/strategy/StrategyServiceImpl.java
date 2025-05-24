package io.strategiz.service.strategy;

import io.strategiz.data.strategy.Strategy;
import io.strategiz.data.strategy.StrategyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the StrategyService interface
 */
@Service
public class StrategyServiceImpl implements StrategyService {

    private static final Logger log = LoggerFactory.getLogger(StrategyServiceImpl.class);

    private final StrategyRepository strategyRepository;

    @Autowired
    public StrategyServiceImpl(StrategyRepository strategyRepository) {
        this.strategyRepository = strategyRepository;
    }

    @Override
    public List<Strategy> getAllStrategiesByUserId(String userId) {
        log.info("Getting all strategies for user: {}", userId);
        return strategyRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<Strategy> getStrategyByIdAndUserId(String strategyId, String userId) {
        log.info("Getting strategy with id: {} for user: {}", strategyId, userId);
        return strategyRepository.findByIdAndUserId(strategyId, userId);
    }

    @Override
    public Strategy saveStrategy(Strategy strategy) {
        // Validate inputs
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }
        
        if (strategy.getUserId() == null || strategy.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // Set timestamps
        String now = new Date().toString();
        
        // For new strategies
        if (strategy.getId() == null || strategy.getId().isEmpty()) {
            strategy.setCreatedAt(now);
            log.info("Creating new strategy for user: {}", strategy.getUserId());
        } else {
            log.info("Updating strategy with id: {} for user: {}", strategy.getId(), strategy.getUserId());
        }
        
        strategy.setUpdatedAt(now);
        
        // Save to repository
        return strategyRepository.save(strategy);
    }

    @Override
    public boolean deleteStrategy(String strategyId, String userId) {
        log.info("Deleting strategy with id: {} for user: {}", strategyId, userId);
        return strategyRepository.deleteByIdAndUserId(strategyId, userId);
    }

    @Override
    public boolean updateStrategyStatus(String strategyId, String userId, String status, String exchangeInfo) {
        log.info("Updating status to '{}' for strategy with id: {} for user: {}", status, strategyId, userId);
        
        // Validate status
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        
        // Create deployment info if moving to LIVE
        Map<String, Object> deploymentInfo = null;
        if ("LIVE".equals(status) && exchangeInfo != null && !exchangeInfo.isEmpty()) {
            deploymentInfo = new HashMap<>();
            deploymentInfo.put("exchange", exchangeInfo);
            deploymentInfo.put("deployedAt", new Date().toString());
            deploymentInfo.put("status", "active");
        }
        
        return strategyRepository.updateStatus(strategyId, userId, status, deploymentInfo);
    }

    @Override
    public Map<String, Object> runBacktest(String strategyId, String userId, Map<String, Object> backtestParams) {
        log.info("Running backtest for strategy with id: {} for user: {}", strategyId, userId);
        
        // Get the strategy
        Optional<Strategy> strategyOpt = strategyRepository.findByIdAndUserId(strategyId, userId);
        if (!strategyOpt.isPresent()) {
            throw new IllegalArgumentException("Strategy not found");
        }
        
        Strategy strategy = strategyOpt.get();
        
        // TODO: Implement actual backtest logic based on strategy language (Python, Java)
        // For now, return mock results
        Map<String, Object> backtestResult = new HashMap<>();
        backtestResult.put("totalReturns", 12.5);
        backtestResult.put("sharpeRatio", 1.2);
        backtestResult.put("maxDrawdown", -5.3);
        backtestResult.put("winRate", 0.65);
        backtestResult.put("trades", 42);
        backtestResult.put("startDate", backtestParams.getOrDefault("startDate", "2023-01-01"));
        backtestResult.put("endDate", backtestParams.getOrDefault("endDate", "2023-12-31"));
        
        // Save backtest results to the strategy
        strategy.setBacktestResults(backtestResult);
        strategyRepository.save(strategy);
        
        return backtestResult;
    }
    
    /**
     * Validate if the strategy status is valid
     */
    private boolean isValidStatus(String status) {
        return "DRAFT".equals(status) || "TESTING".equals(status) || "LIVE".equals(status);
    }
}
