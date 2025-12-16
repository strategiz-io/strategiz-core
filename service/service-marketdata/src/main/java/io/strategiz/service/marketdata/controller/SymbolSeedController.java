package io.strategiz.service.marketdata.controller;

import io.strategiz.business.marketdata.SymbolService;
import io.strategiz.data.symbol.entity.SymbolEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for seeding and managing symbols in Firestore.
 * Provides endpoints to migrate static symbol mappings to Firestore.
 */
@RestController
@RequestMapping("/v1/admin/symbols")
public class SymbolSeedController {

    private static final Logger log = LoggerFactory.getLogger(SymbolSeedController.class);

    private final SymbolService symbolService;

    @Autowired
    public SymbolSeedController(SymbolService symbolService) {
        this.symbolService = symbolService;
    }

    /**
     * Seed all symbols (crypto + stocks + ETFs) into Firestore.
     * This is a one-time migration endpoint.
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedSymbols() {
        log.info("Starting symbol seed operation...");

        List<SymbolEntity> allSymbols = new ArrayList<>();

        // Add crypto symbols
        allSymbols.addAll(createCryptoSymbols());

        // Add stock symbols
        allSymbols.addAll(createStockSymbols());

        // Add ETF symbols
        allSymbols.addAll(createEtfSymbols());

        // Add fiat currencies
        allSymbols.addAll(createFiatSymbols());

        // Import all symbols
        int imported = symbolService.importSymbols(allSymbols);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("totalSymbols", allSymbols.size());
        result.put("imported", imported);
        result.put("message", "Symbol seed completed successfully");

        log.info("Symbol seed completed: {} symbols imported", imported);

        return ResponseEntity.ok(result);
    }

    /**
     * Get all symbols from Firestore
     */
    @GetMapping
    public ResponseEntity<List<SymbolEntity>> getAllSymbols() {
        List<SymbolEntity> symbols = symbolService.getActiveCollectionSymbols();
        return ResponseEntity.ok(symbols);
    }

    /**
     * Get symbols by asset type
     */
    @GetMapping("/type/{assetType}")
    public ResponseEntity<List<SymbolEntity>> getSymbolsByType(@PathVariable String assetType) {
        List<SymbolEntity> symbols = symbolService.getSymbolsByAssetType(assetType);
        return ResponseEntity.ok(symbols);
    }

    // === Symbol Creation Helpers ===

    private List<SymbolEntity> createCryptoSymbols() {
        List<SymbolEntity> symbols = new ArrayList<>();

        // Major Cryptocurrencies - with cross-exchange mappings
        symbols.add(createCrypto("BTC", "Bitcoin", "payment", "The first and largest cryptocurrency", 8,
            Map.of("YAHOO", "BTC-USD", "ALPACA", "BTC/USD", "COINBASE", "BTC-USD", "BINANCE", "BTCUSDT", "KRAKEN", "XXBTZUSD")));
        symbols.add(createCrypto("ETH", "Ethereum", "platform", "Smart contract platform", 18,
            Map.of("YAHOO", "ETH-USD", "ALPACA", "ETH/USD", "COINBASE", "ETH-USD", "BINANCE", "ETHUSDT", "KRAKEN", "XETHZUSD")));
        symbols.add(createCrypto("BNB", "Binance Coin", "exchange", "Binance exchange token", 18,
            Map.of("YAHOO", "BNB-USD", "BINANCE", "BNBUSDT")));
        symbols.add(createCrypto("XRP", "Ripple", "payment", "Cross-border payment system", 6,
            Map.of("YAHOO", "XRP-USD", "COINBASE", "XRP-USD", "BINANCE", "XRPUSDT", "KRAKEN", "XXRPZUSD")));
        symbols.add(createCrypto("ADA", "Cardano", "platform", "Proof-of-stake blockchain platform", 6,
            Map.of("YAHOO", "ADA-USD", "COINBASE", "ADA-USD", "BINANCE", "ADAUSDT", "KRAKEN", "ADAUSD")));
        symbols.add(createCrypto("SOL", "Solana", "platform", "High-performance blockchain", 9,
            Map.of("YAHOO", "SOL-USD", "COINBASE", "SOL-USD", "BINANCE", "SOLUSDT")));
        symbols.add(createCrypto("DOGE", "Dogecoin", "meme", "Popular meme cryptocurrency", 8,
            Map.of("YAHOO", "DOGE-USD", "COINBASE", "DOGE-USD", "BINANCE", "DOGEUSDT", "KRAKEN", "XDGUSD")));
        symbols.add(createCrypto("DOT", "Polkadot", "platform", "Multi-chain protocol", 10,
            Map.of("YAHOO", "DOT-USD", "COINBASE", "DOT-USD", "BINANCE", "DOTUSDT", "KRAKEN", "DOTUSD")));
        symbols.add(createCrypto("MATIC", "Polygon", "layer2", "Ethereum scaling solution", 18,
            Map.of("YAHOO", "MATIC-USD", "COINBASE", "MATIC-USD", "BINANCE", "MATICUSDT")));
        symbols.add(createCrypto("AVAX", "Avalanche", "platform", "Smart contract platform", 18,
            Map.of("YAHOO", "AVAX-USD", "COINBASE", "AVAX-USD", "BINANCE", "AVAXUSDT")));
        symbols.add(createCrypto("LINK", "Chainlink", "oracle", "Decentralized oracle network", 18,
            Map.of("YAHOO", "LINK-USD", "COINBASE", "LINK-USD", "BINANCE", "LINKUSDT", "KRAKEN", "LINKUSD")));
        symbols.add(createCrypto("UNI", "Uniswap", "defi", "Decentralized exchange protocol", 18,
            Map.of("YAHOO", "UNI-USD", "COINBASE", "UNI-USD", "BINANCE", "UNIUSDT")));
        symbols.add(createCrypto("ATOM", "Cosmos", "platform", "Internet of blockchains", 6,
            Map.of("YAHOO", "ATOM-USD", "COINBASE", "ATOM-USD", "BINANCE", "ATOMUSDT", "KRAKEN", "ATOMUSD")));
        symbols.add(createCrypto("LTC", "Litecoin", "payment", "Digital silver to Bitcoin's gold", 8,
            Map.of("YAHOO", "LTC-USD", "COINBASE", "LTC-USD", "BINANCE", "LTCUSDT", "KRAKEN", "XLTCZUSD")));
        symbols.add(createCrypto("XLM", "Stellar", "payment", "Cross-border transfer platform", 7,
            Map.of("YAHOO", "XLM-USD", "COINBASE", "XLM-USD", "BINANCE", "XLMUSDT", "KRAKEN", "XXLMZUSD")));

        // DeFi Tokens
        symbols.add(createCrypto("AAVE", "Aave", "defi", "Lending and borrowing protocol", 18,
            Map.of("YAHOO", "AAVE-USD", "COINBASE", "AAVE-USD", "BINANCE", "AAVEUSDT")));
        symbols.add(createCrypto("MKR", "Maker", "defi", "DAI stablecoin governance", 18,
            Map.of("YAHOO", "MKR-USD", "COINBASE", "MKR-USD", "BINANCE", "MKRUSDT")));
        symbols.add(createCrypto("CRV", "Curve", "defi", "Stablecoin DEX", 18,
            Map.of("YAHOO", "CRV-USD", "COINBASE", "CRV-USD", "BINANCE", "CRVUSDT")));
        symbols.add(createCrypto("SNX", "Synthetix", "defi", "Synthetic assets protocol", 18,
            Map.of("YAHOO", "SNX-USD", "COINBASE", "SNX-USD", "BINANCE", "SNXUSDT")));

        // Stablecoins
        symbols.add(createCrypto("USDT", "Tether", "stablecoin", "USD-pegged stablecoin", 6,
            Map.of("YAHOO", "USDT-USD", "BINANCE", "USDTUSD")));
        symbols.add(createCrypto("USDC", "USD Coin", "stablecoin", "USD-pegged stablecoin", 6,
            Map.of("YAHOO", "USDC-USD", "COINBASE", "USDC-USD", "BINANCE", "USDCUSDT")));

        // Additional popular tokens
        symbols.add(createCrypto("ALGO", "Algorand", "platform", "Pure proof-of-stake blockchain", 6,
            Map.of("YAHOO", "ALGO-USD", "COINBASE", "ALGO-USD", "BINANCE", "ALGOUSDT")));
        symbols.add(createCrypto("VET", "VeChain", "enterprise", "Supply chain platform", 18,
            Map.of("YAHOO", "VET-USD", "BINANCE", "VETUSDT")));
        symbols.add(createCrypto("ICP", "Internet Computer", "platform", "Decentralized computing", 8,
            Map.of("YAHOO", "ICP-USD", "COINBASE", "ICP-USD", "BINANCE", "ICPUSDT")));
        symbols.add(createCrypto("FIL", "Filecoin", "storage", "Decentralized storage", 18,
            Map.of("YAHOO", "FIL-USD", "COINBASE", "FIL-USD", "BINANCE", "FILUSDT")));

        return symbols;
    }

    private List<SymbolEntity> createStockSymbols() {
        List<SymbolEntity> symbols = new ArrayList<>();

        // Major Tech
        symbols.add(createStock("AAPL", "Apple Inc.", "tech", "Consumer electronics and software"));
        symbols.add(createStock("MSFT", "Microsoft Corporation", "tech", "Software and cloud computing"));
        symbols.add(createStock("GOOGL", "Alphabet Inc.", "tech", "Search and advertising"));
        symbols.add(createStock("AMZN", "Amazon.com Inc.", "tech", "E-commerce and cloud"));
        symbols.add(createStock("META", "Meta Platforms Inc.", "tech", "Social media"));
        symbols.add(createStock("NVDA", "NVIDIA Corporation", "tech", "GPU and AI chips"));
        symbols.add(createStock("TSLA", "Tesla Inc.", "auto", "Electric vehicles"));

        // Semiconductors
        symbols.add(createStock("AMD", "Advanced Micro Devices", "tech", "Semiconductors"));
        symbols.add(createStock("INTC", "Intel Corporation", "tech", "Semiconductors"));
        symbols.add(createStock("AVGO", "Broadcom Inc.", "tech", "Semiconductors"));

        // Finance
        symbols.add(createStock("JPM", "JPMorgan Chase & Co.", "finance", "Investment banking"));
        symbols.add(createStock("BAC", "Bank of America Corp.", "finance", "Banking"));
        symbols.add(createStock("GS", "Goldman Sachs Group", "finance", "Investment banking"));
        symbols.add(createStock("V", "Visa Inc.", "finance", "Payment processing"));
        symbols.add(createStock("MA", "Mastercard Inc.", "finance", "Payment processing"));

        // Healthcare
        symbols.add(createStock("JNJ", "Johnson & Johnson", "healthcare", "Pharmaceuticals"));
        symbols.add(createStock("UNH", "UnitedHealth Group", "healthcare", "Health insurance"));
        symbols.add(createStock("PFE", "Pfizer Inc.", "healthcare", "Pharmaceuticals"));

        // Consumer
        symbols.add(createStock("PG", "Procter & Gamble Co.", "consumer", "Consumer goods"));
        symbols.add(createStock("KO", "Coca-Cola Company", "consumer", "Beverages"));
        symbols.add(createStock("WMT", "Walmart Inc.", "retail", "Retail"));

        // Energy
        symbols.add(createStock("XOM", "Exxon Mobil Corporation", "energy", "Oil and gas"));
        symbols.add(createStock("CVX", "Chevron Corporation", "energy", "Oil and gas"));

        return symbols;
    }

    private List<SymbolEntity> createEtfSymbols() {
        List<SymbolEntity> symbols = new ArrayList<>();

        // Major Index ETFs
        symbols.add(createEtf("SPY", "SPDR S&P 500 ETF Trust", "index", "Tracks S&P 500 index"));
        symbols.add(createEtf("VOO", "Vanguard S&P 500 ETF", "index", "Tracks S&P 500 index"));
        symbols.add(createEtf("QQQ", "Invesco QQQ Trust", "index", "Tracks Nasdaq 100"));
        symbols.add(createEtf("DIA", "SPDR Dow Jones Industrial Average ETF", "index", "Tracks Dow Jones"));
        symbols.add(createEtf("IWM", "iShares Russell 2000 ETF", "index", "Small cap stocks"));
        symbols.add(createEtf("VTI", "Vanguard Total Stock Market ETF", "index", "Total US stock market"));

        // Sector ETFs
        symbols.add(createEtf("XLK", "Technology Select Sector SPDR", "sector", "Technology sector"));
        symbols.add(createEtf("XLF", "Financial Select Sector SPDR", "sector", "Financial sector"));
        symbols.add(createEtf("XLE", "Energy Select Sector SPDR", "sector", "Energy sector"));
        symbols.add(createEtf("XLV", "Health Care Select Sector SPDR", "sector", "Healthcare sector"));

        // Bond ETFs
        symbols.add(createEtf("AGG", "iShares Core U.S. Aggregate Bond ETF", "bond", "US investment grade bonds"));
        symbols.add(createEtf("TLT", "iShares 20+ Year Treasury Bond ETF", "bond", "Long-term treasuries"));

        // International ETFs
        symbols.add(createEtf("EFA", "iShares MSCI EAFE ETF", "international", "Developed markets ex-US"));
        symbols.add(createEtf("VWO", "Vanguard FTSE Emerging Markets ETF", "international", "Emerging markets"));

        // Commodity ETFs
        symbols.add(createEtf("GLD", "SPDR Gold Shares", "commodity", "Gold"));
        symbols.add(createEtf("SLV", "iShares Silver Trust", "commodity", "Silver"));

        return symbols;
    }

    private List<SymbolEntity> createFiatSymbols() {
        List<SymbolEntity> symbols = new ArrayList<>();

        symbols.add(createFiat("USD", "US Dollar", "United States Dollar"));
        symbols.add(createFiat("EUR", "Euro", "European Union currency"));
        symbols.add(createFiat("GBP", "British Pound", "Pound Sterling"));
        symbols.add(createFiat("JPY", "Japanese Yen", "Japanese currency"));
        symbols.add(createFiat("CAD", "Canadian Dollar", "Canadian currency"));
        symbols.add(createFiat("AUD", "Australian Dollar", "Australian currency"));
        symbols.add(createFiat("CHF", "Swiss Franc", "Swiss currency"));

        return symbols;
    }

    // === Factory Methods ===

    private SymbolEntity createCrypto(String symbol, String name, String category, String description,
                                       int decimals, Map<String, String> providerSymbols) {
        SymbolEntity entity = new SymbolEntity(symbol, name, "CRYPTO");
        entity.setCategory(category);
        entity.setDescription(description);
        entity.setDecimals(decimals);
        entity.setProviderSymbols(new HashMap<>(providerSymbols));
        entity.setCollectionActive(true);
        entity.setPrimaryDataSource("YAHOO");
        entity.setTimeframes(List.of("1Day"));
        entity.setStatus("ACTIVE");
        return entity;
    }

    private SymbolEntity createStock(String symbol, String name, String category, String description) {
        SymbolEntity entity = new SymbolEntity(symbol, name, "STOCK");
        entity.setCategory(category);
        entity.setDescription(description);
        entity.setDecimals(2);
        entity.setProviderSymbols(Map.of("YAHOO", symbol, "ALPACA", symbol));
        entity.setCollectionActive(true);
        entity.setPrimaryDataSource("YAHOO");
        entity.setTimeframes(List.of("1Day"));
        entity.setStatus("ACTIVE");
        return entity;
    }

    private SymbolEntity createEtf(String symbol, String name, String category, String description) {
        SymbolEntity entity = new SymbolEntity(symbol, name, "ETF");
        entity.setCategory(category);
        entity.setDescription(description);
        entity.setDecimals(2);
        entity.setProviderSymbols(Map.of("YAHOO", symbol, "ALPACA", symbol));
        entity.setCollectionActive(true);
        entity.setPrimaryDataSource("YAHOO");
        entity.setTimeframes(List.of("1Day"));
        entity.setStatus("ACTIVE");
        return entity;
    }

    private SymbolEntity createFiat(String symbol, String name, String description) {
        SymbolEntity entity = new SymbolEntity(symbol, name, "FIAT");
        entity.setCategory("currency");
        entity.setDescription(description);
        entity.setDecimals(2);
        entity.setCollectionActive(false);  // Fiat currencies not collected
        entity.setStatus("ACTIVE");
        return entity;
    }
}
