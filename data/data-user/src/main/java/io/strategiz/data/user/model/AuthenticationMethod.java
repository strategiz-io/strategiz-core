package io.strategiz.data.user.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Base class for authentication methods in the authentication_methods subcollection.
 */
@Data
@NoArgsConstructor
public class AuthenticationMethod {
    private String id;
    private String type; // TOTP, SMS_OTP, PASSKEY, OAUTH_GOOGLE, OAUTH_FACEBOOK
    private String name; // Friendly name for the auth method
    private Date lastVerifiedAt;
    
    // Audit fields
    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
    private Integer version = 1;
    private Boolean isActive = true;
}
