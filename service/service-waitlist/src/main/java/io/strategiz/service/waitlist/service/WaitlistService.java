package io.strategiz.service.waitlist.service;

import com.google.cloud.Timestamp;
import io.strategiz.client.sendgrid.EmailProvider;
import io.strategiz.client.sendgrid.model.EmailMessage;
import io.strategiz.data.waitlist.entity.WaitlistEntity;
import io.strategiz.data.waitlist.repository.WaitlistRepository;
import io.strategiz.service.waitlist.model.response.WaitlistJoinResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Service for managing waitlist operations
 */
@Service
public class WaitlistService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistService.class);

    private final WaitlistRepository repository;
    private final EmailProvider emailProvider;

    public WaitlistService(WaitlistRepository repository, EmailProvider emailProvider) {
        this.repository = repository;
        this.emailProvider = emailProvider;
    }

    /**
     * Add email to waitlist
     */
    public WaitlistJoinResponse joinWaitlist(String email, String ipAddress, String userAgent,
            String referralSource) {
        try {
            // Validate and normalize email
            email = email.trim().toLowerCase();

            // Hash email for duplicate detection
            String emailHash = hashEmail(email);

            // Check for duplicates
            Optional<WaitlistEntity> existing = repository.findByEmailHash(emailHash);
            if (existing.isPresent()) {
                log.info("Duplicate waitlist signup attempt: {}", maskEmail(email));
                return WaitlistJoinResponse.alreadyJoined();
            }

            // Create entity
            WaitlistEntity entity = new WaitlistEntity(email, emailHash);
            entity.setSignupDate(Timestamp.now());
            entity.setIpAddress(ipAddress);
            entity.setUserAgent(userAgent);
            entity.setReferralSource(referralSource);
            entity.setConfirmed(false);

            // Save to Firestore
            repository.save(entity);

            // Send confirmation email
            sendConfirmationEmail(email);

            log.info("New waitlist signup: {}", maskEmail(email));

            return WaitlistJoinResponse.success();
        }
        catch (Exception e) {
            log.error("Error adding to waitlist", e);
            return WaitlistJoinResponse.error("An error occurred. Please try again later.");
        }
    }

    /**
     * Get total waitlist count
     */
    public long getWaitlistCount() {
        return repository.count();
    }

    /**
     * Send confirmation email to new waitlist member
     */
    private void sendConfirmationEmail(String email) {
        try {
            if (!emailProvider.isAvailable()) {
                log.warn("Email provider not available, skipping confirmation email");
                return;
            }

            EmailMessage message = EmailMessage.builder()
                    .toEmail(email)
                    .subject("Welcome to Strategiz Waitlist!")
                    .bodyHtml(buildConfirmationEmailHtml())
                    .bodyText(buildConfirmationEmailText())
                    .build();

            emailProvider.sendEmail(message);
            log.info("Confirmation email sent to: {}", maskEmail(email));
        }
        catch (Exception e) {
            log.error("Failed to send confirmation email to: {}", maskEmail(email), e);
            // Don't fail the signup if email fails
        }
    }

    /**
     * Hash email using SHA-256
     */
    private String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Mask email for logging
     */
    private String maskEmail(String email) {
        if (email == null)
            return null;
        int atIndex = email.indexOf("@");
        if (atIndex > 2) {
            return email.substring(0, 2) + "***" + email.substring(atIndex);
        }
        return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
    }

    /**
     * Build HTML confirmation email
     */
    private String buildConfirmationEmailHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #0a0a0a; color: #e0e0e0; padding: 20px; margin: 0;">
                  <div style="max-width: 600px; margin: 0 auto; background-color: #1a1a1a; padding: 40px; border-radius: 8px; border: 1px solid #39FF14;">
                    <h1 style="color: #39FF14; text-align: center; margin-top: 0;">Welcome to Strategiz!</h1>
                    <p style="font-size: 16px; line-height: 1.6;">Thank you for joining our waitlist. You're now part of an exclusive group getting early access to the future of algorithmic trading.</p>
                    <p style="font-size: 16px; line-height: 1.6;">We'll notify you as soon as we launch. In the meantime, stay connected:</p>
                    <ul style="font-size: 16px; line-height: 1.8;">
                      <li>Follow us on Twitter: <a href="https://twitter.com/strategiz" style="color: #39FF14; text-decoration: none;">@strategiz</a></li>
                      <li>Connect on LinkedIn: <a href="https://linkedin.com/company/strategiz" style="color: #39FF14; text-decoration: none;">/company/strategiz</a></li>
                      <li>Check out our GitHub: <a href="https://github.com/strategiz" style="color: #39FF14; text-decoration: none;">/strategiz</a></li>
                    </ul>
                    <p style="text-align: center; margin-top: 40px;">
                      <a href="https://strategiz.io" style="display: inline-block; background-color: #39FF14; color: #0a0a0a; padding: 12px 30px; text-decoration: none; font-weight: bold; border-radius: 4px;">Visit Strategiz</a>
                    </p>
                    <p style="font-size: 12px; color: #808080; text-align: center; margin-top: 40px; margin-bottom: 0;">
                      © 2026 Strategiz. All rights reserved.
                    </p>
                  </div>
                </body>
                </html>
                """;
    }

    /**
     * Build plain text confirmation email
     */
    private String buildConfirmationEmailText() {
        return """
                Welcome to Strategiz!

                Thank you for joining our waitlist. You're now part of an exclusive group getting early access to the future of algorithmic trading.

                We'll notify you as soon as we launch.

                Follow us:
                - Twitter: https://twitter.com/strategiz
                - LinkedIn: https://linkedin.com/company/strategiz
                - GitHub: https://github.com/strategiz

                Visit us at: https://strategiz.io

                © 2026 Strategiz. All rights reserved.
                """;
    }
}
