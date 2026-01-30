package io.strategiz.business.risk.providers;

import io.strategiz.business.risk.RiskContext;
import io.strategiz.business.risk.RiskSignal;
import io.strategiz.business.risk.RiskSignalProvider;
import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.DeviceIdentityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Detects rapid device rotation — many distinct devices used in a short period
 * is a sign of credential stuffing or account sharing.
 *
 * <p>Scoring: &gt;3 devices in 7 days = +15, &gt;2 = +10</p>
 */
@Component
public class DeviceRotationProvider implements RiskSignalProvider {

	private static final int MAX_SCORE = 15;

	@Autowired
	private DeviceIdentityRepository deviceRepository;

	@Override
	public String name() {
		return "device_rotation";
	}

	@Override
	public RiskSignal evaluate(RiskContext context) {
		List<DeviceIdentity> userDevices = deviceRepository.findByUserId(context.userId());

		// Count devices seen in last 7 days
		Instant cutoff = Instant.now().minus(Duration.ofDays(7));
		long recentDeviceCount = userDevices.stream()
			.filter(d -> d.getLastSeen() != null && d.getLastSeen().isAfter(cutoff))
			.count();

		if (recentDeviceCount > 3) {
			return new RiskSignal(name(), 15, MAX_SCORE,
					"High device rotation: " + recentDeviceCount + " devices in 7 days");
		}
		if (recentDeviceCount > 2) {
			return new RiskSignal(name(), 10, MAX_SCORE,
					"Moderate device rotation: " + recentDeviceCount + " devices in 7 days");
		}

		return RiskSignal.clean(name(), MAX_SCORE);
	}

}
