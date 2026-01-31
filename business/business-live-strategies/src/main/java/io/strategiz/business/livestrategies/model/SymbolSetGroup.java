package io.strategiz.business.livestrategies.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Groups deployments (alerts and bots) by their symbol set. This enables efficient batch
 * processing - all deployments for the same symbol set can be evaluated with a single
 * market data fetch.
 *
 * Symbol sets are sorted and joined for consistent grouping: - ["AAPL"] -> key: "AAPL" -
 * ["AAPL", "MSFT"] -> key: "AAPL,MSFT" - ["MSFT", "AAPL"] -> key: "AAPL,MSFT" (sorted)
 */
public class SymbolSetGroup {

	private final List<String> symbols;

	private final String symbolSetKey;

	private final List<String> alertIds;

	private final List<String> botIds;

	public SymbolSetGroup(List<String> symbols) {
		if (symbols == null || symbols.isEmpty()) {
			throw new IllegalArgumentException("symbols cannot be null or empty");
		}
		// Sort symbols for consistent grouping
		this.symbols = symbols.stream().sorted().collect(Collectors.toList());
		this.symbolSetKey = String.join(",", this.symbols);
		this.alertIds = new ArrayList<>();
		this.botIds = new ArrayList<>();
	}

	/**
	 * Create a SymbolSetGroup from a symbol set key.
	 * @param symbolSetKey comma-separated sorted symbols (e.g., "AAPL,MSFT")
	 */
	public static SymbolSetGroup fromKey(String symbolSetKey) {
		if (symbolSetKey == null || symbolSetKey.isEmpty()) {
			throw new IllegalArgumentException("symbolSetKey cannot be null or empty");
		}
		List<String> symbols = List.of(symbolSetKey.split(","));
		return new SymbolSetGroup(symbols);
	}

	/**
	 * Get the canonical symbol set key for grouping.
	 * @param symbols list of symbols
	 * @return sorted, comma-joined key
	 */
	public static String getKey(List<String> symbols) {
		if (symbols == null || symbols.isEmpty()) {
			return "";
		}
		return symbols.stream().sorted().collect(Collectors.joining(","));
	}

	public void addAlert(String alertId) {
		if (alertId != null && !alertId.isEmpty()) {
			this.alertIds.add(alertId);
		}
	}

	public void addBot(String botId) {
		if (botId != null && !botId.isEmpty()) {
			this.botIds.add(botId);
		}
	}

	public void addAlerts(List<String> alertIds) {
		if (alertIds != null) {
			alertIds.forEach(this::addAlert);
		}
	}

	public void addBots(List<String> botIds) {
		if (botIds != null) {
			botIds.forEach(this::addBot);
		}
	}

	public List<String> getSymbols() {
		return Collections.unmodifiableList(symbols);
	}

	public String getSymbolSetKey() {
		return symbolSetKey;
	}

	public List<String> getAlertIds() {
		return Collections.unmodifiableList(alertIds);
	}

	public List<String> getBotIds() {
		return Collections.unmodifiableList(botIds);
	}

	public int getTotalDeployments() {
		return alertIds.size() + botIds.size();
	}

	public boolean hasAlerts() {
		return !alertIds.isEmpty();
	}

	public boolean hasBots() {
		return !botIds.isEmpty();
	}

	public boolean isEmpty() {
		return alertIds.isEmpty() && botIds.isEmpty();
	}

	public boolean isSingleSymbol() {
		return symbols.size() == 1;
	}

	public boolean isMultiSymbol() {
		return symbols.size() > 1;
	}

	@Override
	public String toString() {
		return String.format("SymbolSetGroup[%s: %d alerts, %d bots]", symbolSetKey, alertIds.size(), botIds.size());
	}

}
