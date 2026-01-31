package io.strategiz.client.sms.twilio;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.strategiz.client.sms.SmsProvider;
import io.strategiz.client.sms.model.SmsDeliveryResult;
import io.strategiz.client.sms.model.SmsMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Twilio implementation of SmsProvider for sending alert notifications.
 *
 * Configuration is loaded from Vault via TwilioSmsConfig. Supports mock mode for
 * development environments.
 */
@Component
@ConditionalOnProperty(name = "twilio.enabled", havingValue = "true", matchIfMissing = true)
public class TwilioSmsClient implements SmsProvider {

	private static final Logger logger = LoggerFactory.getLogger(TwilioSmsClient.class);

	private static final String PROVIDER_NAME = "TWILIO";

	private final TwilioSmsConfig config;

	private boolean initialized = false;

	public TwilioSmsClient(TwilioSmsConfig config) {
		this.config = config;
	}

	@PostConstruct
	public void init() {
		if (config.isConfigured()) {
			try {
				Twilio.init(config.getAccountSid(), config.getAuthToken());
				initialized = true;
				logger.info("Twilio SMS client initialized successfully");
			}
			catch (Exception e) {
				logger.error("Failed to initialize Twilio SMS client: {}", e.getMessage());
				initialized = false;
			}
		}
		else {
			logger.warn("Twilio SMS client not configured - SMS notifications will be disabled");
		}
	}

	@Override
	public SmsDeliveryResult sendSms(SmsMessage message) {
		if (!isAvailable()) {
			logger.warn("Twilio SMS client not available, cannot send message");
			return SmsDeliveryResult.unavailable(PROVIDER_NAME);
		}

		// Mock mode for development
		if (config.isMockEnabled()) {
			logger.info("[MOCK SMS] To: {}, Body: {}", message.getToPhoneNumber(), message.getBody());
			return SmsDeliveryResult.success("mock-" + System.currentTimeMillis(), PROVIDER_NAME);
		}

		try {
			// Determine from number (explicit, config default, or messaging service)
			String fromNumber = message.getFromPhoneNumber() != null ? message.getFromPhoneNumber()
					: config.getPhoneNumber();

			logger.info("Sending SMS via Twilio to {}", maskPhoneNumber(message.getToPhoneNumber()));

			Message twilioMessage;

			// Use Messaging Service if configured (better for high volume)
			if (config.getMessagingServiceSid() != null && !config.getMessagingServiceSid().isEmpty()) {
				twilioMessage = Message.creator(new PhoneNumber(message.getToPhoneNumber()), // to
						config.getMessagingServiceSid(), // messaging service SID
						message.getBody())
					.create();
			}
			else {
				twilioMessage = Message.creator(new PhoneNumber(message.getToPhoneNumber()), // to
						new PhoneNumber(fromNumber), // from
						message.getBody())
					.create();
			}

			String messageId = twilioMessage.getSid();
			Message.Status status = twilioMessage.getStatus();

			logger.info("SMS sent successfully. SID: {}, Status: {}", messageId, status);

			return SmsDeliveryResult.builder()
				.status(mapTwilioStatus(status))
				.messageId(messageId)
				.providerName(PROVIDER_NAME)
				.build();

		}
		catch (com.twilio.exception.ApiException e) {
			logger.error("Twilio API error sending SMS: {} (code: {})", e.getMessage(), e.getCode());

			return SmsDeliveryResult.failure(String.valueOf(e.getCode()), e.getMessage(), PROVIDER_NAME);

		}
		catch (Exception e) {
			logger.error("Unexpected error sending SMS via Twilio", e);

			return SmsDeliveryResult.failure("UNKNOWN", e.getMessage(), PROVIDER_NAME);
		}
	}

	@Override
	public boolean isAvailable() {
		return config.isEnabled() && (initialized || config.isMockEnabled());
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	/**
	 * Map Twilio message status to our delivery status.
	 */
	private SmsDeliveryResult.Status mapTwilioStatus(Message.Status twilioStatus) {
		if (twilioStatus == null) {
			return SmsDeliveryResult.Status.QUEUED;
		}

		return switch (twilioStatus) {
			case QUEUED, ACCEPTED -> SmsDeliveryResult.Status.QUEUED;
			case SENDING, SENT -> SmsDeliveryResult.Status.SENT;
			case DELIVERED -> SmsDeliveryResult.Status.DELIVERED;
			case FAILED, UNDELIVERED -> SmsDeliveryResult.Status.FAILED;
			default -> SmsDeliveryResult.Status.QUEUED;
		};
	}

	/**
	 * Mask phone number for secure logging.
	 */
	private String maskPhoneNumber(String phone) {
		if (phone == null || phone.length() < 4) {
			return "****";
		}
		return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
	}

}
