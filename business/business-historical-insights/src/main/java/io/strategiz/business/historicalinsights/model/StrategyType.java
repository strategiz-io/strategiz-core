package io.strategiz.business.historicalinsights.model;

/**
 * Enum representing the types of trading strategies that can be tested by the
 * optimization engine.
 */
public enum StrategyType {

	RSI_MEAN_REVERSION("RSI Mean Reversion", "Buy when RSI is oversold, sell when overbought"),

	MACD_TREND_FOLLOWING("MACD Trend Following", "Buy on bullish MACD crossover, sell on bearish crossover"),

	BOLLINGER_MEAN_REVERSION("Bollinger Mean Reversion", "Buy at lower band, sell at upper band"),

	BOLLINGER_BREAKOUT("Bollinger Breakout", "Buy on upper band breakout with volume confirmation"),

	MA_CROSSOVER_EMA("EMA Crossover", "Buy when fast EMA crosses above slow EMA"),

	MA_CROSSOVER_SMA("SMA Crossover", "Buy when fast SMA crosses above slow SMA"),

	STOCHASTIC("Stochastic Oscillator", "Buy on oversold crossover, sell on overbought crossover"),

	SWING_TRADING("Swing Trading", "Buy X% drop from recent high, sell Y% rise from recent low"),

	COMBINED_ADX("Combined with ADX Filter", "Top indicator strategy filtered by ADX trend strength"),

	MOMENTUM_TRAILING("Momentum with Trailing Stop", "Ride trends with trailing stops - no fixed profit target"),

	BREAKOUT_MOMENTUM("Breakout Momentum", "Buy breakouts above resistance, trail stop from highest high");

	private final String displayName;

	private final String description;

	StrategyType(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}

}
