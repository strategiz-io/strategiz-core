package io.strategiz.business.strategy.execution.service;

import io.strategiz.business.strategy.execution.model.BacktestPerformance;
import io.strategiz.business.strategy.execution.model.BacktestTrade;
import io.strategiz.business.strategy.execution.model.SignalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business layer service for calculating backtest performance metrics from strategy signals and market data.
 */
@Service
public class BacktestCalculatorBusiness {

    private static final Logger logger = LoggerFactory.getLogger(BacktestCalculatorBusiness.class);
    private static final double DEFAULT_INITIAL_CAPITAL = 10000.0;

    /**
     * Calculate backtest performance metrics from signals and market data.
     *
     * @param signals    List of trading signals (BUY/SELL)
     * @param marketData List of OHLCV data points with timestamp and close price
     * @return BacktestPerformance object with all metrics and trade breakdown
     */
    public BacktestPerformance calculatePerformance(List<SignalData> signals, List<Map<String, Object>> marketData) {
        return calculatePerformance(signals, marketData, DEFAULT_INITIAL_CAPITAL);
    }

    /**
     * Calculate backtest performance metrics from signals and market data.
     *
     * @param signals        List of trading signals (BUY/SELL)
     * @param marketData     List of OHLCV data points with timestamp and close price
     * @param initialCapital Starting capital for backtest
     * @return BacktestPerformance object with all metrics and trade breakdown
     */
    public BacktestPerformance calculatePerformance(List<SignalData> signals, List<Map<String, Object>> marketData, double initialCapital) {
        BacktestPerformance performance = new BacktestPerformance();
        performance.setLastTestedAt(Instant.now().toString());

        // Calculate and set backtest period from market data
        BacktestPeriodInfo periodInfo = calculateBacktestPeriod(marketData);
        performance.setTestPeriod(periodInfo.testPeriod);
        performance.setBacktestStartDate(periodInfo.backtestStartDate);
        performance.setBacktestEndDate(periodInfo.backtestEndDate);
        performance.setBacktestPeriodDays(periodInfo.backtestPeriodDays);

        if (signals == null || signals.isEmpty()) {
            performance.setTrades(new ArrayList<>());
            return performance;
        }

        // Build price lookup map from market data
        Map<String, Double> priceMap = buildPriceMap(marketData);

        // Sort signals by timestamp
        List<SignalData> sortedSignals = signals.stream()
                .sorted(Comparator.comparing(SignalData::getTimestamp))
                .collect(Collectors.toList());

        // Separate buy and sell signals
        List<SignalData> buySignals = sortedSignals.stream()
                .filter(s -> "BUY".equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());

        List<SignalData> sellSignals = sortedSignals.stream()
                .filter(s -> "SELL".equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());

        performance.setBuyCount(buySignals.size());
        performance.setSellCount(sellSignals.size());

        // Pair buy signals with subsequent sell signals to form trades
        List<BacktestTrade> trades = pairSignalsToTrades(buySignals, sellSignals, priceMap);
        performance.setTrades(trades);
        performance.setTotalTrades(trades.size());

        if (trades.isEmpty()) {
            return performance;
        }

        // Calculate metrics from trades
        List<BacktestTrade> winningTrades = trades.stream().filter(BacktestTrade::isWin).collect(Collectors.toList());
        List<BacktestTrade> losingTrades = trades.stream().filter(t -> !t.isWin()).collect(Collectors.toList());

        performance.setProfitableTrades(winningTrades.size());

        // Total P&L
        double totalPnL = trades.stream().mapToDouble(BacktestTrade::getPnl).sum();
        double totalPnLPercent = trades.stream().mapToDouble(BacktestTrade::getPnlPercent).sum();
        performance.setTotalPnL(totalPnL);
        performance.setTotalReturn(totalPnLPercent);

        // Win rate
        double winRate = (double) winningTrades.size() / trades.size() * 100;
        performance.setWinRate(winRate);

        // Average win/loss
        double grossProfit = winningTrades.stream().mapToDouble(BacktestTrade::getPnl).sum();
        double grossLoss = Math.abs(losingTrades.stream().mapToDouble(BacktestTrade::getPnl).sum());

        double avgWin = winningTrades.isEmpty() ? 0 : grossProfit / winningTrades.size();
        double avgLoss = losingTrades.isEmpty() ? 0 : grossLoss / losingTrades.size();
        performance.setAvgWin(avgWin);
        performance.setAvgLoss(avgLoss);

        // Profit factor
        double profitFactor = grossLoss > 0 ? grossProfit / grossLoss : (grossProfit > 0 ? Double.MAX_VALUE : 0);
        performance.setProfitFactor(profitFactor);

        // Max drawdown
        double maxDrawdown = calculateMaxDrawdown(trades, initialCapital);
        performance.setMaxDrawdown(maxDrawdown);

        // Sharpe ratio (simplified - assumes 0% risk-free rate)
        double sharpeRatio = calculateSharpeRatio(trades);
        performance.setSharpeRatio(sharpeRatio);

        return performance;
    }

    /**
     * Build a price lookup map from market data.
     */
    private Map<String, Double> buildPriceMap(List<Map<String, Object>> marketData) {
        if (marketData == null) {
            return Map.of();
        }

        return marketData.stream()
                .filter(bar -> bar.get("timestamp") != null && bar.get("close") != null)
                .collect(Collectors.toMap(
                        bar -> formatDateKey(bar.get("timestamp")),
                        bar -> ((Number) bar.get("close")).doubleValue(),
                        (existing, replacement) -> replacement // Handle duplicates
                ));
    }

    /**
     * Format timestamp to date key for price lookup.
     */
    private String formatDateKey(Object timestamp) {
        if (timestamp == null) {
            return "";
        }

        String ts = timestamp.toString();

        // Handle ISO date-time format
        if (ts.contains("T")) {
            return ts.split("T")[0];
        }

        // Handle Unix timestamp (seconds or milliseconds)
        try {
            long time = Long.parseLong(ts);
            if (time > 1000000000000L) {
                // Milliseconds
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                // Seconds
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (NumberFormatException e) {
            // Already a date string
            return ts;
        }
    }

    /**
     * Pair buy signals with subsequent sell signals to form trades.
     */
    private List<BacktestTrade> pairSignalsToTrades(List<SignalData> buySignals, List<SignalData> sellSignals, Map<String, Double> priceMap) {
        List<BacktestTrade> trades = new ArrayList<>();
        int buyIndex = 0;
        int sellIndex = 0;

        while (buyIndex < buySignals.size() && sellIndex < sellSignals.size()) {
            SignalData buySignal = buySignals.get(buyIndex);
            long buyTime = parseTimestamp(buySignal.getTimestamp());

            // Find next sell signal after this buy
            while (sellIndex < sellSignals.size() && parseTimestamp(sellSignals.get(sellIndex).getTimestamp()) <= buyTime) {
                sellIndex++;
            }

            if (sellIndex >= sellSignals.size()) {
                break;
            }

            SignalData sellSignal = sellSignals.get(sellIndex);

            // Get prices for this trade
            double buyPrice = buySignal.getPrice() > 0 ? buySignal.getPrice() : getPriceFromMap(priceMap, buySignal.getTimestamp());
            double sellPrice = sellSignal.getPrice() > 0 ? sellSignal.getPrice() : getPriceFromMap(priceMap, sellSignal.getTimestamp());

            if (buyPrice > 0 && sellPrice > 0) {
                BacktestTrade trade = new BacktestTrade();
                trade.setBuyTimestamp(buySignal.getTimestamp());
                trade.setSellTimestamp(sellSignal.getTimestamp());
                trade.setBuyPrice(buyPrice);
                trade.setSellPrice(sellPrice);
                trade.setBuyReason(buySignal.getReason());
                trade.setSellReason(sellSignal.getReason());

                double pnl = sellPrice - buyPrice;
                double pnlPercent = (pnl / buyPrice) * 100;
                trade.setPnl(pnl);
                trade.setPnlPercent(pnlPercent);
                trade.setWin(pnl > 0);

                trades.add(trade);
            }

            buyIndex++;
            sellIndex++;
        }

        return trades;
    }

    /**
     * Parse timestamp string to epoch milliseconds.
     */
    private long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return 0;
        }

        try {
            // Try parsing as ISO date-time
            if (timestamp.contains("T")) {
                return Instant.parse(timestamp).toEpochMilli();
            }

            // Try parsing as date only (assume start of day UTC)
            return LocalDateTime.parse(timestamp + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception e) {
            try {
                // Try as Unix timestamp
                long time = Long.parseLong(timestamp);
                return time > 1000000000000L ? time : time * 1000;
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
    }

    /**
     * Get price from map using timestamp.
     */
    private double getPriceFromMap(Map<String, Double> priceMap, String timestamp) {
        String dateKey = formatDateKey(timestamp);
        return priceMap.getOrDefault(dateKey, 0.0);
    }

    /**
     * Calculate maximum drawdown from trades.
     */
    private double calculateMaxDrawdown(List<BacktestTrade> trades, double initialCapital) {
        if (trades.isEmpty()) {
            return 0;
        }

        double equity = initialCapital;
        double peak = initialCapital;
        double maxDrawdown = 0;

        for (BacktestTrade trade : trades) {
            double tradeReturn = trade.getPnlPercent() / 100;
            equity = equity * (1 + tradeReturn);

            if (equity > peak) {
                peak = equity;
            }

            double drawdown = ((peak - equity) / peak) * 100;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown;
    }

    /**
     * Calculate Sharpe ratio from trades (simplified, assumes 0% risk-free rate).
     */
    private double calculateSharpeRatio(List<BacktestTrade> trades) {
        if (trades.size() < 2) {
            return 0;
        }

        double[] returns = trades.stream().mapToDouble(BacktestTrade::getPnlPercent).toArray();

        // Calculate mean return
        double mean = 0;
        for (double r : returns) {
            mean += r;
        }
        mean /= returns.length;

        // Calculate standard deviation
        double variance = 0;
        for (double r : returns) {
            variance += Math.pow(r - mean, 2);
        }
        variance /= returns.length;
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0) {
            return 0;
        }

        // Annualized Sharpe (assuming ~252 trading days)
        return (mean / stdDev) * Math.sqrt(252);
    }

    /**
     * Calculate and format backtest period from market data timestamps.
     *
     * @param marketData List of OHLCV data points with timestamp
     * @return BacktestPeriodInfo with formatted period and dates
     */
    private BacktestPeriodInfo calculateBacktestPeriod(List<Map<String, Object>> marketData) {
        BacktestPeriodInfo info = new BacktestPeriodInfo();

        if (marketData == null || marketData.isEmpty()) {
            info.testPeriod = "No data";
            info.backtestPeriodDays = 0;
            return info;
        }

        try {
            // Extract first and last timestamps from data
            String firstTimestamp = extractTimestamp(marketData.get(0));
            String lastTimestamp = extractTimestamp(marketData.get(marketData.size() - 1));

            if (firstTimestamp == null || lastTimestamp == null) {
                info.testPeriod = "Unknown";
                info.backtestPeriodDays = 0;
                return info;
            }

            // Parse to LocalDateTime
            LocalDateTime startDate = parseTimestampToLocalDateTime(firstTimestamp);
            LocalDateTime endDate = parseTimestampToLocalDateTime(lastTimestamp);

            // Store ISO timestamps
            info.backtestStartDate = startDate.toString();
            info.backtestEndDate = endDate.toString();

            // Calculate days
            long days = ChronoUnit.DAYS.between(startDate, endDate);
            info.backtestPeriodDays = (int) days;

            // Format human-readable
            info.testPeriod = formatPeriod(days, startDate);

        } catch (Exception e) {
            logger.error("Error calculating backtest period", e);
            info.testPeriod = "Unknown";
            info.backtestPeriodDays = 0;
        }

        return info;
    }

    /**
     * Extract timestamp from market data bar.
     */
    private String extractTimestamp(Map<String, Object> bar) {
        if (bar == null) {
            return null;
        }

        // Try "timestamp" field first
        Object ts = bar.get("timestamp");
        if (ts == null) {
            // Fall back to "time" field
            ts = bar.get("time");
        }

        return ts != null ? ts.toString() : null;
    }

    /**
     * Parse timestamp string to LocalDateTime.
     * Handles ISO format, Unix seconds, and Unix milliseconds.
     */
    private LocalDateTime parseTimestampToLocalDateTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // Try parsing as ISO date-time
            if (timestamp.contains("T")) {
                Instant instant = Instant.parse(timestamp);
                return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            }

            // Try parsing as date only (assume start of day UTC)
            if (timestamp.contains("-") && !timestamp.contains(":")) {
                return LocalDateTime.parse(timestamp + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            // Try as Unix timestamp (seconds or milliseconds)
            long time = Long.parseLong(timestamp);
            if (time > 1000000000000L) {
                // Milliseconds
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC);
            } else {
                // Seconds
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneOffset.UTC);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse timestamp '{}', using current time", timestamp);
            return LocalDateTime.now();
        }
    }

    /**
     * Format period as human-readable string.
     * - >= 1 year: "X.XY" (e.g., "5.3Y")
     * - >= 2 months: "XM" (e.g., "6M")
     * - >= 2 weeks: "XW" (e.g., "3W")
     * - < 2 weeks: "XD" (e.g., "10D")
     * - > 5 years: "Since MMM YYYY"
     *
     * @param days      Number of days in the period
     * @param startDate Start date for "Since" format
     * @return Formatted period string
     */
    private String formatPeriod(long days, LocalDateTime startDate) {
        if (days == 0) {
            return "1D";
        }

        // For periods > 5 years, use "Since MMM YYYY" format
        if (days > 1825) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
            return "Since " + startDate.format(formatter);
        }

        // Calculate years
        double years = days / 365.25;
        if (years >= 1.0) {
            // If close to whole number (>= 1.95), round to whole year
            if (years >= 1.95) {
                return String.format("%.0fY", years);
            }
            // Otherwise show one decimal place
            return String.format("%.1fY", years);
        }

        // Calculate months
        double months = days / 30.44;
        if (months >= 2.0) {
            return String.format("%.0fM", months);
        }

        // Calculate weeks
        double weeks = days / 7.0;
        if (weeks >= 2.0) {
            return String.format("%.0fW", weeks);
        }

        // Default to days
        return days + "D";
    }

    /**
     * Inner class to hold backtest period calculation results.
     */
    private static class BacktestPeriodInfo {
        String testPeriod;
        String backtestStartDate;
        String backtestEndDate;
        Integer backtestPeriodDays;
    }
}
