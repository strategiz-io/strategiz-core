package io.strategiz.service.dashboard;

import io.strategiz.service.base.BaseService;
import io.strategiz.business.portfolio.PortfolioManager;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.service.dashboard.model.performancemetrics.PerformanceMetricsData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for portfolio performance metrics operations.
 * This service provides historical performance data and portfolio metrics.
 */
@Service
public class PerformanceMetricsService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMetricsService.class);

    private final PortfolioManager portfolioManager;

    @Autowired
    public PerformanceMetricsService(PortfolioManager portfolioManager) {
        this.portfolioManager = portfolioManager;
    }

    @Override
    protected String getModuleName() {
        return "service-dashboard";
    }

    /**
     * Gets performance metrics for the user's portfolio
     * 
     * @param userId The user ID to fetch performance data for
     * @return Performance metrics response
     */
    public PerformanceMetricsData getPerformanceMetrics(String userId) {
        log.info("Getting performance metrics for user: {}", userId);
        
        try {
            // Get current portfolio data
            PortfolioData portfolioData = portfolioManager.getAggregatedPortfolioData(userId);
            
            // Get historical portfolio values - in a real implementation this would come from a database
            List<PortfolioData> historicalData = getHistoricalPortfolioData(userId);
            
            // Calculate performance metrics
            return calculatePerformanceMetrics(portfolioData, historicalData);
        } catch (Exception e) {
            log.error("Error getting performance metrics for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve performance metrics", e);
        }
    }
    
    /**
     * Gets historical portfolio data for a user.
     * In a real implementation, this would come from a database of historical snapshots.
     * This is a placeholder implementation that generates mock historical data.
     * 
     * @param userId The user ID to fetch historical data for
     * @return List of historical portfolio data points
     */
    private List<PortfolioData> getHistoricalPortfolioData(String userId) {
        // In a real implementation, this would fetch historical data from a database
        // For now, we'll return a simple mock implementation
        
        List<PortfolioData> historicalData = new ArrayList<>();
        // Current data is already being used, no need to add it here
        
        return historicalData;
    }
    
    /**
     * Calculates performance metrics from current and historical portfolio data
     * 
     * @param currentData Current portfolio data
     * @param historicalData Historical portfolio data
     * @return Performance metrics response
     */
    private PerformanceMetricsData calculatePerformanceMetrics(
            PortfolioData currentData, 
            List<PortfolioData> historicalData) {
        
        PerformanceMetricsData response = new PerformanceMetricsData();
        
        // Create historical values list for chart data
        List<PerformanceMetricsData.PortfolioValueDataPoint> dataPoints = 
            new ArrayList<>();
            
        // Current time for the latest data point
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        
        // Add current data point
        PerformanceMetricsData.PortfolioValueDataPoint currentPoint = 
            new PerformanceMetricsData.PortfolioValueDataPoint();
        currentPoint.setTimestamp(now);
        currentPoint.setValue(currentData.getTotalValue() != null ? 
            currentData.getTotalValue() : BigDecimal.ZERO);
        dataPoints.add(currentPoint);
        
        // Add historical data points
        if (historicalData != null) {
            for (PortfolioData historicalPoint : historicalData) {
                // In a real implementation, PortfolioData would have a timestamp field
                // For now, we'll simulate that historicalPoint has this data
                LocalDateTime historicalTimestamp = now.minusDays(1); // Example
                BigDecimal historicalValue = historicalPoint.getTotalValue();
                
                if (historicalValue != null) {
                    PerformanceMetricsData.PortfolioValueDataPoint dataPoint = 
                        new PerformanceMetricsData.PortfolioValueDataPoint();
                    dataPoint.setTimestamp(historicalTimestamp);
                    dataPoint.setValue(historicalValue);
                    dataPoints.add(dataPoint);
                }
            }
        }
        
        response.setHistoricalValues(dataPoints);
        
        // Create performance summary
        PerformanceMetricsData.PerformanceSummary summary = calculateSummary(currentData, historicalData);
        response.setSummary(summary);
        
        return response;
    }
    
    /**
     * Calculates performance summary metrics
     * 
     * @param currentData Current portfolio data
     * @param historicalData Historical portfolio data
     * @return Performance summary
     */
    private PerformanceMetricsData.PerformanceSummary calculateSummary(
            PortfolioData currentData, 
            List<PortfolioData> historicalData) {
        
        PerformanceMetricsData.PerformanceSummary summary = 
            new PerformanceMetricsData.PerformanceSummary();
        
        // Get current total value
        BigDecimal currentValue = currentData.getTotalValue() != null ? 
            currentData.getTotalValue() : BigDecimal.ZERO;
            
        // Initialize with daily change from current data
        summary.setDailyChange(currentData.getDailyChange() != null ? 
            currentData.getDailyChange() : BigDecimal.ZERO);
        summary.setDailyChangePercentage(currentData.getDailyChangePercent() != null ? 
            currentData.getDailyChangePercent() : BigDecimal.ZERO);
        
        // Initialize other metrics with zeros
        summary.setTotalProfitLoss(BigDecimal.ZERO);
        summary.setTotalProfitLossPercentage(BigDecimal.ZERO);
        summary.setWeeklyChange(BigDecimal.ZERO);
        summary.setWeeklyChangePercentage(BigDecimal.ZERO);
        summary.setMonthlyChange(BigDecimal.ZERO);
        summary.setMonthlyChangePercentage(BigDecimal.ZERO);
        summary.setYtdChange(BigDecimal.ZERO);
        summary.setYtdChangePercentage(BigDecimal.ZERO);
        
        // Default to not profitable
        summary.setProfitable(false);
        
        // If we have historical data, calculate the changes
        if (historicalData != null && !historicalData.isEmpty() && currentValue.compareTo(BigDecimal.ZERO) > 0) {
            // In a real implementation, we'd use actual historical data here
            // For now, we'll simulate some basic metrics
            
            // Simulate a slight profit scenario
            BigDecimal profitValue = currentValue.multiply(new BigDecimal("0.05"));
            summary.setTotalProfitLoss(profitValue);
            summary.setTotalProfitLossPercentage(new BigDecimal("5.00"));
            summary.setProfitable(true);
            
            // Set some example weekly and monthly changes
            summary.setWeeklyChange(currentValue.multiply(new BigDecimal("0.02")));
            summary.setWeeklyChangePercentage(new BigDecimal("2.00"));
            
            summary.setMonthlyChange(currentValue.multiply(new BigDecimal("0.03")));
            summary.setMonthlyChangePercentage(new BigDecimal("3.00"));
            
            summary.setYtdChange(currentValue.multiply(new BigDecimal("0.04")));
            summary.setYtdChangePercentage(new BigDecimal("4.00"));
        }
        
        return summary;
    }
    
    /**
     * Finds the historical data point closest to the target date
     * In a real implementation, PortfolioData would have timestamp information
     * 
     * @param historicalData List of historical data points
     * @param targetDate Target date to find closest point to
     * @return Closest historical data point, or null if none found
     */
    private PortfolioData findClosestHistoricalData(List<PortfolioData> historicalData, LocalDateTime targetDate) {
        // In a real implementation, we would compare timestamps
        // For now, just return the first item if available
        return historicalData.isEmpty() ? null : historicalData.get(0);
    }
    
    /**
     * Finds the oldest historical data point
     * In a real implementation, PortfolioData would have timestamp information
     * 
     * @param historicalData List of historical data points
     * @return Oldest historical data point, or null if none found
     */
    private PortfolioData findOldestHistoricalData(List<PortfolioData> historicalData) {
        // In a real implementation, we would compare timestamps
        // For now, just return the first item if available
        return historicalData.isEmpty() ? null : historicalData.get(0);
    }
}
