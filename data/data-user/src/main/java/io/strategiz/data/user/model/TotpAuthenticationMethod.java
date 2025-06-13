package io.strategiz.data.user.model;

import java.util.Date;
import java.util.Objects;

/**
 * Time-based One-Time Password (TOTP) authentication method.
 */
public class TotpAuthenticationMethod extends AuthenticationMethod {
    private String secret; // Encrypted TOTP secret

    // No-argument constructor
    public TotpAuthenticationMethod() {
        super();
    }

    // Custom constructor (preserved and updated to call super constructor properly)
    public TotpAuthenticationMethod(String name, String secret, String createdBy) {
        super(); // Call superclass's NoArgsConstructor
        this.setType("TOTP");
        this.setName(name);
        this.setCreatedBy(createdBy);
        this.setModifiedBy(createdBy); // Assuming modifiedBy should also be set
        // Note: createdAt and modifiedAt would typically be set upon creation/modification
        // For simplicity, we're keeping the original logic here.
        this.secret = secret;
    }

    // All-arguments constructor (including inherited fields)
    public TotpAuthenticationMethod(String id, String type, String name, Date lastVerifiedAt, String createdBy, Date createdAt, String modifiedBy, Date modifiedAt, Integer version, Boolean isActive, String secret) {
        super(id, type, name, lastVerifiedAt, createdBy, createdAt, modifiedBy, modifiedAt, version, isActive);
        this.secret = secret;
    }

    // Getter and Setter for secret
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TotpAuthenticationMethod that = (TotpAuthenticationMethod) o;
        return Objects.equals(secret, that.secret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), secret);
    }

    @Override
    public String toString() {
        String superString = super.toString();
        String content = superString.substring(0, superString.length() - 1);
        return content +
               ", secret='" + secret + '\'' +
               '}';
    }
}
