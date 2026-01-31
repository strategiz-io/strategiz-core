package io.strategiz.service.marketing.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.strategiz.service.marketing.model.response.MarketTickerResponse;
import io.strategiz.service.marketing.service.MarketTickerService;
import io.strategiz.service.base.controller.BaseController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public controller for market ticker data. No authentication required - provides
 * real-time market data for the landing page. Delegates to MarketTickerService for
 * business logic and FMP API calls.
 *
 * Requires FMP integration to be enabled (strategiz.fmp.enabled=true).
 */
@RestController
@RequestMapping("/v1/market/tickers")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
@ConditionalOnProperty(name = "strategiz.fmp.enabled", havingValue = "true")
public class MarketTickerController extends BaseController {

	@Override
	protected String getModuleName() {
		return "service-marketing";
	}

	private static final Logger log = LoggerFactory.getLogger(MarketTickerController.class);

	private final MarketTickerService marketTickerService;

	public MarketTickerController(MarketTickerService marketTickerService) {
		this.marketTickerService = marketTickerService;
	}

	/**
	 * Get market ticker data for popular assets. Cached for 30 seconds to avoid rate
	 * limits.
	 * @return Market ticker data with popular crypto and stock prices from Alpaca
	 */
	@GetMapping
	// TODO: Re-enable caching once cache configuration is fixed
	// @Cacheable(value = "marketTicker", key = "'ticker'", cacheManager = "cacheManager")
	public ResponseEntity<MarketTickerResponse> getMarketTicker() {
		log.info("GET /v1/market/tickers");
		MarketTickerResponse response = marketTickerService.getMarketTicker();
		return ResponseEntity.ok(response);
	}

}
