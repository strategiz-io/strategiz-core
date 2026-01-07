package io.strategiz.business.livestrategies.adapter;

import io.strategiz.business.livestrategies.model.Signal;

/**
 * Interface for routing signals to their destinations.
 * Implementations handle the specific logic for alerts (notifications)
 * and bots (trading).
 */
public interface SignalAdapter {

	/**
	 * Process and route a signal to its destination.
	 * @param signal the signal to process
	 * @return result of the signal processing
	 */
	SignalResult process(Signal signal);

	/**
	 * Check if this adapter can handle the given signal.
	 * @param signal the signal to check
	 * @return true if this adapter can handle the signal
	 */
	boolean canHandle(Signal signal);

	/**
	 * Result of signal processing
	 */
	record SignalResult(
			boolean success,
			String deploymentId,
			String channel,
			String message,
			Exception error
	) {
		public static SignalResult success(String deploymentId, String channel, String message) {
			return new SignalResult(true, deploymentId, channel, message, null);
		}

		public static SignalResult failure(String deploymentId, String channel, String message, Exception error) {
			return new SignalResult(false, deploymentId, channel, message, error);
		}

		public static SignalResult skipped(String deploymentId, String reason) {
			return new SignalResult(true, deploymentId, "SKIPPED", reason, null);
		}
	}

}
