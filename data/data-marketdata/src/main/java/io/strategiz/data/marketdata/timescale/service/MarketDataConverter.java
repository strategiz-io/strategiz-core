package io.strategiz.data.marketdata.timescale.service;

import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.timescale.entity.MarketDataTimescaleEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Converts between Firestore MarketDataEntity and TimescaleDB
 * MarketDataTimescaleEntity.
 */
@Component
public class MarketDataConverter {

	public MarketDataTimescaleEntity toTimescale(MarketDataEntity entity) {
		if (entity == null) {
			return null;
		}

		MarketDataTimescaleEntity ts = new MarketDataTimescaleEntity();
		ts.setSymbol(entity.getSymbol());
		ts.setTimestamp(entity.getTimestamp() != null ? Instant.ofEpochMilli(entity.getTimestamp()) : null);
		ts.setTimeframe(entity.getTimeframe());
		ts.setAssetType(entity.getAssetType());
		ts.setExchange(entity.getExchange());
		ts.setOpen(entity.getOpen());
		ts.setHigh(entity.getHigh());
		ts.setLow(entity.getLow());
		ts.setClose(entity.getClose());
		ts.setVolume(entity.getVolume());
		ts.setVwap(entity.getVwap());
		ts.setTrades(entity.getTrades());
		ts.setChangePercent(entity.getChangePercent());
		ts.setChangeAmount(entity.getChangeAmount());
		ts.setDataSource(entity.getDataSource());
		ts.setDataQuality(entity.getDataQuality());
		ts.setCollectedAt(entity.getCollectedAt() != null ? Instant.ofEpochMilli(entity.getCollectedAt()) : null);
		return ts;
	}

	public MarketDataEntity toFirestore(MarketDataTimescaleEntity ts) {
		if (ts == null) {
			return null;
		}

		MarketDataEntity entity = new MarketDataEntity();
		entity.setSymbol(ts.getSymbol());
		entity.setTimestamp(ts.getTimestamp() != null ? ts.getTimestamp().toEpochMilli() : null);
		entity.setTimeframe(ts.getTimeframe());
		entity.setAssetType(ts.getAssetType());
		entity.setExchange(ts.getExchange());
		entity.setOpen(ts.getOpen());
		entity.setHigh(ts.getHigh());
		entity.setLow(ts.getLow());
		entity.setClose(ts.getClose());
		entity.setVolume(ts.getVolume());
		entity.setVwap(ts.getVwap());
		entity.setTrades(ts.getTrades());
		entity.setChangePercent(ts.getChangePercent());
		entity.setChangeAmount(ts.getChangeAmount());
		entity.setDataSource(ts.getDataSource());
		entity.setDataQuality(ts.getDataQuality());
		entity.setCollectedAt(ts.getCollectedAt() != null ? ts.getCollectedAt().toEpochMilli() : null);

		// Generate ID for compatibility
		if (ts.getSymbol() != null && ts.getTimestamp() != null && ts.getTimeframe() != null) {
			entity.setId(
					MarketDataEntity.createId(ts.getSymbol(), ts.getTimestamp().toEpochMilli(), ts.getTimeframe()));
		}

		return entity;
	}

}
