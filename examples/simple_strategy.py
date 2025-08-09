# Simple Moving Average Crossover Strategy
# This strategy generates buy/sell signals based on SMA crossovers

log("Starting Simple Moving Average Strategy")

# Get price data
closes = get_closes()
log(f"Loaded {len(closes)} price points")

if len(closes) < 50:
    log("Not enough data for analysis")
else:
    # Calculate moving averages
    sma_fast = sma(closes, 10)  # 10-period fast SMA
    sma_slow = sma(closes, 20)  # 20-period slow SMA
    
    log(f"Calculated SMA10: {len(sma_fast)} points")
    log(f"Calculated SMA20: {len(sma_slow)} points")
    
    # Store indicators
    add_indicator("SMA_10", sma_fast[-10:])  # Last 10 values
    add_indicator("SMA_20", sma_slow[-10:])  # Last 10 values
    
    # Check for crossover signals
    if len(sma_fast) >= 2 and len(sma_slow) >= 2:
        current_fast = sma_fast[-1]
        current_slow = sma_slow[-1]
        previous_fast = sma_fast[-2]
        previous_slow = sma_slow[-2]
        
        current_price = closes[-1]
        
        # Bullish crossover: fast SMA crosses above slow SMA
        if previous_fast <= previous_slow and current_fast > current_slow:
            add_signal("BUY", current_price, 100, "SMA bullish crossover", 0.8)
            log(f"BUY signal at {current_price} - SMA crossover")
            
        # Bearish crossover: fast SMA crosses below slow SMA
        elif previous_fast >= previous_slow and current_fast < current_slow:
            add_signal("SELL", current_price, 100, "SMA bearish crossover", 0.8)
            log(f"SELL signal at {current_price} - SMA crossover")
            
        else:
            add_signal("HOLD", current_price, 0, "No crossover signal", 0.5)
            log(f"HOLD at {current_price} - No crossover")

log("Strategy execution completed")