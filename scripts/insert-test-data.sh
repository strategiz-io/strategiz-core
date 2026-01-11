#!/bin/bash
# Quick script to insert test market data into ClickHouse for chart display

# ClickHouse connection details (from Vault in production)
# You'll need to set these environment variables:
# CLICKHOUSE_HOST, CLICKHOUSE_PORT, CLICKHOUSE_USER, CLICKHOUSE_PASSWORD, CLICKHOUSE_DB

# Generate sample OHLCV data for AAPL for the last 30 days
# This is just to get the chart populating while we wait for the full backfill

echo "Inserting test market data for AAPL..."

# Sample data generator (simplified - would use real API in actual backfill)
# Format: timestamp, symbol, timeframe, open, high, low, close, volume

# Insert 30 days of daily data for AAPL
for i in {0..30}; do
  DATE=$(date -u -v-${i}d '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date -u --date="-${i} days" '+%Y-%m-%d %H:%M:%S')
  TIMESTAMP="${DATE}.000"

  # Generate random OHLCV (simplified - just for testing)
  OPEN=$(echo "180 + $RANDOM % 20" | bc)
  HIGH=$(echo "$OPEN + $RANDOM % 5" | bc)
  LOW=$(echo "$OPEN - $RANDOM % 5" | bc)
  CLOSE=$(echo "$OPEN + ($RANDOM % 10) - 5" | bc)
  VOLUME=$(echo "50000000 + $RANDOM % 30000000" | bc)

  echo "INSERT INTO market_data (timestamp, symbol, timeframe, open, high, low, close, volume) VALUES ('$TIMESTAMP', 'AAPL', '1Day', $OPEN, $HIGH, $LOW, $CLOSE, $VOLUME);"
done

echo "Test data generation complete. Run this output against ClickHouse to insert data."
