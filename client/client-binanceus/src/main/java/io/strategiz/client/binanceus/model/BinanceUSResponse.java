package io.strategiz.client.binanceus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Base response class for Binance US API responses
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceUSResponse {

	private boolean success;

	private String message;

	// Constructors
	public BinanceUSResponse() {
	}

	public BinanceUSResponse(boolean success, String message) {
		this.success = success;
		this.message = message;
	}

	// Getters and setters
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BinanceUSResponse that = (BinanceUSResponse) o;
		return success == that.success && Objects.equals(message, that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(success, message);
	}

	@Override
	public String toString() {
		return "BinanceUSResponse{" + "success=" + success + ", message='" + message + '\'' + '}';
	}

}
