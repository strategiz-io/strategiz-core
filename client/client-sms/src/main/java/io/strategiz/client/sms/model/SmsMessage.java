package io.strategiz.client.sms.model;

/**
 * Model representing an SMS message to be sent.
 */
public class SmsMessage {

	private final String toPhoneNumber;

	private final String body;

	private String fromPhoneNumber;

	private String messagingServiceSid;

	/**
	 * Create an SMS message.
	 * @param toPhoneNumber The recipient phone number in E.164 format (e.g.,
	 * +14155551234)
	 * @param body The message body (max 1600 characters for Twilio)
	 */
	public SmsMessage(String toPhoneNumber, String body) {
		this.toPhoneNumber = toPhoneNumber;
		this.body = body;
	}

	public String getToPhoneNumber() {
		return toPhoneNumber;
	}

	public String getBody() {
		return body;
	}

	public String getFromPhoneNumber() {
		return fromPhoneNumber;
	}

	public void setFromPhoneNumber(String fromPhoneNumber) {
		this.fromPhoneNumber = fromPhoneNumber;
	}

	public String getMessagingServiceSid() {
		return messagingServiceSid;
	}

	public void setMessagingServiceSid(String messagingServiceSid) {
		this.messagingServiceSid = messagingServiceSid;
	}

	@Override
	public String toString() {
		return "SmsMessage{" + "to='" + maskPhoneNumber(toPhoneNumber) + '\'' + ", bodyLength="
				+ (body != null ? body.length() : 0) + '}';
	}

	/**
	 * Mask phone number for secure logging.
	 */
	private String maskPhoneNumber(String phone) {
		if (phone == null || phone.length() < 4) {
			return "****";
		}
		return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
	}

}
