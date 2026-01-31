package io.strategiz.business.risk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrator that runs all registered {@link RiskSignalProvider} beans and aggregates
 * their scores into a single {@link RiskAssessmentResult}.
 *
 * <p>
 * Providers are auto-discovered via Spring component scanning. Adding a new risk check =
 * creating a new {@code @Component} that implements {@link RiskSignalProvider}.
 * </p>
 */
@Component
public class RiskAssessmentBusiness {

	private static final Logger log = LoggerFactory.getLogger(RiskAssessmentBusiness.class);

	private final List<RiskSignalProvider> providers;

	@Autowired
	public RiskAssessmentBusiness(List<RiskSignalProvider> providers) {
		this.providers = providers;
		log.info("RiskAssessmentBusiness initialized with {} signal providers", providers.size());
	}

	/**
	 * Assess the risk of an authentication request by running all signal providers.
	 * @param context the authentication context
	 * @return aggregated risk assessment result
	 */
	public RiskAssessmentResult assess(RiskContext context) {
		log.debug("Assessing risk for user: {}, device: {}", context.userId(), context.deviceId());

		List<RiskSignal> signals = new ArrayList<>();
		int totalScore = 0;

		for (RiskSignalProvider provider : providers) {
			try {
				RiskSignal signal = provider.evaluate(context);
				signals.add(signal);
				totalScore += signal.score();
				if (signal.score() > 0) {
					log.info("Risk signal from {}: +{} ({})", signal.name(), signal.score(), signal.reason());
				}
			}
			catch (Exception e) {
				log.error("Risk signal provider {} threw exception: {}", provider.name(), e.getMessage());
				// Don't let a single provider failure block authentication
			}
		}

		// Cap at 100
		totalScore = Math.min(100, totalScore);
		RiskLevel level = RiskLevel.fromScore(totalScore);

		log.info("Risk assessment for user {}: score={}, level={} ({} signals)", context.userId(), totalScore, level,
				signals.size());

		return new RiskAssessmentResult(totalScore, level, signals);
	}

}
