package io.strategiz.business.fundamentals.model;

/**
 * Result of processing a single symbol during fundamentals collection.
 */
public class SymbolResult {

	private String symbol;

	private boolean success;

	private String errorMessage;

	public SymbolResult(String symbol, boolean success) {
		this.symbol = symbol;
		this.success = success;
	}

	public SymbolResult(String symbol, boolean success, String errorMessage) {
		this.symbol = symbol;
		this.success = success;
		this.errorMessage = errorMessage;
	}

	// Getters and Setters

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString() {
		return success ? String.format("SymbolResult[%s: SUCCESS]", symbol)
				: String.format("SymbolResult[%s: FAILED - %s]", symbol, errorMessage);
	}

}
