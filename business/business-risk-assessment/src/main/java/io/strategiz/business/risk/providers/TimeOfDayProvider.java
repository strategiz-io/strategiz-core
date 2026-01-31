package io.strategiz.business.risk.providers;

import io.strategiz.business.risk.RiskContext;
import io.strategiz.business.risk.RiskSignal;
import io.strategiz.business.risk.RiskSignalProvider;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Flags logins outside typical business hours (2am-5am local time) as slightly higher
 * risk, since most legitimate logins happen during waking hours.
 *
 * <p>
 * Scoring: Login during 2am-5am = +15
 * </p>
 */
@Component
public class TimeOfDayProvider implements RiskSignalProvider {

	private static final int MAX_SCORE = 15;

	@Override
	public String name() {
		return "time_of_day";
	}

	@Override
	public RiskSignal evaluate(RiskContext context) {
		// Use device timezone if available, otherwise UTC
		ZoneId zone = ZoneId.of("UTC");
		if (context.deviceIdentity() != null && context.deviceIdentity().getTimezone() != null) {
			try {
				zone = ZoneId.of(context.deviceIdentity().getTimezone());
			}
			catch (Exception ignored) {
				// Fall back to UTC
			}
		}

		ZonedDateTime localTime = context.requestTime().atZone(zone);
		int hour = localTime.getHour();

		if (hour >= 2 && hour < 5) {
			return new RiskSignal(name(), 15, MAX_SCORE, "Login at unusual hour: " + hour + ":00 local time");
		}

		return RiskSignal.clean(name(), MAX_SCORE);
	}

}
