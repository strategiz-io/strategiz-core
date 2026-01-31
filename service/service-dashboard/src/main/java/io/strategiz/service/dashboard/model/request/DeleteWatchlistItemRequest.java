package io.strategiz.service.dashboard.model.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request object for deleting a watchlist item
 */
public class DeleteWatchlistItemRequest {

	@NotBlank(message = "Item ID is required")
	private String itemId;

	private String symbol; // Optional, for validation or logging

	// Constructors
	public DeleteWatchlistItemRequest() {
	}

	public DeleteWatchlistItemRequest(String itemId) {
		this.itemId = itemId;
	}

	public DeleteWatchlistItemRequest(String itemId, String symbol) {
		this.itemId = itemId;
		this.symbol = symbol;
	}

	// Static factory methods
	public static DeleteWatchlistItemRequest forId(String itemId) {
		return new DeleteWatchlistItemRequest(itemId);
	}

	public static DeleteWatchlistItemRequest forSymbol(String itemId, String symbol) {
		return new DeleteWatchlistItemRequest(itemId, symbol);
	}

	// Getters and Setters
	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public String toString() {
		return "DeleteWatchlistItemRequest{" + "itemId='" + itemId + '\'' + ", symbol='" + symbol + '\'' + '}';
	}

}