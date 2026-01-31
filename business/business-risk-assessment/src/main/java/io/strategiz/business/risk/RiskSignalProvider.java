package io.strategiz.business.risk;

/**
 * Pluggable interface for risk signal providers.
 *
 * <p>
 * Each implementation evaluates a specific risk dimension (geo-anomaly, login velocity,
 * time-of-day, etc.) and returns a {@link RiskSignal}.
 * </p>
 *
 * <p>
 * Implementations are discovered as Spring beans via {@code @Component}. To add a new
 * risk check, simply create a new class implementing this interface.
 * </p>
 */
public interface RiskSignalProvider {

	/**
	 * The name of this risk signal provider.
	 */
	String name();

	/**
	 * Evaluate risk for the given context.
	 * @param context the authentication context to evaluate
	 * @return a risk signal with score and explanation
	 */
	RiskSignal evaluate(RiskContext context);

}
