package io.strategiz.service.auth.service.totp;

import java.time.Instant;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for handling TOTP registration operations
 * Manages the generation of TOTP secrets, QR codes, and TOTP setup/disabling
 */
@Service
public class TotpRegistrationService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TotpRegistrationService.class);
    
    // TOTP configuration constants
    private static final int DEFAULT_TOTP_CODE_DIGITS = 6;
    private static final int DEFAULT_PERIOD_SECONDS = 30;
    private static final String TOTP_ALGORITHM = "SHA1"; // Most compatible with authenticator apps
    private static final String ISSUER = "Strategiz";
    
    // QR code configuration
    private static final int QR_SIZE = 300;
    private static final int LOGO_SIZE = 60;
    private static final int BORDER_SIZE = 0; // Minimal border
    private static final Color QR_COLOR = Color.BLACK;
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private static final String LOGO_PATH = "assets/strategiz-logo.png";
    
    private final UserRepository userRepository;
    private final AuthenticationMethodRepository authMethodRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;
    
    public TotpRegistrationService(UserRepository userRepository, 
                                   AuthenticationMethodRepository authMethodRepository,
                                   SessionAuthBusiness sessionAuthBusiness) {
        this.userRepository = userRepository;
        this.authMethodRepository = authMethodRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
        this.secretGenerator = new DefaultSecretGenerator();
        
        // Configure code verifier with timing window
        TimeProvider timeProvider = new SystemTimeProvider();
        this.codeVerifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(HashingAlgorithm.SHA1),
            timeProvider
        );
    }
    
    /**
     * Result holder for TOTP secret generation
     */
    public static class TotpGenerationResult {
        private final String secret;
        private final String qrCodeUri;
        
        public TotpGenerationResult(String secret, String qrCodeUri) {
            this.secret = secret;
            this.qrCodeUri = qrCodeUri;
        }
        
        public String getSecret() { return secret; }
        public String getQrCodeUri() { return qrCodeUri; }
    }
    
    /**
     * Generate a new TOTP secret for a user (legacy method)
     * @param username the username to generate the secret for
     * @return the generated QR code as a data URI
     * @deprecated Use generateTotpSecretWithDetails for new implementations
     */
    @Deprecated
    public String generateTotpSecret(String username) {
        return generateTotpSecretWithDetails(username).getQrCodeUri();
    }
    
    /**
     * Generate a new TOTP secret for a user with both secret and QR code
     * @param username the username (user ID) to generate the secret for
     * @return TotpGenerationResult containing both secret and QR code
     */
    public TotpGenerationResult generateTotpSecretWithDetails(String username) {
        log.info("Starting TOTP secret generation for user: {}", username);

        // Try to find user by ID to get their email for a better TOTP label
        // If user document doesn't exist, we can still proceed with TOTP setup using userId as label
        String userId = username;
        String userEmail = username; // Default to using username/userId as label

        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found by ID: {}, trying by email", username);
            userOpt = userRepository.findByEmail(username);
        }

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            userId = user.getId();
            log.info("Found user with ID: {} for TOTP setup", userId);

            // Get the user's email for the TOTP label if available
            if (user.getProfile() != null && user.getProfile().getEmail() != null && !user.getProfile().getEmail().isEmpty()) {
                userEmail = user.getProfile().getEmail();
            } else {
                log.warn("User {} has no email in profile, using user ID as TOTP label", userId);
                userEmail = userId;
            }
        } else {
            // User document not found - this can happen for passkey-only users
            // Proceed with TOTP setup using the username/userId directly
            log.warn("User document not found for: {}, proceeding with TOTP setup using userId as label", username);
        }

        // Generate a new TOTP secret
        String secret = secretGenerator.generate();

        // Find existing TOTP method or create a new one
        List<AuthenticationMethodEntity> existingMethods = authMethodRepository.findByUserIdAndType(userId, AuthenticationMethodType.TOTP);
        AuthenticationMethodEntity totpAuth = null;
        
        if (!existingMethods.isEmpty()) {
            totpAuth = existingMethods.get(0); // Use first TOTP method
        }
        
        if (totpAuth == null) {
            totpAuth = new AuthenticationMethodEntity(AuthenticationMethodType.TOTP, "Authenticator App");
        }
        
        // Store TOTP-specific data in metadata using constants
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.SECRET_KEY, secret);
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFIED, false);
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.QR_CODE_GENERATED, true);
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.BACKUP_CODES, new ArrayList<String>()); // Empty backup codes initially
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.BACKUP_CODES_USED, new ArrayList<String>());
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.ALGORITHM, "SHA1"); // Default TOTP algorithm
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.DIGITS, 6); // Standard 6-digit codes
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.PERIOD, 30); // 30-second time step
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.ISSUER, "Strategiz");
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.ACCOUNT_NAME, userEmail); // Store email for user-friendly display
        totpAuth.setIsActive(false); // Not enabled until verified
        
        log.info("Saving TOTP auth method for user ID: {} (email: {}) with secret: {}", userId, userEmail, secret.substring(0, 4) + "...");
        authMethodRepository.saveForUser(userId, totpAuth);
        log.info("Successfully saved TOTP auth method for user ID: {}", userId);
        
        // Generate the QR code using the email for display
        String qrCodeUri = generateQrCodeUri(userEmail, secret);
        return new TotpGenerationResult(secret, qrCodeUri);
    }
    
    /**
     * Generate a QR code URI for TOTP setup with embedded Strategiz logo
     * @param username the username
     * @param secret the TOTP secret
     * @return the QR code as a data URI string
     */
    private String generateQrCodeUri(String username, String secret) {
        try {
            // Build TOTP URL
            String totpUrl = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
                    ISSUER, username, secret, ISSUER, TOTP_ALGORITHM, DEFAULT_TOTP_CODE_DIGITS, DEFAULT_PERIOD_SECONDS);
            
            return generateQrCodeWithLogo(totpUrl);
        } catch (Exception e) {
            log.error("Error generating QR code for user {}: {}", username, e.getMessage());
            throw new StrategizException(AuthErrors.QR_GENERATION_FAILED, e.getMessage());
        }
    }
    
    /**
     * Generate QR code with embedded logo
     * @param data The data to encode (TOTP URL)
     * @return Base64 encoded PNG image as data URI
     */
    private String generateQrCodeWithLogo(String data) {
        try {
            // Create QR code with high error correction
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // High error correction for logo embedding
            hints.put(EncodeHintType.MARGIN, BORDER_SIZE); // Minimal margin
            
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrWriter.encode(data, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            
            // Convert BitMatrix to BufferedImage
            BufferedImage qrImage = createQrImage(bitMatrix);
            
            // Load and overlay logo
            BufferedImage finalImage = embedLogo(qrImage);
            
            // Convert to base64 data URI
            return convertToDataUri(finalImage);
            
        } catch (Exception e) {
            log.error("Error generating QR code with logo: {}", e.getMessage());
            // Fallback to simple QR code without logo
            return generateSimpleQrCode(data);
        }
    }
    
    /**
     * Convert BitMatrix to BufferedImage
     */
    private BufferedImage createQrImage(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? QR_COLOR.getRGB() : BACKGROUND_COLOR.getRGB());
            }
        }
        
        return image;
    }
    
    /**
     * Embed logo in the center of QR code
     */
    private BufferedImage embedLogo(BufferedImage qrImage) {
        try {
            // Load logo from resources
            ClassPathResource logoResource = new ClassPathResource(LOGO_PATH);
            BufferedImage logo;
            
            try (InputStream logoStream = logoResource.getInputStream()) {
                logo = ImageIO.read(logoStream);
            }
            
            if (logo == null) {
                log.warn("Logo not found, returning QR code without logo");
                return qrImage;
            }
            
            // Create final image
            BufferedImage finalImage = new BufferedImage(qrImage.getWidth(), qrImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = finalImage.createGraphics();
            
            // Enable anti-aliasing for smooth rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Draw QR code
            g2d.drawImage(qrImage, 0, 0, null);
            
            // Calculate logo position (center)
            int logoX = (qrImage.getWidth() - LOGO_SIZE) / 2;
            int logoY = (qrImage.getHeight() - LOGO_SIZE) / 2;
            
            // Create white background with rounded corners for logo
            int padding = 8;
            int backgroundSize = LOGO_SIZE + (padding * 2);
            int backgroundX = logoX - padding;
            int backgroundY = logoY - padding;
            
            g2d.setColor(BACKGROUND_COLOR);
            g2d.fill(new RoundRectangle2D.Float(backgroundX, backgroundY, backgroundSize, backgroundSize, 15, 15));
            
            // Draw logo
            Image scaledLogo = logo.getScaledInstance(LOGO_SIZE, LOGO_SIZE, Image.SCALE_SMOOTH);
            g2d.drawImage(scaledLogo, logoX, logoY, null);
            
            g2d.dispose();
            return finalImage;
            
        } catch (Exception e) {
            log.error("Error embedding logo: {}", e.getMessage());
            return qrImage;
        }
    }
    
    /**
     * Convert BufferedImage to base64 data URI
     */
    private String convertToDataUri(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64Image;
    }
    
    /**
     * Fallback: Generate simple QR code without logo
     */
    private String generateSimpleQrCode(String data) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, BORDER_SIZE);
            
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrWriter.encode(data, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            
            BufferedImage qrImage = createQrImage(bitMatrix);
            return convertToDataUri(qrImage);
            
        } catch (Exception e) {
            log.error("Error generating fallback QR code: {}", e.getMessage());
            return "data:image/png;base64,"; // Empty data URI as last resort
        }
    }
    
    /**
     * Verify TOTP code during registration completion
     * This method allows verification of codes against unconfigured TOTP methods
     * @param username the username
     * @param code the TOTP code to verify
     * @return true if the code is valid for the pending TOTP setup, false otherwise
     */
    protected boolean verifyRegistrationCode(String username, String code) {
        log.info("Starting TOTP registration verification for user: {}", username);
        
        // First try to find user by ID (which is what we're using as username)
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found by ID, trying by email: {}", username);
            // Try finding by email as fallback
            userOpt = userRepository.findByEmail(username);
            if (userOpt.isEmpty()) {
                log.warn("User not found for TOTP registration verification: {}", username);
                return false;
            }
        }
        
        UserEntity user = userOpt.get();
        log.info("Found user with ID: {} for username: {}", user.getId(), username);
        
        // Log more details about the search
        log.info("Searching for TOTP methods with userId: {} and type: {}", user.getId(), AuthenticationMethodType.TOTP);
        List<AuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndType(user.getId(), AuthenticationMethodType.TOTP);
        log.info("Found {} TOTP methods for user ID: {}", totpMethods.size(), user.getId());
        
        // Also try to list all auth methods for debugging
        if (totpMethods.isEmpty()) {
            log.warn("No TOTP methods found. Listing all auth methods for user ID: {}", user.getId());
            List<AuthenticationMethodEntity> allMethods = authMethodRepository.findByUserId(user.getId());
            log.warn("Total auth methods for user: {}, types: {}",
                allMethods.size(),
                allMethods.stream().map(m -> m.getAuthenticationMethod().toString()).collect(java.util.stream.Collectors.joining(", "))
            );
        }
        
        if (totpMethods.isEmpty()) {
            log.warn("TOTP auth method not found for registration verification: {}", username);
            return false;
        }
        
        AuthenticationMethodEntity totpAuth = totpMethods.get(0);
        String secret = totpAuth.getMetadataAsString(AuthenticationMethodMetadata.TotpMetadata.SECRET_KEY);
        
        if (secret == null) {
            log.warn("TOTP secret not found for registration verification: {}", username);
            return false;
        }
        
        // Verify the code against the secret, regardless of configured status
        boolean isValid = codeVerifier.isValidCode(secret, code);
        if (isValid) {
            log.info("TOTP registration code verified successfully for user: {}", username);
        } else {
            log.warn("Invalid TOTP registration code for user: {}", username);
        }
        
        return isValid;
    }
    
    /**
     * Enable TOTP for a user's session and return updated auth tokens
     * @param username the username
     * @param accessToken the current access token
     * @param code the TOTP code to verify
     * @return Map containing success status and updated tokens, or null if failed
     */
    public Map<String, Object> enableTotpWithTokenUpdate(String username, String accessToken, String code) {
        // Verify the code using registration-specific verification
        if (!verifyRegistrationCode(username, code)) {
            return null;
        }

        // Get the user and update the TOTP authentication method
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return null;
        }

        UserEntity user = userOpt.get();
        List<AuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndType(user.getId(), AuthenticationMethodType.TOTP);

        if (totpMethods.isEmpty()) {
            log.warn("TOTP auth method not found for user: {}", username);
            return null;
        }

        AuthenticationMethodEntity totpAuth = totpMethods.get(0);

        // Mark the TOTP as verified and enabled
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFIED, true);
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFICATION_TIME, Instant.now().toString());
        totpAuth.setIsActive(true);
        totpAuth.markAsUsed();
        authMethodRepository.saveForUser(user.getId(), totpAuth);

        // Update the session with TOTP as an authenticated method
        // This generates a new PASETO access token with ACR=2 and TOTP in auth methods
        Map<String, Object> authResult = sessionAuthBusiness.addAuthenticationMethod(
            accessToken,
            "totp",
            2 // ACR level 2 for TOTP
        );

        log.info("TOTP enabled for user {} with updated session", username);
        return authResult;
    }

    /**
     * Enable TOTP for a user's session (backward compatibility)
     * @param username the username
     * @param accessToken the current access token
     * @param code the TOTP code to verify
     * @return true if TOTP was successfully enabled, false otherwise
     */
    public boolean enableTotp(String username, String accessToken, String code) {
        Map<String, Object> result = enableTotpWithTokenUpdate(username, accessToken, code);
        return result != null;
    }
    
    /**
     * Check if TOTP is set up for a user
     * @param username the username
     * @return true if TOTP is set up, false otherwise
     */
    public boolean isTotpSetUp(String username) {
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        UserEntity user = userOpt.get();
        
        // Find TOTP authentication method
        List<AuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndTypeAndIsEnabled(user.getId(), AuthenticationMethodType.TOTP, true);
        
        if (totpMethods.isEmpty()) {
            return false;
        }
        
        AuthenticationMethodEntity totpAuth = totpMethods.get(0);
        // Check if TOTP is configured and verified
        return Boolean.TRUE.equals(totpAuth.getMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFIED));
    }
    
    /**
     * Disable TOTP for a user
     * @param username the username
     */
    public void disableTotp(String username) {
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return;
        }
        UserEntity user = userOpt.get();
        
        // Remove TOTP authentication methods
        authMethodRepository.deleteByUserIdAndType(user.getId(), AuthenticationMethodType.TOTP);
        log.info("TOTP disabled for user {}", username);
    }
    
    /**
     * Helper method to find the TOTP authentication method for a user
     * @param user the user
     * @return the TOTP authentication method, or null if not found
     */
    private AuthenticationMethodEntity findTotpAuthMethod(UserEntity user) {
        List<AuthenticationMethodEntity> authMethods = authMethodRepository.findByUserIdAndType(
            user.getId(), AuthenticationMethodType.TOTP
        );
        
        // Return the first enabled TOTP method
        return authMethods.stream()
            .filter(method -> Boolean.TRUE.equals(method.getIsActive()))
            .findFirst()
            .orElse(null);
    }
}
