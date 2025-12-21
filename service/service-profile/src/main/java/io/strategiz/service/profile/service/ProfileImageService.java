package io.strategiz.service.profile.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.profile.exception.ProfileErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing profile image uploads to Firebase Storage.
 */
@Service
public class ProfileImageService {

	private static final Logger log = LoggerFactory.getLogger(ProfileImageService.class);

	private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

	private static final String[] ALLOWED_CONTENT_TYPES = { "image/jpeg", "image/png", "image/gif", "image/webp" };

	private final UserRepository userRepository;

	@Value("${firebase.storage.bucket:strategiz-io.firebasestorage.app}")
	private String storageBucket;

	public ProfileImageService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Uploads a profile image for a user and updates their profile.
	 * @param userId the user ID
	 * @param file the image file to upload
	 * @return the public URL of the uploaded image
	 */
	public String uploadProfileImage(String userId, MultipartFile file) {
		log.info("Uploading profile image for user: {}", userId);

		// Validate file
		validateFile(file);

		// Get user and profile
		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "User not found: " + userId);
		}

		UserEntity user = userOpt.get();
		UserProfileEntity profile = user.getProfile();
		if (profile == null) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "Profile not found for user: " + userId);
		}

		try {
			// Get Firebase Storage bucket
			Bucket bucket = StorageClient.getInstance().bucket(storageBucket);

			// Generate unique filename
			String extension = getFileExtension(file.getOriginalFilename());
			String filename = String.format("profile-images/%s/%s%s", userId, UUID.randomUUID(), extension);

			// Delete old image if exists (before uploading new one)
			String oldImageUrl = profile.getPhotoURL();
			deleteOldImage(bucket, oldImageUrl);

			// Upload to Firebase Storage
			Blob blob = bucket.create(filename, file.getBytes(), file.getContentType());

			// Make the blob publicly readable
			blob.createAcl(com.google.cloud.storage.Acl.of(com.google.cloud.storage.Acl.User.ofAllUsers(),
					com.google.cloud.storage.Acl.Role.READER));

			// Generate public URL using Firebase Storage URL format
			String encodedPath = URLEncoder.encode(filename, StandardCharsets.UTF_8);
			String imageUrl = String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
					storageBucket, encodedPath);

			// Update profile with new image URL
			profile.setPhotoURL(imageUrl);
			userRepository.save(user);

			log.info("Profile image uploaded successfully for user: {}. URL: {}", userId, imageUrl);
			return imageUrl;
		}
		catch (IOException e) {
			log.error("Failed to upload profile image for user: {}", userId, e);
			throw new StrategizException(ProfileErrors.PROFILE_UPDATE_FAILED, "Failed to upload profile image");
		}
	}

	/**
	 * Validates the uploaded file.
	 */
	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new StrategizException(ProfileErrors.PROFILE_UPDATE_FAILED, "No file provided");
		}

		if (file.getSize() > MAX_FILE_SIZE) {
			throw new StrategizException(ProfileErrors.PROFILE_UPDATE_FAILED,
					"File size exceeds maximum limit of 5MB");
		}

		String contentType = file.getContentType();
		boolean isValidType = false;
		for (String allowedType : ALLOWED_CONTENT_TYPES) {
			if (allowedType.equals(contentType)) {
				isValidType = true;
				break;
			}
		}

		if (!isValidType) {
			throw new StrategizException(ProfileErrors.PROFILE_UPDATE_FAILED,
					"Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
		}
	}

	/**
	 * Gets the file extension from the filename.
	 */
	private String getFileExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return ".jpg"; // Default extension
		}
		return filename.substring(filename.lastIndexOf("."));
	}

	/**
	 * Deletes the old profile image from Firebase Storage if it exists.
	 */
	private void deleteOldImage(Bucket bucket, String oldImageUrl) {
		if (oldImageUrl == null || oldImageUrl.isEmpty()) {
			return;
		}

		// Only delete if it's from our Firebase Storage
		if (!oldImageUrl.contains("firebasestorage.googleapis.com") || !oldImageUrl.contains(storageBucket)) {
			return;
		}

		try {
			// Extract the path from the URL
			// URL format:
			// https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{encoded-path}?alt=media
			String marker = "/o/";
			int startIdx = oldImageUrl.indexOf(marker);
			if (startIdx == -1) {
				return;
			}

			String encodedPath = oldImageUrl.substring(startIdx + marker.length());
			int queryIdx = encodedPath.indexOf("?");
			if (queryIdx != -1) {
				encodedPath = encodedPath.substring(0, queryIdx);
			}

			// Decode the path
			String path = java.net.URLDecoder.decode(encodedPath, StandardCharsets.UTF_8);

			// Delete the blob
			Blob blob = bucket.get(path);
			if (blob != null && blob.exists()) {
				blob.delete();
				log.info("Deleted old profile image: {}", path);
			}
		}
		catch (Exception e) {
			// Log but don't fail - the old image might not exist
			log.warn("Failed to delete old profile image: {}", oldImageUrl, e);
		}
	}

}
