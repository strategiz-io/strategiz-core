package io.strategiz.service.console.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom business metrics for Strategiz platform observability.
 *
 * <p>Provides metrics for:
 * <ul>
 *   <li>Authentication events (logins, MFA, OAuth)</li>
 *   <li>Provider operations (sync, refresh, errors)</li>
 *   <li>Portfolio calculations</li>
 *   <li>Strategy executions</li>
 *   <li>Market data collection</li>
 * </ul>
 *
 * <p>These metrics are exported to Prometheus and visualized in Grafana dashboards.
 */
@Component
public class StrategizMetrics {

	private final MeterRegistry registry;

	private final ConcurrentHashMap<String, AtomicLong> gaugeValues = new ConcurrentHashMap<>();

	// Authentication Counters
	private final Counter loginSuccessCounter;

	private final Counter loginFailureCounter;

	private final Counter mfaVerificationCounter;

	private final Counter oauthCallbackCounter;

	private final Counter sessionCreatedCounter;

	private final Counter sessionExpiredCounter;

	// Provider Counters
	private final Counter providerSyncSuccessCounter;

	private final Counter providerSyncFailureCounter;

	private final Counter providerTokenRefreshCounter;

	// Timers
	private final Timer authenticationTimer;

	private final Timer providerSyncTimer;

	private final Timer portfolioCalculationTimer;

	public StrategizMetrics(MeterRegistry registry) {
		this.registry = registry;

		// Authentication metrics
		this.loginSuccessCounter = Counter.builder("strategiz.auth.login.success")
			.description("Number of successful logins")
			.register(registry);

		this.loginFailureCounter = Counter.builder("strategiz.auth.login.failure")
			.description("Number of failed login attempts")
			.register(registry);

		this.mfaVerificationCounter = Counter.builder("strategiz.auth.mfa.verification")
			.description("Number of MFA verifications")
			.tag("type", "all")
			.register(registry);

		this.oauthCallbackCounter = Counter.builder("strategiz.auth.oauth.callback")
			.description("Number of OAuth callbacks processed")
			.tag("provider", "all")
			.register(registry);

		this.sessionCreatedCounter = Counter.builder("strategiz.session.created")
			.description("Number of sessions created")
			.register(registry);

		this.sessionExpiredCounter = Counter.builder("strategiz.session.expired")
			.description("Number of sessions expired")
			.register(registry);

		// Provider metrics
		this.providerSyncSuccessCounter = Counter.builder("strategiz.provider.sync.success")
			.description("Number of successful provider syncs")
			.tag("provider", "all")
			.register(registry);

		this.providerSyncFailureCounter = Counter.builder("strategiz.provider.sync.failure")
			.description("Number of failed provider syncs")
			.tag("provider", "all")
			.register(registry);

		this.providerTokenRefreshCounter = Counter.builder("strategiz.provider.token.refresh")
			.description("Number of provider token refreshes")
			.tag("provider", "all")
			.register(registry);

		// Timers
		this.authenticationTimer = Timer.builder("strategiz.auth.duration")
			.description("Authentication operation duration")
			.register(registry);

		this.providerSyncTimer = Timer.builder("strategiz.provider.sync.duration")
			.description("Provider sync operation duration")
			.register(registry);

		this.portfolioCalculationTimer = Timer.builder("strategiz.portfolio.calculation.duration")
			.description("Portfolio calculation duration")
			.register(registry);

		// Register gauges for active counts
		registerGauge("strategiz.sessions.active", "Number of active sessions");
		registerGauge("strategiz.providers.connected", "Number of connected providers");
		registerGauge("strategiz.strategies.active", "Number of active strategies");
	}

	private void registerGauge(String name, String description) {
		AtomicLong value = new AtomicLong(0);
		gaugeValues.put(name, value);
		Gauge.builder(name, value, AtomicLong::get).description(description).register(registry);
	}

	// Authentication recording methods
	public void recordLoginSuccess() {
		loginSuccessCounter.increment();
	}

	public void recordLoginFailure() {
		loginFailureCounter.increment();
	}

	public void recordMfaVerification(String type) {
		mfaVerificationCounter.increment();
		Counter.builder("strategiz.auth.mfa.verification")
			.description("MFA verification by type")
			.tag("type", type)
			.register(registry)
			.increment();
	}

	public void recordOAuthCallback(String provider, boolean success) {
		oauthCallbackCounter.increment();
		Counter.builder("strategiz.auth.oauth.callback")
			.description("OAuth callback by provider")
			.tag("provider", provider)
			.tag("status", success ? "success" : "failure")
			.register(registry)
			.increment();
	}

	public void recordSessionCreated() {
		sessionCreatedCounter.increment();
		gaugeValues.get("strategiz.sessions.active").incrementAndGet();
	}

	public void recordSessionExpired() {
		sessionExpiredCounter.increment();
		gaugeValues.get("strategiz.sessions.active").decrementAndGet();
	}

	// Provider recording methods
	public void recordProviderSync(String provider, boolean success, long durationMs) {
		if (success) {
			providerSyncSuccessCounter.increment();
			Counter.builder("strategiz.provider.sync.success")
				.tag("provider", provider)
				.register(registry)
				.increment();
		}
		else {
			providerSyncFailureCounter.increment();
			Counter.builder("strategiz.provider.sync.failure")
				.tag("provider", provider)
				.register(registry)
				.increment();
		}
		providerSyncTimer.record(durationMs, TimeUnit.MILLISECONDS);
	}

	public void recordProviderTokenRefresh(String provider) {
		providerTokenRefreshCounter.increment();
		Counter.builder("strategiz.provider.token.refresh")
			.tag("provider", provider)
			.register(registry)
			.increment();
	}

	// Timer methods for measuring durations
	public Timer.Sample startAuthTimer() {
		return Timer.start(registry);
	}

	public void stopAuthTimer(Timer.Sample sample) {
		sample.stop(authenticationTimer);
	}

	public Timer.Sample startProviderSyncTimer() {
		return Timer.start(registry);
	}

	public void stopProviderSyncTimer(Timer.Sample sample) {
		sample.stop(providerSyncTimer);
	}

	public Timer.Sample startPortfolioCalculationTimer() {
		return Timer.start(registry);
	}

	public void stopPortfolioCalculationTimer(Timer.Sample sample) {
		sample.stop(portfolioCalculationTimer);
	}

	// Gauge update methods
	public void setActiveSessions(long count) {
		gaugeValues.get("strategiz.sessions.active").set(count);
	}

	public void setConnectedProviders(long count) {
		gaugeValues.get("strategiz.providers.connected").set(count);
	}

	public void setActiveStrategies(long count) {
		gaugeValues.get("strategiz.strategies.active").set(count);
	}

}
