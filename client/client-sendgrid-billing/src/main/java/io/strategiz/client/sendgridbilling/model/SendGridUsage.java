package io.strategiz.client.sendgridbilling.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Usage metrics for SendGrid email service
 */
public record SendGridUsage(String accountId, String accountName, BigDecimal emailsSent, BigDecimal emailsDelivered,
		BigDecimal emailsBounced, BigDecimal apiRequests, Instant timestamp) {
}
