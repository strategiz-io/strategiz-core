package io.strategiz.service.auth.config;

import io.strategiz.client.firebasesms.FirebaseSmsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Configuration adapter that bridges service-specific SMS OTP configuration to the client
 * interface required by the Firebase SMS client.
 *
 * This adapter follows the Adapter pattern to decouple the client module from
 * service-specific configuration details.
 */
@Component
public class FirebaseSmsConfigAdapter implements FirebaseSmsConfig {

	private final SmsOtpConfig smsOtpConfig;

	@Autowired
	public FirebaseSmsConfigAdapter(SmsOtpConfig smsOtpConfig) {
		this.smsOtpConfig = smsOtpConfig;
	}

	@Override
	public boolean isEnabled() {
		return smsOtpConfig.isFirebaseEnabled();
	}

	@Override
	public boolean isMockSmsEnabled() {
		return smsOtpConfig.isDevMockSmsEnabled();
	}

	@Override
	public boolean isLogOtpCodes() {
		return smsOtpConfig.isDevLogOtpCodes();
	}

	@Override
	public String getProjectId() {
		return smsOtpConfig.getFirebaseProjectId();
	}

	@Override
	public String getServiceAccountKeyPath() {
		return smsOtpConfig.getFirebaseServiceAccountKeyPath();
	}

	@Override
	public long getPhoneAuthTimeoutSeconds() {
		return smsOtpConfig.getFirebasePhoneAuthTimeoutSeconds();
	}

	@Override
	public boolean isAutoRetrieveEnabled() {
		return smsOtpConfig.isFirebaseAutoRetrieveEnabled();
	}

}