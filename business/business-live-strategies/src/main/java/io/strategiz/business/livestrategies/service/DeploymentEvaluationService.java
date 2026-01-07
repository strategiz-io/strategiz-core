package io.strategiz.business.livestrategies.service;

import io.strategiz.business.livestrategies.adapter.AlertSignalAdapter;
import io.strategiz.business.livestrategies.adapter.BotSignalAdapter;
import io.strategiz.business.livestrategies.adapter.SignalAdapter;
import io.strategiz.business.livestrategies.model.Signal;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.UpdateAlertDeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Core service for evaluating deployments and routing signals.
 * Orchestrates the signal processing flow:
 * 1. Receives signals from strategy execution
 * 2. Routes to appropriate adapter (alert or bot)
 * 3. Handles errors and updates deployment state
 */
@Service
public class DeploymentEvaluationService {

	private static final Logger log = LoggerFactory.getLogger(DeploymentEvaluationService.class);

	private final AlertSignalAdapter alertSignalAdapter;
	private final BotSignalAdapter botSignalAdapter;
	private final ReadAlertDeploymentRepository readAlertDeploymentRepository;
	private final UpdateAlertDeploymentRepository updateAlertDeploymentRepository;

	public DeploymentEvaluationService(
			AlertSignalAdapter alertSignalAdapter,
			BotSignalAdapter botSignalAdapter,
			ReadAlertDeploymentRepository readAlertDeploymentRepository,
			UpdateAlertDeploymentRepository updateAlertDeploymentRepository) {
		this.alertSignalAdapter = alertSignalAdapter;
		this.botSignalAdapter = botSignalAdapter;
		this.readAlertDeploymentRepository = readAlertDeploymentRepository;
		this.updateAlertDeploymentRepository = updateAlertDeploymentRepository;
		log.info("DeploymentEvaluationService initialized");
	}

	/**
	 * Process a list of signals from strategy execution.
	 * @param signals list of signals to process
	 * @return evaluation result with statistics
	 */
	public EvaluationResult processSignals(List<Signal> signals) {
		if (signals == null || signals.isEmpty()) {
			return new EvaluationResult(0, 0, 0, 0, List.of());
		}

		int totalSignals = signals.size();
		int alertsProcessed = 0;
		int botsProcessed = 0;
		int errorsCount = 0;
		List<SignalAdapter.SignalResult> results = new ArrayList<>();

		for (Signal signal : signals) {
			try {
				SignalAdapter.SignalResult result = processSignal(signal);
				results.add(result);

				if (result.success()) {
					if (signal.isAlertSignal()) {
						alertsProcessed++;
					} else if (signal.isBotSignal()) {
						botsProcessed++;
					}
				} else {
					errorsCount++;
				}

			} catch (Exception e) {
				log.error("Unexpected error processing signal {}: {}", signal, e.getMessage(), e);
				errorsCount++;
				results.add(SignalAdapter.SignalResult.failure(
						signal.getDeploymentId(), "UNKNOWN", e.getMessage(), e));
			}
		}

		log.info("Processed {} signals: {} alerts, {} bots, {} errors",
				totalSignals, alertsProcessed, botsProcessed, errorsCount);

		return new EvaluationResult(totalSignals, alertsProcessed, botsProcessed, errorsCount, results);
	}

	/**
	 * Process a single signal.
	 */
	private SignalAdapter.SignalResult processSignal(Signal signal) {
		// Route to appropriate adapter
		if (signal.isAlertSignal()) {
			return processAlertSignal(signal);
		} else if (signal.isBotSignal()) {
			return processBotSignal(signal);
		} else {
			return SignalAdapter.SignalResult.skipped(signal.getDeploymentId(),
					"Unknown deployment type: " + signal.getDeploymentType());
		}
	}

	/**
	 * Process an alert signal with circuit breaker.
	 */
	private SignalAdapter.SignalResult processAlertSignal(Signal signal) {
		String alertId = signal.getDeploymentId();

		try {
			// Load alert to check circuit breaker
			AlertDeployment alert = readAlertDeploymentRepository.findById(alertId)
					.orElse(null);

			if (alert != null && alert.shouldTripCircuitBreaker()) {
				log.warn("Circuit breaker OPEN for alert {}, skipping", alertId);
				return SignalAdapter.SignalResult.skipped(alertId, "Circuit breaker open");
			}

			// Process through adapter
			SignalAdapter.SignalResult result = alertSignalAdapter.process(signal);

			// Handle errors and update circuit breaker state
			if (!result.success() && alert != null) {
				incrementConsecutiveErrors(alert, signal);
			}

			return result;

		} catch (Exception e) {
			log.error("Error processing alert signal {}: {}", alertId, e.getMessage(), e);
			return SignalAdapter.SignalResult.failure(alertId, "ALERT", e.getMessage(), e);
		}
	}

	/**
	 * Process a bot signal with circuit breaker.
	 */
	private SignalAdapter.SignalResult processBotSignal(Signal signal) {
		// TODO: Add circuit breaker logic when BotDeployment exists
		return botSignalAdapter.process(signal);
	}

	/**
	 * Increment consecutive errors and potentially trip circuit breaker.
	 */
	private void incrementConsecutiveErrors(AlertDeployment alert, Signal signal) {
		String alertId = alert.getId();
		String userId = alert.getUserId(); // Use userId from deployment

		String errorMessage = signal != null ? "Signal processing failed" : "Unknown error";

		// Use repository method which handles circuit breaker logic
		updateAlertDeploymentRepository.incrementConsecutiveErrors(alertId, userId, errorMessage)
				.ifPresent(updatedAlert -> {
					if (updatedAlert.shouldTripCircuitBreaker()) {
						log.warn("Circuit breaker tripped for alert {}, pausing alert", alertId);
						// TODO: Notify user that alert has been paused
					}
				});
	}

	/**
	 * Result of batch signal evaluation
	 */
	public record EvaluationResult(
			int totalSignals,
			int alertsProcessed,
			int botsProcessed,
			int errorsCount,
			List<SignalAdapter.SignalResult> results
	) {
		public boolean hasErrors() {
			return errorsCount > 0;
		}

		public double successRate() {
			if (totalSignals == 0) return 1.0;
			return (double) (alertsProcessed + botsProcessed) / totalSignals;
		}
	}

}
