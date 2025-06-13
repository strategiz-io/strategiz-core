package io.strategiz.data.user.model;

import java.util.Date;
import java.util.Objects;

public class PasswordAuthenticationMethod extends AuthenticationMethod {
    private String hash;

    // No-argument constructor
    public PasswordAuthenticationMethod() {
        super();
    }

    // Custom constructor (preserved from original)
    public PasswordAuthenticationMethod(String name, String hash) {
        super(); // Call superclass's NoArgsConstructor
        setType("PASSWORD"); // Standardize the type
        setName(name);
        this.hash = hash;
    }

    // All-arguments constructor (including inherited fields)
    public PasswordAuthenticationMethod(String id, String type, String name, Date lastVerifiedAt, String createdBy, Date createdAt, String modifiedBy, Date modifiedAt, Integer version, Boolean isActive, String hash) {
        super(id, type, name, lastVerifiedAt, createdBy, createdAt, modifiedBy, modifiedAt, version, isActive);
        this.hash = hash;
    }

    // Getter and Setter for hash
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false; // Call super.equals()
        PasswordAuthenticationMethod that = (PasswordAuthenticationMethod) o;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), hash); // Include super.hashCode()
    }

    @Override
    public String toString() {
        String superString = super.toString();
        // Assumes super.toString() ends with '}'
        String content = superString.substring(0, superString.length() - 1);
        return content +
               ", hash='" + hash + '\'' +
               '}';
    }
}
