package io.strategiz.execution.service;

import com.google.protobuf.util.Timestamps;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.strategiz.execution.grpc.*;
import io.strategiz.service.labs.service.PythonStrategyExecutor;
import io.strategiz.service.labs.model.ExecuteStrategyResponse;
import io.strategiz.business.strategy.execution.service.BacktestCalculatorBusiness;
import io.strategiz.business.strategy.execution.model.BacktestPerformance;
import io.strategiz.business.strategy.execution.model.SignalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * gRPC Service Implementation for Strategy Execution
 *
 * Handles execution requests from main API and orchestrates:
 * - Python strategy execution (via subprocess)
 * - Java strategy execution (via ta4j)
 * - Backtest calculation
 * - Performance metrics
 */
@Service
public class StrategyExecutionServiceImpl extends StrategyExecutionServiceGrpc.StrategyExecutionServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(StrategyExecutionServiceImpl.class);

    private final PythonStrategyExecutor pythonExecutor;
    private final BacktestCalculatorBusiness backtestCalculator;

    @Autowired
    public StrategyExecutionServiceImpl(
            PythonStrategyExecutor pythonExecutor,
            BacktestCalculatorBusiness backtestCalculator) {
        this.pythonExecutor = pythonExecutor;
        this.backtestCalculator = backtestCalculator;
    }

    @Override
    public void executeStrategy(ExecuteStrategyRequest request,
                               StreamObserver<io.strategiz.execution.grpc.ExecuteStrategyResponse> responseObserver) {

        long startTime = System.currentTimeMillis();

        logger.info("Executing {} strategy for user={}, strategy={}",
            request.getLanguage(), request.getUserId(), request.getStrategyId());

        try {
            // Validate request
            validateRequest(request);

            // Convert market data from gRPC to JSON
            List<Map<String, Object>> marketDataList = convertMarketData(request.getMarketDataList());
            String marketDataJson = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(marketDataList);

            // Execute based on language
            io.strategiz.execution.grpc.ExecuteStrategyResponse grpcResponse;

            if ("python".equalsIgnoreCase(request.getLanguage())) {
                grpcResponse = executePythonStrategy(request, marketDataJson, marketDataList);
            } else if ("java".equalsIgnoreCase(request.getLanguage())) {
                // TODO: Implement Java strategy execution with ta4j
                responseObserver.onError(Status.UNIMPLEMENTED
                    .withDescription("Java strategy execution not yet implemented")
                    .asRuntimeException());
                return;
            } else {
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Unsupported language: " + request.getLanguage())
                    .asRuntimeException());
                return;
            }

            // Set execution time
            long executionTime = System.currentTimeMillis() - startTime;
            grpcResponse = grpcResponse.toBuilder()
                .setExecutionTimeMs((int) executionTime)
                .build();

            logger.info("Strategy execution completed in {}ms", executionTime);

            // Send response
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException());

        } catch (Exception e) {
            logger.error("Strategy execution failed", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Execution failed: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void validateCode(ValidateCodeRequest request,
                            StreamObserver<ValidateCodeResponse> responseObserver) {

        logger.info("Validating {} code", request.getLanguage());

        try {
            // Basic validation
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                ValidateCodeResponse response = ValidateCodeResponse.newBuilder()
                    .setValid(false)
                    .addErrors("Code is empty")
                    .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Language-specific validation
            ValidateCodeResponse response;

            if ("python".equalsIgnoreCase(request.getLanguage())) {
                response = validatePythonCode(request.getCode());
            } else {
                response = ValidateCodeResponse.newBuilder()
                    .setValid(false)
                    .addErrors("Unsupported language: " + request.getLanguage())
                    .build();
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Code validation failed", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Validation failed: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void getHealth(HealthRequest request,
                         StreamObserver<HealthResponse> responseObserver) {

        HealthResponse response = HealthResponse.newBuilder()
            .setStatus("SERVING")
            .addSupportedLanguages("python")
            .addSupportedLanguages("java")
            .setMaxTimeoutSeconds(30)
            .setMaxMemoryMb(1024)
            .putMetadata("version", "1.0.0")
            .putMetadata("build", Instant.now().toString())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // --- Private Helper Methods ---

    private void validateRequest(ExecuteStrategyRequest request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Code is required");
        }
        if (request.getLanguage() == null || request.getLanguage().trim().isEmpty()) {
            throw new IllegalArgumentException("Language is required");
        }
        if (request.getMarketDataList() == null || request.getMarketDataList().isEmpty()) {
            throw new IllegalArgumentException("Market data is required");
        }
    }

    private List<Map<String, Object>> convertMarketData(List<MarketDataBar> bars) {
        return bars.stream()
            .map(bar -> {
                Map<String, Object> map = new HashMap<>();
                map.put("timestamp", bar.getTimestamp());
                map.put("time", bar.getTimestamp());
                map.put("open", bar.getOpen());
                map.put("high", bar.getHigh());
                map.put("low", bar.getLow());
                map.put("close", bar.getClose());
                map.put("volume", bar.getVolume());
                return map;
            })
            .collect(Collectors.toList());
    }

    private io.strategiz.execution.grpc.ExecuteStrategyResponse executePythonStrategy(
            ExecuteStrategyRequest request,
            String marketDataJson,
            List<Map<String, Object>> marketDataList) throws Exception {

        // Execute Python code via subprocess
        ExecuteStrategyResponse pythonResponse = pythonExecutor.executePythonCode(
            request.getCode(),
            marketDataJson
        );

        // Convert to gRPC response
        io.strategiz.execution.grpc.ExecuteStrategyResponse.Builder responseBuilder =
            io.strategiz.execution.grpc.ExecuteStrategyResponse.newBuilder();

        // Check for errors
        if (pythonResponse.getErrors() != null && !pythonResponse.getErrors().isEmpty()) {
            return responseBuilder
                .setSuccess(false)
                .setError(String.join("; ", pythonResponse.getErrors()))
                .addAllLogs(pythonResponse.getLogs() != null ? pythonResponse.getLogs() : List.of())
                .build();
        }

        // Convert signals
        if (pythonResponse.getSignals() != null) {
            for (ExecuteStrategyResponse.Signal sig : pythonResponse.getSignals()) {
                Signal grpcSignal = Signal.newBuilder()
                    .setTimestamp(sig.getTimestamp())
                    .setType(sig.getType())
                    .setPrice(sig.getPrice())
                    .setQuantity(1)  // Default quantity
                    .setReason(sig.getText() != null ? sig.getText() : sig.getType())
                    .build();
                responseBuilder.addSignals(grpcSignal);
            }
        }

        // Convert indicators
        if (pythonResponse.getIndicators() != null) {
            for (ExecuteStrategyResponse.Indicator ind : pythonResponse.getIndicators()) {
                Indicator.Builder indicatorBuilder = Indicator.newBuilder()
                    .setName(ind.getName());

                if (ind.getData() != null) {
                    for (ExecuteStrategyResponse.Indicator.DataPoint dp : ind.getData()) {
                        DataPoint grpcDataPoint = DataPoint.newBuilder()
                            .setTimestamp(dp.getTime())
                            .setValue(dp.getValue())
                            .build();
                        indicatorBuilder.addData(grpcDataPoint);
                    }
                }

                responseBuilder.addIndicators(indicatorBuilder.build());
            }
        }

        // Calculate backtest performance if we have signals
        if (pythonResponse.getSignals() != null && !pythonResponse.getSignals().isEmpty()) {
            List<SignalData> signalDataList = pythonResponse.getSignals().stream()
                .map(s -> {
                    SignalData sd = new SignalData();
                    sd.setTimestamp(s.getTimestamp());
                    sd.setType(s.getType());
                    sd.setPrice(s.getPrice());
                    sd.setQuantity(1);
                    sd.setReason(s.getText());
                    return sd;
                })
                .collect(Collectors.toList());

            BacktestPerformance backtest = backtestCalculator.calculatePerformance(
                signalDataList,
                marketDataList
            );

            Performance grpcPerformance = convertBacktestPerformance(backtest);
            responseBuilder.setPerformance(grpcPerformance);
        }

        // Add logs
        if (pythonResponse.getLogs() != null) {
            responseBuilder.addAllLogs(pythonResponse.getLogs());
        }

        return responseBuilder
            .setSuccess(true)
            .build();
    }

    private Performance convertBacktestPerformance(BacktestPerformance backtest) {
        Performance.Builder perfBuilder = Performance.newBuilder()
            .setTotalReturn(backtest.getTotalReturn())
            .setTotalPnl(backtest.getTotalPnL())
            .setWinRate(backtest.getWinRate())
            .setTotalTrades(backtest.getTotalTrades())
            .setProfitableTrades(backtest.getProfitableTrades())
            .setBuyCount(backtest.getBuyCount())
            .setSellCount(backtest.getSellCount())
            .setAvgWin(backtest.getAvgWin())
            .setAvgLoss(backtest.getAvgLoss())
            .setProfitFactor(backtest.getProfitFactor())
            .setMaxDrawdown(backtest.getMaxDrawdown())
            .setSharpeRatio(backtest.getSharpeRatio())
            .setLastTestedAt(backtest.getLastTestedAt() != null ?
                backtest.getLastTestedAt() : Instant.now().toString());

        // Convert trades
        if (backtest.getTrades() != null) {
            for (io.strategiz.business.strategy.execution.model.BacktestPerformance.Trade bt : backtest.getTrades()) {
                Trade grpcTrade = Trade.newBuilder()
                    .setBuyTimestamp(bt.getBuyTimestamp())
                    .setSellTimestamp(bt.getSellTimestamp())
                    .setBuyPrice(bt.getBuyPrice())
                    .setSellPrice(bt.getSellPrice())
                    .setPnl(bt.getPnl())
                    .setPnlPercent(bt.getPnlPercent())
                    .setWin(bt.isWin())
                    .setBuyReason(bt.getBuyReason() != null ? bt.getBuyReason() : "")
                    .setSellReason(bt.getSellReason() != null ? bt.getSellReason() : "")
                    .build();
                perfBuilder.addTrades(grpcTrade);
            }
        }

        return perfBuilder.build();
    }

    private ValidateCodeResponse validatePythonCode(String code) {
        ValidateCodeResponse.Builder responseBuilder = ValidateCodeResponse.newBuilder();

        // Check for required function
        if (!code.contains("def strategy(")) {
            responseBuilder
                .setValid(false)
                .addErrors("Missing required function: def strategy(data)");
        } else {
            responseBuilder.setValid(true);
        }

        // Check for common issues
        if (!code.contains("SYMBOL")) {
            responseBuilder.addWarnings("Missing SYMBOL constant - should define symbol to trade");
        }

        if (code.contains("import os") || code.contains("import sys")) {
            responseBuilder.addWarnings("Importing os/sys may be restricted for security");
        }

        // Add suggestions
        if (!code.contains("pandas") && !code.contains("ta.")) {
            responseBuilder.addSuggestions("Consider using pandas-ta for technical indicators");
        }

        return responseBuilder.build();
    }
}
