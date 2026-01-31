package io.strategiz.service.dashboard.model.response;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Response object for watchlist operations (create, update, delete)
 */
public class WatchlistOperationResponse {

	private Boolean success;

	private String operation; // CREATE, UPDATE, DELETE

	private String id;

	private String symbol;

	private String message;

	private String errorCode;

	private LocalDateTime timestamp;

	// Constructors
	public WatchlistOperationResponse() {
		this.timestamp = LocalDateTime.now();
	}

	public WatchlistOperationResponse(Boolean success, String operation, String message) {
		this.success = success;
		this.operation = operation;
		this.message = message;
		this.timestamp = LocalDateTime.now();
	}

	public WatchlistOperationResponse(Boolean success, String operation, String id, String symbol, String message) {
		this.success = success;
		this.operation = operation;
		this.id = id;
		this.symbol = symbol;
		this.message = message;
		this.timestamp = LocalDateTime.now();
	}

	// Static factory methods
	public static WatchlistOperationResponse success(String operation, String message) {
		return new WatchlistOperationResponse(true, operation, message);
	}

	public static WatchlistOperationResponse success(String operation, String id, String symbol, String message) {
		return new WatchlistOperationResponse(true, operation, id, symbol, message);
	}

	public static WatchlistOperationResponse failure(String operation, String message) {
		return new WatchlistOperationResponse(false, operation, message);
	}

	public static WatchlistOperationResponse failure(String operation, String message, String errorCode) {
		WatchlistOperationResponse response = new WatchlistOperationResponse(false, operation, message);
		response.setErrorCode(errorCode);
		return response;
	}

	// Getters and Setters
	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	// Convenience methods
	public boolean isSuccess() {
		return Boolean.TRUE.equals(success);
	}

	public boolean isFailure() {
		return !isSuccess();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		WatchlistOperationResponse that = (WatchlistOperationResponse) o;
		return Objects.equals(success, that.success) && Objects.equals(operation, that.operation)
				&& Objects.equals(id, that.id) && Objects.equals(symbol, that.symbol)
				&& Objects.equals(message, that.message) && Objects.equals(errorCode, that.errorCode)
				&& Objects.equals(timestamp, that.timestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(success, operation, id, symbol, message, errorCode, timestamp);
	}

	@Override
	public String toString() {
		return "WatchlistOperationResponse{" + "success=" + success + ", operation='" + operation + '\'' + ", id='" + id
				+ '\'' + ", symbol='" + symbol + '\'' + ", message='" + message + '\'' + ", errorCode='" + errorCode
				+ '\'' + ", timestamp=" + timestamp + '}';
	}

}