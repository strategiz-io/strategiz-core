package io.strategiz.client.fcm.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Push notification message model for alert notifications.
 */
public class PushMessage {

	private String deviceToken;

	private String title;

	private String body;

	private String imageUrl;

	private String clickAction;

	private Map<String, String> data;

	// Android-specific
	private String androidChannelId;

	private Priority priority;

	// iOS-specific
	private String iosBadge;

	private String iosSound;

	public enum Priority {

		HIGH, NORMAL

	}

	private PushMessage() {
	}

	public String getDeviceToken() {
		return deviceToken;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getClickAction() {
		return clickAction;
	}

	public Map<String, String> getData() {
		return data;
	}

	public String getAndroidChannelId() {
		return androidChannelId;
	}

	public Priority getPriority() {
		return priority;
	}

	public String getIosBadge() {
		return iosBadge;
	}

	public String getIosSound() {
		return iosSound;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final PushMessage message = new PushMessage();

		public Builder deviceToken(String deviceToken) {
			message.deviceToken = deviceToken;
			return this;
		}

		public Builder title(String title) {
			message.title = title;
			return this;
		}

		public Builder body(String body) {
			message.body = body;
			return this;
		}

		public Builder imageUrl(String imageUrl) {
			message.imageUrl = imageUrl;
			return this;
		}

		public Builder clickAction(String clickAction) {
			message.clickAction = clickAction;
			return this;
		}

		public Builder data(Map<String, String> data) {
			message.data = data;
			return this;
		}

		public Builder addData(String key, String value) {
			if (message.data == null) {
				message.data = new HashMap<>();
			}
			message.data.put(key, value);
			return this;
		}

		public Builder androidChannelId(String channelId) {
			message.androidChannelId = channelId;
			return this;
		}

		public Builder priority(Priority priority) {
			message.priority = priority;
			return this;
		}

		public Builder iosBadge(String badge) {
			message.iosBadge = badge;
			return this;
		}

		public Builder iosSound(String sound) {
			message.iosSound = sound;
			return this;
		}

		public PushMessage build() {
			if (message.title == null || message.title.isEmpty()) {
				throw new IllegalArgumentException("title is required");
			}
			if (message.body == null || message.body.isEmpty()) {
				throw new IllegalArgumentException("body is required");
			}
			if (message.priority == null) {
				message.priority = Priority.HIGH;
			}
			return message;
		}

	}

}
