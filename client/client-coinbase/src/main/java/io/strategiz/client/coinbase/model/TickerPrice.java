package io.strategiz.client.coinbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Model class representing a Coinbase ticker price
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickerPrice {

	private String base;

	private String currency;

	private String amount;

	@JsonProperty("product_id")
	private String productId;

	public TickerPrice() {
	}

	public TickerPrice(String base, String currency, String amount, String productId) {
		this.base = base;
		this.currency = currency;
		this.amount = amount;
		this.productId = productId;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TickerPrice that = (TickerPrice) o;
		return Objects.equals(base, that.base) && Objects.equals(currency, that.currency)
				&& Objects.equals(amount, that.amount) && Objects.equals(productId, that.productId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(base, currency, amount, productId);
	}

	@Override
	public String toString() {
		return "TickerPrice{" + "base='" + base + '\'' + ", currency='" + currency + '\'' + ", amount='" + amount + '\''
				+ ", productId='" + productId + '\'' + '}';
	}

}
