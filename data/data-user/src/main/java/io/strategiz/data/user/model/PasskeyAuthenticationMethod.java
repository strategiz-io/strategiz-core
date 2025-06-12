package io.strategiz.data.user.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Passkey/WebAuthn authentication method.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PasskeyAuthenticationMethod extends AuthenticationMethod {
    private String credentialId;
    private String publicKey;
    private Long counter = 0L;
    
    public PasskeyAuthenticationMethod(String name, String credentialId, String publicKey, String createdBy) {
        this.setType("PASSKEY");
        this.setName(name);
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.setCreatedBy(createdBy);
        this.setModifiedBy(createdBy);
    }
}
