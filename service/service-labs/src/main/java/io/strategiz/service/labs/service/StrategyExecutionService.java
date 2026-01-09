package io.strategiz.service.labs.service;

import io.strategiz.client.execution.ExecutionServiceClient;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.execution.grpc.MarketDataBar;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import io.strategiz.service.labs.model.ExecuteStrategyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lightweight service for strategy execution via gRPC microservice.
 *
 * Architecture: Controller → Service (this) → DAO (ExecutionServiceClient) → gRPC
 *
 * Responsibilities:
 * 1. Fetch market data (one DB call)
 * 2. Call gRPC DAO (one external call)
 * 3. Map POJO to DTO (pure transformation)
 *
 * IMPORTANT: Keep this lightweight - no complex business logic or chatty DB operations.
 * The real business logic is in the Python code. This is just a wrapper.
 */
@Service
public class StrategyExecutionService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(StrategyExecutionService.class);

    private final ExecutionServiceClient executionServiceClient;
    private final MarketDataRepository marketDataRepository;

    public StrategyExecutionService(
            ExecutionServiceClient executionServiceClient,
            MarketDataRepository marketDataRepository) {
        this.executionServiceClient = executionServiceClient;
        this.marketDataRepository = marketDataRepository;
    }

    @Override
    protected String getModuleName() {
        return "service-labs";
    }

    /**
     * Execute Python strategy code via gRPC microservice.
     *
     * @param code Python strategy code
     * @param language Programming language (always "python" for now)
     * @param symbol Stock/crypto symbol (e.g., "AAPL", "BTC")
     * @param timeframe Chart timeframe (e.g., "1D", "1H")
     * @param period Backtest period (e.g., "6mo", "1y", "2y", "5y", "7y", "max")
     * @param userId User ID for tracking
     * @param strategy Optional strategy entity (for seedFundingDate)
     * @return REST DTO with ALL fields initialized
     */
    public ExecuteStrategyResponse executeStrategy(
            String code,
            String language,
            String symbol,
            String timeframe,
            String period,
            String userId,
            Strategy strategy) {

        logger.info("Executing strategy for user={}, symbol={}, language={}, timeframe={}, period={}", userId, symbol, language, timeframe, period);

        // 1. Fetch market data (one DB call)
        List<Map<String, Object>> marketDataList = fetchMarketData(symbol, timeframe, period, strategy);
        logger.info("Fetched {} market data bars for symbol {} with timeframe {}", marketDataList.size(), symbol, timeframe);

        // 2. Convert to gRPC format
        List<MarketDataBar> grpcMarketData = marketDataList.stream()
            .map(ExecutionServiceClient::createMarketDataBar)
            .collect(Collectors.toList());

        // 3. Call DAO layer (gRPC client)
        logger.info("Calling gRPC execution service...");
        io.strategiz.client.execution.model.ExecutionResponse grpcResponse = executionServiceClient.executeStrategy(
            code,
            language,
            grpcMarketData,
            userId,
            "execution-" + System.currentTimeMillis(),
            30  // timeout seconds
        );

        logger.info("gRPC execution complete: success={}, executionTime={}ms",
            grpcResponse.isSuccess(), grpcResponse.getExecutionTimeMs());

        // 4. Map POJO to DTO (pure transformation, no DB calls)
        return mapToRestDto(grpcResponse, symbol, timeframe);
    }

    /**
     * Fetch market data from repository.
     * Lightweight - single DB query.
     */
    private List<Map<String, Object>> fetchMarketData(String symbol, String timeframe, String period, Strategy strategy) {
        logger.info("Fetching market data for symbol: {} with timeframe: {} and period: {}", symbol, timeframe, period);

        // Calculate date range
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = calculateStartDate(strategy, endDate, timeframe, period);

        logger.info("Date range: {} to {} ({} days)",
            startDate, endDate, java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate));

        // Convert timeframe to database format (1H -> 1Hour, 1D -> 1Day, etc.)
        String dbTimeframe = convertToDbTimeframe(timeframe);
        logger.info("Converted timeframe {} to database format: {}", timeframe, dbTimeframe);

        // Single query to repository with timeframe filter (limit to 3000 bars)
        List<MarketDataEntity> entities = marketDataRepository.findBySymbolAndDateRange(
            symbol, startDate, endDate, dbTimeframe, 3000
        );

        // Validate data exists
        if (entities == null || entities.isEmpty()) {
            logger.warn("No market data found for symbol: {}", symbol);
            throwModuleException(
                ServiceStrategyErrorDetails.MARKET_DATA_NOT_FOUND,
                String.format("No market data found for symbol: %s. Please ensure batch job has run.", symbol)
            );
        }

        // Convert to map format for gRPC
        return entities.stream()
            .map(this::convertToMap)
            .collect(Collectors.toList());
    }

    /**
     * Calculate backtest start date based on:
     * 1. User-selected period (highest priority)
     * 2. Strategy's seedFundingDate (if set)
     * 3. Dynamic timeframe-based default
     *
     * Period options: 6mo, 1y, 2y, 5y, 7y, max
     */
    private LocalDate calculateStartDate(Strategy strategy, LocalDate endDate, String timeframe, String period) {
        // 1. Use period if explicitly provided by user (highest priority)
        if (period != null && !period.isEmpty()) {
            LocalDate periodStart = calculateStartDateFromPeriod(endDate, period);
            logger.info("Using user-selected period '{}': start={}", period, periodStart);
            return periodStart;
        }

        // 2. Use seedFundingDate if provided (strategy-level override)
        if (strategy != null && strategy.getSeedFundingDate() != null) {
            try {
                LocalDate seedDate = strategy.getSeedFundingDate()
                    .toDate()
                    .toInstant()
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();

                // Validate not in future
                if (seedDate.isAfter(endDate)) {
                    logger.warn("Seed funding date {} is in future, using dynamic default for timeframe {}",
                        seedDate, timeframe);
                    return calculateDynamicStartDate(endDate, timeframe);
                }

                logger.info("Using seed funding date: {}", seedDate);
                return seedDate;
            } catch (Exception e) {
                logger.error("Error parsing seed funding date, using dynamic default for timeframe {}", timeframe, e);
            }
        }

        // 3. Dynamic defaults based on timeframe
        return calculateDynamicStartDate(endDate, timeframe);
    }

    /**
     * Calculate start date based on user-selected period.
     *
     * @param endDate End date (yesterday)
     * @param period One of: 6mo, 1y, 2y, 5y, 7y, max
     * @return Start date for the backtest period
     */
    private LocalDate calculateStartDateFromPeriod(LocalDate endDate, String period) {
        return switch (period.toLowerCase()) {
            case "6mo" -> endDate.minusMonths(6);
            case "1y" -> endDate.minusYears(1);
            case "2y" -> endDate.minusYears(2);
            case "5y" -> endDate.minusYears(5);
            case "7y" -> endDate.minusYears(7);
            case "max" -> LocalDate.of(2018, 1, 1);  // Earliest available data
            default -> endDate.minusYears(2);        // Safe default
        };
    }

    /**
     * Calculate dynamic start date based on timeframe (fallback).
     */
    private LocalDate calculateDynamicStartDate(LocalDate endDate, String timeframe) {
        return switch (timeframe) {
            case "1H", "4H" -> endDate.minusMonths(6);   // Hourly: 6 months
            case "1D" -> endDate.minusYears(2);          // Daily: 2 years (current)
            case "1W" -> endDate.minusYears(5);          // Weekly: 5 years
            case "1M" -> endDate.minusYears(7);          // Monthly: 7 years
            default -> endDate.minusYears(2);            // Safe default
        };
    }

    /**
     * Convert frontend timeframe format to database format.
     *
     * Frontend/Python uses: 1H, 1D, 1W, 1M
     * Database stores: 1Hour, 1Day, 1Week, 1Month
     */
    private String convertToDbTimeframe(String timeframe) {
        return switch (timeframe) {
            case "1H" -> "1Hour";
            case "4H" -> "4Hour";
            case "1D" -> "1Day";
            case "1W" -> "1Week";
            case "1M" -> "1Month";
            default -> "1Day";  // Safe default
        };
    }

    /**
     * Convert MarketDataEntity to map for gRPC.
     */
    private Map<String, Object> convertToMap(MarketDataEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", entity.getTimestampAsLocalDateTime().toString());
        map.put("open", entity.getOpen().doubleValue());
        map.put("high", entity.getHigh().doubleValue());
        map.put("low", entity.getLow().doubleValue());
        map.put("close", entity.getClose().doubleValue());
        map.put("volume", entity.getVolume() != null ? entity.getVolume().longValue() : 0L);
        return map;
    }

    /**
     * Map gRPC POJO to REST DTO.
     *
     * Leave null/empty fields as-is - Jackson will automatically exclude them from JSON response.
     */
    private ExecuteStrategyResponse mapToRestDto(
            io.strategiz.client.execution.model.ExecutionResponse grpcResponse,
            String symbol,
            String timeframe) {

        ExecuteStrategyResponse dto = new ExecuteStrategyResponse();

        // Basic fields - always set
        dto.setSymbol(symbol);
        dto.setExecutionTime(grpcResponse.getExecutionTimeMs());

        // Optional fields - only set if present (Jackson excludes null/empty)
        if (grpcResponse.getLogs() != null && !grpcResponse.getLogs().isEmpty()) {
            dto.setLogs(grpcResponse.getLogs());
        }

        // Errors - only set if execution failed
        if (!grpcResponse.isSuccess() && grpcResponse.getError() != null) {
            dto.setErrors(List.of(grpcResponse.getError()));
        }

        // Signals - only set if present
        List<ExecuteStrategyResponse.Signal> signals = mapSignals(grpcResponse.getSignals());
        if (signals != null && !signals.isEmpty()) {
            dto.setSignals(signals);
        }

        // Indicators - only set if present
        List<ExecuteStrategyResponse.Indicator> indicators = mapIndicators(grpcResponse.getIndicators());
        if (indicators != null && !indicators.isEmpty()) {
            dto.setIndicators(indicators);
        }

        // Performance - only set if present
        ExecuteStrategyResponse.Performance performance = mapPerformance(grpcResponse.getPerformance(), timeframe);
        if (performance != null) {
            dto.setPerformance(performance);
        }

        return dto;
    }

    /**
     * Map signals from gRPC to REST DTO.
     * Returns null if input is null/empty (Jackson will exclude from response).
     */
    private List<ExecuteStrategyResponse.Signal> mapSignals(
            List<io.strategiz.client.execution.model.Signal> grpcSignals) {

        if (grpcSignals == null || grpcSignals.isEmpty()) {
            return null;
        }

        return grpcSignals.stream()
            .map(s -> {
                ExecuteStrategyResponse.Signal signal = new ExecuteStrategyResponse.Signal();
                signal.setTimestamp(s.getTimestamp());
                signal.setType(s.getType());
                signal.setPrice(s.getPrice());
                signal.setQuantity(s.getQuantity());
                signal.setReason(s.getReason());
                return signal;
            })
            .collect(Collectors.toList());
    }

    /**
     * Map indicators from gRPC to REST DTO.
     * Returns null if input is null/empty (Jackson will exclude from response).
     */
    private List<ExecuteStrategyResponse.Indicator> mapIndicators(
            List<io.strategiz.client.execution.model.Indicator> grpcIndicators) {

        if (grpcIndicators == null || grpcIndicators.isEmpty()) {
            return null;
        }

        return grpcIndicators.stream()
            .map(i -> {
                ExecuteStrategyResponse.Indicator indicator = new ExecuteStrategyResponse.Indicator();
                indicator.setName(i.getName());

                // Map data points - only set if present
                if (i.getData() != null && !i.getData().isEmpty()) {
                    List<ExecuteStrategyResponse.Indicator.DataPoint> dataPoints = i.getData().stream()
                        .map(dp -> {
                            ExecuteStrategyResponse.Indicator.DataPoint point =
                                new ExecuteStrategyResponse.Indicator.DataPoint();
                            point.setTime(dp.getTimestamp());
                            point.setValue(dp.getValue());
                            return point;
                        })
                        .collect(Collectors.toList());
                    indicator.setData(dataPoints);
                }

                return indicator;
            })
            .collect(Collectors.toList());
    }

    /**
     * Map performance from gRPC to REST DTO.
     * Returns null if input is null (no performance data).
     */
    private ExecuteStrategyResponse.Performance mapPerformance(
            io.strategiz.client.execution.model.Performance grpcPerf,
            String timeframe) {

        if (grpcPerf == null) {
            return null;
        }

        ExecuteStrategyResponse.Performance performance = new ExecuteStrategyResponse.Performance();
        performance.setTotalReturn(grpcPerf.getTotalReturn());
        performance.setTotalPnL(grpcPerf.getTotalPnl());
        performance.setWinRate(grpcPerf.getWinRate());
        performance.setTotalTrades(grpcPerf.getTotalTrades());
        performance.setProfitableTrades(grpcPerf.getProfitableTrades());
        performance.setBuyCount(grpcPerf.getBuyCount());
        performance.setSellCount(grpcPerf.getSellCount());
        performance.setAvgWin(grpcPerf.getAvgWin());
        performance.setAvgLoss(grpcPerf.getAvgLoss());
        performance.setProfitFactor(grpcPerf.getProfitFactor());
        performance.setMaxDrawdown(grpcPerf.getMaxDrawdown());
        performance.setSharpeRatio(grpcPerf.getSharpeRatio());
        performance.setLastTestedAt(grpcPerf.getLastTestedAt());

        // New fields: test period info
        performance.setStartDate(grpcPerf.getStartDate());
        performance.setEndDate(grpcPerf.getEndDate());
        performance.setTestPeriod(grpcPerf.getTestPeriod());
        performance.setTimeframe(timeframe);  // Set the timeframe from request

        // New fields: buy & hold comparison
        performance.setBuyAndHoldReturn(grpcPerf.getBuyAndHoldReturn());
        performance.setBuyAndHoldReturnPercent(grpcPerf.getBuyAndHoldReturnPercent());
        performance.setOutperformance(grpcPerf.getOutperformance());

        // Map trades
        performance.setTrades(mapTrades(grpcPerf.getTrades()));

        // Map equity curve
        performance.setEquityCurve(mapEquityCurve(grpcPerf.getEquityCurve()));

        return performance;
    }

    /**
     * Map trades from gRPC to REST DTO.
     * Returns null if input is null/empty (Jackson will exclude from response).
     */
    private List<ExecuteStrategyResponse.Trade> mapTrades(
            List<io.strategiz.client.execution.model.Trade> grpcTrades) {

        if (grpcTrades == null || grpcTrades.isEmpty()) {
            return null;
        }

        return grpcTrades.stream()
            .map(t -> {
                ExecuteStrategyResponse.Trade trade = new ExecuteStrategyResponse.Trade();
                trade.setBuyTimestamp(t.getBuyTimestamp());
                trade.setSellTimestamp(t.getSellTimestamp());
                trade.setBuyPrice(t.getBuyPrice());
                trade.setSellPrice(t.getSellPrice());
                trade.setPnl(t.getPnl());
                trade.setPnlPercent(t.getPnlPercent());
                trade.setWin(t.isWin());
                trade.setBuyReason(t.getBuyReason());
                trade.setSellReason(t.getSellReason());
                return trade;
            })
            .collect(Collectors.toList());
    }

    /**
     * Map equity curve from gRPC to REST DTO.
     * Returns null if input is null/empty (Jackson will exclude from response).
     */
    private List<ExecuteStrategyResponse.EquityPoint> mapEquityCurve(
            List<io.strategiz.client.execution.model.EquityPoint> grpcEquityCurve) {

        if (grpcEquityCurve == null || grpcEquityCurve.isEmpty()) {
            return null;
        }

        return grpcEquityCurve.stream()
            .map(ep -> {
                ExecuteStrategyResponse.EquityPoint point = new ExecuteStrategyResponse.EquityPoint();
                point.setTimestamp(ep.getTimestamp());
                point.setPortfolioValue(ep.getPortfolioValue());
                point.setType(ep.getType());
                return point;
            })
            .collect(Collectors.toList());
    }
}
