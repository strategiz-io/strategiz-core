-- Market Data Coverage Snapshot Table
-- Stores periodic (daily) snapshots of market data coverage metrics
-- Replaces Firestore marketdata_coverage collection for better time-series analytics

CREATE TABLE IF NOT EXISTS market_data_coverage_snapshot
(
    snapshot_id String,
    calculated_at DateTime,

    -- Overall metrics
    total_symbols UInt16,
    total_timeframes UInt8,
    overall_freshness_percent Float64,
    fresh_pairs UInt32,
    stale_pairs UInt32,
    failed_pairs UInt32,
    total_pairs UInt32,

    -- Per-timeframe metrics (denormalized for fast queries)
    -- 1Hour
    tf_1hour_total UInt16 DEFAULT 0,
    tf_1hour_fresh UInt16 DEFAULT 0,
    tf_1hour_stale UInt16 DEFAULT 0,
    tf_1hour_failed UInt16 DEFAULT 0,
    tf_1hour_percent Float64 DEFAULT 0,
    tf_1hour_total_bars UInt64 DEFAULT 0,
    tf_1hour_avg_bars_per_symbol Float64 DEFAULT 0,
    tf_1hour_date_range_start String DEFAULT '',
    tf_1hour_date_range_end String DEFAULT '',

    -- 1Day
    tf_1day_total UInt16 DEFAULT 0,
    tf_1day_fresh UInt16 DEFAULT 0,
    tf_1day_stale UInt16 DEFAULT 0,
    tf_1day_failed UInt16 DEFAULT 0,
    tf_1day_percent Float64 DEFAULT 0,
    tf_1day_total_bars UInt64 DEFAULT 0,
    tf_1day_avg_bars_per_symbol Float64 DEFAULT 0,
    tf_1day_date_range_start String DEFAULT '',
    tf_1day_date_range_end String DEFAULT '',

    -- 1Week
    tf_1week_total UInt16 DEFAULT 0,
    tf_1week_fresh UInt16 DEFAULT 0,
    tf_1week_stale UInt16 DEFAULT 0,
    tf_1week_failed UInt16 DEFAULT 0,
    tf_1week_percent Float64 DEFAULT 0,
    tf_1week_total_bars UInt64 DEFAULT 0,
    tf_1week_avg_bars_per_symbol Float64 DEFAULT 0,
    tf_1week_date_range_start String DEFAULT '',
    tf_1week_date_range_end String DEFAULT '',

    -- 1Month
    tf_1month_total UInt16 DEFAULT 0,
    tf_1month_fresh UInt16 DEFAULT 0,
    tf_1month_stale UInt16 DEFAULT 0,
    tf_1month_failed UInt16 DEFAULT 0,
    tf_1month_percent Float64 DEFAULT 0,
    tf_1month_total_bars UInt64 DEFAULT 0,
    tf_1month_avg_bars_per_symbol Float64 DEFAULT 0,
    tf_1month_date_range_start String DEFAULT '',
    tf_1month_date_range_end String DEFAULT '',

    -- Storage stats
    total_rows UInt64 DEFAULT 0,
    storage_bytes UInt64 DEFAULT 0,
    estimated_cost_per_month Float64 DEFAULT 0,

    -- Data quality stats
    quality_good UInt16 DEFAULT 0,
    quality_partial UInt16 DEFAULT 0,
    quality_poor UInt16 DEFAULT 0,

    -- Metadata
    triggered_by String,
    calculation_duration_ms UInt32,

    -- Additional context (JSON strings for flexibility)
    gaps_json String DEFAULT '[]',
    missing_symbols_json String DEFAULT '[]'

) ENGINE = MergeTree()
ORDER BY calculated_at
PARTITION BY toYYYYMM(calculated_at)
SETTINGS index_granularity = 8192;

-- Index for fast latest snapshot queries
CREATE INDEX IF NOT EXISTS idx_snapshot_id ON market_data_coverage_snapshot(snapshot_id) TYPE minmax GRANULARITY 1;

-- Comments for documentation
ALTER TABLE market_data_coverage_snapshot
COMMENT 'Periodic snapshots of market data coverage metrics. Calculated daily to track coverage trends over time. Replaces Firestore marketdata_coverage collection.';
