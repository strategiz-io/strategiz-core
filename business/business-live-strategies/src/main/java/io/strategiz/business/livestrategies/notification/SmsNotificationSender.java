package io.strategiz.business.livestrategies.notification;

import io.strategiz.business.livestrategies.adapter.AlertSignalAdapter.NotificationSender;
import io.strategiz.business.livestrategies.model.Signal;
import io.strategiz.client.sms.SmsProvider;
import io.strategiz.client.sms.model.SmsDeliveryResult;
import io.strategiz.client.sms.model.SmsMessage;
import io.strategiz.data.strategy.entity.AlertDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;

/**
 * SMS notification sender for strategy alerts. Uses Twilio to deliver SMS notifications
 * when alerts are triggered.
 */
@Component
@ConditionalOnBean(SmsProvider.class)
public class SmsNotificationSender implements NotificationSender {

	private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

	private static final String CHANNEL = "sms";

	private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");

	// SMS has 160 char limit for standard GSM encoding
	private static final int MAX_SMS_LENGTH = 160;

	private final SmsProvider smsProvider;

	public SmsNotificationSender(SmsProvider smsProvider) {
		this.smsProvider = smsProvider;
		log.info("SmsNotificationSender initialized");
	}

	@Override
	public boolean supports(String channel) {
		return CHANNEL.equalsIgnoreCase(channel);
	}

	@Override
	public void send(AlertDeployment alert, Signal signal) {
		if (!smsProvider.isAvailable()) {
			throw new RuntimeException("SMS provider not available");
		}

		String phoneNumber = alert.getNotificationPhone();
		if (phoneNumber == null || phoneNumber.isEmpty()) {
			log.warn("No phone number configured for alert {}", alert.getId());
			return;
		}

		SmsMessage sms = buildSmsMessage(alert, signal, phoneNumber);

		SmsDeliveryResult result = smsProvider.sendSms(sms);

		if (!result.isSuccess()) {
			throw new RuntimeException(
					"SMS delivery failed: " + result.getErrorCode() + " - " + result.getErrorMessage());
		}

		log.info("SMS notification sent for alert {} to {}", alert.getId(), maskPhoneNumber(phoneNumber));
	}

	/**
	 * Build the SMS message from alert and signal data. Keep it concise for SMS character
	 * limits.
	 */
	private SmsMessage buildSmsMessage(AlertDeployment alert, Signal signal, String phoneNumber) {
		String strategyName = alert.getAlertName() != null ? truncate(alert.getAlertName(), 20) : "Strategy";

		String emoji = signal.getType() == Signal.Type.BUY ? "📈" : "📉";
		String price = "$" + PRICE_FORMAT.format(signal.getPrice());

		// Keep message under 160 chars
		String message = String.format("%s %s %s @ %s\n%s\nstrategiz.io", emoji, signal.getType(), signal.getSymbol(),
				price, strategyName);

		// Truncate if necessary
		if (message.length() > MAX_SMS_LENGTH) {
			message = message.substring(0, MAX_SMS_LENGTH - 3) + "...";
		}

		return new SmsMessage(phoneNumber, message);
	}

	private String truncate(String str, int maxLength) {
		if (str == null || str.length() <= maxLength) {
			return str;
		}
		return str.substring(0, maxLength - 3) + "...";
	}

	private String maskPhoneNumber(String phone) {
		if (phone == null || phone.length() < 4) {
			return "****";
		}
		return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
	}

}
