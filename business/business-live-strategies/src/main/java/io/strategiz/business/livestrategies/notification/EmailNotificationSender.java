package io.strategiz.business.livestrategies.notification;

import io.strategiz.business.livestrategies.adapter.AlertSignalAdapter.NotificationSender;
import io.strategiz.business.livestrategies.model.Signal;
import io.strategiz.client.sendgrid.EmailProvider;
import io.strategiz.client.sendgrid.model.EmailDeliveryResult;
import io.strategiz.client.sendgrid.model.EmailMessage;
import io.strategiz.data.strategy.entity.AlertDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Email notification sender for strategy alerts. Uses SendGrid to deliver email
 * notifications when alerts are triggered.
 */
@Component
@ConditionalOnBean(EmailProvider.class)
public class EmailNotificationSender implements NotificationSender {

	private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

	private static final String CHANNEL = "email";

	private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a z")
		.withZone(ZoneId.of("America/New_York"));

	private final EmailProvider emailProvider;

	public EmailNotificationSender(EmailProvider emailProvider) {
		this.emailProvider = emailProvider;
		log.info("EmailNotificationSender initialized");
	}

	@Override
	public boolean supports(String channel) {
		return CHANNEL.equalsIgnoreCase(channel);
	}

	@Override
	public void send(AlertDeployment alert, Signal signal) {
		if (!emailProvider.isAvailable()) {
			throw new RuntimeException("Email provider not available");
		}

		String userEmail = alert.getNotificationEmail();
		if (userEmail == null || userEmail.isEmpty()) {
			log.warn("No email address configured for alert {}", alert.getId());
			return;
		}

		EmailMessage email = buildEmailMessage(alert, signal, userEmail);

		EmailDeliveryResult result = emailProvider.sendEmail(email);

		if (!result.isSuccess()) {
			throw new RuntimeException(
					"Email delivery failed: " + result.getErrorCode() + " - " + result.getErrorMessage());
		}

		log.info("Email notification sent for alert {} to {}", alert.getId(), maskEmail(userEmail));
	}

	/**
	 * Build the email message from alert and signal data.
	 */
	private EmailMessage buildEmailMessage(AlertDeployment alert, Signal signal, String toEmail) {
		String strategyName = alert.getAlertName() != null ? alert.getAlertName() : "Strategy Alert";

		String subject = buildSubject(signal, strategyName);
		String bodyHtml = buildHtmlBody(alert, signal, strategyName);
		String bodyText = buildTextBody(alert, signal, strategyName);

		return EmailMessage.builder().toEmail(toEmail).subject(subject).bodyHtml(bodyHtml).bodyText(bodyText).build();
	}

	/**
	 * Build email subject line.
	 */
	private String buildSubject(Signal signal, String strategyName) {
		String emoji = signal.getType() == Signal.Type.BUY ? "📈" : "📉";
		return String.format("%s %s Signal: %s - %s", emoji, signal.getType(), signal.getSymbol(), strategyName);
	}

	/**
	 * Build HTML email body.
	 */
	private String buildHtmlBody(AlertDeployment alert, Signal signal, String strategyName) {
		String signalColor = signal.getType() == Signal.Type.BUY ? "#22c55e" : "#ef4444";
		String signalEmoji = signal.getType() == Signal.Type.BUY ? "📈" : "📉";
		String timestamp = TIME_FORMAT.format(Instant.now());

		return """
				<!DOCTYPE html>
				<html>
				<head>
				  <meta charset="utf-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1.0">
				</head>
				<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0a0a0a; margin: 0; padding: 20px;">
				  <div style="max-width: 600px; margin: 0 auto; background-color: #171717; border-radius: 12px; overflow: hidden; border: 1px solid #262626;">
				    <!-- Header -->
				    <div style="background: linear-gradient(135deg, #22c55e 0%%, #16a34a 100%%); padding: 24px; text-align: center;">
				      <h1 style="color: white; margin: 0; font-size: 24px;">%s Strategy Alert</h1>
				    </div>

				    <!-- Signal Box -->
				    <div style="padding: 24px;">
				      <div style="background-color: #262626; border-radius: 8px; padding: 20px; margin-bottom: 20px; border-left: 4px solid %s;">
				        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px;">
				          <span style="font-size: 32px;">%s</span>
				          <div>
				            <div style="color: %s; font-size: 24px; font-weight: bold;">%s %s</div>
				            <div style="color: #a3a3a3; font-size: 14px;">%s</div>
				          </div>
				        </div>

				        <!-- Details Grid -->
				        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
				          <div style="background: #1a1a1a; padding: 12px; border-radius: 6px;">
				            <div style="color: #a3a3a3; font-size: 12px; margin-bottom: 4px;">Current Price</div>
				            <div style="color: white; font-size: 18px; font-weight: bold;">$%s</div>
				          </div>
				          <div style="background: #1a1a1a; padding: 12px; border-radius: 6px;">
				            <div style="color: #a3a3a3; font-size: 12px; margin-bottom: 4px;">Confidence</div>
				            <div style="color: white; font-size: 18px; font-weight: bold;">%s%%</div>
				          </div>
				        </div>
				      </div>

				      <!-- Strategy Info -->
				      <div style="color: #a3a3a3; font-size: 14px; margin-bottom: 16px;">
				        <strong style="color: white;">Strategy:</strong> %s
				      </div>

				      <!-- Timestamp -->
				      <div style="color: #737373; font-size: 12px; text-align: center; padding-top: 16px; border-top: 1px solid #262626;">
				        Triggered at %s
				      </div>
				    </div>

				    <!-- Footer -->
				    <div style="background-color: #1a1a1a; padding: 16px; text-align: center; color: #737373; font-size: 12px;">
				      <p style="margin: 0 0 8px 0;">This is an automated alert from Strategiz.</p>
				      <a href="https://strategiz.io/dashboard" style="color: #22c55e; text-decoration: none;">View Dashboard</a>
				    </div>
				  </div>
				</body>
				</html>
				"""
			.formatted(signalEmoji, signalColor, signalEmoji, signalColor, signal.getType(), signal.getSymbol(),
					strategyName, formatPrice(signal.getPrice()), 85, strategyName, timestamp);
	}

	/**
	 * Build plain text email body (fallback).
	 */
	private String buildTextBody(AlertDeployment alert, Signal signal, String strategyName) {
		String timestamp = TIME_FORMAT.format(Instant.now());

		return """
				STRATEGY ALERT

				Signal: %s %s
				Strategy: %s
				Price: $%s
				Confidence: %s%%

				Triggered at %s

				View Dashboard: https://strategiz.io/dashboard

				---
				This is an automated alert from Strategiz.
				""".formatted(signal.getType(), signal.getSymbol(), strategyName, formatPrice(signal.getPrice()), 85,
				timestamp);
	}

	private String formatPrice(double price) {
		return PRICE_FORMAT.format(price);
	}

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
