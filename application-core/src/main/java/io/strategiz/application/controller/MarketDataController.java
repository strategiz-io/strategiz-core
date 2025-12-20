package io.strategiz.application.controller;

import io.strategiz.application.dto.MarketDataBarDTO;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.service.base.controller.BaseController;
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
 * REST controller for market data endpoints
 * Provides access to historical OHLCV data for charting and analysis
 */
@RestController
@RequestMapping("/v1/market-data")
@Tag(name = "Market Data", description = "Access historical market data (OHLCV bars)")
public class MarketDataController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(MarketDataController.class);

    private final MarketDataService marketDataService;

    @Autowired
    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    /**
     * Get market data bars for a symbol
     * GET /v1/market-data/bars?symbol=AAPL&timeframe=1Day&startDate=2024-01-01T00:00:00Z&endDate=2024-12-31T23:59:59Z
     *
     * @param symbol Stock symbol (required, e.g., "AAPL")
     * @param timeframe Timeframe for the bars (required, e.g., "1Day", "1Hour")
     * @param startDate Start date in ISO 8601 format (optional, e.g., "2024-01-01T00:00:00Z")
     * @param endDate End date in ISO 8601 format (optional, e.g., "2024-12-31T23:59:59Z")
     * @return List of market data bars sorted by timestamp ascending
     */
    @GetMapping("/bars")
    @Operation(
        summary = "Get market data bars",
        description = "Retrieve historical OHLCV market data for a symbol within a date range"
    )
    public ResponseEntity<List<MarketDataBarDTO>> getMarketDataBars(
        @Parameter(description = "Stock symbol (e.g., AAPL)", required = true)
        @RequestParam(required = true) String symbol,

        @Parameter(description = "Timeframe (e.g., 1Day, 1Hour)", required = true)
        @RequestParam(required = true) String timeframe,

        @Parameter(description = "Start date in ISO 8601 format (e.g., 2024-01-01T00:00:00Z)")
        @RequestParam(required = false) String startDate,

        @Parameter(description = "End date in ISO 8601 format (e.g., 2024-12-31T23:59:59Z)")
        @RequestParam(required = false) String endDate
    ) {
        try {
            log.info("GET /v1/market-data/bars - symbol={}, timeframe={}, startDate={}, endDate={}",
                symbol, timeframe, startDate, endDate);

            // Validate required parameters
            validateRequiredParam("symbol", symbol);
            validateRequiredParam("timeframe", timeframe);

            // Fetch market data from service
            List<MarketDataEntity> entities = marketDataService.getMarketDataBars(
                symbol, timeframe, startDate, endDate
            );

            // Convert entities to DTOs
            List<MarketDataBarDTO> bars = entities.stream()
                .map(MarketDataBarDTO::fromEntity)
                .collect(Collectors.toList());

            log.info("Returning {} market data bars for symbol={}", bars.size(), symbol);
            return ResponseEntity.ok(bars);

        } catch (Exception e) {
            log.error("Error fetching market data bars for symbol={}", symbol, e);
            throw handleException(e, "market-data.fetch-failed");
        }
    }

    /**
     * Get the latest market data for a symbol
     * GET /v1/market-data/latest?symbol=AAPL
     *
     * @param symbol Stock symbol (required)
     * @return Latest market data bar
     */
    @GetMapping("/latest")
    @Operation(
        summary = "Get latest market data",
        description = "Retrieve the most recent market data bar for a symbol"
    )
    public ResponseEntity<MarketDataBarDTO> getLatestMarketData(
        @Parameter(description = "Stock symbol (e.g., AAPL)", required = true)
        @RequestParam(required = true) String symbol
    ) {
        try {
            log.info("GET /v1/market-data/latest - symbol={}", symbol);

            // Validate required parameters
            validateRequiredParam("symbol", symbol);

            // Fetch latest market data
            MarketDataEntity entity = marketDataService.getLatestMarketData(symbol);

            if (entity == null) {
                log.warn("No market data found for symbol={}", symbol);
                return ResponseEntity.notFound().build();
            }

            // Convert to DTO
            MarketDataBarDTO bar = MarketDataBarDTO.fromEntity(entity);

            log.info("Returning latest market data for symbol={}", symbol);
            return ResponseEntity.ok(bar);

        } catch (Exception e) {
            log.error("Error fetching latest market data for symbol={}", symbol, e);
            throw handleException(e, "market-data.fetch-failed");
        }
    }

    /**
     * Get all available symbols in the database
     * GET /v1/market-data/symbols
     *
     * @return List of distinct symbols
     */
    @GetMapping("/symbols")
    @Operation(
        summary = "Get available symbols",
        description = "Retrieve list of all symbols with market data in the database"
    )
    public ResponseEntity<List<String>> getAvailableSymbols() {
        try {
            log.info("GET /v1/market-data/symbols");

            List<String> symbols = marketDataService.getAvailableSymbols();

            log.info("Returning {} available symbols", symbols.size());
            return ResponseEntity.ok(symbols);

        } catch (Exception e) {
            log.error("Error fetching available symbols", e);
            throw handleException(e, "market-data.fetch-symbols-failed");
        }
    }

    @Override
    protected String getModuleName() {
        return "market-data";
    }
}
