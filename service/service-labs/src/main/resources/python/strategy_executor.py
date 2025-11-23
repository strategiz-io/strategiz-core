#!/usr/bin/env python3
"""
Strategy Executor - TradingView-style Python Execution Wrapper

Provides plot() and signal() API functions for user strategies.
Receives market data via stdin, executes user code, outputs indicators/signals as JSON.
"""

import json
import sys
import traceback
from datetime import datetime
from typing import List, Dict, Any, Optional

# Global state to collect outputs from user code
_indicators = []
_signals = []
_errors = []


def plot(series, name: str, color: str = "#2196F3", linewidth: int = 2, overlay: bool = True):
    """
    Add an indicator line to the chart (TradingView-style API)

    Args:
        series: Data series (list, pandas Series, or iterable of values)
        name: Display name for the indicator
        color: Hex color code (default: blue)
        linewidth: Line thickness in pixels
        overlay: Whether to overlay on price chart (True) or separate panel (False)

    Example:
        sma_20 = data['close'].rolling(window=20).mean()
        plot(sma_20, name="SMA 20", color="#00BFFF", linewidth=2)
    """
    try:
        data_points = []

        # Handle different series types
        if hasattr(series, 'items'):  # pandas Series
            for timestamp, value in series.items():
                if value is not None and not (hasattr(value, '__iter__') and len(str(value)) == 0):
                    # Check for NaN
                    if hasattr(value, '__float__'):
                        import math
                        if math.isnan(float(value)):
                            continue

                    data_points.append({
                        "time": timestamp.strftime("%Y-%m-%d") if hasattr(timestamp, 'strftime') else str(timestamp),
                        "value": float(value)
                    })
        elif hasattr(series, '__iter__'):  # List or iterable
            for i, value in enumerate(series):
                if value is not None:
                    # Check for NaN
                    if hasattr(value, '__float__'):
                        import math
                        if math.isnan(float(value)):
                            continue

                    data_points.append({
                        "time": str(i),  # Use index as time
                        "value": float(value)
                    })

        _indicators.append({
            "name": name,
            "color": color,
            "linewidth": linewidth,
            "overlay": overlay,
            "data": data_points
        })

    except Exception as e:
        _errors.append(f"Error in plot() for '{name}': {str(e)}")


def signal(signal_type: str, timestamp, price: float, text: Optional[str] = None, shape: str = "circle"):
    """
    Add a buy/sell signal marker to the chart (TradingView-style API)

    Args:
        signal_type: "BUY" or "SELL"
        timestamp: Time of the signal (datetime or string)
        price: Price level for the marker
        text: Optional label text (defaults to signal_type)
        shape: Marker shape: "circle", "arrow_up", "arrow_down", "triangle"

    Example:
        if sma_20 > sma_50:
            signal("BUY", timestamp=data.index[i], price=data['close'][i], text="Golden Cross")
    """
    try:
        _signals.append({
            "type": signal_type.upper(),
            "timestamp": timestamp.strftime("%Y-%m-%d") if hasattr(timestamp, 'strftime') else str(timestamp),
            "price": float(price),
            "text": text or signal_type,
            "shape": shape
        })

    except Exception as e:
        _errors.append(f"Error in signal() for '{signal_type}': {str(e)}")


def main():
    """Main execution entry point"""
    try:
        # Read market data from stdin (JSON format)
        market_data_json = sys.stdin.read()

        if not market_data_json or market_data_json.strip() == "":
            raise ValueError("No market data received from stdin")

        market_data = json.loads(market_data_json)

        # Convert to pandas DataFrame for user convenience
        try:
            import pandas as pd

            data = pd.DataFrame(market_data)

            # Convert time column to datetime if it exists
            if 'time' in data.columns:
                data['time'] = pd.to_datetime(data['time'])
                data.set_index('time', inplace=True)

            # Make 'data' available to user code
            globals()['data'] = data
            globals()['pd'] = pd

        except ImportError:
            # Pandas not available, provide raw data as list of dicts
            globals()['data'] = market_data
            _errors.append("Warning: pandas not installed, using raw data format")

        # Execute user code (will be injected by Java)
        # USER_CODE_PLACEHOLDER

        # This is a placeholder that will be replaced by actual user code
        # For testing purposes, here's a sample strategy:
        if 'USER_CODE_PLACEHOLDER' in open(__file__).read():
            # Sample strategy for testing
            try:
                import pandas as pd

                # Calculate simple moving averages
                sma_short = data['close'].rolling(window=20).mean()
                sma_long = data['close'].rolling(window=50).mean()

                # Plot indicators
                plot(sma_short, name="SMA 20", color="#00BFFF", linewidth=2)
                plot(sma_long, name="SMA 50", color="#FF6B6B", linewidth=2)

                # Generate signals on crossovers
                for i in range(1, len(data)):
                    if sma_short.iloc[i] > sma_long.iloc[i] and sma_short.iloc[i-1] <= sma_long.iloc[i-1]:
                        signal("BUY", timestamp=data.index[i], price=data['close'].iloc[i], text="Golden Cross")
                    elif sma_short.iloc[i] < sma_long.iloc[i] and sma_short.iloc[i-1] >= sma_long.iloc[i-1]:
                        signal("SELL", timestamp=data.index[i], price=data['close'].iloc[i], text="Death Cross")

            except Exception as e:
                _errors.append(f"Sample strategy error: {str(e)}")

    except Exception as e:
        _errors.append(f"Fatal execution error: {str(e)}\n{traceback.format_exc()}")

    finally:
        # Output results as JSON to stdout
        results = {
            "indicators": _indicators,
            "signals": _signals,
            "errors": _errors
        }

        print(json.dumps(results, indent=2))


if __name__ == "__main__":
    main()
