package io.strategiz.data.waitlist.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

/**
 * Waitlist entry for pre-launch signups. Stored in waitlist/{id} collection.
 */
@Collection("waitlist")
public class WaitlistEntity extends BaseEntity {

	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	private String id;

	@PropertyName("email")
	@JsonProperty("email")
	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String email;

	@PropertyName("emailHash")
	@JsonProperty("emailHash")
	private String emailHash; // SHA-256 hash for duplicate detection

	@PropertyName("signupDate")
	@JsonProperty("signupDate")
	private Timestamp signupDate;

	@PropertyName("ipAddress")
	@JsonProperty("ipAddress")
	private String ipAddress; // For abuse prevention

	@PropertyName("userAgent")
	@JsonProperty("userAgent")
	private String userAgent;

	@PropertyName("referralSource")
	@JsonProperty("referralSource")
	private String referralSource; // UTM source tracking

	@PropertyName("confirmed")
	@JsonProperty("confirmed")
	private boolean confirmed = false;

	@PropertyName("confirmedDate")
	@JsonProperty("confirmedDate")
	private Timestamp confirmedDate;

	@PropertyName("converted")
	@JsonProperty("converted")
	private boolean converted = false; // Did they sign up after launch?

	@PropertyName("convertedDate")
	@JsonProperty("convertedDate")
	private Timestamp convertedDate;

	// Constructors
	public WaitlistEntity() {
		super();
	}

	public WaitlistEntity(String email, String emailHash) {
		super();
		this.email = email;
		this.emailHash = emailHash;
		this.signupDate = Timestamp.now();
		this.confirmed = false;
		this.converted = false;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmailHash() {
		return emailHash;
	}

	public void setEmailHash(String emailHash) {
		this.emailHash = emailHash;
	}

	public Timestamp getSignupDate() {
		return signupDate;
	}

	public void setSignupDate(Timestamp signupDate) {
		this.signupDate = signupDate;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getReferralSource() {
		return referralSource;
	}

	public void setReferralSource(String referralSource) {
		this.referralSource = referralSource;
	}

	public boolean isConfirmed() {
		return confirmed;
	}

	public void setConfirmed(boolean confirmed) {
		this.confirmed = confirmed;
	}

	public Timestamp getConfirmedDate() {
		return confirmedDate;
	}

	public void setConfirmedDate(Timestamp confirmedDate) {
		this.confirmedDate = confirmedDate;
	}

	public boolean isConverted() {
		return converted;
	}

	public void setConverted(boolean converted) {
		this.converted = converted;
	}

	public Timestamp getConvertedDate() {
		return convertedDate;
	}

	public void setConvertedDate(Timestamp convertedDate) {
		this.convertedDate = convertedDate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		WaitlistEntity that = (WaitlistEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(emailHash, that.emailHash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id, emailHash);
	}

	@Override
	public String toString() {
		return "WaitlistEntity{" + "id='" + id + '\'' + ", emailHash='"
				+ (emailHash != null ? emailHash.substring(0, 8) + "..." : null) + '\'' + ", signupDate=" + signupDate
				+ ", confirmed=" + confirmed + ", converted=" + converted + '}';
	}

}
