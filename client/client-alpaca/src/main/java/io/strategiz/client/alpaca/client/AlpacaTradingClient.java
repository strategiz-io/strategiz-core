package io.strategiz.client.alpaca.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.client.alpaca.error.AlpacaErrors;
import io.strategiz.framework.exception.StrategizException;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for executing trades via Alpaca API. Supports both live and paper trading.
 */
@Component
public class AlpacaTradingClient {

	private static final Logger log = LoggerFactory.getLogger(AlpacaTradingClient.class);

	private final RestTemplate restTemplate;

	private final ObjectMapper objectMapper;

	@Value("${oauth.providers.alpaca.api-url:https://api.alpaca.markets}")
	private String liveApiUrl;

	@Value("${oauth.providers.alpaca.paper-api-url:https://paper-api.alpaca.markets}")
	private String paperApiUrl;

	public AlpacaTradingClient(@Qualifier("alpacaRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		this.objectMapper = new ObjectMapper();
		log.info("AlpacaTradingClient initialized");
	}

	/**
	 * Place a market order.
	 * @param accessToken OAuth access token
	 * @param symbol Stock symbol (e.g., "AAPL")
	 * @param side Order side ("buy" or "sell")
	 * @param qty Quantity of shares
	 * @param isPaper Whether to use paper trading
	 * @return Order result with orderId, status, filledQty, filledAvgPrice
	 */
	public OrderResult placeMarketOrder(String accessToken, String symbol, String side, double qty, boolean isPaper) {
		log.info("Placing {} market order: {} {} shares (paper={})", side, symbol, qty, isPaper);

		Map<String, Object> orderRequest = new HashMap<>();
		orderRequest.put("symbol", symbol.toUpperCase());
		orderRequest.put("side", side.toLowerCase());
		orderRequest.put("type", "market");
		orderRequest.put("time_in_force", "day");

		// Handle fractional shares
		if (qty == Math.floor(qty)) {
			orderRequest.put("qty", String.valueOf((int) qty));
		}
		else {
			orderRequest.put("notional", String.format("%.2f", qty)); // For fractional,
																		// use notional
		}

		return submitOrder(accessToken, orderRequest, isPaper);
	}

	/**
	 * Place a limit order.
	 * @param accessToken OAuth access token
	 * @param symbol Stock symbol
	 * @param side Order side ("buy" or "sell")
	 * @param qty Quantity of shares
	 * @param limitPrice Limit price
	 * @param isPaper Whether to use paper trading
	 * @return Order result
	 */
	public OrderResult placeLimitOrder(String accessToken, String symbol, String side, double qty, double limitPrice,
			boolean isPaper) {
		log.info("Placing {} limit order: {} {} shares @ {} (paper={})", side, symbol, qty, limitPrice, isPaper);

		Map<String, Object> orderRequest = new HashMap<>();
		orderRequest.put("symbol", symbol.toUpperCase());
		orderRequest.put("side", side.toLowerCase());
		orderRequest.put("type", "limit");
		orderRequest.put("limit_price", String.valueOf(limitPrice));
		orderRequest.put("time_in_force", "day");
		orderRequest.put("qty", String.valueOf((int) qty));

		return submitOrder(accessToken, orderRequest, isPaper);
	}

	/**
	 * Place a stop-loss order.
	 * @param accessToken OAuth access token
	 * @param symbol Stock symbol
	 * @param side Order side ("buy" or "sell")
	 * @param qty Quantity of shares
	 * @param stopPrice Stop price that triggers the order
	 * @param isPaper Whether to use paper trading
	 * @return Order result
	 */
	public OrderResult placeStopOrder(String accessToken, String symbol, String side, double qty, double stopPrice,
			boolean isPaper) {
		log.info("Placing {} stop order: {} {} shares with stop @ {} (paper={})", side, symbol, qty, stopPrice,
				isPaper);

		Map<String, Object> orderRequest = new HashMap<>();
		orderRequest.put("symbol", symbol.toUpperCase());
		orderRequest.put("side", side.toLowerCase());
		orderRequest.put("type", "stop");
		orderRequest.put("stop_price", String.valueOf(stopPrice));
		orderRequest.put("time_in_force", "day");
		orderRequest.put("qty", String.valueOf((int) qty));

		return submitOrder(accessToken, orderRequest, isPaper);
	}

	/**
	 * Cancel an order.
	 * @param accessToken OAuth access token
	 * @param orderId Order ID to cancel
	 * @param isPaper Whether the order was placed via paper trading
	 * @return true if cancelled successfully
	 */
	public boolean cancelOrder(String accessToken, String orderId, boolean isPaper) {
		log.info("Cancelling order: {} (paper={})", orderId, isPaper);

		try {
			String apiUrl = isPaper ? paperApiUrl : liveApiUrl;
			URI uri = new URIBuilder(apiUrl + "/v2/orders/" + orderId).build();

			HttpHeaders headers = createHeaders(accessToken);
			HttpEntity<String> entity = new HttpEntity<>(headers);

			restTemplate.exchange(uri, HttpMethod.DELETE, entity, Void.class);

			log.info("Successfully cancelled order: {}", orderId);
			return true;

		}
		catch (RestClientResponseException e) {
			log.error("Failed to cancel order {}: {} - {}", orderId, e.getStatusCode(), e.getResponseBodyAsString());
			return false;
		}
		catch (Exception e) {
			log.error("Error cancelling order {}: {}", orderId, e.getMessage());
			return false;
		}
	}

	/**
	 * Get order status.
	 * @param accessToken OAuth access token
	 * @param orderId Order ID
	 * @param isPaper Whether the order was placed via paper trading
	 * @return Order details
	 */
	public Map<String, Object> getOrder(String accessToken, String orderId, boolean isPaper) {
		try {
			String apiUrl = isPaper ? paperApiUrl : liveApiUrl;
			URI uri = new URIBuilder(apiUrl + "/v2/orders/" + orderId).build();

			HttpHeaders headers = createHeaders(accessToken);
			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<Object> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Object.class);

			if (response.getBody() instanceof Map) {
				return (Map<String, Object>) response.getBody();
			}
			return Collections.emptyMap();

		}
		catch (Exception e) {
			log.error("Error getting order {}: {}", orderId, e.getMessage());
			throw new StrategizException(AlpacaErrors.ALPACA_API_ERROR, "Failed to get order: " + e.getMessage());
		}
	}

	/**
	 * Close a position (liquidate all shares).
	 * @param accessToken OAuth access token
	 * @param symbol Stock symbol
	 * @param isPaper Whether to use paper trading
	 * @return Order result from the closing order
	 */
	public OrderResult closePosition(String accessToken, String symbol, boolean isPaper) {
		log.info("Closing position for {} (paper={})", symbol, isPaper);

		try {
			String apiUrl = isPaper ? paperApiUrl : liveApiUrl;
			URI uri = new URIBuilder(apiUrl + "/v2/positions/" + symbol.toUpperCase()).build();

			HttpHeaders headers = createHeaders(accessToken);
			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<Object> response = restTemplate.exchange(uri, HttpMethod.DELETE, entity, Object.class);

			if (response.getBody() instanceof Map) {
				Map<String, Object> orderData = (Map<String, Object>) response.getBody();
				return mapToOrderResult(orderData);
			}

			return OrderResult.failed("Empty response from close position");

		}
		catch (RestClientResponseException e) {
			String error = String.format("Failed to close position %s: %s", symbol, e.getResponseBodyAsString());
			log.error(error);
			return OrderResult.failed(error);
		}
		catch (Exception e) {
			log.error("Error closing position for {}: {}", symbol, e.getMessage());
			return OrderResult.failed(e.getMessage());
		}
	}

	/**
	 * Submit an order to Alpaca.
	 */
	private OrderResult submitOrder(String accessToken, Map<String, Object> orderRequest, boolean isPaper) {
		try {
			String apiUrl = isPaper ? paperApiUrl : liveApiUrl;
			URI uri = new URIBuilder(apiUrl + "/v2/orders").build();

			HttpHeaders headers = createHeaders(accessToken);
			String body = objectMapper.writeValueAsString(orderRequest);

			HttpEntity<String> entity = new HttpEntity<>(body, headers);

			log.debug("Submitting order to {}: {}", uri, body);

			ResponseEntity<Object> response = restTemplate.exchange(uri, HttpMethod.POST, entity, Object.class);

			if (response.getBody() instanceof Map) {
				Map<String, Object> orderData = (Map<String, Object>) response.getBody();
				OrderResult result = mapToOrderResult(orderData);
				log.info("Order submitted successfully: orderId={}, status={}", result.getOrderId(),
						result.getStatus());
				return result;
			}

			return OrderResult.failed("Unexpected response format");

		}
		catch (RestClientResponseException e) {
			int statusCode = e.getStatusCode().value();
			String responseBody = e.getResponseBodyAsString();

			log.error("Order submission failed - HTTP {}: {}", statusCode, responseBody);

			if (statusCode == 403) {
				return OrderResult.failed("Insufficient buying power or trading not allowed");
			}
			else if (statusCode == 422) {
				return OrderResult.failed("Invalid order parameters: " + responseBody);
			}
			else if (statusCode == 401) {
				throw new StrategizException(AlpacaErrors.ALPACA_TOKEN_EXPIRED,
						"Access token expired. Please reconnect your Alpaca account.");
			}

			return OrderResult.failed("Order rejected: " + responseBody);

		}
		catch (JsonProcessingException e) {
			log.error("Failed to serialize order request: {}", e.getMessage());
			return OrderResult.failed("Invalid order format");
		}
		catch (Exception e) {
			log.error("Error submitting order: {}", e.getMessage(), e);
			return OrderResult.failed("Order submission failed: " + e.getMessage());
		}
	}

	/**
	 * Create HTTP headers with OAuth Bearer token.
	 */
	private HttpHeaders createHeaders(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return headers;
	}

	/**
	 * Map Alpaca order response to OrderResult.
	 */
	private OrderResult mapToOrderResult(Map<String, Object> orderData) {
		return OrderResult.builder()
			.orderId((String) orderData.get("id"))
			.clientOrderId((String) orderData.get("client_order_id"))
			.status((String) orderData.get("status"))
			.symbol((String) orderData.get("symbol"))
			.side((String) orderData.get("side"))
			.type((String) orderData.get("type"))
			.qty(parseDouble(orderData.get("qty")))
			.filledQty(parseDouble(orderData.get("filled_qty")))
			.filledAvgPrice(parseDouble(orderData.get("filled_avg_price")))
			.limitPrice(parseDouble(orderData.get("limit_price")))
			.stopPrice(parseDouble(orderData.get("stop_price")))
			.createdAt((String) orderData.get("created_at"))
			.filledAt((String) orderData.get("filled_at"))
			.success(true)
			.build();
	}

	/**
	 * Parse a value to double, handling null and string values.
	 */
	private double parseDouble(Object value) {
		if (value == null) {
			return 0.0;
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		if (value instanceof String) {
			try {
				return Double.parseDouble((String) value);
			}
			catch (NumberFormatException e) {
				return 0.0;
			}
		}
		return 0.0;
	}

	/**
	 * Result of an order operation.
	 */
	public static class OrderResult {

		private final boolean success;

		private final String orderId;

		private final String clientOrderId;

		private final String status;

		private final String symbol;

		private final String side;

		private final String type;

		private final double qty;

		private final double filledQty;

		private final double filledAvgPrice;

		private final double limitPrice;

		private final double stopPrice;

		private final String createdAt;

		private final String filledAt;

		private final String error;

		private OrderResult(Builder builder) {
			this.success = builder.success;
			this.orderId = builder.orderId;
			this.clientOrderId = builder.clientOrderId;
			this.status = builder.status;
			this.symbol = builder.symbol;
			this.side = builder.side;
			this.type = builder.type;
			this.qty = builder.qty;
			this.filledQty = builder.filledQty;
			this.filledAvgPrice = builder.filledAvgPrice;
			this.limitPrice = builder.limitPrice;
			this.stopPrice = builder.stopPrice;
			this.createdAt = builder.createdAt;
			this.filledAt = builder.filledAt;
			this.error = builder.error;
		}

		public static Builder builder() {
			return new Builder();
		}

		public static OrderResult failed(String error) {
			return builder().success(false).error(error).build();
		}

		public boolean isSuccess() {
			return success;
		}

		public String getOrderId() {
			return orderId;
		}

		public String getClientOrderId() {
			return clientOrderId;
		}

		public String getStatus() {
			return status;
		}

		public String getSymbol() {
			return symbol;
		}

		public String getSide() {
			return side;
		}

		public String getType() {
			return type;
		}

		public double getQty() {
			return qty;
		}

		public double getFilledQty() {
			return filledQty;
		}

		public double getFilledAvgPrice() {
			return filledAvgPrice;
		}

		public double getLimitPrice() {
			return limitPrice;
		}

		public double getStopPrice() {
			return stopPrice;
		}

		public String getCreatedAt() {
			return createdAt;
		}

		public String getFilledAt() {
			return filledAt;
		}

		public String getError() {
			return error;
		}

		public boolean isFilled() {
			return "filled".equalsIgnoreCase(status);
		}

		public boolean isPending() {
			return "new".equalsIgnoreCase(status) || "pending_new".equalsIgnoreCase(status)
					|| "accepted".equalsIgnoreCase(status) || "partially_filled".equalsIgnoreCase(status);
		}

		public boolean isRejected() {
			return "rejected".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status)
					|| "expired".equalsIgnoreCase(status);
		}

		public static class Builder {

			private boolean success;

			private String orderId;

			private String clientOrderId;

			private String status;

			private String symbol;

			private String side;

			private String type;

			private double qty;

			private double filledQty;

			private double filledAvgPrice;

			private double limitPrice;

			private double stopPrice;

			private String createdAt;

			private String filledAt;

			private String error;

			public Builder success(boolean success) {
				this.success = success;
				return this;
			}

			public Builder orderId(String orderId) {
				this.orderId = orderId;
				return this;
			}

			public Builder clientOrderId(String clientOrderId) {
				this.clientOrderId = clientOrderId;
				return this;
			}

			public Builder status(String status) {
				this.status = status;
				return this;
			}

			public Builder symbol(String symbol) {
				this.symbol = symbol;
				return this;
			}

			public Builder side(String side) {
				this.side = side;
				return this;
			}

			public Builder type(String type) {
				this.type = type;
				return this;
			}

			public Builder qty(double qty) {
				this.qty = qty;
				return this;
			}

			public Builder filledQty(double filledQty) {
				this.filledQty = filledQty;
				return this;
			}

			public Builder filledAvgPrice(double filledAvgPrice) {
				this.filledAvgPrice = filledAvgPrice;
				return this;
			}

			public Builder limitPrice(double limitPrice) {
				this.limitPrice = limitPrice;
				return this;
			}

			public Builder stopPrice(double stopPrice) {
				this.stopPrice = stopPrice;
				return this;
			}

			public Builder createdAt(String createdAt) {
				this.createdAt = createdAt;
				return this;
			}

			public Builder filledAt(String filledAt) {
				this.filledAt = filledAt;
				return this;
			}

			public Builder error(String error) {
				this.error = error;
				return this;
			}

			public OrderResult build() {
				return new OrderResult(this);
			}

		}

	}

}
