package io.strategiz.client.sendgrid;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import io.strategiz.client.sendgrid.model.EmailDeliveryResult;
import io.strategiz.client.sendgrid.model.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SendGrid implementation of EmailProvider for sending alert notifications.
 *
 * Supports both plain text/HTML emails and SendGrid dynamic templates. Configuration is
 * loaded from Vault via SendGridVaultConfig.
 */
@Component
@ConditionalOnProperty(name = "sendgrid.enabled", havingValue = "true", matchIfMissing = true)
public class SendGridClient implements EmailProvider {

	private static final Logger logger = LoggerFactory.getLogger(SendGridClient.class);

	private static final String PROVIDER_NAME = "SENDGRID";

	private final SendGridConfig config;

	private volatile SendGrid sendGrid;

	private volatile boolean initialized = false;

	private volatile boolean initAttempted = false;

	public SendGridClient(SendGridConfig config) {
		this.config = config;
	}

	/**
	 * Lazily initialize the SendGrid client on first use. This ensures Vault
	 * configuration is loaded before we try to use the API key.
	 */
	private synchronized void ensureInitialized() {
		if (initAttempted) {
			return;
		}
		initAttempted = true;

		if (config.isConfigured()) {
			try {
				sendGrid = new SendGrid(config.getApiKey());
				initialized = true;
				logger.info("SendGrid client initialized successfully");
			}
			catch (Exception e) {
				logger.error("Failed to initialize SendGrid client: {}", e.getMessage());
				initialized = false;
			}
		}
		else {
			logger.warn(
					"SendGrid client not configured - email notifications will be disabled. "
							+ "API key present: {}, From email present: {}",
					config.getApiKey() != null && !config.getApiKey().isEmpty(),
					config.getFromEmail() != null && !config.getFromEmail().isEmpty());
		}
	}

	@Override
	public EmailDeliveryResult sendEmail(EmailMessage message) {
		if (!isAvailable()) {
			logger.warn("SendGrid client not available, cannot send email");
			return EmailDeliveryResult.unavailable(PROVIDER_NAME);
		}

		// Mock mode for development
		if (config.isMockEnabled()) {
			logger.info("[MOCK EMAIL] To: {}, Subject: {}", message.getToEmail(), message.getSubject());
			return EmailDeliveryResult.success("mock-" + System.currentTimeMillis(), PROVIDER_NAME);
		}

		try {
			Mail mail = buildMail(message);
			Request request = new Request();
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());

			logger.info("Sending email via SendGrid to {}", maskEmail(message.getToEmail()));

			Response response = sendGrid.api(request);

			if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
				// Extract message ID from headers
				String messageId = response.getHeaders().get("X-Message-Id");
				if (messageId == null) {
					messageId = "sg-" + System.currentTimeMillis();
				}

				logger.info("Email sent successfully. Status: {}, MessageId: {}", response.getStatusCode(), messageId);

				return EmailDeliveryResult.success(messageId, PROVIDER_NAME);
			}
			else {
				logger.error("SendGrid API error: {} - {}", response.getStatusCode(), response.getBody());

				return EmailDeliveryResult.failure(String.valueOf(response.getStatusCode()), response.getBody(),
						PROVIDER_NAME);
			}
		}
		catch (IOException e) {
			logger.error("Failed to send email via SendGrid", e);
			return EmailDeliveryResult.failure("IO_ERROR", e.getMessage(), PROVIDER_NAME);
		}
		catch (Exception e) {
			logger.error("Unexpected error sending email via SendGrid", e);
			return EmailDeliveryResult.failure("UNKNOWN", e.getMessage(), PROVIDER_NAME);
		}
	}

	@Override
	public boolean isAvailable() {
		ensureInitialized();
		return config.isEnabled() && (initialized || config.isMockEnabled());
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	/**
	 * Build SendGrid Mail object from EmailMessage.
	 */
	private Mail buildMail(EmailMessage message) {
		// From email
		String fromEmail = message.getFromEmail() != null ? message.getFromEmail() : config.getFromEmail();
		String fromName = message.getFromName() != null ? message.getFromName() : config.getFromName();
		Email from = new Email(fromEmail, fromName);

		// To email
		Email to = new Email(message.getToEmail(), message.getToName());

		Mail mail = new Mail();
		mail.setFrom(from);

		// Use dynamic template if specified
		if (message.hasTemplate()) {
			mail.setTemplateId(message.getTemplateId());

			Personalization personalization = new Personalization();
			personalization.addTo(to);

			// Add template data
			if (message.getTemplateData() != null) {
				message.getTemplateData().forEach(personalization::addDynamicTemplateData);
			}

			mail.addPersonalization(personalization);
		}
		else {
			// Plain text/HTML email
			mail.setSubject(message.getSubject());

			Personalization personalization = new Personalization();
			personalization.addTo(to);
			mail.addPersonalization(personalization);

			if (message.getBodyText() != null) {
				mail.addContent(new Content("text/plain", message.getBodyText()));
			}
			if (message.getBodyHtml() != null) {
				mail.addContent(new Content("text/html", message.getBodyHtml()));
			}
		}

		return mail;
	}

	/**
	 * Mask email for secure logging.
	 */
	private String maskEmail(String email) {
		if (email == null || !email.contains("@")) {
			return "****";
		}
		int atIndex = email.indexOf("@");
		if (atIndex <= 2) {
			return "***" + email.substring(atIndex);
		}
		return email.substring(0, 2) + "***" + email.substring(atIndex);
	}

}
