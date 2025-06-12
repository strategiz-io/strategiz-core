package io.strategiz.data.user.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * SMS One-Time Password authentication method.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SmsOtpAuthenticationMethod extends AuthenticationMethod {
    private String phoneNumber;
    private Boolean verified = false;
    
    public SmsOtpAuthenticationMethod(String name, String phoneNumber, Boolean verified, String createdBy) {
        this.setType("SMS_OTP");
        this.setName(name);
        this.phoneNumber = phoneNumber;
        this.verified = verified;
        this.setCreatedBy(createdBy);
        this.setModifiedBy(createdBy);
    }
}
