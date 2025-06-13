package io.strategiz.data.user.model;

import java.util.Date;
import java.util.Objects;

/**
 * Passkey/WebAuthn authentication method.
 */
public class PasskeyAuthenticationMethod extends AuthenticationMethod {
    private String credentialId;
    private String publicKey;
    private Long counter = 0L;

    // No-argument constructor
    public PasskeyAuthenticationMethod() {
        super();
    }

    // Custom constructor (preserved and updated)
    public PasskeyAuthenticationMethod(String name, String credentialId, String publicKey, String createdBy) {
        super();
        this.setType("PASSKEY");
        this.setName(name);
        this.setCreatedBy(createdBy);
        this.setModifiedBy(createdBy);
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        // counter will use its default value 0L unless set otherwise
    }

    // All-arguments constructor (including inherited fields)
    public PasskeyAuthenticationMethod(String id, String type, String name, Date lastVerifiedAt, String createdBy, Date createdAt, String modifiedBy, Date modifiedAt, Integer version, Boolean isActive, String credentialId, String publicKey, Long counter) {
        super(id, type, name, lastVerifiedAt, createdBy, createdAt, modifiedBy, modifiedAt, version, isActive);
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.counter = counter;
    }

    // Getters and Setters
    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public Long getCounter() {
        return counter;
    }

    public void setCounter(Long counter) {
        this.counter = counter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PasskeyAuthenticationMethod that = (PasskeyAuthenticationMethod) o;
        return Objects.equals(credentialId, that.credentialId) &&
               Objects.equals(publicKey, that.publicKey) &&
               Objects.equals(counter, that.counter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), credentialId, publicKey, counter);
    }

    @Override
    public String toString() {
        String superString = super.toString();
        String content = superString.substring(0, superString.length() - 1);
        return content +
               ", credentialId='" + credentialId + '\'' +
               ", publicKey='" + publicKey + '\'' +
               ", counter=" + counter +
               '}';
    }
}
