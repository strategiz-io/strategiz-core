package io.strategiz.business.risk.providers;

import io.strategiz.business.risk.RiskContext;
import io.strategiz.business.risk.RiskSignal;
import io.strategiz.business.risk.RiskSignalProvider;
import io.strategiz.data.session.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Detects unusually high login frequency by counting recent sessions.
 *
 * <p>Scoring: &gt;5 sessions in recent history = +25, &gt;3 = +15</p>
 */
@Component
public class LoginVelocityProvider implements RiskSignalProvider {

	private static final int MAX_SCORE = 25;

	@Autowired
	private SessionRepository sessionRepository;

	@Override
	public String name() {
		return "login_velocity";
	}

	@Override
	public RiskSignal evaluate(RiskContext context) {
		// Count active sessions as proxy for login frequency
		long sessionCount = sessionRepository.countByUserIdAndRevokedFalse(context.userId());

		if (sessionCount > 5) {
			return new RiskSignal(name(), 25, MAX_SCORE,
					"High login velocity: " + sessionCount + " active sessions");
		}
		if (sessionCount > 3) {
			return new RiskSignal(name(), 15, MAX_SCORE,
					"Elevated login velocity: " + sessionCount + " active sessions");
		}

		return RiskSignal.clean(name(), MAX_SCORE);
	}

}
