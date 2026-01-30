package io.strategiz.business.risk.providers;

import io.strategiz.business.risk.RiskContext;
import io.strategiz.business.risk.RiskSignal;
import io.strategiz.business.risk.RiskSignalProvider;
import io.strategiz.data.device.model.DeviceIdentity;
import org.springframework.stereotype.Component;

/**
 * Checks device trust indicators for VPN, proxy, Tor, and bot detection.
 * These indicators are collected during device fingerprinting.
 *
 * <p>Scoring: VPN/proxy/Tor/bot detected = up to +15</p>
 */
@Component
public class IpReputationProvider implements RiskSignalProvider {

	private static final int MAX_SCORE = 15;

	@Override
	public String name() {
		return "ip_reputation";
	}

	@Override
	public RiskSignal evaluate(RiskContext context) {
		DeviceIdentity device = context.deviceIdentity();
		if (device == null) {
			return RiskSignal.clean(name(), MAX_SCORE);
		}

		int score = 0;
		StringBuilder reasons = new StringBuilder();

		if (Boolean.TRUE.equals(device.getBotDetected())) {
			score += 15;
			reasons.append("Bot detected. ");
		}
		else {
			if (Boolean.TRUE.equals(device.getVpnDetected())) {
				score += 5;
				reasons.append("VPN detected. ");
			}
			if (Boolean.TRUE.equals(device.getProxyDetected())) {
				score += 5;
				reasons.append("Proxy detected. ");
			}
			if (Boolean.TRUE.equals(device.getTamperingDetected())) {
				score += 10;
				reasons.append("Tampering detected. ");
			}
		}

		score = Math.min(score, MAX_SCORE);

		if (score > 0) {
			return new RiskSignal(name(), score, MAX_SCORE, reasons.toString().trim());
		}

		return RiskSignal.clean(name(), MAX_SCORE);
	}

}
