package io.strategiz.client.execution;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.strategiz.client.execution.model.*;
import io.strategiz.framework.exception.ErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * gRPC client for Strategy Execution Service
 * <p>
 * Provides method to execute trading strategies in an isolated Python sandbox environment.
 */
public class ExecutionServiceClient {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionServiceClient.class);

	private final ManagedChannel channel;

	private final io.strategiz.execution.grpc.StrategyExecutionServiceGrpc.StrategyExecutionServiceBlockingStub blockingStub;

	private final int timeoutSeconds;

	/**
	 * Create execution service client
	 * @param host gRPC service host
	 * @param port gRPC service port
	 * @param useTls whether to use TLS
	 * @param timeoutSeconds request timeout in seconds
	 */
	public ExecutionServiceClient(String host, int port, boolean useTls, int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;

		ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);

		if (useTls) {
			channelBuilder.useTransportSecurity();
		}
		else {
			channelBuilder.usePlaintext();
		}

		this.channel = channelBuilder.build();
		this.blockingStub = io.strategiz.execution.grpc.StrategyExecutionServiceGrpc.newBlockingStub(channel);

		logger.info("Initialized ExecutionServiceClient: host={}, port={}, tls={}", host, port, useTls);
	}

	/**
	 * Execute a trading strategy
	 * @param request execution request with code, language, market data
	 * @return execution response with signals, indicators, performance
	 */
	public ExecutionResponse executeStrategy(ExecutionRequest request) {
		logger.debug("Executing strategy: language={}, userId={}, strategyId={}", request.getLanguage(),
				request.getUserId(), request.getStrategyId());

		try {
			// Convert to proto
			io.strategiz.execution.grpc.ExecuteStrategyRequest protoRequest = toProtoRequest(request);

			// Call gRPC service with timeout
			io.strategiz.execution.grpc.ExecuteStrategyResponse protoResponse = blockingStub
				.withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS)
				.executeStrategy(protoRequest);

			// Convert from proto
			ExecutionResponse response = fromProtoResponse(protoResponse);

			logger.debug("Strategy execution completed: success={}, executionTime={}ms", response.isSuccess(),
					response.getExecutionTimeMs());

			return response;
		}
		catch (StatusRuntimeException e) {
			logger.error("gRPC call failed: {}", e.getStatus(), e);
			throw new StrategizException(ExecutionErrorDetails.GRPC_CALL_FAILED,
					"Strategy execution failed: " + e.getStatus().getDescription());
		}
		catch (Exception e) {
			logger.error("Unexpected error during strategy execution", e);
			throw new StrategizException(ExecutionErrorDetails.EXECUTION_ERROR,
					"Unexpected error: " + e.getMessage());
		}
	}

	/**
	 * Validate strategy code without executing
	 * @param code strategy code
	 * @param language programming language
	 * @return validation result
	 */
	public ValidationResponse validateCode(String code, String language) {
		logger.debug("Validating code: language={}", language);

		try {
			io.strategiz.execution.grpc.ValidateCodeRequest protoRequest = io.strategiz.execution.grpc.ValidateCodeRequest
				.newBuilder()
				.setCode(code)
				.setLanguage(language)
				.build();

			io.strategiz.execution.grpc.ValidateCodeResponse protoResponse = blockingStub
				.withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS)
				.validateCode(protoRequest);

			return ValidationResponse.builder()
				.valid(protoResponse.getValid())
				.errors(protoResponse.getErrorsList())
				.warnings(protoResponse.getWarningsList())
				.suggestions(protoResponse.getSuggestionsList())
				.build();
		}
		catch (StatusRuntimeException e) {
			logger.error("Code validation failed: {}", e.getStatus(), e);
			throw new StrategizException(ExecutionErrorDetails.GRPC_CALL_FAILED,
					"Code validation failed: " + e.getStatus().getDescription());
		}
	}

	/**
	 * Check service health
	 * @return health response
	 */
	public HealthResponse checkHealth() {
		try {
			io.strategiz.execution.grpc.HealthRequest request = io.strategiz.execution.grpc.HealthRequest
				.newBuilder()
				.build();

			io.strategiz.execution.grpc.HealthResponse protoResponse = blockingStub
				.withDeadlineAfter(5, TimeUnit.SECONDS)
				.getHealth(request);

			return HealthResponse.builder()
				.status(protoResponse.getStatus())
				.supportedLanguages(protoResponse.getSupportedLanguagesList())
				.maxTimeoutSeconds(protoResponse.getMaxTimeoutSeconds())
				.maxMemoryMb(protoResponse.getMaxMemoryMb())
				.metadata(protoResponse.getMetadataMap())
				.build();
		}
		catch (StatusRuntimeException e) {
			logger.warn("Health check failed: {}", e.getStatus());
			return HealthResponse.builder().status("UNAVAILABLE").build();
		}
	}

	/**
	 * Shutdown the client gracefully
	 */
	@PreDestroy
	public void shutdown() {
		try {
			channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
			logger.info("ExecutionServiceClient shutdown complete");
		}
		catch (InterruptedException e) {
			logger.warn("Shutdown interrupted", e);
			Thread.currentThread().interrupt();
		}
	}

	// --- Conversion Methods ---

	private io.strategiz.execution.grpc.ExecuteStrategyRequest toProtoRequest(ExecutionRequest request) {
		io.strategiz.execution.grpc.ExecuteStrategyRequest.Builder builder = io.strategiz.execution.grpc.ExecuteStrategyRequest
			.newBuilder()
			.setCode(request.getCode())
			.setLanguage(request.getLanguage())
			.setUserId(request.getUserId())
			.setStrategyId(request.getStrategyId());

		if (request.getTimeoutSeconds() != null) {
			builder.setTimeoutSeconds(request.getTimeoutSeconds());
		}

		// Convert market data
		if (request.getMarketData() != null) {
			List<io.strategiz.execution.grpc.MarketDataBar> protoBars = request.getMarketData()
				.stream()
				.map(this::toProtoBar)
				.collect(Collectors.toList());
			builder.addAllMarketData(protoBars);
		}

		return builder.build();
	}

	private io.strategiz.execution.grpc.MarketDataBar toProtoBar(MarketDataBar bar) {
		return io.strategiz.execution.grpc.MarketDataBar.newBuilder()
			.setTimestamp(bar.getTimestamp())
			.setOpen(bar.getOpen())
			.setHigh(bar.getHigh())
			.setLow(bar.getLow())
			.setClose(bar.getClose())
			.setVolume(bar.getVolume())
			.build();
	}

	private ExecutionResponse fromProtoResponse(
			io.strategiz.execution.grpc.ExecuteStrategyResponse proto) {
		ExecutionResponse.ExecutionResponseBuilder builder = ExecutionResponse.builder()
			.success(proto.getSuccess())
			.executionTimeMs(proto.getExecutionTimeMs())
			.logs(proto.getLogsList());

		if (!proto.getSuccess()) {
			builder.error(proto.getError());
		}

		// Convert signals
		if (proto.getSignalsCount() > 0) {
			List<Signal> signals = proto.getSignalsList()
				.stream()
				.map(this::fromProtoSignal)
				.collect(Collectors.toList());
			builder.signals(signals);
		}

		// Convert indicators
		if (proto.getIndicatorsCount() > 0) {
			List<Indicator> indicators = proto.getIndicatorsList()
				.stream()
				.map(this::fromProtoIndicator)
				.collect(Collectors.toList());
			builder.indicators(indicators);
		}

		// Convert performance
		if (proto.hasPerformance()) {
			builder.performance(fromProtoPerformance(proto.getPerformance()));
		}

		return builder.build();
	}

	private Signal fromProtoSignal(io.strategiz.execution.grpc.Signal proto) {
		return Signal.builder()
			.timestamp(proto.getTimestamp())
			.type(proto.getType())
			.price(proto.getPrice())
			.quantity(proto.getQuantity())
			.reason(proto.getReason())
			.build();
	}

	private Indicator fromProtoIndicator(io.strategiz.execution.grpc.Indicator proto) {
		List<DataPoint> dataPoints = proto.getDataList()
			.stream()
			.map(dp -> DataPoint.builder().timestamp(dp.getTimestamp()).value(dp.getValue()).build())
			.collect(Collectors.toList());

		return Indicator.builder().name(proto.getName()).data(dataPoints).build();
	}

	private Performance fromProtoPerformance(io.strategiz.execution.grpc.Performance proto) {
		List<Trade> trades = proto.getTradesList()
			.stream()
			.map(t -> Trade.builder()
				.buyTimestamp(t.getBuyTimestamp())
				.sellTimestamp(t.getSellTimestamp())
				.buyPrice(t.getBuyPrice())
				.sellPrice(t.getSellPrice())
				.pnl(t.getPnl())
				.pnlPercent(t.getPnlPercent())
				.win(t.getWin())
				.buyReason(t.getBuyReason())
				.sellReason(t.getSellReason())
				.build())
			.collect(Collectors.toList());

		return Performance.builder()
			.totalReturn(proto.getTotalReturn())
			.totalPnl(proto.getTotalPnl())
			.winRate(proto.getWinRate())
			.totalTrades(proto.getTotalTrades())
			.profitableTrades(proto.getProfitableTrades())
			.buyCount(proto.getBuyCount())
			.sellCount(proto.getSellCount())
			.avgWin(proto.getAvgWin())
			.avgLoss(proto.getAvgLoss())
			.profitFactor(proto.getProfitFactor())
			.maxDrawdown(proto.getMaxDrawdown())
			.sharpeRatio(proto.getSharpeRatio())
			.lastTestedAt(proto.getLastTestedAt())
			.trades(trades)
			.build();
	}

	/**
	 * Error details for execution service
	 */
	public enum ExecutionErrorDetails implements ErrorDetails {

		GRPC_CALL_FAILED(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "grpc-call-failed"),
		EXECUTION_ERROR(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "execution-error"),
		VALIDATION_ERROR(org.springframework.http.HttpStatus.BAD_REQUEST, "validation-error");

		private final org.springframework.http.HttpStatus httpStatus;

		private final String propertyKey;

		ExecutionErrorDetails(org.springframework.http.HttpStatus httpStatus, String propertyKey) {
			this.httpStatus = httpStatus;
			this.propertyKey = propertyKey;
		}

		@Override
		public org.springframework.http.HttpStatus getHttpStatus() {
			return httpStatus;
		}

		@Override
		public String getPropertyKey() {
			return propertyKey;
		}

	}

}
