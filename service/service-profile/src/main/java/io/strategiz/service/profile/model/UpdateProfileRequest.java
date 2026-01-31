package io.strategiz.service.profile.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * User profile request DTO for creating/updating user profiles
 */
public class UpdateProfileRequest {

	@NotBlank(message = "Name is required")
	@Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
	private String name;

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	private String email;

	private String photoURL;

	private String subscriptionTier;

	private Boolean demoMode;

	@Size(max = 500, message = "Bio must be less than 500 characters")
	private String bio;

	@Size(max = 100, message = "Location must be less than 100 characters")
	private String location;

	@Size(max = 100, message = "Occupation must be less than 100 characters")
	private String occupation;

	@Size(max = 100, message = "Education must be less than 100 characters")
	private String education;

	// Default constructor
	public UpdateProfileRequest() {
	}

	// Constructor with all fields
	public UpdateProfileRequest(String name, String email, String photoURL, String subscriptionTier, Boolean demoMode) {
		this.name = name;
		this.email = email;
		this.photoURL = photoURL;
		this.subscriptionTier = subscriptionTier;
		this.demoMode = demoMode;
	}

	// Getters and setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhotoURL() {
		return photoURL;
	}

	public void setPhotoURL(String photoURL) {
		this.photoURL = photoURL;
	}

	public String getSubscriptionTier() {
		return subscriptionTier;
	}

	public void setSubscriptionTier(String subscriptionTier) {
		this.subscriptionTier = subscriptionTier;
	}

	public Boolean getDemoMode() {
		return demoMode;
	}

	public void setDemoMode(Boolean demoMode) {
		this.demoMode = demoMode;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getOccupation() {
		return occupation;
	}

	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	public String getEducation() {
		return education;
	}

	public void setEducation(String education) {
		this.education = education;
	}

}
