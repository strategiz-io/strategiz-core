package io.strategiz.service.auth.model.smsotp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for sending SMS OTP
 * 
 * @param phoneNumber The phone number to send OTP to (E.164 format)
 * @param ipAddress The IP address of the requester (for audit/security)
 * @param countryCode The country code for SMS routing (ISO 3166-1 alpha-2)
 */
public record SmsOtpSendRequest(
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format (e.g., +1234567890)")
    String phoneNumber,
    
    String ipAddress,
    
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be a valid ISO 3166-1 alpha-2 code")
    String countryCode
) {
    /**
     * Constructor with automatic country code detection
     * 
     * @param phoneNumber The phone number in E.164 format
     * @param ipAddress The IP address of the requester
     */
    public SmsOtpSendRequest(String phoneNumber, String ipAddress) {
        this(phoneNumber, ipAddress, detectCountryFromPhoneNumber(phoneNumber));
    }
    
    /**
     * Simple country code detection based on phone number prefix
     * This is a basic implementation - in production you'd use a proper phone number library
     * 
     * @param phoneNumber The phone number in E.164 format
     * @return The detected country code
     */
    private static String detectCountryFromPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "US";
        
        if (phoneNumber.startsWith("+1")) return "US"; // US/Canada
        if (phoneNumber.startsWith("+44")) return "GB"; // UK
        if (phoneNumber.startsWith("+33")) return "FR"; // France
        if (phoneNumber.startsWith("+49")) return "DE"; // Germany
        if (phoneNumber.startsWith("+81")) return "JP"; // Japan
        if (phoneNumber.startsWith("+86")) return "CN"; // China
        if (phoneNumber.startsWith("+91")) return "IN"; // India
        if (phoneNumber.startsWith("+61")) return "AU"; // Australia
        if (phoneNumber.startsWith("+55")) return "BR"; // Brazil
        
        return "US"; // Default to US
    }
    
    /**
     * Mask the phone number for security/privacy in logs
     * 
     * @return Masked phone number (e.g., "+1234***7890")
     */
    public String getMaskedPhoneNumber() {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***-***-****";
        }
        
        String countryCode = phoneNumber.substring(0, phoneNumber.length() - 7);
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return countryCode + "***" + lastFour;
    }
}