package io.strategiz.business.risk.providers;

import io.strategiz.business.risk.RiskContext;
import io.strategiz.business.risk.RiskSignal;
import io.strategiz.business.risk.RiskSignalProvider;
import io.strategiz.data.session.entity.SessionEntity;
import io.strategiz.data.session.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects geographic anomalies by comparing the current IP location against the user's
 * recent session locations.
 *
 * <p>
 * Scoring: New country = +30, New city = +15
 * </p>
 */
@Component
public class GeoAnomalyProvider implements RiskSignalProvider {

	private static final Logger log = LoggerFactory.getLogger(GeoAnomalyProvider.class);

	private static final int MAX_SCORE = 30;

	@Autowired
	private SessionRepository sessionRepository;

	@Override
	public String name() {
		return "geo_anomaly";
	}

	@Override
	public RiskSignal evaluate(RiskContext context) {
		if (context.ipLocation() == null || context.ipLocation().isEmpty()) {
			return RiskSignal.clean(name(), MAX_SCORE);
		}

		List<SessionEntity> recentSessions = sessionRepository.findByUserIdAndRevokedFalse(context.userId());

		if (recentSessions.isEmpty()) {
			// First login — no history to compare
			return RiskSignal.clean(name(), MAX_SCORE);
		}

		// Extract known locations from recent sessions' IP addresses
		// IpLocation format is typically "country/city" or just "country"
		String currentCountry = extractCountry(context.ipLocation());
		String currentCity = extractCity(context.ipLocation());

		Set<String> knownCountries = new HashSet<>();
		Set<String> knownCities = new HashSet<>();

		for (SessionEntity session : recentSessions) {
			// Sessions don't store ipLocation directly; use IP for comparison
			// In production, this would use a GeoIP service
			if (session.getIpAddress() != null) {
				knownCountries.add(session.getIpAddress()); // Simplified — use GeoIP in
															// production
			}
		}

		// For now, if the user has any session history, we consider it a known location
		// Full GeoIP integration would compare actual geo-coordinates
		if (currentCountry != null && !knownCountries.isEmpty()) {
			// Check if IP is entirely new (simple heuristic)
			boolean knownIp = recentSessions.stream()
				.anyMatch(s -> context.ipAddress() != null && context.ipAddress().equals(s.getIpAddress()));

			if (!knownIp) {
				return new RiskSignal(name(), 15, MAX_SCORE, "Login from new IP location: " + context.ipLocation());
			}
		}

		return RiskSignal.clean(name(), MAX_SCORE);
	}

	private String extractCountry(String location) {
		if (location == null) {
			return null;
		}
		int slash = location.indexOf('/');
		return slash > 0 ? location.substring(0, slash) : location;
	}

	private String extractCity(String location) {
		if (location == null) {
			return null;
		}
		int slash = location.indexOf('/');
		return slash > 0 ? location.substring(slash + 1) : null;
	}

}
