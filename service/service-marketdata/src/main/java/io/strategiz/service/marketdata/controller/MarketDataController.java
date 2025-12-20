package io.strategiz.service.marketdata.controller;

import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.service.marketdata.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for market data access.
 * Provides OHLCV bar data for charting and analysis.
 */
@RestController
@RequestMapping("/v1/market-data")
@Tag(name = "Market Data", description = "Market data for charting and analysis")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io",
		"https://strategiz-io.web.app" })
public class MarketDataController {

	private static final Logger log = LoggerFactory.getLogger(MarketDataController.class);

	private final MarketDataService marketDataService;

	@Autowired
	public MarketDataController(MarketDataService marketDataService) {
		this.marketDataService = marketDataService;
	}

	@GetMapping("/bars")
	@Operation(summary = "Get OHLCV bars for charting",
			description = "Fetches historical market data bars for a symbol within a date range")
	public ResponseEntity<List<BarResponse>> getBars(
			@Parameter(description = "Stock symbol (e.g., AAPL)") @RequestParam String symbol,
			@Parameter(description = "Timeframe (e.g., 1Min, 1Day)") @RequestParam(defaultValue = "1Day") String timeframe,
			@Parameter(description = "Start date ISO format") @RequestParam String startDate,
			@Parameter(description = "End date ISO format") @RequestParam String endDate) {

		log.info("GET /v1/market-data/bars - symbol={}, timeframe={}, start={}, end={}", symbol, timeframe, startDate,
				endDate);

		List<MarketDataEntity> data = marketDataService.getMarketDataBars(symbol, timeframe, startDate, endDate);

		List<BarResponse> response = data.stream().map(this::toBarResponse).collect(Collectors.toList());

		log.info("Returning {} bars for {}", response.size(), symbol);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/latest")
	@Operation(summary = "Get latest price for a symbol", description = "Fetches the most recent market data for a symbol")
	public ResponseEntity<BarResponse> getLatest(
			@Parameter(description = "Stock symbol (e.g., AAPL)") @RequestParam String symbol) {

		log.info("GET /v1/market-data/latest - symbol={}", symbol);

		MarketDataEntity data = marketDataService.getLatestMarketData(symbol);

		if (data == null) {
			log.warn("No data found for symbol {}", symbol);
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(toBarResponse(data));
	}

	@GetMapping("/symbols")
	@Operation(summary = "Get available symbols", description = "Returns list of symbols with available market data")
	public ResponseEntity<List<String>> getSymbols() {
		log.info("GET /v1/market-data/symbols");

		List<String> symbols = marketDataService.getAvailableSymbols();
		return ResponseEntity.ok(symbols);
	}

	private BarResponse toBarResponse(MarketDataEntity entity) {
		BarResponse bar = new BarResponse();
		bar.symbol = entity.getSymbol();
		bar.timestamp = entity.getTimestamp() != null
				? java.time.Instant.ofEpochMilli(entity.getTimestamp()).toString()
				: null;
		bar.timeframe = entity.getTimeframe();
		bar.open = entity.getOpen() != null ? entity.getOpen().doubleValue() : 0;
		bar.high = entity.getHigh() != null ? entity.getHigh().doubleValue() : 0;
		bar.low = entity.getLow() != null ? entity.getLow().doubleValue() : 0;
		bar.close = entity.getClose() != null ? entity.getClose().doubleValue() : 0;
		bar.volume = entity.getVolume() != null ? entity.getVolume().longValue() : 0;
		return bar;
	}

	/**
	 * Response DTO for market data bars
	 */
	public static class BarResponse {

		public String symbol;

		public String timestamp;

		public String timeframe;

		public double open;

		public double high;

		public double low;

		public double close;

		public long volume;

	}

}
