package io.strategiz.business.historicalinsights.template;

import io.strategiz.business.historicalinsights.model.StrategyType;

import java.util.Map;

/**
 * Python code templates for each strategy type. All templates include: - ATR-based dynamic stop
 * loss - Configurable take profit - Position state tracking - Proper indicator plotting
 *
 * Templates use placeholder substitution for parameters.
 */
public final class StrategyCodeTemplates {

	private StrategyCodeTemplates() {
		// Utility class
	}

	/**
	 * Generates Python code for a strategy with the given parameters.
	 * @param type The strategy type
	 * @param params The parameter values to substitute
	 * @return Complete Python strategy code
	 */
	public static String generateCode(StrategyType type, Map<String, Object> params) {
		return switch (type) {
			case RSI_MEAN_REVERSION -> generateRSIMeanReversion(params);
			case MACD_TREND_FOLLOWING -> generateMACDTrendFollowing(params);
			case BOLLINGER_MEAN_REVERSION -> generateBollingerMeanReversion(params);
			case BOLLINGER_BREAKOUT -> generateBollingerBreakout(params);
			case MA_CROSSOVER_EMA -> generateMACrossoverEMA(params);
			case MA_CROSSOVER_SMA -> generateMACrossoverSMA(params);
			case STOCHASTIC -> generateStochastic(params);
			case SWING_TRADING -> generateSwingTrading(params);
			case COMBINED_ADX -> generateCombinedADX(params);
			case MOMENTUM_TRAILING -> generateMomentumTrailing(params);
			case BREAKOUT_MOMENTUM -> generateBreakoutMomentum(params);
		};
	}

	/**
	 * RSI Mean Reversion Strategy. Buys when RSI drops below oversold threshold, sells when RSI
	 * rises above overbought threshold.
	 *
	 * Parameters: - period: RSI period (7, 10, 14, 21) - oversold: Buy threshold (20, 25, 30, 35) -
	 * overbought: Sell threshold (65, 70, 75, 80) - atr_multiplier: Stop loss ATR multiplier (1.5,
	 * 2.0, 2.5)
	 */
	private static String generateRSIMeanReversion(Map<String, Object> params) {
		int period = getInt(params, "period", 14);
		int oversold = getInt(params, "oversold", 30);
		int overbought = getInt(params, "overbought", 70);
		double atrMult = getDouble(params, "atr_multiplier", 2.0);

		return String.format("""
				# RSI Mean Reversion Strategy
				# Parameters: period=%d, oversold=%d, overbought=%d, atr_mult=%.1f

				import pandas as pd
				import numpy as np

				# Strategy parameters
				RSI_PERIOD = %d
				OVERSOLD = %d
				OVERBOUGHT = %d
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f
				TAKE_PROFIT_RATIO = 2.0  # Risk-reward ratio

				def calculate_rsi(data, period):
				    delta = data['close'].diff()
				    gain = delta.where(delta > 0, 0.0)
				    loss = (-delta).where(delta < 0, 0.0)
				    avg_gain = gain.rolling(window=period, min_periods=1).mean()
				    avg_loss = loss.rolling(window=period, min_periods=1).mean()
				    rs = avg_gain / avg_loss.replace(0, np.inf)
				    return 100 - (100 / (1 + rs))

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate indicators
				data['rsi'] = calculate_rsi(data, RSI_PERIOD)
				data['atr'] = calculate_atr(data, ATR_PERIOD)

				# Plot RSI
				plot(data['rsi'], 'RSI', color='purple', linewidth=1, overlay=False)

				# Trading logic
				position = None
				entry_price = 0
				stop_loss = 0
				take_profit = 0

				for i in range(RSI_PERIOD + 1, len(data)):
				    row = data.iloc[i]
				    price = row['close']
				    timestamp = row['timestamp']
				    rsi = row['rsi']
				    atr = row['atr']

				    if position is None:
				        # Entry: RSI below oversold
				        if rsi < OVERSOLD:
				            position = 'long'
				            entry_price = price
				            stop_loss = price - (atr * ATR_MULTIPLIER)
				            take_profit = price + (atr * ATR_MULTIPLIER * TAKE_PROFIT_RATIO)
				            signal('BUY', timestamp, price, f'RSI={rsi:.1f} (oversold)', 'arrow_up')
				    else:
				        # Exit conditions
				        if rsi > OVERBOUGHT:
				            signal('SELL', timestamp, price, f'RSI={rsi:.1f} (overbought)', 'arrow_down')
				            position = None
				        elif price <= stop_loss:
				            signal('SELL', timestamp, price, 'Stop Loss', 'arrow_down')
				            position = None
				        elif price >= take_profit:
				            signal('SELL', timestamp, price, 'Take Profit', 'arrow_down')
				            position = None
				""", period, oversold, overbought, atrMult, period, oversold, overbought, atrMult);
	}

	/**
	 * MACD Trend Following Strategy. Buys on bullish crossover (MACD crosses above signal), sells
	 * on bearish crossover.
	 *
	 * Parameters: - fast: Fast EMA period (8, 10, 12) - slow: Slow EMA period (20, 26, 30) -
	 * signal_period: Signal line period (7, 9, 12) - atr_multiplier: Stop loss ATR multiplier
	 */
	private static String generateMACDTrendFollowing(Map<String, Object> params) {
		int fast = getInt(params, "fast", 12);
		int slow = getInt(params, "slow", 26);
		int signalPeriod = getInt(params, "signal_period", 9);
		double atrMult = getDouble(params, "atr_multiplier", 2.0);

		return String.format("""
				# MACD Trend Following Strategy
				# Parameters: fast=%d, slow=%d, signal=%d, atr_mult=%.1f

				import pandas as pd
				import numpy as np

				# Strategy parameters
				FAST_PERIOD = %d
				SLOW_PERIOD = %d
				SIGNAL_PERIOD = %d
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f
				TAKE_PROFIT_RATIO = 2.0

				def calculate_ema(series, period):
				    return series.ewm(span=period, adjust=False).mean()

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate MACD
				ema_fast = calculate_ema(data['close'], FAST_PERIOD)
				ema_slow = calculate_ema(data['close'], SLOW_PERIOD)
				data['macd'] = ema_fast - ema_slow
				data['macd_signal'] = calculate_ema(data['macd'], SIGNAL_PERIOD)
				data['macd_hist'] = data['macd'] - data['macd_signal']
				data['atr'] = calculate_atr(data, ATR_PERIOD)

				# Plot MACD
				plot(data['macd'], 'MACD', color='blue', linewidth=1, overlay=False)
				plot(data['macd_signal'], 'Signal', color='orange', linewidth=1, overlay=False)

				# Trading logic
				position = None
				entry_price = 0
				stop_loss = 0
				take_profit = 0

				for i in range(SLOW_PERIOD + SIGNAL_PERIOD, len(data)):
				    row = data.iloc[i]
				    prev_row = data.iloc[i - 1]
				    price = row['close']
				    timestamp = row['timestamp']
				    atr = row['atr']

				    macd = row['macd']
				    macd_signal = row['macd_signal']
				    prev_macd = prev_row['macd']
				    prev_signal = prev_row['macd_signal']

				    if position is None:
				        # Bullish crossover
				        if prev_macd <= prev_signal and macd > macd_signal:
				            position = 'long'
				            entry_price = price
				            stop_loss = price - (atr * ATR_MULTIPLIER)
				            take_profit = price + (atr * ATR_MULTIPLIER * TAKE_PROFIT_RATIO)
				            signal('BUY', timestamp, price, 'MACD bullish crossover', 'arrow_up')
				    else:
				        # Exit conditions
				        if prev_macd >= prev_signal and macd < macd_signal:
				            signal('SELL', timestamp, price, 'MACD bearish crossover', 'arrow_down')
				            position = None
				        elif price <= stop_loss:
				            signal('SELL', timestamp, price, 'Stop Loss', 'arrow_down')
				            position = None
				        elif price >= take_profit:
				            signal('SELL', timestamp, price, 'Take Profit', 'arrow_down')
				            position = None
				""", fast, slow, signalPeriod, atrMult, fast, slow, signalPeriod, atrMult);
	}

	/**
	 * Bollinger Bands Mean Reversion Strategy. Buys at lower band, sells at upper band.
	 *
	 * Parameters: - period: Bollinger period (10, 15, 20, 25) - std_mult: Standard deviation
	 * multiplier (1.5, 2.0, 2.5) - atr_multiplier: Stop loss ATR multiplier
	 */
	private static String generateBollingerMeanReversion(Map<String, Object> params) {
		int period = getInt(params, "period", 20);
		double stdMult = getDouble(params, "std_mult", 2.0);
		double atrMult = getDouble(params, "atr_multiplier", 2.0);

		return String.format("""
				# Bollinger Bands Mean Reversion Strategy
				# Parameters: period=%d, std_mult=%.1f, atr_mult=%.1f

				import pandas as pd
				import numpy as np

				# Strategy parameters
				BB_PERIOD = %d
				STD_MULT = %.1f
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f
				TAKE_PROFIT_RATIO = 2.0

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate Bollinger Bands
				data['bb_middle'] = data['close'].rolling(window=BB_PERIOD).mean()
				data['bb_std'] = data['close'].rolling(window=BB_PERIOD).std()
				data['bb_upper'] = data['bb_middle'] + (data['bb_std'] * STD_MULT)
				data['bb_lower'] = data['bb_middle'] - (data['bb_std'] * STD_MULT)
				data['atr'] = calculate_atr(data, ATR_PERIOD)

				# Plot Bollinger Bands
				plot(data['bb_upper'], 'BB Upper', color='red', linewidth=1, overlay=True)
				plot(data['bb_middle'], 'BB Middle', color='gray', linewidth=1, overlay=True)
				plot(data['bb_lower'], 'BB Lower', color='green', linewidth=1, overlay=True)

				# Trading logic
				position = None
				entry_price = 0
				stop_loss = 0
				take_profit = 0

				for i in range(BB_PERIOD + 1, len(data)):
				    row = data.iloc[i]
				    price = row['close']
				    timestamp = row['timestamp']
				    atr = row['atr']
				    bb_lower = row['bb_lower']
				    bb_upper = row['bb_upper']
				    bb_middle = row['bb_middle']

				    if position is None:
				        # Buy at lower band
				        if price <= bb_lower:
				            position = 'long'
				            entry_price = price
				            stop_loss = price - (atr * ATR_MULTIPLIER)
				            take_profit = bb_middle  # Target middle band
				            signal('BUY', timestamp, price, 'Price at lower band', 'arrow_up')
				    else:
				        # Exit conditions
				        if price >= bb_upper:
				            signal('SELL', timestamp, price, 'Price at upper band', 'arrow_down')
				            position = None
				        elif price >= bb_middle and price > entry_price:
				            signal('SELL', timestamp, price, 'Reached middle band', 'arrow_down')
				            position = None
				        elif price <= stop_loss:
				            signal('SELL', timestamp, price, 'Stop Loss', 'arrow_down')
				            position = None
				""", period, stdMult, atrMult, period, stdMult, atrMult);
	}

	/**
	 * Bollinger Bands Breakout Strategy. Buys on upper band breakout with volume confirmation.
	 *
	 * Parameters: - period: Bollinger period (10, 15, 20, 25) - std_mult: Standard deviation
	 * multiplier (1.5, 2.0, 2.5) - atr_multiplier: Stop loss ATR multiplier
	 */
	private static String generateBollingerBreakout(Map<String, Object> params) {
		int period = getInt(params, "period", 20);
		double stdMult = getDouble(params, "std_mult", 2.0);
		double atrMult = getDouble(params, "atr_multiplier", 2.0);

		return String.format("""
				# Bollinger Bands Breakout Strategy
				# Parameters: period=%d, std_mult=%.1f, atr_mult=%.1f

				import pandas as pd
				import numpy as np

				# Strategy parameters
				BB_PERIOD = %d
				STD_MULT = %.1f
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f
				TAKE_PROFIT_RATIO = 2.0
				VOLUME_MULT = 1.5  # Volume must be 1.5x average

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate Bollinger Bands
				data['bb_middle'] = data['close'].rolling(window=BB_PERIOD).mean()
				data['bb_std'] = data['close'].rolling(window=BB_PERIOD).std()
				data['bb_upper'] = data['bb_middle'] + (data['bb_std'] * STD_MULT)
				data['bb_lower'] = data['bb_middle'] - (data['bb_std'] * STD_MULT)
				data['atr'] = calculate_atr(data, ATR_PERIOD)
				data['vol_avg'] = data['volume'].rolling(window=20).mean()

				# Plot Bollinger Bands
				plot(data['bb_upper'], 'BB Upper', color='red', linewidth=1, overlay=True)
				plot(data['bb_middle'], 'BB Middle', color='gray', linewidth=1, overlay=True)
				plot(data['bb_lower'], 'BB Lower', color='green', linewidth=1, overlay=True)

				# Trading logic
				position = None
				entry_price = 0
				stop_loss = 0
				take_profit = 0

				for i in range(BB_PERIOD + 1, len(data)):
				    row = data.iloc[i]
				    prev_row = data.iloc[i - 1]
				    price = row['close']
				    timestamp = row['timestamp']
				    atr = row['atr']
				    bb_upper = row['bb_upper']
				    bb_lower = row['bb_lower']
				    volume = row['volume']
				    vol_avg = row['vol_avg']

				    if position is None:
				        # Breakout above upper band with volume
				        if price > bb_upper and prev_row['close'] <= prev_row['bb_upper']:
				            if volume > vol_avg * VOLUME_MULT:
				                position = 'long'
				                entry_price = price
				                stop_loss = bb_lower  # Stop at lower band
				                take_profit = price + (atr * ATR_MULTIPLIER * TAKE_PROFIT_RATIO)
				                signal('BUY', timestamp, price, f'Breakout with volume', 'arrow_up')
				    else:
				        # Exit conditions
				        if price < row['bb_middle']:
				            signal('SELL', timestamp, price, 'Price below middle band', 'arrow_down')
				            position = None
				        elif price <= stop_loss:
				            signal('SELL', timestamp, price, 'Stop Loss', 'arrow_down')
				            position = None
				        elif price >= take_profit:
				            signal('SELL', timestamp, price, 'Take Profit', 'arrow_down')
				            position = None
				""", period, stdMult, atrMult, period, stdMult, atrMult);
	}

	/**
	 * EMA Crossover Strategy. Buys when fast EMA crosses above slow EMA.
	 *
	 * Parameters: - fast: Fast EMA period (5, 10, 20) - slow: Slow EMA period (20, 50, 100, 200) -
	 * atr_multiplier: Stop loss ATR multiplier
	 */
	private static String generateMACrossoverEMA(Map<String, Object> params) {
		int fast = getInt(params, "fast", 10);
		int slow = getInt(params, "slow", 50);
		double atrMult = getDouble(params, "atr_multiplier", 2.0);

		return String.format("""
				# EMA Crossover Strategy
				# Parameters: fast=%d, slow=%d, atr_mult=%.1f

				import pandas as pd
				import numpy as np

				# Strategy parameters
				FAST_PERIOD = %d
				SLOW_PERIOD = %d
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f
				TAKE_PROFIT_RATIO = 2.0

				def calculate_ema(series, period):
				    return series.ewm(span=period, adjust=False).mean()

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate EMAs
				data['ema_fast'] = calculate_ema(data['close'], FAST_PERIOD)
				data['ema_slow'] = calculate_ema(data['close'], SLOW_PERIOD)
				data['atr'] = calculate_atr(data, ATR_PERIOD)

				# Plot EMAs
				plot(data['ema_fast'], f'EMA {FAST_PERIOD}', color='blue', linewidth=1, overlay=True)
				plot(data['ema_slow'], f'EMA {SLOW_PERIOD}', color='red', linewidth=1, overlay=True)

				# Trading logic
				position = None
				entry_price = 0
				stop_loss = 0
				take_profit = 0

				for i in range(SLOW_PERIOD + 1, len(data)):
				    row = data.iloc[i]
				    prev_row = data.iloc[i - 1]
				    price = row['close']
				    timestamp = row['timestamp']
				    atr = row['atr']

				    ema_fast = row['ema_fast']
				    ema_slow = row['ema_slow']
				    prev_fast = prev_row['ema_fast']
				    prev_slow = prev_row['ema_slow']

				    if position is None:
				        # Golden cross: fast crosses above slow
				        if prev_fast <= prev_slow and ema_fast > ema_slow:
				            position = 'long'
				            entry_price = price
				            stop_loss = price - (atr * ATR_MULTIPLIER)
				            take_profit = price + (atr * ATR_MULTIPLIER * TAKE_PROFIT_RATIO)
				            signal('BUY', timestamp, price, f'EMA golden cross', 'arrow_up')
				    else:
				        # Exit conditions
				        if prev_fast >= prev_slow and ema_fast < ema_slow:
				            signal('SELL', timestamp, price, 'EMA death cross', 'arrow_down')
				            position = None
				        elif price <= stop_loss:
				            signal('SELL', timestamp, price, 'Stop Loss', 'arrow_down')
				            position = None
				        elif price >= take_profit:
				            signal('SELL', timestamp, price, 'Take Profit', 'arrow_down')
				            position = None
				""", fast, slow, atrMult, fast, slow, atrMult);
	}

	/**
	 * SMA Crossover Strategy. Buys when fast SMA crosses above slow SMA.
	 *
	 * Parameters: - fast: Fast SMA period (5, 10, 20) - slow: Slow SMA period (20, 50, 100, 200) -
	 * atr_multiplier: Stop loss ATR multiplier
	 */
	private static String generateMACrossoverSMA(Map<String, Object> params) {
		int fast = getInt(params, "fast", 10);
		int slow = getInt(params, "slow", 50);
		double atrMult = getDouble(params, "atr_multiplier", 2.0);

		return String.format("""
				# SMA Crossover Strategy
				# Parameters: fast=%d, slow=%d, atr_mult=%.1f

				import pandas as pd
				import numpy as np

				# Strategy parameters
				FAST_PERIOD = %d
				SLOW_PERIOD = %d
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f
				TAKE_PROFIT_RATIO = 2.0

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate SMAs
				data['sma_fast'] = data['close'].rolling(window=FAST_PERIOD).mean()
				data['sma_slow'] = data['close'].rolling(window=SLOW_PERIOD).mean()
				data['atr'] = calculate_atr(data, ATR_PERIOD)

				# Plot SMAs
				plot(data['sma_fast'], f'SMA {FAST_PERIOD}', color='blue', linewidth=1, overlay=True)
				plot(data['sma_slow'], f'SMA {SLOW_PERIOD}', color='red', linewidth=1, overlay=True)

				# Trading logic
				position = None
				entry_price = 0
				stop_loss = 0
				take_profit = 0

				for i in range(SLOW_PERIOD + 1, len(data)):
				    row = data.iloc[i]
				    prev_row = data.iloc[i - 1]
				    price = row['close']
				    timestamp = row['timestamp']
				    atr = row['atr']

				    sma_fast = row['sma_fast']
				    sma_slow = row['sma_slow']
				    prev_fast = prev_row['sma_fast']
				    prev_slow = prev_row['sma_slow']

				    if position is None:
				        # Golden cross: fast crosses above slow
				        if prev_fast <= prev_slow and sma_fast > sma_slow:
				            position = 'long'
				            entry_price = price
				            stop_loss = price - (atr * ATR_MULTIPLIER)
				            take_profit = price + (atr * ATR_MULTIPLIER * TAKE_PROFIT_RATIO)
				            signal('BUY', timestamp, price, f'SMA golden cross', 'arrow_up')
				    else:
				        # Exit conditions
				        if prev_fast >= prev_slow and sma_fast < sma_slow:
				            signal('SELL', timestamp, price, 'SMA death cross', 'arrow_down')
				            position = None
				        elif price <= stop_loss:
				            signal('SELL', timestamp, price, 'Stop Loss', 'arrow_down')
				            position = None
				        elif price >= take_profit:
				            signal('SELL', timestamp, price, 'Take Profit', 'arrow_down')
				            position = None
				""", fast, slow, atrMult, fast, slow, atrMult);
	}

	/**
	 * Stochastic Oscillator Strategy. Buys on oversold crossover, sells on overbought crossover.
	 *
	 * Parameters: - k_period: %K period (10, 14, 20) - d_period: %D period (3, 5) - oversold:
	 * Oversold level (20, 25, 30) - overbought: Overbought level (70, 75, 80) - atr_multiplier:
	 * Stop loss ATR multiplier
	 */
	private static String generateStochastic(Map<String, Object> params) {
		int kPeriod = getInt(params, "k_period", 14);
		int dPeriod = getInt(params, "d_period", 3);
		int oversold = getInt(params, "oversold", 20);
		int overbought = getInt(params, "overbought", 80);
		double atrMult = getDouble(params, "atr_multiplier", 2.0);

		return String.format("""
				# Stochastic Oscillator Strategy
				# Parameters: k=%d, d=%d, oversold=%d, overbought=%d, atr_mult=%.1f

				import pandas as pd
				import numpy as np

				# Strategy parameters
				K_PERIOD = %d
				D_PERIOD = %d
				OVERSOLD = %d
				OVERBOUGHT = %d
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f
				TAKE_PROFIT_RATIO = 2.0

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate Stochastic
				low_min = data['low'].rolling(window=K_PERIOD).min()
				high_max = data['high'].rolling(window=K_PERIOD).max()
				data['stoch_k'] = 100 * (data['close'] - low_min) / (high_max - low_min + 0.0001)
				data['stoch_d'] = data['stoch_k'].rolling(window=D_PERIOD).mean()
				data['atr'] = calculate_atr(data, ATR_PERIOD)

				# Plot Stochastic
				plot(data['stoch_k'], '%%K', color='blue', linewidth=1, overlay=False)
				plot(data['stoch_d'], '%%D', color='orange', linewidth=1, overlay=False)

				# Trading logic
				position = None
				entry_price = 0
				stop_loss = 0
				take_profit = 0

				for i in range(K_PERIOD + D_PERIOD + 1, len(data)):
				    row = data.iloc[i]
				    prev_row = data.iloc[i - 1]
				    price = row['close']
				    timestamp = row['timestamp']
				    atr = row['atr']

				    k = row['stoch_k']
				    d = row['stoch_d']
				    prev_k = prev_row['stoch_k']
				    prev_d = prev_row['stoch_d']

				    if position is None:
				        # Bullish crossover in oversold zone
				        if prev_k <= prev_d and k > d and k < OVERSOLD:
				            position = 'long'
				            entry_price = price
				            stop_loss = price - (atr * ATR_MULTIPLIER)
				            take_profit = price + (atr * ATR_MULTIPLIER * TAKE_PROFIT_RATIO)
				            signal('BUY', timestamp, price, f'Stoch bullish crossover (K={k:.1f})', 'arrow_up')
				    else:
				        # Exit conditions
				        if prev_k >= prev_d and k < d and k > OVERBOUGHT:
				            signal('SELL', timestamp, price, f'Stoch bearish crossover (K={k:.1f})', 'arrow_down')
				            position = None
				        elif price <= stop_loss:
				            signal('SELL', timestamp, price, 'Stop Loss', 'arrow_down')
				            position = None
				        elif price >= take_profit:
				            signal('SELL', timestamp, price, 'Take Profit', 'arrow_down')
				            position = None
				""", kPeriod, dPeriod, oversold, overbought, atrMult, kPeriod, dPeriod, oversold, overbought, atrMult);
	}

	/**
	 * Swing Trading Strategy. Buys X% drop from recent high, sells Y% rise from recent low.
	 *
	 * Parameters: - buy_threshold: Percentage drop to buy (5, 8, 10, 12) - sell_threshold:
	 * Percentage rise to sell (8, 12, 15, 20) - lookback: Lookback period for high/low (10, 20,
	 * 30) - atr_multiplier: Stop loss ATR multiplier
	 */
	private static String generateSwingTrading(Map<String, Object> params) {
		int buyThreshold = getInt(params, "buy_threshold", 10);
		int sellThreshold = getInt(params, "sell_threshold", 15);
		int lookback = getInt(params, "lookback", 20);
		double atrMult = getDouble(params, "atr_multiplier", 2.0);

		return String.format("""
				# Swing Trading Strategy
				# Parameters: buy_thresh=%d%%, sell_thresh=%d%%, lookback=%d, atr_mult=%.1f

				import pandas as pd
				import numpy as np

				# Strategy parameters
				BUY_THRESHOLD = %d  # Buy when X%% below recent high
				SELL_THRESHOLD = %d  # Sell when X%% above recent low
				LOOKBACK = %d
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f
				STOP_LOSS_PERCENT = %.1f  # Additional stop loss below buy threshold

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate rolling high/low
				data['rolling_high'] = data['high'].rolling(window=LOOKBACK).max()
				data['rolling_low'] = data['low'].rolling(window=LOOKBACK).min()
				data['atr'] = calculate_atr(data, ATR_PERIOD)

				# Calculate drop from high and rise from low
				data['pct_from_high'] = (data['rolling_high'] - data['close']) / data['rolling_high'] * 100
				data['pct_from_low'] = (data['close'] - data['rolling_low']) / data['rolling_low'] * 100

				# Plot percentage from high (for debugging)
				plot(data['pct_from_high'], 'Drop from High %%', color='red', linewidth=1, overlay=False)

				# Trading logic
				position = None
				entry_price = 0
				stop_loss = 0

				for i in range(LOOKBACK + 1, len(data)):
				    row = data.iloc[i]
				    price = row['close']
				    timestamp = row['timestamp']
				    atr = row['atr']
				    pct_from_high = row['pct_from_high']
				    pct_from_low = row['pct_from_low']

				    if position is None:
				        # Buy when dropped X%% from recent high
				        if pct_from_high >= BUY_THRESHOLD:
				            position = 'long'
				            entry_price = price
				            stop_loss = price - (atr * ATR_MULTIPLIER)
				            signal('BUY', timestamp, price, f'{pct_from_high:.1f}%% from high', 'arrow_up')
				    else:
				        # Exit conditions
				        pct_gain = (price - entry_price) / entry_price * 100
				        if pct_gain >= SELL_THRESHOLD:
				            signal('SELL', timestamp, price, f'Target {pct_gain:.1f}%% gain', 'arrow_down')
				            position = None
				        elif price <= stop_loss:
				            signal('SELL', timestamp, price, 'Stop Loss', 'arrow_down')
				            position = None
				""", buyThreshold, sellThreshold, lookback, atrMult, buyThreshold, sellThreshold, lookback, atrMult,
				buyThreshold * 0.5);
	}

	/**
	 * Combined Strategy with ADX Filter. Uses a primary indicator (RSI) but only trades when ADX
	 * confirms trend strength.
	 *
	 * Parameters: - adx_threshold: Minimum ADX for trend confirmation (20, 25, 30) - rsi_period:
	 * RSI period - rsi_oversold: RSI buy threshold - rsi_overbought: RSI sell threshold -
	 * atr_multiplier: Stop loss ATR multiplier
	 */
	private static String generateCombinedADX(Map<String, Object> params) {
		int adxThreshold = getInt(params, "adx_threshold", 25);
		int rsiPeriod = getInt(params, "rsi_period", 14);
		int rsiOversold = getInt(params, "rsi_oversold", 30);
		int rsiOverbought = getInt(params, "rsi_overbought", 70);
		double atrMult = getDouble(params, "atr_multiplier", 2.0);

		return String.format("""
				# Combined RSI + ADX Filter Strategy
				# Parameters: adx_thresh=%d, rsi_period=%d, oversold=%d, overbought=%d, atr_mult=%.1f

				import pandas as pd
				import numpy as np

				# Strategy parameters
				ADX_THRESHOLD = %d
				ADX_PERIOD = 14
				RSI_PERIOD = %d
				OVERSOLD = %d
				OVERBOUGHT = %d
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f
				TAKE_PROFIT_RATIO = 2.0

				def calculate_rsi(data, period):
				    delta = data['close'].diff()
				    gain = delta.where(delta > 0, 0.0)
				    loss = (-delta).where(delta < 0, 0.0)
				    avg_gain = gain.rolling(window=period, min_periods=1).mean()
				    avg_loss = loss.rolling(window=period, min_periods=1).mean()
				    rs = avg_gain / avg_loss.replace(0, np.inf)
				    return 100 - (100 / (1 + rs))

				def calculate_adx(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close']

				    plus_dm = high.diff()
				    minus_dm = -low.diff()
				    plus_dm = plus_dm.where((plus_dm > minus_dm) & (plus_dm > 0), 0.0)
				    minus_dm = minus_dm.where((minus_dm > plus_dm) & (minus_dm > 0), 0.0)

				    tr = pd.concat([high - low, (high - close.shift(1)).abs(), (low - close.shift(1)).abs()], axis=1).max(axis=1)
				    atr = tr.rolling(window=period).mean()

				    plus_di = 100 * (plus_dm.rolling(window=period).mean() / atr)
				    minus_di = 100 * (minus_dm.rolling(window=period).mean() / atr)

				    dx = 100 * ((plus_di - minus_di).abs() / (plus_di + minus_di + 0.0001))
				    adx = dx.rolling(window=period).mean()
				    return adx, plus_di, minus_di

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate indicators
				data['rsi'] = calculate_rsi(data, RSI_PERIOD)
				data['adx'], data['plus_di'], data['minus_di'] = calculate_adx(data, ADX_PERIOD)
				data['atr'] = calculate_atr(data, ATR_PERIOD)

				# Plot indicators
				plot(data['rsi'], 'RSI', color='purple', linewidth=1, overlay=False)
				plot(data['adx'], 'ADX', color='blue', linewidth=1, overlay=False)

				# Trading logic
				position = None
				entry_price = 0
				stop_loss = 0
				take_profit = 0

				for i in range(max(RSI_PERIOD, ADX_PERIOD * 2) + 1, len(data)):
				    row = data.iloc[i]
				    price = row['close']
				    timestamp = row['timestamp']
				    rsi = row['rsi']
				    adx = row['adx']
				    plus_di = row['plus_di']
				    minus_di = row['minus_di']
				    atr = row['atr']

				    if position is None:
				        # Buy when RSI oversold AND ADX shows trend
				        if rsi < OVERSOLD and adx > ADX_THRESHOLD:
				            position = 'long'
				            entry_price = price
				            stop_loss = price - (atr * ATR_MULTIPLIER)
				            take_profit = price + (atr * ATR_MULTIPLIER * TAKE_PROFIT_RATIO)
				            signal('BUY', timestamp, price, f'RSI={rsi:.1f}, ADX={adx:.1f}', 'arrow_up')
				    else:
				        # Exit conditions
				        if rsi > OVERBOUGHT:
				            signal('SELL', timestamp, price, f'RSI={rsi:.1f} (overbought)', 'arrow_down')
				            position = None
				        elif adx < ADX_THRESHOLD * 0.7:  # Trend weakening
				            signal('SELL', timestamp, price, f'ADX={adx:.1f} (trend weak)', 'arrow_down')
				            position = None
				        elif price <= stop_loss:
				            signal('SELL', timestamp, price, 'Stop Loss', 'arrow_down')
				            position = None
				        elif price >= take_profit:
				            signal('SELL', timestamp, price, 'Take Profit', 'arrow_down')
				            position = None
				""", adxThreshold, rsiPeriod, rsiOversold, rsiOverbought, atrMult, adxThreshold, rsiPeriod, rsiOversold,
				rsiOverbought, atrMult);
	}

	/**
	 * Momentum Strategy with Trailing Stop - designed to outperform buy-and-hold.
	 * Key feature: NO fixed profit target - lets winners run with trailing stop.
	 * Uses EMA for trend confirmation and trails stop from highest high.
	 *
	 * Parameters:
	 * - ema_period: EMA period for trend filter (20, 50, 100)
	 * - trail_percent: Trailing stop percentage from high (5, 8, 10, 12)
	 * - atr_multiplier: Initial stop loss ATR multiplier
	 */
	private static String generateMomentumTrailing(Map<String, Object> params) {
		int emaPeriod = getInt(params, "ema_period", 50);
		double trailPercent = getDouble(params, "trail_percent", 8.0);
		double atrMult = getDouble(params, "atr_multiplier", 2.5);

		return String.format("""
				# Momentum Strategy with Trailing Stop
				# Designed to OUTPERFORM buy-and-hold by riding trends
				# Parameters: ema=%d, trail=%.1f%%, atr_mult=%.1f

				import pandas as pd
				import numpy as np

				# Strategy parameters
				EMA_PERIOD = %d
				TRAIL_PERCENT = %.1f  # Trail stop this %% below highest high
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f

				def calculate_ema(series, period):
				    return series.ewm(span=period, adjust=False).mean()

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate indicators
				data['ema'] = calculate_ema(data['close'], EMA_PERIOD)
				data['atr'] = calculate_atr(data, ATR_PERIOD)

				# Plot EMA
				plot(data['ema'], f'EMA {EMA_PERIOD}', color='blue', linewidth=1.5, overlay=True)

				# Trading logic - KEY: trailing stop, no profit target
				position = None
				entry_price = 0
				highest_since_entry = 0
				trailing_stop = 0

				for i in range(EMA_PERIOD + 1, len(data)):
				    row = data.iloc[i]
				    prev_row = data.iloc[i - 1]
				    price = row['close']
				    high = row['high']
				    timestamp = row['timestamp']
				    ema = row['ema']
				    prev_ema = prev_row['ema']
				    atr = row['atr']

				    if position is None:
				        # Entry: Price crosses above EMA (uptrend confirmed)
				        if prev_row['close'] <= prev_ema and price > ema:
				            position = 'long'
				            entry_price = price
				            highest_since_entry = high
				            trailing_stop = price - (atr * ATR_MULTIPLIER)
				            signal('BUY', timestamp, price, f'Price > EMA{EMA_PERIOD} (trend start)', 'arrow_up')
				    else:
				        # Update highest high and trailing stop
				        if high > highest_since_entry:
				            highest_since_entry = high
				            # Trail stop at X%% below highest high
				            trailing_stop = max(trailing_stop, highest_since_entry * (1 - TRAIL_PERCENT / 100))

				        # Exit ONLY on trailing stop - let winners RUN
				        if price <= trailing_stop:
				            pct_gain = (price - entry_price) / entry_price * 100
				            signal('SELL', timestamp, price, f'Trailing stop (gain: {pct_gain:.1f}%%)', 'arrow_down')
				            position = None
				        # Also exit if price drops significantly below EMA
				        elif price < ema * 0.95:
				            pct_gain = (price - entry_price) / entry_price * 100
				            signal('SELL', timestamp, price, f'Below EMA (gain: {pct_gain:.1f}%%)', 'arrow_down')
				            position = None
				""", emaPeriod, trailPercent, atrMult, emaPeriod, trailPercent, atrMult);
	}

	/**
	 * Breakout Momentum Strategy - buys breakouts above resistance.
	 * Uses trailing stop from the breakout high - no fixed profit target.
	 *
	 * Parameters:
	 * - lookback: Period for finding resistance (20, 50, 100)
	 * - breakout_buffer: Percentage above resistance to confirm breakout (0.5, 1.0, 1.5)
	 * - trail_percent: Trailing stop percentage (6, 8, 10, 12)
	 */
	private static String generateBreakoutMomentum(Map<String, Object> params) {
		int lookback = getInt(params, "lookback", 50);
		double breakoutBuffer = getDouble(params, "breakout_buffer", 1.0);
		double trailPercent = getDouble(params, "trail_percent", 8.0);
		double atrMult = getDouble(params, "atr_multiplier", 2.5);

		return String.format("""
				# Breakout Momentum Strategy
				# Buys breakouts above recent highs, trails stop - no profit target
				# Parameters: lookback=%d, buffer=%.1f%%, trail=%.1f%%

				import pandas as pd
				import numpy as np

				# Strategy parameters
				LOOKBACK = %d
				BREAKOUT_BUFFER = %.1f  # Percent above resistance to confirm
				TRAIL_PERCENT = %.1f
				ATR_PERIOD = 14
				ATR_MULTIPLIER = %.1f

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				# Calculate resistance (rolling max) and ATR
				data['resistance'] = data['high'].rolling(window=LOOKBACK).max().shift(1)
				data['atr'] = calculate_atr(data, ATR_PERIOD)

				# Plot resistance
				plot(data['resistance'], 'Resistance', color='red', linewidth=1, overlay=True)

				# Trading logic
				position = None
				entry_price = 0
				highest_since_entry = 0
				trailing_stop = 0

				for i in range(LOOKBACK + 1, len(data)):
				    row = data.iloc[i]
				    price = row['close']
				    high = row['high']
				    timestamp = row['timestamp']
				    resistance = row['resistance']
				    atr = row['atr']

				    if pd.isna(resistance):
				        continue

				    breakout_level = resistance * (1 + BREAKOUT_BUFFER / 100)

				    if position is None:
				        # Entry: Price breaks above resistance + buffer
				        if price > breakout_level:
				            position = 'long'
				            entry_price = price
				            highest_since_entry = high
				            trailing_stop = price - (atr * ATR_MULTIPLIER)
				            signal('BUY', timestamp, price, f'Breakout above {resistance:.2f}', 'arrow_up')
				    else:
				        # Update highest high and trailing stop
				        if high > highest_since_entry:
				            highest_since_entry = high
				            trailing_stop = max(trailing_stop, highest_since_entry * (1 - TRAIL_PERCENT / 100))

				        # Exit ONLY on trailing stop
				        if price <= trailing_stop:
				            pct_gain = (price - entry_price) / entry_price * 100
				            signal('SELL', timestamp, price, f'Trailing stop (gain: {pct_gain:.1f}%%)', 'arrow_down')
				            position = None
				""", lookback, breakoutBuffer, trailPercent, lookback, breakoutBuffer, trailPercent, atrMult);
	}

	// Helper methods

	private static int getInt(Map<String, Object> params, String key, int defaultValue) {
		Object value = params.get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return defaultValue;
	}

	private static double getDouble(Map<String, Object> params, String key, double defaultValue) {
		Object value = params.get(key);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		return defaultValue;
	}

}
