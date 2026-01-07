package io.strategiz.client.sendgrid.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Email message model for alert notifications.
 */
public class EmailMessage {

	private String toEmail;

	private String toName;

	private String fromEmail;

	private String fromName;

	private String subject;

	private String bodyText;

	private String bodyHtml;

	private String templateId;

	private Map<String, Object> templateData;

	private EmailMessage() {
	}

	public String getToEmail() {
		return toEmail;
	}

	public String getToName() {
		return toName;
	}

	public String getFromEmail() {
		return fromEmail;
	}

	public String getFromName() {
		return fromName;
	}

	public String getSubject() {
		return subject;
	}

	public String getBodyText() {
		return bodyText;
	}

	public String getBodyHtml() {
		return bodyHtml;
	}

	public String getTemplateId() {
		return templateId;
	}

	public Map<String, Object> getTemplateData() {
		return templateData;
	}

	public boolean hasTemplate() {
		return templateId != null && !templateId.isEmpty();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final EmailMessage message = new EmailMessage();

		public Builder toEmail(String toEmail) {
			message.toEmail = toEmail;
			return this;
		}

		public Builder toName(String toName) {
			message.toName = toName;
			return this;
		}

		public Builder fromEmail(String fromEmail) {
			message.fromEmail = fromEmail;
			return this;
		}

		public Builder fromName(String fromName) {
			message.fromName = fromName;
			return this;
		}

		public Builder subject(String subject) {
			message.subject = subject;
			return this;
		}

		public Builder bodyText(String bodyText) {
			message.bodyText = bodyText;
			return this;
		}

		public Builder bodyHtml(String bodyHtml) {
			message.bodyHtml = bodyHtml;
			return this;
		}

		public Builder templateId(String templateId) {
			message.templateId = templateId;
			return this;
		}

		public Builder templateData(Map<String, Object> templateData) {
			message.templateData = templateData;
			return this;
		}

		public Builder addTemplateData(String key, Object value) {
			if (message.templateData == null) {
				message.templateData = new HashMap<>();
			}
			message.templateData.put(key, value);
			return this;
		}

		public EmailMessage build() {
			if (message.toEmail == null || message.toEmail.isEmpty()) {
				throw new IllegalArgumentException("toEmail is required");
			}
			return message;
		}

	}

}
