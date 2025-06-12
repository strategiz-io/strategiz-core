package io.strategiz.data.user.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Time-based One-Time Password (TOTP) authentication method.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TotpAuthenticationMethod extends AuthenticationMethod {
    private String secret; // Encrypted TOTP secret
    
    public TotpAuthenticationMethod(String name, String secret, String createdBy) {
        this.setType("TOTP");
        this.setName(name);
        this.secret = secret;
        this.setCreatedBy(createdBy);
        this.setModifiedBy(createdBy);
    }
}
