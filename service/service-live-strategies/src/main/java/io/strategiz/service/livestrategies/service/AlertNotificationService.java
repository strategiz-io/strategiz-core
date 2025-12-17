package io.strategiz.service.livestrategies.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import io.strategiz.business.strategy.execution.model.ExecutionResult;
import io.strategiz.business.preferences.service.AlertPreferencesService;
import io.strategiz.client.sms.SmsProvider;
import io.strategiz.client.sms.model.SmsDeliveryResult;
import io.strategiz.client.sms.model.SmsMessage;
import io.strategiz.data.strategy.entity.StrategyAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending alert notifications through multiple channels.
 * Supports email (SendGrid), push notifications (FCM), and in-app notifications (Firestore).
 */
@Service
public class AlertNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AlertNotificationService.class);

    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:noreply@strategiz.io}")
    private String fromEmail;

    @Value("${sendgrid.from.name:Strategiz Alerts}")
    private String fromName;

    @Value("${app.frontend.url:https://app.strategiz.io}")
    private String frontendUrl;

    private final Firestore firestore;
    private final FirebaseMessaging firebaseMessaging;
    private final SmsProvider smsProvider;
    private final AlertPreferencesService alertPreferencesService;

    @Autowired
    public AlertNotificationService(
            Firestore firestore,
            @Autowired(required = false) FirebaseMessaging firebaseMessaging,
            @Autowired(required = false) SmsProvider smsProvider,
            @Autowired(required = false) AlertPreferencesService alertPreferencesService) {
        this.firestore = firestore;
        this.firebaseMessaging = firebaseMessaging;
        this.smsProvider = smsProvider;
        this.alertPreferencesService = alertPreferencesService;
    }

    /**
     * Send notification when a trading signal is detected
     *
     * @param alert The alert that triggered
     * @param signal The trading signal (BUY/SELL/HOLD)
     * @param symbol The symbol that triggered
     * @param currentPrice Current price of the symbol
     */
    public void sendSignalNotification(
            StrategyAlert alert,
            ExecutionResult.Signal signal,
            String symbol,
            Double currentPrice) {

        logger.info("Sending {} signal notification for alert {} (symbol: {}, price: ${})",
            signal.getType(), alert.getAlertName(), symbol, currentPrice);

        // Send to each configured notification channel
        for (String channel : alert.getNotificationChannels()) {
            try {
                switch (channel.toLowerCase()) {
                    case "email":
                        sendEmailNotification(alert, signal, symbol, currentPrice);
                        break;
                    case "sms":
                        sendSmsNotification(alert, signal, symbol, currentPrice);
                        break;
                    case "push":
                        sendPushNotification(alert, signal, symbol, currentPrice);
                        break;
                    case "in-app":
                    case "inapp":
                        sendInAppNotification(alert, signal, symbol, currentPrice);
                        break;
                    default:
                        logger.warn("Unknown notification channel: {}", channel);
                }
            } catch (Exception e) {
                logger.error("Failed to send {} notification for alert {}: {}",
                    channel, alert.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Send email notification via SendGrid
     */
    private void sendEmailNotification(
            StrategyAlert alert,
            ExecutionResult.Signal signal,
            String symbol,
            Double currentPrice) {

        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            logger.warn("SendGrid API key not configured, skipping email notification");
            return;
        }

        try {
            // Get user email from Firebase Auth or use a default for now
            // TODO: Fetch actual user email from UserEntity
            String userEmail = "user@example.com"; // Placeholder

            Email from = new Email(fromEmail, fromName);
            Email to = new Email(userEmail);

            String subject = String.format("[Strategiz Alert] %s signal for %s", signal.getType(), symbol);

            // Create HTML email body
            String htmlBody = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2 style="color: #10b981;">Trading Alert Triggered!</h2>
                    <p>Your alert <strong>"%s"</strong> has detected a trading signal:</p>

                    <table style="border-collapse: collapse; width: 100%%; max-width: 500px; margin: 20px 0;">
                        <tr style="background-color: #f3f4f6;">
                            <td style="padding: 10px; border: 1px solid #e5e7eb;"><strong>Signal:</strong></td>
                            <td style="padding: 10px; border: 1px solid #e5e7eb; color: %s;"><strong>%s</strong></td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; border: 1px solid #e5e7eb;"><strong>Symbol:</strong></td>
                            <td style="padding: 10px; border: 1px solid #e5e7eb;">%s</td>
                        </tr>
                        <tr style="background-color: #f3f4f6;">
                            <td style="padding: 10px; border: 1px solid #e5e7eb;"><strong>Price:</strong></td>
                            <td style="padding: 10px; border: 1px solid #e5e7eb;">$%.2f</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; border: 1px solid #e5e7eb;"><strong>Reason:</strong></td>
                            <td style="padding: 10px; border: 1px solid #e5e7eb;">%s</td>
                        </tr>
                    </table>

                    <p style="margin-top: 30px;">
                        <a href="%s/live-strategies"
                           style="background-color: #10b981; color: white; padding: 12px 24px;
                                  text-decoration: none; border-radius: 6px; display: inline-block;">
                            View in Strategiz
                        </a>
                    </p>

                    <p style="color: #6b7280; font-size: 12px; margin-top: 30px;">
                        This is an automated alert from Strategiz. To manage your alerts, visit your
                        <a href="%s/live-strategies">Live Strategies</a> page.
                    </p>
                </body>
                </html>
                """,
                    alert.getAlertName(),
                    signal.getType().equals("BUY") ? "#10b981" : "#ef4444",
                    signal.getType(),
                    symbol,
                    currentPrice,
                    signal.getReason() != null ? signal.getReason() : "Strategy logic triggered",
                    frontendUrl,
                    frontendUrl
            );

            Content content = new Content("text/html", htmlBody);
            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("Email notification sent successfully to {} for {} signal on {}",
                          userEmail, signal.getType(), symbol);
            } else {
                logger.error("Failed to send email notification. Status: {}, Body: {}",
                           response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            logger.error("Error sending email notification", e);
        }
    }

    /**
     * Send SMS notification via Twilio
     */
    private void sendSmsNotification(
            StrategyAlert alert,
            ExecutionResult.Signal signal,
            String symbol,
            Double currentPrice) {

        if (smsProvider == null || !smsProvider.isAvailable()) {
            logger.warn("SMS provider not available, skipping SMS notification");
            return;
        }

        try {
            // Fetch user's phone number from AlertNotificationPreferences
            String userPhone = null;
            if (alertPreferencesService != null) {
                userPhone = alertPreferencesService.getSmsPhoneNumber(alert.getUserId());
            }

            if (userPhone == null || userPhone.isEmpty()) {
                logger.warn("User {} has no verified phone number configured for SMS alerts", alert.getUserId());
                return;
            }

            // Check quiet hours
            if (alertPreferencesService != null && alertPreferencesService.isInQuietHours(alert.getUserId())) {
                logger.info("Skipping SMS for user {} - currently in quiet hours", alert.getUserId());
                return;
            }

            // Check rate limit
            if (alertPreferencesService != null && !alertPreferencesService.canSendAlert(alert.getUserId())) {
                logger.warn("Rate limit exceeded for user {} - skipping SMS", alert.getUserId());
                return;
            }

            // Build SMS message
            String signalEmoji = "BUY".equals(signal.getType()) ? "+" : "-";
            String message = String.format(
                    "[Strategiz] %s %s signal for %s at $%.2f. Alert: %s",
                    signalEmoji,
                    signal.getType(),
                    symbol,
                    currentPrice,
                    alert.getAlertName()
            );

            // Add reason if available
            if (signal.getReason() != null && !signal.getReason().isEmpty()) {
                String reason = signal.getReason();
                // Truncate reason if message would be too long (SMS limit ~160 chars)
                int remainingChars = 160 - message.length() - 3; // -3 for " - "
                if (remainingChars > 10 && reason.length() > remainingChars) {
                    reason = reason.substring(0, remainingChars - 3) + "...";
                }
                if (remainingChars > 10) {
                    message += " - " + reason;
                }
            }

            SmsMessage smsMessage = new SmsMessage(userPhone, message);
            SmsDeliveryResult result = smsProvider.sendSms(smsMessage);

            if (result.isSuccess()) {
                logger.info("SMS notification sent successfully to {} for {} signal on {}. MessageId: {}",
                        maskPhoneNumber(userPhone), signal.getType(), symbol, result.getMessageId());

                // Record alert sent for rate limiting
                if (alertPreferencesService != null) {
                    alertPreferencesService.recordAlertSent(alert.getUserId());
                }
            } else {
                logger.error("Failed to send SMS notification. Status: {}, Error: {}",
                        result.getStatus(), result.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("Error sending SMS notification", e);
        }
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

    /**
     * Send push notification via Firebase Cloud Messaging
     */
    private void sendPushNotification(
            StrategyAlert alert,
            ExecutionResult.Signal signal,
            String symbol,
            Double currentPrice) {

        if (firebaseMessaging == null) {
            logger.warn("Firebase Messaging not configured, skipping push notification");
            return;
        }

        try {
            // TODO: Fetch user's FCM device tokens from Firestore
            // For now, we'll skip if no tokens are available
            // In production, fetch from /users/{userId}/devices collection

            String title = String.format("%s Signal: %s", signal.getType(), symbol);
            String body = String.format("Your alert \"%s\" triggered at $%.2f", alert.getAlertName(), currentPrice);

            // Build notification
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // Add custom data
            Map<String, String> data = new HashMap<>();
            data.put("alertId", alert.getId());
            data.put("signal", signal.getType());
            data.put("symbol", symbol);
            data.put("price", String.valueOf(currentPrice));
            data.put("reason", signal.getReason() != null ? signal.getReason() : "");
            data.put("timestamp", Instant.now().toString());

            // TODO: Send to user's registered device tokens
            // Example:
            // Message message = Message.builder()
            //     .setNotification(notification)
            //     .putAllData(data)
            //     .setToken(deviceToken)
            //     .build();
            // firebaseMessaging.send(message);

            logger.info("Push notification would be sent for {} signal on {} (device tokens not yet fetched)",
                      signal.getType(), symbol);

        } catch (Exception e) {
            logger.error("Error sending push notification", e);
        }
    }

    /**
     * Send in-app notification via Firestore
     */
    private void sendInAppNotification(
            StrategyAlert alert,
            ExecutionResult.Signal signal,
            String symbol,
            Double currentPrice) {

        try {
            // Create notification document
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "alert_triggered");
            notification.put("alertId", alert.getId());
            notification.put("alertName", alert.getAlertName());
            notification.put("signal", signal.getType());
            notification.put("symbol", symbol);
            notification.put("price", currentPrice);
            notification.put("reason", signal.getReason() != null ? signal.getReason() : "Strategy logic triggered");
            notification.put("timestamp", com.google.cloud.Timestamp.now());
            notification.put("read", false);

            // Store in Firestore: /users/{userId}/notifications
            firestore.collection("users")
                    .document(alert.getUserId())
                    .collection("notifications")
                    .add(notification)
                    .get(); // Wait for completion

            logger.info("In-app notification created for user {} - {} signal on {}",
                      alert.getUserId(), signal.getType(), symbol);

        } catch (Exception e) {
            logger.error("Error creating in-app notification", e);
        }
    }

    /**
     * Send test notification (used by POST /v1/alerts/{id}/test endpoint)
     */
    public void sendTestNotification(StrategyAlert alert) {
        logger.info("Sending test notification for alert {}", alert.getAlertName());

        ExecutionResult.Signal testSignal = new ExecutionResult.Signal();
        testSignal.setType("BUY");
        testSignal.setPrice(100.0);
        testSignal.setReason("This is a test notification");

        String testSymbol = alert.getSymbols().isEmpty() ? "TEST" : alert.getSymbols().get(0);

        sendSignalNotification(alert, testSignal, testSymbol, 100.0);
    }
}
