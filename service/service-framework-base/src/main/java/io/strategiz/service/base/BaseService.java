package io.strategiz.service.base;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;
import io.strategiz.framework.exception.ErrorDetails;
import io.strategiz.service.base.exception.ServiceBaseErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Enhanced base service class for all services in the Strategiz application. Provides
 * common functionality including transaction management, caching, event publishing, and
 * resilience patterns.
 *
 * Services should: - Throw StrategizException for business errors (handled by
 * GlobalExceptionHandler) - Use provided utility methods for consistent error handling -
 * Override template methods for specific implementations - Use logging and metrics
 * tracking patterns - Implement getModuleName() to identify their module for error
 * handling
 */
public abstract class BaseService implements ApplicationEventPublisherAware {

	/**
	 * Get the module name for this service. Each service must implement this to identify
	 * its module.
	 * @return Module name from ModuleConstants
	 */
	protected abstract String getModuleName();

	protected final Logger log = LoggerFactory.getLogger(getClass());

	// Application event publisher for domain events
	private ApplicationEventPublisher eventPublisher;

	// Transaction template for programmatic transaction control
	private TransactionTemplate transactionTemplate;

	// Platform transaction manager (injected by Spring)
	private PlatformTransactionManager transactionManager;

	// Connection validation cache to avoid repeated checks
	private static final Map<String, Instant> connectionValidationCache = new ConcurrentHashMap<>();

	private static final Duration VALIDATION_CACHE_DURATION = Duration.ofMinutes(5);

	// Circuit breaker state management
	private static final Map<String, CircuitBreakerState> circuitBreakerStates = new ConcurrentHashMap<>();

	// Performance metrics
	private static final Map<String, AtomicLong> operationCounters = new ConcurrentHashMap<>();

	private static final Map<String, AtomicLong> operationDurations = new ConcurrentHashMap<>();

	@Value("${strategiz.environment:production}")
	protected String environment;

	@Value("${strategiz.api.validation.enabled:true}")
	protected boolean apiValidationEnabled;

	@Value("${strategiz.circuit-breaker.enabled:true}")
	protected boolean circuitBreakerEnabled;

	@Value("${strategiz.circuit-breaker.failure-threshold:5}")
	protected int circuitBreakerFailureThreshold;

	@Value("${strategiz.circuit-breaker.timeout:60000}")
	protected long circuitBreakerTimeoutMs;

	// Validation support
	private Validator validator;

	/**
	 * Default constructor.
	 */
	protected BaseService() {
		log.debug("Initializing service: {}", this.getClass().getSimpleName());
		initializeMetrics();
	}

	/**
	 * Initialize performance metrics for this service
	 */
	private void initializeMetrics() {
		String serviceName = getClass().getSimpleName();
		operationCounters.putIfAbsent(serviceName, new AtomicLong(0));
		operationDurations.putIfAbsent(serviceName, new AtomicLong(0));
	}

	/**
	 * Set the application event publisher
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Set the transaction manager (optional - for programmatic transactions)
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		if (transactionManager != null) {
			this.transactionTemplate = new TransactionTemplate(transactionManager);
		}
	}

	// === TRANSACTION MANAGEMENT ===
	// IMPORTANT: Transactions should ONLY be used in the SERVICE layer
	// - Controllers: NO transactions (HTTP boundary)
	// - Services: YES transactions (business operation boundary)
	// - Business modules: NO transactions (pure domain logic)
	// - Repositories: NO transactions (participate in service transaction)

	/**
	 * Execute with read-write transaction (default). Use for: Create, Update, Delete
	 * operations in services.
	 *
	 * Example: <pre>
	 * public Strategy createStrategy(String userId, CreateStrategyRequest request) {
	 *     return executeWithTransaction("create_strategy", () -> {
	 *         Strategy strategy = strategyRepository.save(new Strategy(request));
	 *         logBusinessEvent("strategy_created", userId);
	 *         return strategy;
	 *     });
	 * }
	 * </pre>
	 */
	@Transactional
	protected <T> T executeWithTransaction(String operation, Supplier<T> serviceOperation) {
		try {
			long start = System.currentTimeMillis();
			T result = serviceOperation.get();
			long duration = System.currentTimeMillis() - start;

			logPerformance(operation, duration, Map.of("type", "transaction"));
			return result;
		}
		catch (RuntimeException e) {
			log.error("[{}] Transaction failed for operation: {}", getModuleName(), operation, e);
			throw e;
		}
	}

	/**
	 * Execute with read-only transaction. Use for: Read operations, reports, queries.
	 * Performance benefit: Database can optimize for read-only access.
	 *
	 * Example: <pre>
	 * public PortfolioReport generateReport(String userId) {
	 *     return executeWithReadOnlyTransaction("generate_report", () -> {
	 *         Portfolio portfolio = portfolioRepository.findByUser(userId);
	 *         List<Holding> holdings = holdingsRepository.findByUser(userId);
	 *         return new PortfolioReport(portfolio, holdings);
	 *     });
	 * }
	 * </pre>
	 */
	@Transactional(readOnly = true)
	protected <T> T executeWithReadOnlyTransaction(String operation, Supplier<T> serviceOperation) {
		try {
			long start = System.currentTimeMillis();
			T result = serviceOperation.get();
			long duration = System.currentTimeMillis() - start;

			logPerformance(operation, duration, Map.of("type", "read-only-transaction"));
			return result;
		}
		catch (RuntimeException e) {
			log.error("[{}] Read-only transaction failed for operation: {}", getModuleName(), operation, e);
			throw e;
		}
	}

	/**
	 * Execute with transaction + retry on failure. Use for: Operations that may fail due
	 * to optimistic locking, deadlocks, temporary issues.
	 *
	 * Example: <pre>
	 * public Portfolio syncPortfolio(String userId, String providerId) {
	 *     return executeWithTransactionAndRetry("sync_portfolio", () -> {
	 *         ProviderData data = providerClient.fetchData(providerId);
	 *         return portfolioRepository.update(userId, data);
	 *     }, 3);
	 * }
	 * </pre>
	 */
	@Transactional
	protected <T> T executeWithTransactionAndRetry(String operation, Supplier<T> serviceOperation, int maxRetries) {
		return executeWithRetry(operation, serviceOperation, maxRetries);
	}

	/**
	 * Execute with REQUIRES_NEW propagation (always creates new transaction). Use for:
	 * Audit logging, notifications that must commit regardless of parent transaction.
	 *
	 * Example: <pre>
	 * public void processPayment(String userId, PaymentRequest request) {
	 *     executeWithTransaction("process_payment", () -> {
	 *         Payment payment = paymentRepository.save(request);
	 *
	 *         // Audit log ALWAYS commits (even if payment fails)
	 *         executeWithNewTransaction("audit_payment", () -> {
	 *             auditRepository.log("payment_attempt", userId, payment.getId());
	 *             return null;
	 *         });
	 *
	 *         stripeService.charge(payment);
	 *         return payment;
	 *     });
	 * }
	 * </pre>
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	protected <T> T executeWithNewTransaction(String operation, Supplier<T> serviceOperation) {
		try {
			long start = System.currentTimeMillis();
			T result = serviceOperation.get();
			long duration = System.currentTimeMillis() - start;

			logPerformance(operation, duration, Map.of("type", "new-transaction"));
			return result;
		}
		catch (RuntimeException e) {
			log.error("[{}] New transaction failed for operation: {}", getModuleName(), operation, e);
			throw e;
		}
	}

	/**
	 * Execute with SERIALIZABLE isolation (strictest consistency). Use for: Financial
	 * transactions, critical data consistency requirements.
	 *
	 * Example: <pre>
	 * public Transfer transferFunds(String fromUserId, String toUserId, BigDecimal amount) {
	 *     return executeWithSerializableTransaction("transfer_funds", () -> {
	 *         Account from = accountRepository.findById(fromUserId);
	 *         Account to = accountRepository.findById(toUserId);
	 *
	 *         from.withdraw(amount);
	 *         to.deposit(amount);
	 *
	 *         accountRepository.save(from);
	 *         accountRepository.save(to);
	 *
	 *         return new Transfer(from, to, amount);
	 *     });
	 * }
	 * </pre>
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE)
	protected <T> T executeWithSerializableTransaction(String operation, Supplier<T> serviceOperation) {
		try {
			long start = System.currentTimeMillis();
			T result = serviceOperation.get();
			long duration = System.currentTimeMillis() - start;

			logPerformance(operation, duration, Map.of("type", "serializable-transaction"));
			return result;
		}
		catch (RuntimeException e) {
			log.error("[{}] Serializable transaction failed for operation: {}", getModuleName(), operation, e);
			throw e;
		}
	}

	/**
	 * Execute multiple operations in same transaction (batch). Use for: Bulk operations,
	 * data migrations.
	 *
	 * Example: <pre>
	 * public void bulkUpdateStrategies(List<Strategy> strategies) {
	 *     executeInBatch("bulk_update_strategies",
	 *         strategies.stream()
	 *             .map(s -> (Runnable) () -> strategyRepository.update(s))
	 *             .collect(Collectors.toList())
	 *     );
	 * }
	 * </pre>
	 */
	@Transactional
	protected void executeInBatch(String operation, List<Runnable> operations) {
		try {
			long start = System.currentTimeMillis();

			for (int i = 0; i < operations.size(); i++) {
				operations.get(i).run();

				// Log progress for large batches
				if ((i + 1) % 100 == 0) {
					log.debug("[{}] Batch progress: {}/{}", getModuleName(), i + 1, operations.size());
				}
			}

			long duration = System.currentTimeMillis() - start;
			logPerformance(operation, duration,
					Map.of("batchSize", operations.size(), "avgTimePerOp", duration / operations.size()));
		}
		catch (Exception e) {
			log.error("[{}] Batch transaction failed for operation: {}", getModuleName(), operation, e);
			throw e;
		}
	}

	/**
	 * Execute with programmatic transaction control. Use for: Dynamic commit/rollback
	 * logic, conditional transactions.
	 *
	 * Example: <pre>
	 * public SubscriptionResult upgradeSubscription(String userId, String tierId) {
	 *     return executeWithProgrammaticTransaction("upgrade_subscription", (status) -> {
	 *         Subscription current = subscriptionRepository.findByUser(userId);
	 *
	 *         if (current.getTier().equals(tierId)) {
	 *             status.setRollbackOnly(); // No changes needed
	 *             return SubscriptionResult.noChange();
	 *         }
	 *
	 *         try {
	 *             paymentService.charge(userId, getPriceForTier(tierId));
	 *             current.setTier(tierId);
	 *             subscriptionRepository.update(current);
	 *             return SubscriptionResult.success(current);
	 *         } catch (PaymentException e) {
	 *             status.setRollbackOnly();
	 *             return SubscriptionResult.paymentFailed(e.getMessage());
	 *         }
	 *     });
	 * }
	 * </pre>
	 */
	protected <T> T executeWithProgrammaticTransaction(String operation, TransactionCallback<T> transactionCallback) {
		if (transactionTemplate == null) {
			log.warn("[{}] TransactionTemplate not configured, executing without transaction: {}", getModuleName(),
					operation);
			throwModuleException(ServiceBaseErrorDetails.CONFIGURATION_ERROR,
					"TransactionManager not configured. Add @Autowired setTransactionManager() or use declarative @Transactional");
		}

		try {
			long start = System.currentTimeMillis();
			T result = transactionTemplate.execute(transactionCallback);
			long duration = System.currentTimeMillis() - start;

			logPerformance(operation, duration, Map.of());
			return result;
		}
		catch (Exception e) {
			log.error("[{}] Programmatic transaction failed for operation: {}", getModuleName(), operation, e);
			throw e;
		}
	}

	// === STRUCTURED LOGGING HELPERS ===
	// These methods provide specialized logging using the StructuredLogger from
	// framework-logging

	/**
	 * Log performance metrics for an operation.
	 * @param operation The operation name
	 * @param durationMs Duration in milliseconds
	 * @param metrics Additional metrics to log
	 */
	protected void logPerformance(String operation, long durationMs, Map<String, Object> metrics) {
		io.strategiz.framework.logging.StructuredLogger.performance()
			.operation(operation)
			.component(getModuleName())
			.duration(durationMs)
			.fields(metrics)
			.log("Performance metric: " + operation);
	}

	/**
	 * Log security-related events (auth, authz, access control).
	 * @param event The security event type
	 * @param userId The user ID involved
	 * @param eventData Additional event data
	 */
	protected void logSecurityEvent(String event, String userId, Map<String, Object> eventData) {
		io.strategiz.framework.logging.StructuredLogger.security()
			.operation(event)
			.component(getModuleName())
			.userId(userId)
			.fields(eventData)
			.log("Security event: " + event);
	}

	/**
	 * Log business events (strategy created, subscription upgraded, etc.).
	 * @param operation The business operation
	 * @param userId The user ID involved
	 * @param eventData Additional event data
	 */
	protected void logBusinessEvent(String operation, String userId, Map<String, Object> eventData) {
		io.strategiz.framework.logging.StructuredLogger.business()
			.operation(operation)
			.component(getModuleName())
			.userId(userId)
			.fields(eventData)
			.log("Business event: " + operation);
	}

	/**
	 * Log audit trail events for compliance and debugging.
	 * @param operation The operation being audited
	 * @param userId The user ID involved
	 * @param auditData Audit trail data
	 */
	protected void logAuditEvent(String operation, String userId, Map<String, Object> auditData) {
		io.strategiz.framework.logging.StructuredLogger.audit()
			.operation(operation)
			.component(getModuleName())
			.userId(userId)
			.fields(auditData)
			.log("Audit event: " + operation);
	}

	/**
	 * Log external API calls (Alpaca, CoinGecko, etc.).
	 * @param apiName The API name
	 * @param endpoint The endpoint called
	 * @param durationMs Duration in milliseconds
	 * @param statusCode HTTP status code
	 * @param additionalData Additional data to log
	 */
	protected void logExternalApiCall(String apiName, String endpoint, long durationMs, int statusCode,
			Map<String, Object> additionalData) {
		Map<String, Object> data = new java.util.HashMap<>(additionalData);
		data.put("api", apiName);
		data.put("endpoint", endpoint);
		data.put("statusCode", statusCode);

		logPerformance("external_api_call", durationMs, data);
	}

	/**
	 * Log database query operations.
	 * @param collection The collection/table name
	 * @param operation The operation type (read, write, update, delete)
	 * @param durationMs Duration in milliseconds
	 * @param queryData Additional query data
	 */
	protected void logDatabaseQuery(String collection, String operation, long durationMs,
			Map<String, Object> queryData) {
		Map<String, Object> data = new java.util.HashMap<>(queryData);
		data.put("collection", collection);
		data.put("dbOperation", operation);

		logPerformance("database_query", durationMs, data);
	}

	/**
	 * Log cache operations (hit, miss, eviction).
	 * @param cacheName The cache name
	 * @param operation The cache operation (hit, miss, evict)
	 * @param key The cache key
	 */
	protected void logCacheOperation(String cacheName, String operation, String key) {
		log.debug("[{}] Cache {} for key {} in cache {}", getModuleName(), operation, key, cacheName);
	}

	/**
	 * Log async operation tracking.
	 * @param operation The async operation name
	 * @param status The status (started, completed, failed)
	 * @param metadata Additional metadata
	 */
	protected void logAsyncOperation(String operation, String status, Map<String, Object> metadata) {
		Map<String, Object> data = new java.util.HashMap<>(metadata);
		data.put("asyncStatus", status);

		io.strategiz.framework.logging.StructuredLogger.business()
			.operation(operation)
			.component(getModuleName())
			.fields(data)
			.log("Async operation: " + operation + " - " + status);
	}

	/**
	 * Execute operation with circuit breaker pattern
	 */
	protected <T> T executeWithCircuitBreaker(String operation, Supplier<T> serviceOperation, Supplier<T> fallback) {
		if (!circuitBreakerEnabled) {
			return serviceOperation.get();
		}

		String serviceName = getClass().getSimpleName();
		String key = serviceName + ":" + operation;

		CircuitBreakerState state = circuitBreakerStates.computeIfAbsent(key,
				k -> new CircuitBreakerState(circuitBreakerFailureThreshold, circuitBreakerTimeoutMs));

		// Check if circuit is open
		if (state.isOpen()) {
			log.warn("Circuit breaker is OPEN for {}, executing fallback", key);
			return fallback.get();
		}

		try {
			T result = serviceOperation.get();
			state.recordSuccess();
			return result;
		}
		catch (Exception e) {
			state.recordFailure();
			log.error("Circuit breaker recorded failure for {}, state: {}", key, state.getState(), e);

			if (state.isOpen()) {
				log.warn("Circuit breaker opened for {}, executing fallback", key);
				return fallback.get();
			}

			throw e;
		}
	}

	/**
	 * Execute operation with retry logic
	 */
	protected <T> T executeWithRetry(String operation, Supplier<T> serviceOperation, int maxRetries) {
		int attempts = 0;
		Exception lastException = null;

		while (attempts < maxRetries) {
			try {
				return serviceOperation.get();
			}
			catch (Exception e) {
				lastException = e;
				attempts++;

				if (attempts < maxRetries) {
					long delay = calculateRetryDelay(attempts);
					log.warn("Retry attempt {} for operation {}, retrying in {}ms", attempts, operation, delay);

					try {
						Thread.sleep(delay);
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new StrategizException(ServiceBaseErrorDetails.RETRY_INTERRUPTED,
								"service-framework-base", ie, operation);
					}
				}
			}
		}

		throw new StrategizException(ServiceBaseErrorDetails.RETRY_EXHAUSTED, "service-framework-base", lastException,
				operation, maxRetries);
	}

	/**
	 * Calculate exponential backoff delay
	 */
	private long calculateRetryDelay(int attempt) {
		return Math.min(1000 * (1L << attempt), 30000); // Cap at 30 seconds
	}

	/**
	 * Cache operation result
	 */
	@Cacheable(value = "serviceCache", key = "#operation + ':' + #parameters")
	protected <T> T executeWithCaching(String operation, String parameters, Supplier<T> serviceOperation) {
		try {
			return executeWithLogging(operation, "cached:" + parameters, serviceOperation::get);
		}
		catch (Exception e) {
			throw new StrategizException(ServiceBaseErrorDetails.CACHED_OPERATION_FAILED, "service-framework-base", e,
					operation);
		}
	}

	/**
	 * Invalidate cache for operation
	 */
	@CacheEvict(value = "serviceCache", key = "#operation + ':' + #parameters")
	protected void invalidateCache(String operation, String parameters) {
		log.debug("Cache invalidated for operation: {}, parameters: {}", operation, parameters);
	}

	/**
	 * Validate input data using JSR-303 validation
	 * @throws StrategizException if validation fails
	 */
	protected <T> void validateInput(T input, String operation) {
		if (validator == null) {
			log.debug("Validator not configured, skipping validation for {}", operation);
			return;
		}

		Set<ConstraintViolation<T>> violations = validator.validate(input);
		if (!violations.isEmpty()) {
			StringBuilder message = new StringBuilder("Validation failed for " + operation + ": ");
			for (ConstraintViolation<T> violation : violations) {
				message.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("; ");
			}
			throw new StrategizException(ErrorCode.VALIDATION_ERROR, message.toString());
		}
	}

	/**
	 * Validate required parameter
	 * @throws StrategizException if parameter is null or empty
	 */
	protected void validateRequired(String paramName, Object paramValue) {
		if (paramValue == null) {
			throw new StrategizException(ErrorCode.VALIDATION_ERROR,
					"Required parameter '" + paramName + "' is missing");
		}

		if (paramValue instanceof String && ((String) paramValue).trim().isEmpty()) {
			throw new StrategizException(ErrorCode.VALIDATION_ERROR,
					"Required parameter '" + paramName + "' cannot be empty");
		}
	}

	/**
	 * Validate business rule
	 * @throws StrategizException if condition is false
	 */
	protected void validateBusinessRule(boolean condition, Enum<?> errorCode, String errorMessage) {
		if (!condition) {
			throw new StrategizException(errorCode, errorMessage);
		}
	}

	/**
	 * Validate business rule with default error code
	 * @throws StrategizException if condition is false
	 */
	protected void validateBusinessRule(boolean condition, String errorMessage) {
		if (!condition) {
			throw new StrategizException(ErrorCode.BUSINESS_RULE_VIOLATION, errorMessage);
		}
	}

	/**
	 * Ensure service dependency is available
	 * @throws StrategizException if dependency is not available
	 */
	protected void ensureDependencyAvailable(Object dependency, String dependencyName) {
		if (dependency == null) {
			throw new StrategizException(ErrorCode.SERVICE_UNAVAILABLE,
					"Required dependency '" + dependencyName + "' is not available");
		}
	}

	/**
	 * Validate entity exists
	 * @throws StrategizException if entity is null
	 */
	protected <T> T validateEntityExists(T entity, String entityType, String entityId) {
		if (entity == null) {
			throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
					entityType + " with ID '" + entityId + "' not found");
		}
		return entity;
	}

	/**
	 * Publish domain event
	 */
	protected void publishEvent(Object event) {
		if (eventPublisher != null) {
			eventPublisher.publishEvent(event);
			log.debug("Published event: {}", event.getClass().getSimpleName());
		}
		else {
			log.warn("EventPublisher not configured, event not published: {}", event.getClass().getSimpleName());
		}
	}

	/**
	 * Log business operation for audit trail
	 */
	protected void logBusinessOperation(String userId, String operation, String entityType, String entityId,
			String details) {
		log.info("[{}] BUSINESS_OP - User: {}, Operation: {}, Entity: {}:{}, Details: {}", getModuleName(), userId,
				operation, entityType, entityId, details);
	}

	/**
	 * Record performance metrics
	 */
	protected void recordMetrics(String operation, long durationMs) {
		String serviceName = getClass().getSimpleName();
		operationCounters.get(serviceName).incrementAndGet();
		operationDurations.get(serviceName).addAndGet(durationMs);

		// Add to MDC for structured logging
		MDC.put("operation", operation);
		MDC.put("duration", String.valueOf(durationMs));
		MDC.put("service", serviceName);
	}

	/**
	 * Get service performance metrics
	 */
	protected Map<String, Object> getServiceMetrics() {
		String serviceName = getClass().getSimpleName();
		long count = operationCounters.getOrDefault(serviceName, new AtomicLong(0)).get();
		long totalDuration = operationDurations.getOrDefault(serviceName, new AtomicLong(0)).get();

		return Map.of("service", serviceName, "operationCount", count, "totalDurationMs", totalDuration,
				"averageDurationMs", count > 0 ? totalDuration / count : 0);
	}

	/**
	 * Enhanced execute with logging that includes metrics
	 */
	protected <T> T executeWithLogging(String operation, String context, ServiceOperation<T> serviceOperation)
			throws Exception {
		logServiceOperation(operation, context);
		Instant start = Instant.now();

		try {
			T result = serviceOperation.execute();
			long durationMs = Duration.between(start, Instant.now()).toMillis();

			logServiceOperationSuccess(operation, context, result);
			logServiceOperationTiming(operation, context, durationMs);
			recordMetrics(operation, durationMs);

			return result;
		}
		catch (Exception e) {
			long durationMs = Duration.between(start, Instant.now()).toMillis();
			logServiceOperationFailure(operation, context, e);
			logServiceOperationTiming(operation, context, durationMs);
			recordMetrics(operation, durationMs);
			throw e;
		}
		finally {
			// Clean up MDC
			MDC.remove("operation");
			MDC.remove("duration");
			MDC.remove("service");
		}
	}

	/**
	 * Validate that the real API connection is available before making requests.
	 * Strategiz ONLY uses real API data - never mock data or simulated responses.
	 * @param serviceName The name of the service making the request
	 * @return true if the connection is available, false otherwise
	 */
	protected boolean validateRealApiConnection(String serviceName) {
		if (!apiValidationEnabled) {
			log.debug("API validation disabled for service: {}", serviceName);
			return true;
		}

		// Check cache first
		Instant lastValidation = connectionValidationCache.get(serviceName);
		if (lastValidation != null
				&& Duration.between(lastValidation, Instant.now()).compareTo(VALIDATION_CACHE_DURATION) < 0) {
			log.debug("Using cached validation for service: {}", serviceName);
			return true;
		}

		log.info("Validating real API connection for: {}", serviceName);

		// Implement actual validation logic in subclasses
		// For base implementation, we assume connection is valid
		boolean isValid = performApiConnectionValidation(serviceName);

		if (isValid) {
			connectionValidationCache.put(serviceName, Instant.now());
			log.debug("API connection validated for service: {}", serviceName);
		}
		else {
			log.warn("API connection validation failed for service: {}", serviceName);
		}

		return isValid;
	}

	/**
	 * Perform actual API connection validation. Subclasses should override this method to
	 * implement specific validation logic.
	 * @param serviceName The name of the service making the request
	 * @return true if the connection is valid, false otherwise
	 */
	protected boolean performApiConnectionValidation(String serviceName) {
		// Default implementation - subclasses should override
		return true;
	}

	/**
	 * Ensures we're working with real API data, not mocks or simulations. This is a core
	 * principle of the Strategiz platform.
	 * @param dataSource The name of the data source being accessed
	 * @throws IllegalStateException if mock data would be returned
	 */
	protected void ensureRealApiData(String dataSource) {
		log.debug("Ensuring real API data from: {}", dataSource);

		// Check if we're in a test environment where mocks might be acceptable
		if ("test".equalsIgnoreCase(environment)) {
			log.debug("Test environment detected - allowing potential mock data for: {}", dataSource);
			return;
		}

		// Any implementation that would return mock data should throw an exception
		// instead of returning simulated data
		validateNoMockData(dataSource);
	}

	/**
	 * Validate that no mock data is being used. Subclasses can override this method to
	 * implement specific mock detection.
	 * @param dataSource The name of the data source being accessed
	 * @throws IllegalStateException if mock data would be returned
	 */
	protected void validateNoMockData(String dataSource) {
		// Default implementation - subclasses can override
		// This is a placeholder for more sophisticated mock detection
		log.debug("No mock data validation completed for: {}", dataSource);
	}

	/**
	 * Log service operation start
	 */
	protected void logServiceOperation(String operation, String context) {
		log.info("[{}] SERVICE_OP - Operation: {}, Context: {}", getModuleName(), operation, context);
	}

	/**
	 * Log service operation success
	 */
	protected void logServiceOperationSuccess(String operation, String context, Object result) {
		log.info("[{}] SERVICE_SUCCESS - Operation: {}, Context: {}, Result: {}", getModuleName(), operation, context,
				result != null ? result.getClass().getSimpleName() : "null");
	}

	/**
	 * Log service operation failure
	 */
	protected void logServiceOperationFailure(String operation, String context, Exception error) {
		log.error("[{}] SERVICE_FAILURE - Operation: {}, Context: {}, Error: {}", getModuleName(), operation, context,
				error.getMessage(), error);
	}

	/**
	 * Log service operation with timing
	 */
	protected void logServiceOperationTiming(String operation, String context, long durationMs) {
		log.info("[{}] SERVICE_TIMING - Operation: {}, Context: {}, Duration: {}ms", getModuleName(), operation,
				context, durationMs);
	}

	/**
	 * Clear the connection validation cache Useful for testing or when connection state
	 * changes
	 */
	protected void clearConnectionValidationCache() {
		connectionValidationCache.clear();
		log.debug("Connection validation cache cleared");
	}

	/**
	 * Get the current environment
	 */
	protected String getEnvironment() {
		return environment;
	}

	/**
	 * Check if running in production environment
	 */
	protected boolean isProductionEnvironment() {
		return "production".equalsIgnoreCase(environment);
	}

	/**
	 * Check if running in test environment
	 */
	protected boolean isTestEnvironment() {
		return "test".equalsIgnoreCase(environment);
	}

	// === MODULE-AWARE EXCEPTION HANDLING ===

	/**
	 * Throw a StrategizException with module context automatically included.
	 * @param errorDetails The error details enum
	 * @param args Arguments for error message formatting
	 * @throws StrategizException Always throws
	 */
	protected void throwModuleException(ErrorDetails errorDetails, Object... args) {
		throw new StrategizException(errorDetails, getModuleName(), args);
	}

	/**
	 * Throw a StrategizException with module context and cause.
	 * @param errorDetails The error details enum
	 * @param cause The underlying cause
	 * @param args Arguments for error message formatting
	 * @throws StrategizException Always throws
	 */
	protected void throwModuleException(ErrorDetails errorDetails, Throwable cause, Object... args) {
		throw new StrategizException(errorDetails, getModuleName(), cause, args);
	}

	/**
	 * Validate a business rule and throw module-aware exception if validation fails.
	 * @param condition The condition to validate
	 * @param errorDetails The error to throw if condition is false
	 * @param args Arguments for error message formatting
	 * @throws StrategizException if condition is false
	 */
	protected void validateModuleRule(boolean condition, ErrorDetails errorDetails, Object... args) {
		if (!condition) {
			throwModuleException(errorDetails, args);
		}
	}

	/**
	 * Functional interface for service operations
	 */
	@FunctionalInterface
	protected interface ServiceOperation<T> {

		T execute() throws Exception;

	}

	/**
	 * Circuit breaker state management
	 */
	private static class CircuitBreakerState {

		private final int failureThreshold;

		private final long timeoutMs;

		private final AtomicInteger failureCount = new AtomicInteger(0);

		private volatile long lastFailureTime = 0;

		private volatile boolean isOpen = false;

		public CircuitBreakerState(int failureThreshold, long timeoutMs) {
			this.failureThreshold = failureThreshold;
			this.timeoutMs = timeoutMs;
		}

		public boolean isOpen() {
			if (isOpen && System.currentTimeMillis() - lastFailureTime > timeoutMs) {
				isOpen = false;
				failureCount.set(0);
			}
			return isOpen;
		}

		public void recordSuccess() {
			failureCount.set(0);
			isOpen = false;
		}

		public void recordFailure() {
			lastFailureTime = System.currentTimeMillis();
			if (failureCount.incrementAndGet() >= failureThreshold) {
				isOpen = true;
			}
		}

		public String getState() {
			return isOpen ? "OPEN" : "CLOSED";
		}

	}

}
