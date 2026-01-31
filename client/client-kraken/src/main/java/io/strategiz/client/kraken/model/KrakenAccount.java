package io.strategiz.client.kraken.model;

import java.util.Map;
import java.util.Objects;
import java.util.Arrays;

/**
 * Model class for Kraken account data This represents the completely unmodified raw data
 * from the Kraken API
 */
public class KrakenAccount {

	private String[] error;

	private Map<String, Object> result; // Using Object to handle different value types

	// Constructors
	public KrakenAccount() {
	}

	public KrakenAccount(String[] error, Map<String, Object> result) {
		this.error = error;
		this.result = result;
	}

	// Getters and setters
	public String[] getError() {
		return error;
	}

	public void setError(String[] error) {
		this.error = error;
	}

	public Map<String, Object> getResult() {
		return result;
	}

	public void setResult(Map<String, Object> result) {
		this.result = result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		KrakenAccount that = (KrakenAccount) o;
		return Arrays.equals(error, that.error) && Objects.equals(result, that.result);
	}

	@Override
	public int hashCode() {
		int result1 = Objects.hash(result);
		result1 = 31 * result1 + Arrays.hashCode(error);
		return result1;
	}

	@Override
	public String toString() {
		return "KrakenAccount{" + "error=" + Arrays.toString(error) + ", result=" + result + '}';
	}

}
