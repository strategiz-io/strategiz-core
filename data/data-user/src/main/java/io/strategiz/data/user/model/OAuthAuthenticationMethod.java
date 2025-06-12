package io.strategiz.data.user.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * OAuth authentication method for services like Google and Facebook.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OAuthAuthenticationMethod extends AuthenticationMethod {
    private String provider; // "GOOGLE" or "FACEBOOK"
    private String uid; // Provider-specific user ID
    private String email; // Email from provider
    
    public OAuthAuthenticationMethod(String provider, String uid, String email, String createdBy) {
        this.setType("OAUTH_" + provider.toUpperCase());
        this.setName(provider + " Account");
        this.provider = provider;
        this.uid = uid;
        this.email = email;
        this.setCreatedBy(createdBy);
        this.setModifiedBy(createdBy);
    }
}
