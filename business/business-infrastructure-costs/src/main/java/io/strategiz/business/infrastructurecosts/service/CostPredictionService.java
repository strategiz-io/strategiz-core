package io.strategiz.business.infrastructurecosts.service;

import io.strategiz.business.infrastructurecosts.model.CostPrediction;
import io.strategiz.data.infrastructurecosts.entity.DailyCostEntity;
import io.strategiz.data.infrastructurecosts.repository.DailyCostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Service for predicting infrastructure costs using linear regression.
 * Uses weighted recent data to project end-of-month costs.
 */
@Service
public class CostPredictionService {

    private static final Logger log = LoggerFactory.getLogger(CostPredictionService.class);
    private static final int HISTORICAL_DAYS = 30;

    private final DailyCostRepository dailyCostRepository;
    private final CostAggregationService costAggregationService;

    public CostPredictionService(
            DailyCostRepository dailyCostRepository,
            CostAggregationService costAggregationService) {
        this.dailyCostRepository = dailyCostRepository;
        this.costAggregationService = costAggregationService;
    }

    /**
     * Predict monthly costs using weighted linear regression
     */
    public CostPrediction predictMonthlyCosts() {
        log.info("Calculating cost prediction for current month");

        try {
            // Get historical daily costs
            List<DailyCostEntity> historicalCosts = dailyCostRepository.findRecent(HISTORICAL_DAYS);

            if (historicalCosts.isEmpty()) {
                log.warn("No historical data available for prediction");
                return predictFromCurrentMonth();
            }

            // Extract cost values for regression
            List<BigDecimal> costs = historicalCosts.stream()
                    .filter(c -> c.getTotalCost() != null)
                    .map(DailyCostEntity::getTotalCost)
                    .toList();

            if (costs.isEmpty()) {
                return predictFromCurrentMonth();
            }

            // Calculate weighted average (recent days weighted higher)
            BigDecimal weightedSum = BigDecimal.ZERO;
            BigDecimal weightSum = BigDecimal.ZERO;

            for (int i = 0; i < costs.size(); i++) {
                // Weight: more recent = higher weight
                // Weight decay: 1.0, 0.95, 0.90, ... (5% decay per day)
                double weight = Math.max(0.5, 1.0 - (i * 0.05));
                BigDecimal weightBd = BigDecimal.valueOf(weight);

                weightedSum = weightedSum.add(costs.get(i).multiply(weightBd));
                weightSum = weightSum.add(weightBd);
            }

            BigDecimal avgDailyCost = weightedSum.divide(weightSum, 4, RoundingMode.HALF_UP);

            // Calculate remaining days in month
            LocalDate today = LocalDate.now();
            int totalDaysInMonth = YearMonth.now().lengthOfMonth();
            int daysRemaining = totalDaysInMonth - today.getDayOfMonth();

            // Get current month spend so far
            BigDecimal currentMonthSpend = historicalCosts.stream()
                    .filter(c -> c.getDate() != null && c.getDate().startsWith(today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))))
                    .filter(c -> c.getTotalCost() != null)
                    .map(DailyCostEntity::getTotalCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Predict end-of-month total
            BigDecimal projectedRemaining = avgDailyCost.multiply(BigDecimal.valueOf(daysRemaining));
            BigDecimal predictedTotal = currentMonthSpend.add(projectedRemaining);

            // Calculate variance for confidence interval
            BigDecimal variance = calculateVariance(costs, avgDailyCost);
            BigDecimal stdDev = sqrt(variance);

            // 90% confidence interval (1.645 * stdDev)
            BigDecimal confidenceMargin = stdDev.multiply(BigDecimal.valueOf(1.645))
                    .multiply(BigDecimal.valueOf(Math.sqrt(daysRemaining)));

            BigDecimal lowEstimate = predictedTotal.subtract(confidenceMargin).max(BigDecimal.ZERO);
            BigDecimal highEstimate = predictedTotal.add(confidenceMargin);

            // Calculate confidence level based on data quality
            BigDecimal confidenceLevel = calculateConfidenceLevel(costs.size(), variance);

            // Break down by service (using average ratios from historical data)
            Map<String, BigDecimal> breakdown = calculateServiceBreakdown(historicalCosts, predictedTotal);

            return new CostPrediction(
                    predictedTotal.setScale(2, RoundingMode.HALF_UP),
                    new CostPrediction.ConfidenceInterval(
                            lowEstimate.setScale(2, RoundingMode.HALF_UP),
                            highEstimate.setScale(2, RoundingMode.HALF_UP)
                    ),
                    confidenceLevel.setScale(2, RoundingMode.HALF_UP),
                    breakdown
            );

        } catch (Exception e) {
            log.error("Error calculating cost prediction: {}", e.getMessage(), e);
            return CostPrediction.empty();
        }
    }

    /**
     * Fallback prediction from current month data when no historical data exists
     */
    private CostPrediction predictFromCurrentMonth() {
        try {
            var summary = costAggregationService.getCurrentMonthSummary();

            if (summary.daysSoFar() == 0) {
                return CostPrediction.empty();
            }

            // Simple projection: (current spend / days so far) * days in month
            int totalDays = YearMonth.now().lengthOfMonth();
            BigDecimal projectedTotal = summary.totalCost()
                    .divide(BigDecimal.valueOf(summary.daysSoFar()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(totalDays));

            // Low confidence due to limited data
            BigDecimal margin = projectedTotal.multiply(BigDecimal.valueOf(0.2)); // 20% margin

            return new CostPrediction(
                    projectedTotal.setScale(2, RoundingMode.HALF_UP),
                    new CostPrediction.ConfidenceInterval(
                            projectedTotal.subtract(margin).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                            projectedTotal.add(margin).setScale(2, RoundingMode.HALF_UP)
                    ),
                    BigDecimal.valueOf(0.5), // 50% confidence
                    summary.costByService()
            );
        } catch (Exception e) {
            log.error("Error in fallback prediction: {}", e.getMessage());
            return CostPrediction.empty();
        }
    }

    private BigDecimal calculateVariance(List<BigDecimal> values, BigDecimal mean) {
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            BigDecimal diff = value.subtract(mean);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }

        return sumSquaredDiff.divide(BigDecimal.valueOf(values.size() - 1), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.sqrt(value.doubleValue()));
    }

    private BigDecimal calculateConfidenceLevel(int dataPoints, BigDecimal variance) {
        // More data points and lower variance = higher confidence
        // Base confidence starts at 0.5, max at 0.95

        double dataPointsFactor = Math.min(1.0, dataPoints / 30.0);
        double varianceFactor = 1.0 / (1.0 + variance.doubleValue());

        double confidence = 0.5 + (0.45 * dataPointsFactor * varianceFactor);
        return BigDecimal.valueOf(Math.min(0.95, confidence));
    }

    private Map<String, BigDecimal> calculateServiceBreakdown(
            List<DailyCostEntity> historicalCosts,
            BigDecimal predictedTotal) {

        // Aggregate service costs to calculate ratios
        Map<String, BigDecimal> serviceTotals = new HashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (DailyCostEntity entity : historicalCosts) {
            if (entity.getCostByService() != null) {
                for (Map.Entry<String, BigDecimal> entry : entity.getCostByService().entrySet()) {
                    serviceTotals.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
                    grandTotal = grandTotal.add(entry.getValue());
                }
            }
        }

        // Calculate predicted breakdown using ratios
        Map<String, BigDecimal> breakdown = new HashMap<>();
        if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> entry : serviceTotals.entrySet()) {
                BigDecimal ratio = entry.getValue().divide(grandTotal, 6, RoundingMode.HALF_UP);
                BigDecimal predictedServiceCost = predictedTotal.multiply(ratio);
                breakdown.put(entry.getKey(), predictedServiceCost.setScale(2, RoundingMode.HALF_UP));
            }
        }

        return breakdown;
    }
}
