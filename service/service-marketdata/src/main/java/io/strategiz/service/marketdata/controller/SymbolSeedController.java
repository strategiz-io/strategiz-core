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

        // ============================================================
        // S&P 500 COMPLETE LIST (503 stocks as of Dec 2024)
        // ============================================================

        // Mega Cap Tech
        symbols.add(createStock("NVDA", "NVIDIA Corporation", "tech", "GPU and AI chips"));
        symbols.add(createStock("AAPL", "Apple Inc.", "tech", "Consumer electronics"));
        symbols.add(createStock("GOOGL", "Alphabet Inc. Class A", "tech", "Search and advertising"));
        symbols.add(createStock("GOOG", "Alphabet Inc. Class C", "tech", "Search and advertising"));
        symbols.add(createStock("MSFT", "Microsoft Corporation", "tech", "Software and cloud"));
        symbols.add(createStock("AMZN", "Amazon.com Inc.", "tech", "E-commerce and cloud"));
        symbols.add(createStock("META", "Meta Platforms Inc.", "tech", "Social media"));
        symbols.add(createStock("TSLA", "Tesla Inc.", "auto", "Electric vehicles"));
        symbols.add(createStock("AVGO", "Broadcom Inc.", "tech", "Semiconductors"));

        // Finance - Major Banks & Financial Services
        symbols.add(createStock("BRK.B", "Berkshire Hathaway B", "finance", "Conglomerate"));
        symbols.add(createStock("JPM", "JPMorgan Chase", "finance", "Banking"));
        symbols.add(createStock("V", "Visa Inc.", "finance", "Payments"));
        symbols.add(createStock("MA", "Mastercard Inc.", "finance", "Payments"));
        symbols.add(createStock("BAC", "Bank of America", "finance", "Banking"));
        symbols.add(createStock("WFC", "Wells Fargo", "finance", "Banking"));
        symbols.add(createStock("MS", "Morgan Stanley", "finance", "Investment banking"));
        symbols.add(createStock("GS", "Goldman Sachs", "finance", "Investment banking"));
        symbols.add(createStock("SCHW", "Charles Schwab", "finance", "Brokerage"));
        symbols.add(createStock("C", "Citigroup Inc.", "finance", "Banking"));
        symbols.add(createStock("AXP", "American Express", "finance", "Financial services"));
        symbols.add(createStock("BLK", "BlackRock Inc.", "finance", "Asset management"));
        symbols.add(createStock("BX", "Blackstone Inc.", "finance", "Private equity"));
        symbols.add(createStock("KKR", "KKR & Co.", "finance", "Private equity"));
        symbols.add(createStock("COF", "Capital One", "finance", "Banking"));
        symbols.add(createStock("USB", "U.S. Bancorp", "finance", "Banking"));
        symbols.add(createStock("PNC", "PNC Financial", "finance", "Banking"));
        symbols.add(createStock("TFC", "Truist Financial", "finance", "Banking"));
        symbols.add(createStock("BK", "Bank of New York Mellon", "finance", "Banking"));
        symbols.add(createStock("AIG", "American International Group", "finance", "Insurance"));
        symbols.add(createStock("MET", "MetLife Inc.", "finance", "Insurance"));
        symbols.add(createStock("PRU", "Prudential Financial", "finance", "Insurance"));
        symbols.add(createStock("ALL", "Allstate Corp.", "finance", "Insurance"));
        symbols.add(createStock("AFL", "Aflac Inc.", "finance", "Insurance"));
        symbols.add(createStock("CB", "Chubb Limited", "finance", "Insurance"));
        symbols.add(createStock("TRV", "Travelers Companies", "finance", "Insurance"));
        symbols.add(createStock("AMP", "Ameriprise Financial", "finance", "Financial services"));
        symbols.add(createStock("CME", "CME Group", "finance", "Exchanges"));
        symbols.add(createStock("ICE", "Intercontinental Exchange", "finance", "Exchanges"));
        symbols.add(createStock("SPGI", "S&P Global", "finance", "Financial data"));
        symbols.add(createStock("MCO", "Moody's Corp.", "finance", "Credit ratings"));
        symbols.add(createStock("MMC", "Marsh & McLennan", "finance", "Insurance brokerage"));
        symbols.add(createStock("AON", "Aon plc", "finance", "Insurance brokerage"));
        symbols.add(createStock("MSCI", "MSCI Inc.", "finance", "Financial indices"));
        symbols.add(createStock("NDAQ", "Nasdaq Inc.", "finance", "Exchanges"));
        symbols.add(createStock("FIS", "Fidelity National", "finance", "Fintech"));
        symbols.add(createStock("FISV", "Fiserv Inc.", "finance", "Fintech"));
        symbols.add(createStock("PYPL", "PayPal Holdings", "finance", "Digital payments"));
        symbols.add(createStock("SYF", "Synchrony Financial", "finance", "Consumer finance"));
        symbols.add(createStock("DFS", "Discover Financial", "finance", "Credit cards"));
        symbols.add(createStock("FITB", "Fifth Third Bancorp", "finance", "Banking"));
        symbols.add(createStock("MTB", "M&T Bank Corp.", "finance", "Banking"));
        symbols.add(createStock("HBAN", "Huntington Bancshares", "finance", "Banking"));
        symbols.add(createStock("CFG", "Citizens Financial", "finance", "Banking"));
        symbols.add(createStock("RF", "Regions Financial", "finance", "Banking"));
        symbols.add(createStock("KEY", "KeyCorp", "finance", "Banking"));
        symbols.add(createStock("STT", "State Street Corp.", "finance", "Asset management"));
        symbols.add(createStock("NTRS", "Northern Trust", "finance", "Asset management"));
        symbols.add(createStock("TROW", "T. Rowe Price", "finance", "Asset management"));
        symbols.add(createStock("BEN", "Franklin Resources", "finance", "Asset management"));
        symbols.add(createStock("IVZ", "Invesco Ltd.", "finance", "Asset management"));
        symbols.add(createStock("ACGL", "Arch Capital Group", "finance", "Insurance"));
        symbols.add(createStock("HIG", "Hartford Financial", "finance", "Insurance"));
        symbols.add(createStock("WRB", "W.R. Berkley", "finance", "Insurance"));
        symbols.add(createStock("CINF", "Cincinnati Financial", "finance", "Insurance"));
        symbols.add(createStock("L", "Loews Corp.", "finance", "Conglomerate"));
        symbols.add(createStock("GL", "Globe Life Inc.", "finance", "Insurance"));
        symbols.add(createStock("AIZ", "Assurant Inc.", "finance", "Insurance"));
        symbols.add(createStock("RJF", "Raymond James", "finance", "Financial services"));
        symbols.add(createStock("HOOD", "Robinhood Markets", "finance", "Brokerage"));
        symbols.add(createStock("COIN", "Coinbase Global", "finance", "Crypto exchange"));
        symbols.add(createStock("APO", "Apollo Global", "finance", "Private equity"));
        symbols.add(createStock("ARES", "Ares Management", "finance", "Private equity"));
        symbols.add(createStock("IBKR", "Interactive Brokers", "finance", "Brokerage"));

        // Healthcare & Pharmaceuticals
        symbols.add(createStock("LLY", "Eli Lilly", "healthcare", "Pharmaceuticals"));
        symbols.add(createStock("UNH", "UnitedHealth Group", "healthcare", "Health insurance"));
        symbols.add(createStock("JNJ", "Johnson & Johnson", "healthcare", "Pharmaceuticals"));
        symbols.add(createStock("ABBV", "AbbVie Inc.", "healthcare", "Pharmaceuticals"));
        symbols.add(createStock("MRK", "Merck & Co.", "healthcare", "Pharmaceuticals"));
        symbols.add(createStock("PFE", "Pfizer Inc.", "healthcare", "Pharmaceuticals"));
        symbols.add(createStock("TMO", "Thermo Fisher", "healthcare", "Life sciences"));
        symbols.add(createStock("ABT", "Abbott Labs", "healthcare", "Medical devices"));
        symbols.add(createStock("DHR", "Danaher Corp.", "healthcare", "Life sciences"));
        symbols.add(createStock("BMY", "Bristol-Myers Squibb", "healthcare", "Pharmaceuticals"));
        symbols.add(createStock("AMGN", "Amgen Inc.", "healthcare", "Biotechnology"));
        symbols.add(createStock("GILD", "Gilead Sciences", "healthcare", "Biotechnology"));
        symbols.add(createStock("VRTX", "Vertex Pharmaceuticals", "healthcare", "Biotechnology"));
        symbols.add(createStock("REGN", "Regeneron", "healthcare", "Biotechnology"));
        symbols.add(createStock("ISRG", "Intuitive Surgical", "healthcare", "Medical devices"));
        symbols.add(createStock("SYK", "Stryker Corp.", "healthcare", "Medical devices"));
        symbols.add(createStock("BSX", "Boston Scientific", "healthcare", "Medical devices"));
        symbols.add(createStock("MDT", "Medtronic plc", "healthcare", "Medical devices"));
        symbols.add(createStock("ELV", "Elevance Health", "healthcare", "Health insurance"));
        symbols.add(createStock("CI", "Cigna Group", "healthcare", "Health insurance"));
        symbols.add(createStock("HCA", "HCA Healthcare", "healthcare", "Hospitals"));
        symbols.add(createStock("MCK", "McKesson Corp.", "healthcare", "Healthcare distribution"));
        symbols.add(createStock("CVS", "CVS Health", "healthcare", "Pharmacy"));
        symbols.add(createStock("COR", "Cencora Inc.", "healthcare", "Healthcare distribution"));
        symbols.add(createStock("CAH", "Cardinal Health", "healthcare", "Healthcare distribution"));
        symbols.add(createStock("EW", "Edwards Lifesciences", "healthcare", "Medical devices"));
        symbols.add(createStock("ZTS", "Zoetis Inc.", "healthcare", "Animal health"));
        symbols.add(createStock("BDX", "Becton Dickinson", "healthcare", "Medical devices"));
        symbols.add(createStock("IDXX", "IDEXX Labs", "healthcare", "Diagnostics"));
        symbols.add(createStock("IQV", "IQVIA Holdings", "healthcare", "Healthcare analytics"));
        symbols.add(createStock("RMD", "ResMed Inc.", "healthcare", "Medical devices"));
        symbols.add(createStock("A", "Agilent Technologies", "healthcare", "Life sciences"));
        symbols.add(createStock("DXCM", "Dexcom Inc.", "healthcare", "Medical devices"));
        symbols.add(createStock("GEHC", "GE HealthCare", "healthcare", "Medical equipment"));
        symbols.add(createStock("HUM", "Humana Inc.", "healthcare", "Health insurance"));
        symbols.add(createStock("CNC", "Centene Corp.", "healthcare", "Health insurance"));
        symbols.add(createStock("MOH", "Molina Healthcare", "healthcare", "Health insurance"));
        symbols.add(createStock("LH", "Labcorp Holdings", "healthcare", "Diagnostics"));
        symbols.add(createStock("DGX", "Quest Diagnostics", "healthcare", "Diagnostics"));
        symbols.add(createStock("STE", "Steris plc", "healthcare", "Medical equipment"));
        symbols.add(createStock("WAT", "Waters Corp.", "healthcare", "Life sciences"));
        symbols.add(createStock("BIIB", "Biogen Inc.", "healthcare", "Biotechnology"));
        symbols.add(createStock("MRNA", "Moderna Inc.", "healthcare", "Biotechnology"));
        symbols.add(createStock("ALGN", "Align Technology", "healthcare", "Medical devices"));
        symbols.add(createStock("PODD", "Insulet Corp.", "healthcare", "Medical devices"));
        symbols.add(createStock("INCY", "Incyte Corp.", "healthcare", "Biotechnology"));
        symbols.add(createStock("HOLX", "Hologic Inc.", "healthcare", "Medical devices"));
        symbols.add(createStock("BAX", "Baxter International", "healthcare", "Medical devices"));
        symbols.add(createStock("VTRS", "Viatris Inc.", "healthcare", "Pharmaceuticals"));
        symbols.add(createStock("CRL", "Charles River Labs", "healthcare", "Life sciences"));
        symbols.add(createStock("TECH", "Bio-Techne Corp.", "healthcare", "Life sciences"));
        symbols.add(createStock("RVTY", "Revvity Inc.", "healthcare", "Life sciences"));
        symbols.add(createStock("DVA", "DaVita Inc.", "healthcare", "Dialysis services"));
        symbols.add(createStock("UHS", "Universal Health", "healthcare", "Hospitals"));
        symbols.add(createStock("HSIC", "Henry Schein", "healthcare", "Healthcare distribution"));

        // Tech - Semiconductors
        symbols.add(createStock("MU", "Micron Technology", "tech", "Semiconductors"));
        symbols.add(createStock("AMD", "Advanced Micro Devices", "tech", "Semiconductors"));
        symbols.add(createStock("QCOM", "Qualcomm Inc.", "tech", "Semiconductors"));
        symbols.add(createStock("INTC", "Intel Corp.", "tech", "Semiconductors"));
        symbols.add(createStock("TXN", "Texas Instruments", "tech", "Semiconductors"));
        symbols.add(createStock("LRCX", "Lam Research", "tech", "Semiconductor equipment"));
        symbols.add(createStock("AMAT", "Applied Materials", "tech", "Semiconductor equipment"));
        symbols.add(createStock("KLAC", "KLA Corp.", "tech", "Semiconductor equipment"));
        symbols.add(createStock("ADI", "Analog Devices", "tech", "Semiconductors"));
        symbols.add(createStock("NXPI", "NXP Semiconductors", "tech", "Semiconductors"));
        symbols.add(createStock("MCHP", "Microchip Technology", "tech", "Semiconductors"));
        symbols.add(createStock("ON", "ON Semiconductor", "tech", "Semiconductors"));
        symbols.add(createStock("MPWR", "Monolithic Power", "tech", "Semiconductors"));
        symbols.add(createStock("SWKS", "Skyworks Solutions", "tech", "Semiconductors"));
        symbols.add(createStock("MRVL", "Marvell Technology", "tech", "Semiconductors"));
        symbols.add(createStock("TER", "Teradyne Inc.", "tech", "Semiconductor equipment"));
        symbols.add(createStock("SMCI", "Super Micro Computer", "tech", "Servers"));

        // Tech - Software & Cloud
        symbols.add(createStock("ORCL", "Oracle Corp.", "tech", "Enterprise software"));
        symbols.add(createStock("CRM", "Salesforce Inc.", "tech", "Cloud CRM"));
        symbols.add(createStock("INTU", "Intuit Inc.", "tech", "Financial software"));
        symbols.add(createStock("ADBE", "Adobe Inc.", "tech", "Creative software"));
        symbols.add(createStock("NOW", "ServiceNow Inc.", "tech", "Cloud workflow"));
        symbols.add(createStock("PANW", "Palo Alto Networks", "tech", "Cybersecurity"));
        symbols.add(createStock("SNPS", "Synopsys Inc.", "tech", "EDA software"));
        symbols.add(createStock("CDNS", "Cadence Design", "tech", "EDA software"));
        symbols.add(createStock("CRWD", "CrowdStrike", "tech", "Cybersecurity"));
        symbols.add(createStock("WDAY", "Workday Inc.", "tech", "HR software"));
        symbols.add(createStock("FTNT", "Fortinet Inc.", "tech", "Cybersecurity"));
        symbols.add(createStock("ADSK", "Autodesk Inc.", "tech", "Design software"));
        symbols.add(createStock("DDOG", "Datadog Inc.", "tech", "Cloud monitoring"));
        symbols.add(createStock("FICO", "Fair Isaac Corp.", "tech", "Analytics software"));
        symbols.add(createStock("CTSH", "Cognizant Tech", "tech", "IT services"));
        symbols.add(createStock("ANSS", "ANSYS Inc.", "tech", "Simulation software"));
        symbols.add(createStock("IT", "Gartner Inc.", "tech", "IT research"));
        symbols.add(createStock("VRSN", "VeriSign Inc.", "tech", "Internet infrastructure"));
        symbols.add(createStock("AKAM", "Akamai Technologies", "tech", "Cloud delivery"));
        symbols.add(createStock("FFIV", "F5 Inc.", "tech", "Networking"));
        symbols.add(createStock("PTC", "PTC Inc.", "tech", "Industrial software"));
        symbols.add(createStock("TYL", "Tyler Technologies", "tech", "Government software"));
        symbols.add(createStock("PAYC", "Paycom Software", "tech", "HR software"));
        symbols.add(createStock("GDDY", "GoDaddy Inc.", "tech", "Web services"));
        symbols.add(createStock("EPAM", "EPAM Systems", "tech", "IT services"));
        symbols.add(createStock("CPAY", "Corpay Inc.", "tech", "Payments"));

        // Tech - Hardware & Networking
        symbols.add(createStock("CSCO", "Cisco Systems", "tech", "Networking"));
        symbols.add(createStock("IBM", "IBM Corp.", "tech", "Enterprise technology"));
        symbols.add(createStock("ANET", "Arista Networks", "tech", "Networking"));
        symbols.add(createStock("APH", "Amphenol Corp.", "tech", "Connectors"));
        symbols.add(createStock("GLW", "Corning Inc.", "tech", "Specialty glass"));
        symbols.add(createStock("TEL", "TE Connectivity", "tech", "Connectors"));
        symbols.add(createStock("MSI", "Motorola Solutions", "tech", "Communications"));
        symbols.add(createStock("HPQ", "HP Inc.", "tech", "PCs and printers"));
        symbols.add(createStock("HPE", "Hewlett Packard Enterprise", "tech", "Enterprise IT"));
        symbols.add(createStock("DELL", "Dell Technologies", "tech", "Computers"));
        symbols.add(createStock("STX", "Seagate Technology", "tech", "Storage"));
        symbols.add(createStock("WDC", "Western Digital", "tech", "Storage"));
        symbols.add(createStock("NTAP", "NetApp Inc.", "tech", "Storage"));
        symbols.add(createStock("ZBRA", "Zebra Technologies", "tech", "Barcode/RFID"));
        symbols.add(createStock("KEYS", "Keysight Technologies", "tech", "Test equipment"));
        symbols.add(createStock("TRMB", "Trimble Inc.", "tech", "GPS technology"));
        symbols.add(createStock("JBL", "Jabil Inc.", "tech", "Electronics manufacturing"));
        symbols.add(createStock("CDW", "CDW Corp.", "tech", "IT solutions"));

        // Consumer - Retail & E-commerce
        symbols.add(createStock("WMT", "Walmart Inc.", "retail", "Discount retail"));
        symbols.add(createStock("COST", "Costco Wholesale", "retail", "Warehouse retail"));
        symbols.add(createStock("HD", "Home Depot", "retail", "Home improvement"));
        symbols.add(createStock("TGT", "Target Corp.", "retail", "Discount retail"));
        symbols.add(createStock("LOW", "Lowe's Companies", "retail", "Home improvement"));
        symbols.add(createStock("TJX", "TJX Companies", "retail", "Off-price retail"));
        symbols.add(createStock("ORLY", "O'Reilly Automotive", "retail", "Auto parts"));
        symbols.add(createStock("AZO", "AutoZone Inc.", "retail", "Auto parts"));
        symbols.add(createStock("ROST", "Ross Stores", "retail", "Off-price retail"));
        symbols.add(createStock("EBAY", "eBay Inc.", "retail", "E-commerce"));
        symbols.add(createStock("DLTR", "Dollar Tree", "retail", "Discount retail"));
        symbols.add(createStock("DG", "Dollar General", "retail", "Discount retail"));
        symbols.add(createStock("BBY", "Best Buy Co.", "retail", "Electronics retail"));
        symbols.add(createStock("TSCO", "Tractor Supply", "retail", "Farm supplies"));
        symbols.add(createStock("ULTA", "Ulta Beauty", "retail", "Beauty retail"));
        symbols.add(createStock("KR", "Kroger Co.", "retail", "Grocery"));
        symbols.add(createStock("WSM", "Williams-Sonoma", "retail", "Home goods"));
        symbols.add(createStock("DECK", "Deckers Outdoor", "retail", "Footwear"));
        symbols.add(createStock("POOL", "Pool Corp.", "retail", "Pool supplies"));
        symbols.add(createStock("CTAS", "Cintas Corp.", "services", "Uniforms"));
        symbols.add(createStock("FAST", "Fastenal Co.", "industrial", "Industrial distribution"));

        // Consumer - Food & Beverage
        symbols.add(createStock("KO", "Coca-Cola Co.", "consumer", "Beverages"));
        symbols.add(createStock("PEP", "PepsiCo Inc.", "consumer", "Beverages"));
        symbols.add(createStock("PG", "Procter & Gamble", "consumer", "Consumer goods"));
        symbols.add(createStock("PM", "Philip Morris", "consumer", "Tobacco"));
        symbols.add(createStock("MO", "Altria Group", "consumer", "Tobacco"));
        symbols.add(createStock("MDLZ", "Mondelez Intl", "consumer", "Snacks"));
        symbols.add(createStock("CL", "Colgate-Palmolive", "consumer", "Consumer products"));
        symbols.add(createStock("KMB", "Kimberly-Clark", "consumer", "Consumer products"));
        symbols.add(createStock("GIS", "General Mills", "consumer", "Food products"));
        symbols.add(createStock("KDP", "Keurig Dr Pepper", "consumer", "Beverages"));
        symbols.add(createStock("HSY", "Hershey Co.", "consumer", "Confectionery"));
        symbols.add(createStock("STZ", "Constellation Brands", "consumer", "Alcoholic beverages"));
        symbols.add(createStock("MKC", "McCormick & Co.", "consumer", "Spices"));
        symbols.add(createStock("ADM", "Archer-Daniels-Midland", "consumer", "Agriculture"));
        symbols.add(createStock("SJM", "J.M. Smucker", "consumer", "Food products"));
        symbols.add(createStock("CAG", "Conagra Brands", "consumer", "Food products"));
        symbols.add(createStock("CPB", "Campbell Soup", "consumer", "Food products"));
        symbols.add(createStock("TSN", "Tyson Foods", "consumer", "Food products"));
        symbols.add(createStock("HRL", "Hormel Foods", "consumer", "Food products"));
        symbols.add(createStock("CHD", "Church & Dwight", "consumer", "Consumer products"));
        symbols.add(createStock("CLX", "Clorox Co.", "consumer", "Consumer products"));
        symbols.add(createStock("EL", "Estee Lauder", "consumer", "Cosmetics"));
        symbols.add(createStock("MNST", "Monster Beverage", "consumer", "Beverages"));
        symbols.add(createStock("TAP", "Molson Coors", "consumer", "Beverages"));
        symbols.add(createStock("BG", "Bunge Global", "consumer", "Agriculture"));
        symbols.add(createStock("KHC", "Kraft Heinz", "consumer", "Food products"));

        // Consumer - Restaurants & Leisure
        symbols.add(createStock("MCD", "McDonald's Corp.", "consumer", "Fast food"));
        symbols.add(createStock("SBUX", "Starbucks Corp.", "consumer", "Coffee"));
        symbols.add(createStock("NKE", "Nike Inc.", "consumer", "Athletic apparel"));
        symbols.add(createStock("BKNG", "Booking Holdings", "consumer", "Travel"));
        symbols.add(createStock("CMG", "Chipotle Mexican Grill", "consumer", "Fast casual"));
        symbols.add(createStock("YUM", "Yum! Brands", "consumer", "Fast food"));
        symbols.add(createStock("DRI", "Darden Restaurants", "consumer", "Casual dining"));
        symbols.add(createStock("DPZ", "Domino's Pizza", "consumer", "Pizza"));
        symbols.add(createStock("LVS", "Las Vegas Sands", "consumer", "Casinos"));
        symbols.add(createStock("WYNN", "Wynn Resorts", "consumer", "Casinos"));
        symbols.add(createStock("MGM", "MGM Resorts", "consumer", "Casinos"));
        symbols.add(createStock("MAR", "Marriott Intl", "consumer", "Hotels"));
        symbols.add(createStock("HLT", "Hilton Worldwide", "consumer", "Hotels"));
        symbols.add(createStock("EXPE", "Expedia Group", "consumer", "Travel"));
        symbols.add(createStock("ABNB", "Airbnb Inc.", "consumer", "Travel"));
        symbols.add(createStock("CCL", "Carnival Corp.", "consumer", "Cruise lines"));
        symbols.add(createStock("RCL", "Royal Caribbean", "consumer", "Cruise lines"));
        symbols.add(createStock("NCLH", "Norwegian Cruise Line", "consumer", "Cruise lines"));
        symbols.add(createStock("LYV", "Live Nation", "consumer", "Entertainment"));
        symbols.add(createStock("LULU", "Lululemon", "consumer", "Athletic apparel"));
        symbols.add(createStock("TPR", "Tapestry Inc.", "consumer", "Luxury goods"));
        symbols.add(createStock("RL", "Ralph Lauren", "consumer", "Apparel"));
        symbols.add(createStock("HAS", "Hasbro Inc.", "consumer", "Toys"));

        // Media & Communications
        symbols.add(createStock("DIS", "Walt Disney", "media", "Entertainment"));
        symbols.add(createStock("NFLX", "Netflix Inc.", "media", "Streaming"));
        symbols.add(createStock("CMCSA", "Comcast Corp.", "media", "Cable"));
        symbols.add(createStock("T", "AT&T Inc.", "telecom", "Telecommunications"));
        symbols.add(createStock("VZ", "Verizon Communications", "telecom", "Telecommunications"));
        symbols.add(createStock("TMUS", "T-Mobile US", "telecom", "Telecommunications"));
        symbols.add(createStock("CHTR", "Charter Communications", "media", "Cable"));
        symbols.add(createStock("TTWO", "Take-Two Interactive", "media", "Video games"));
        symbols.add(createStock("EA", "Electronic Arts", "media", "Video games"));
        symbols.add(createStock("WBD", "Warner Bros Discovery", "media", "Entertainment"));
        symbols.add(createStock("FOXA", "Fox Corp. Class A", "media", "Broadcasting"));
        symbols.add(createStock("FOX", "Fox Corp. Class B", "media", "Broadcasting"));
        symbols.add(createStock("NWS", "News Corp. Class B", "media", "Publishing"));
        symbols.add(createStock("NWSA", "News Corp. Class A", "media", "Publishing"));
        symbols.add(createStock("OMC", "Omnicom Group", "media", "Advertising"));
        symbols.add(createStock("IPG", "Interpublic Group", "media", "Advertising"));
        symbols.add(createStock("PARA", "Paramount Global", "media", "Entertainment"));
        symbols.add(createStock("MTCH", "Match Group", "tech", "Online dating"));
        symbols.add(createStock("TTD", "Trade Desk Inc.", "tech", "Digital advertising"));

        // Tech - Internet & Digital
        symbols.add(createStock("PLTR", "Palantir Technologies", "tech", "Data analytics"));
        symbols.add(createStock("UBER", "Uber Technologies", "tech", "Ride-sharing"));
        symbols.add(createStock("DASH", "DoorDash Inc.", "tech", "Food delivery"));
        symbols.add(createStock("CVNA", "Carvana Co.", "tech", "Auto e-commerce"));
        symbols.add(createStock("APP", "AppLovin Corp.", "tech", "Mobile advertising"));
        symbols.add(createStock("AXON", "Axon Enterprise", "tech", "Law enforcement tech"));

        // Industrials - Aerospace & Defense
        symbols.add(createStock("BA", "Boeing Co.", "industrial", "Aerospace"));
        symbols.add(createStock("RTX", "RTX Corp.", "industrial", "Aerospace & defense"));
        symbols.add(createStock("LMT", "Lockheed Martin", "industrial", "Defense"));
        symbols.add(createStock("GE", "General Electric", "industrial", "Conglomerate"));
        symbols.add(createStock("GEV", "GE Vernova", "industrial", "Energy equipment"));
        symbols.add(createStock("NOC", "Northrop Grumman", "industrial", "Defense"));
        symbols.add(createStock("GD", "General Dynamics", "industrial", "Defense"));
        symbols.add(createStock("LHX", "L3Harris Technologies", "industrial", "Defense electronics"));
        symbols.add(createStock("HII", "Huntington Ingalls", "industrial", "Shipbuilding"));
        symbols.add(createStock("TXT", "Textron Inc.", "industrial", "Aerospace"));
        symbols.add(createStock("TDG", "TransDigm Group", "industrial", "Aerospace components"));
        symbols.add(createStock("HWM", "Howmet Aerospace", "industrial", "Aerospace components"));

        // Industrials - Machinery & Equipment
        symbols.add(createStock("CAT", "Caterpillar Inc.", "industrial", "Heavy equipment"));
        symbols.add(createStock("DE", "Deere & Company", "industrial", "Agricultural equipment"));
        symbols.add(createStock("HON", "Honeywell Intl", "industrial", "Diversified industrial"));
        symbols.add(createStock("ETN", "Eaton Corp.", "industrial", "Electrical equipment"));
        symbols.add(createStock("PH", "Parker-Hannifin", "industrial", "Motion control"));
        symbols.add(createStock("EMR", "Emerson Electric", "industrial", "Automation"));
        symbols.add(createStock("ROK", "Rockwell Automation", "industrial", "Industrial automation"));
        symbols.add(createStock("ITW", "Illinois Tool Works", "industrial", "Diversified industrial"));
        symbols.add(createStock("CMI", "Cummins Inc.", "industrial", "Engines"));
        symbols.add(createStock("PCAR", "PACCAR Inc.", "industrial", "Trucks"));
        symbols.add(createStock("AME", "AMETEK Inc.", "industrial", "Electronic instruments"));
        symbols.add(createStock("DOV", "Dover Corp.", "industrial", "Industrial equipment"));
        symbols.add(createStock("SWK", "Stanley Black & Decker", "industrial", "Tools"));
        symbols.add(createStock("GWW", "W.W. Grainger", "industrial", "Industrial distribution"));
        symbols.add(createStock("ROP", "Roper Technologies", "industrial", "Diversified industrial"));
        symbols.add(createStock("IR", "Ingersoll Rand", "industrial", "Industrial equipment"));
        symbols.add(createStock("FTV", "Fortive Corp.", "industrial", "Industrial technology"));
        symbols.add(createStock("OTIS", "Otis Worldwide", "industrial", "Elevators"));
        symbols.add(createStock("CARR", "Carrier Global", "industrial", "HVAC"));
        symbols.add(createStock("TT", "Trane Technologies", "industrial", "HVAC"));
        symbols.add(createStock("JCI", "Johnson Controls", "industrial", "Building tech"));
        symbols.add(createStock("XYL", "Xylem Inc.", "industrial", "Water technology"));
        symbols.add(createStock("IEX", "IDEX Corp.", "industrial", "Pumps"));
        symbols.add(createStock("NDSN", "Nordson Corp.", "industrial", "Precision equipment"));
        symbols.add(createStock("PNR", "Pentair plc", "industrial", "Water equipment"));
        symbols.add(createStock("SNA", "Snap-on Inc.", "industrial", "Tools"));
        symbols.add(createStock("AOS", "A.O. Smith", "industrial", "Water heaters"));

        // Industrials - Transportation
        symbols.add(createStock("UPS", "United Parcel Service", "industrial", "Logistics"));
        symbols.add(createStock("FDX", "FedEx Corp.", "industrial", "Logistics"));
        symbols.add(createStock("UNP", "Union Pacific", "industrial", "Railroads"));
        symbols.add(createStock("CSX", "CSX Corp.", "industrial", "Railroads"));
        symbols.add(createStock("NSC", "Norfolk Southern", "industrial", "Railroads"));
        symbols.add(createStock("DAL", "Delta Air Lines", "industrial", "Airlines"));
        symbols.add(createStock("UAL", "United Airlines", "industrial", "Airlines"));
        symbols.add(createStock("LUV", "Southwest Airlines", "industrial", "Airlines"));
        symbols.add(createStock("ODFL", "Old Dominion Freight", "industrial", "Trucking"));
        symbols.add(createStock("JBHT", "J.B. Hunt Transport", "industrial", "Trucking"));
        symbols.add(createStock("CHRW", "C.H. Robinson", "industrial", "Freight"));
        symbols.add(createStock("EXPD", "Expeditors Intl", "industrial", "Freight"));
        symbols.add(createStock("WAB", "Westinghouse Air Brake", "industrial", "Rail equipment"));

        // Industrials - Building & Construction
        symbols.add(createStock("VMC", "Vulcan Materials", "industrial", "Construction materials"));
        symbols.add(createStock("MLM", "Martin Marietta", "industrial", "Construction materials"));
        symbols.add(createStock("CRH", "CRH plc", "industrial", "Building materials"));
        symbols.add(createStock("FCX", "Freeport-McMoRan", "industrial", "Mining"));
        symbols.add(createStock("NUE", "Nucor Corp.", "industrial", "Steel"));
        symbols.add(createStock("STLD", "Steel Dynamics", "industrial", "Steel"));
        symbols.add(createStock("NEM", "Newmont Corp.", "industrial", "Gold mining"));
        symbols.add(createStock("EME", "EMCOR Group", "industrial", "Construction"));
        symbols.add(createStock("PWR", "Quanta Services", "industrial", "Infrastructure"));
        symbols.add(createStock("BLDR", "Builders FirstSource", "industrial", "Building products"));
        symbols.add(createStock("LEN", "Lennar Corp.", "industrial", "Homebuilding"));
        symbols.add(createStock("DHI", "D.R. Horton", "industrial", "Homebuilding"));
        symbols.add(createStock("PHM", "PulteGroup Inc.", "industrial", "Homebuilding"));
        symbols.add(createStock("NVR", "NVR Inc.", "industrial", "Homebuilding"));
        symbols.add(createStock("MAS", "Masco Corp.", "industrial", "Building products"));
        symbols.add(createStock("ALLE", "Allegion plc", "industrial", "Security products"));
        symbols.add(createStock("FIX", "Comfort Systems", "industrial", "HVAC services"));
        symbols.add(createStock("LII", "Lennox International", "industrial", "HVAC"));

        // Industrials - Business Services
        symbols.add(createStock("ADP", "Automatic Data Processing", "services", "Payroll services"));
        symbols.add(createStock("PAYX", "Paychex Inc.", "services", "Payroll services"));
        symbols.add(createStock("WM", "Waste Management", "services", "Waste services"));
        symbols.add(createStock("RSG", "Republic Services", "services", "Waste services"));
        symbols.add(createStock("VRSK", "Verisk Analytics", "services", "Data analytics"));
        symbols.add(createStock("BR", "Broadridge Financial", "services", "Investor communications"));
        symbols.add(createStock("CPRT", "Copart Inc.", "services", "Auto auctions"));
        symbols.add(createStock("ROL", "Rollins Inc.", "services", "Pest control"));
        symbols.add(createStock("LDOS", "Leidos Holdings", "services", "Government IT"));
        symbols.add(createStock("CSGP", "CoStar Group", "services", "Real estate data"));
        symbols.add(createStock("EFX", "Equifax Inc.", "services", "Credit data"));
        symbols.add(createStock("CBOE", "Cboe Global Markets", "finance", "Exchanges"));
        symbols.add(createStock("GPN", "Global Payments", "services", "Payments"));
        symbols.add(createStock("FDS", "FactSet Research", "services", "Financial data"));
        symbols.add(createStock("TKO", "TKO Group", "media", "Sports entertainment"));

        // Energy
        symbols.add(createStock("XOM", "Exxon Mobil", "energy", "Oil & gas"));
        symbols.add(createStock("CVX", "Chevron Corp.", "energy", "Oil & gas"));
        symbols.add(createStock("COP", "ConocoPhillips", "energy", "Oil & gas"));
        symbols.add(createStock("SLB", "SLB (Schlumberger)", "energy", "Oilfield services"));
        symbols.add(createStock("EOG", "EOG Resources", "energy", "Oil & gas"));
        symbols.add(createStock("MPC", "Marathon Petroleum", "energy", "Refining"));
        symbols.add(createStock("PSX", "Phillips 66", "energy", "Refining"));
        symbols.add(createStock("VLO", "Valero Energy", "energy", "Refining"));
        symbols.add(createStock("OXY", "Occidental Petroleum", "energy", "Oil & gas"));
        symbols.add(createStock("WMB", "Williams Companies", "energy", "Pipelines"));
        symbols.add(createStock("KMI", "Kinder Morgan", "energy", "Pipelines"));
        symbols.add(createStock("OKE", "ONEOK Inc.", "energy", "Pipelines"));
        symbols.add(createStock("TRGP", "Targa Resources", "energy", "Midstream"));
        symbols.add(createStock("HAL", "Halliburton Co.", "energy", "Oilfield services"));
        symbols.add(createStock("BKR", "Baker Hughes", "energy", "Oilfield services"));
        symbols.add(createStock("DVN", "Devon Energy", "energy", "Oil & gas"));
        symbols.add(createStock("FANG", "Diamondback Energy", "energy", "Oil & gas"));
        symbols.add(createStock("EQT", "EQT Corp.", "energy", "Natural gas"));
        symbols.add(createStock("CTRA", "Coterra Energy", "energy", "Oil & gas"));
        symbols.add(createStock("APA", "APA Corp.", "energy", "Oil & gas"));

        // Utilities
        symbols.add(createStock("NEE", "NextEra Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("SO", "Southern Co.", "utilities", "Electric utilities"));
        symbols.add(createStock("DUK", "Duke Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("CEG", "Constellation Energy", "utilities", "Nuclear power"));
        symbols.add(createStock("VST", "Vistra Corp.", "utilities", "Electric utilities"));
        symbols.add(createStock("SRE", "Sempra", "utilities", "Utilities"));
        symbols.add(createStock("AEP", "American Electric Power", "utilities", "Electric utilities"));
        symbols.add(createStock("D", "Dominion Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("EXC", "Exelon Corp.", "utilities", "Electric utilities"));
        symbols.add(createStock("XEL", "Xcel Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("PEG", "Public Service Enterprise", "utilities", "Electric utilities"));
        symbols.add(createStock("PCG", "PG&E Corp.", "utilities", "Electric utilities"));
        symbols.add(createStock("ED", "Consolidated Edison", "utilities", "Electric utilities"));
        symbols.add(createStock("WEC", "WEC Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("EIX", "Edison International", "utilities", "Electric utilities"));
        symbols.add(createStock("ETR", "Entergy Corp.", "utilities", "Electric utilities"));
        symbols.add(createStock("DTE", "DTE Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("AEE", "Ameren Corp.", "utilities", "Electric utilities"));
        symbols.add(createStock("FE", "FirstEnergy Corp.", "utilities", "Electric utilities"));
        symbols.add(createStock("PPL", "PPL Corp.", "utilities", "Electric utilities"));
        symbols.add(createStock("ES", "Eversource Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("CNP", "CenterPoint Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("NI", "NiSource Inc.", "utilities", "Electric utilities"));
        symbols.add(createStock("CMS", "CMS Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("ATO", "Atmos Energy", "utilities", "Natural gas utilities"));
        symbols.add(createStock("LNT", "Alliant Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("EVRG", "Evergy Inc.", "utilities", "Electric utilities"));
        symbols.add(createStock("NRG", "NRG Energy", "utilities", "Electric utilities"));
        symbols.add(createStock("AWK", "American Water Works", "utilities", "Water utilities"));
        symbols.add(createStock("PNW", "Pinnacle West", "utilities", "Electric utilities"));

        // Real Estate (REITs)
        symbols.add(createStock("AMT", "American Tower", "realestate", "Cell towers"));
        symbols.add(createStock("PLD", "Prologis Inc.", "realestate", "Industrial"));
        symbols.add(createStock("EQIX", "Equinix Inc.", "realestate", "Data centers"));
        symbols.add(createStock("WELL", "Welltower Inc.", "realestate", "Healthcare"));
        symbols.add(createStock("DLR", "Digital Realty", "realestate", "Data centers"));
        symbols.add(createStock("SPG", "Simon Property", "realestate", "Retail"));
        symbols.add(createStock("PSA", "Public Storage", "realestate", "Self-storage"));
        symbols.add(createStock("O", "Realty Income", "realestate", "Net lease"));
        symbols.add(createStock("CCI", "Crown Castle", "realestate", "Cell towers"));
        symbols.add(createStock("SBAC", "SBA Communications", "realestate", "Cell towers"));
        symbols.add(createStock("VICI", "VICI Properties", "realestate", "Gaming"));
        symbols.add(createStock("EXR", "Extra Space Storage", "realestate", "Self-storage"));
        symbols.add(createStock("AVB", "AvalonBay Communities", "realestate", "Apartments"));
        symbols.add(createStock("EQR", "Equity Residential", "realestate", "Apartments"));
        symbols.add(createStock("VTR", "Ventas Inc.", "realestate", "Healthcare"));
        symbols.add(createStock("IRM", "Iron Mountain", "realestate", "Storage"));
        symbols.add(createStock("ARE", "Alexandria Real Estate", "realestate", "Life science"));
        symbols.add(createStock("CBRE", "CBRE Group", "realestate", "Real estate services"));
        symbols.add(createStock("MAA", "Mid-America Apartment", "realestate", "Apartments"));
        symbols.add(createStock("UDR", "UDR Inc.", "realestate", "Apartments"));
        symbols.add(createStock("ESS", "Essex Property", "realestate", "Apartments"));
        symbols.add(createStock("KIM", "Kimco Realty", "realestate", "Retail"));
        symbols.add(createStock("REG", "Regency Centers", "realestate", "Retail"));
        symbols.add(createStock("HST", "Host Hotels", "realestate", "Hotels"));
        symbols.add(createStock("INVH", "Invitation Homes", "realestate", "Single-family rental"));
        symbols.add(createStock("BXP", "Boston Properties", "realestate", "Office"));
        symbols.add(createStock("FRT", "Federal Realty", "realestate", "Retail"));
        symbols.add(createStock("CPT", "Camden Property", "realestate", "Apartments"));
        symbols.add(createStock("DOC", "Healthpeak Properties", "realestate", "Healthcare"));

        // Autos & Mobility
        symbols.add(createStock("F", "Ford Motor", "auto", "Automobiles"));
        symbols.add(createStock("GM", "General Motors", "auto", "Automobiles"));
        symbols.add(createStock("APTV", "Aptiv plc", "auto", "Auto parts"));
        symbols.add(createStock("BWA", "BorgWarner", "auto", "Auto parts"));

        // Miscellaneous / Other S&P 500
        symbols.add(createStock("LW", "Lamb Weston", "consumer", "Food products"));
        symbols.add(createStock("GRMN", "Garmin Ltd.", "tech", "GPS devices"));
        symbols.add(createStock("IP", "International Paper", "industrial", "Paper"));
        symbols.add(createStock("WY", "Weyerhaeuser", "industrial", "Timber"));
        symbols.add(createStock("PKG", "Packaging Corp", "industrial", "Packaging"));
        symbols.add(createStock("AVY", "Avery Dennison", "industrial", "Labels"));
        symbols.add(createStock("BALL", "Ball Corp.", "industrial", "Packaging"));
        symbols.add(createStock("AMCR", "Amcor plc", "industrial", "Packaging"));
        symbols.add(createStock("IFF", "Intl Flavors & Fragrances", "consumer", "Ingredients"));
        symbols.add(createStock("ALB", "Albemarle Corp.", "industrial", "Specialty chemicals"));
        symbols.add(createStock("DD", "DuPont de Nemours", "industrial", "Specialty chemicals"));
        symbols.add(createStock("PPG", "PPG Industries", "industrial", "Coatings"));
        symbols.add(createStock("SHW", "Sherwin-Williams", "industrial", "Coatings"));
        symbols.add(createStock("ECL", "Ecolab Inc.", "industrial", "Cleaning products"));
        symbols.add(createStock("APD", "Air Products", "industrial", "Industrial gases"));
        symbols.add(createStock("LIN", "Linde plc", "industrial", "Industrial gases"));
        symbols.add(createStock("DOW", "Dow Inc.", "industrial", "Chemicals"));
        symbols.add(createStock("LYB", "LyondellBasell", "industrial", "Chemicals"));
        symbols.add(createStock("CF", "CF Industries", "industrial", "Fertilizers"));
        symbols.add(createStock("MOS", "Mosaic Co.", "industrial", "Fertilizers"));
        symbols.add(createStock("FMC", "FMC Corp.", "industrial", "Agricultural chemicals"));
        symbols.add(createStock("CTVA", "Corteva Inc.", "industrial", "Agricultural chemicals"));
        symbols.add(createStock("CE", "Celanese Corp.", "industrial", "Chemicals"));
        symbols.add(createStock("KVUE", "Kenvue Inc.", "consumer", "Consumer health"));
        symbols.add(createStock("VLTO", "Veralto Corp.", "industrial", "Water quality"));
        symbols.add(createStock("GEN", "Gen Digital", "tech", "Cybersecurity"));
        symbols.add(createStock("FSLR", "First Solar", "energy", "Solar"));
        symbols.add(createStock("ENPH", "Enphase Energy", "energy", "Solar"));
        symbols.add(createStock("GNRC", "Generac Holdings", "industrial", "Generators"));
        symbols.add(createStock("WTW", "Willis Towers Watson", "finance", "Insurance brokerage"));
        symbols.add(createStock("AJG", "Arthur J. Gallagher", "finance", "Insurance brokerage"));
        symbols.add(createStock("BRO", "Brown & Brown", "finance", "Insurance brokerage"));
        symbols.add(createStock("ERIE", "Erie Indemnity", "finance", "Insurance"));
        symbols.add(createStock("MTD", "Mettler-Toledo", "healthcare", "Lab equipment"));
        symbols.add(createStock("COO", "Cooper Companies", "healthcare", "Medical devices"));
        symbols.add(createStock("J", "Jacobs Solutions", "industrial", "Engineering"));
        symbols.add(createStock("WST", "West Pharmaceutical", "healthcare", "Packaging"));
        symbols.add(createStock("SW", "Smurfit Westrock", "industrial", "Packaging"));
        symbols.add(createStock("SOLV", "Solventum Corp.", "healthcare", "Medical products"));
        symbols.add(createStock("EXE", "Expand Energy", "energy", "Natural gas"));
        symbols.add(createStock("DAY", "Dayforce Inc.", "tech", "HR software"));
        symbols.add(createStock("PSKY", "Parsons Corp.", "industrial", "Defense tech"));
        symbols.add(createStock("XYZ", "Block Inc.", "tech", "Fintech"));
        symbols.add(createStock("TPL", "Texas Pacific Land", "energy", "Land/royalties"));
        symbols.add(createStock("SNDK", "Sandisk Corp.", "tech", "Storage"));
        symbols.add(createStock("HUBB", "Hubbell Inc.", "industrial", "Electrical equipment"));
        symbols.add(createStock("AES", "AES Corp.", "utilities", "Electric utilities"));
        symbols.add(createStock("Q", "Quintessential SX", "industrial", "Industrial"));
        symbols.add(createStock("BF.B", "Brown-Forman B", "consumer", "Spirits"));

        return symbols;
    }

    private List<SymbolEntity> createEtfSymbols() {
        List<SymbolEntity> symbols = new ArrayList<>();

        // Major Index ETFs (S&P 500)
        symbols.add(createEtf("SPY", "SPDR S&P 500 ETF Trust", "index", "Tracks S&P 500 index"));
        symbols.add(createEtf("VOO", "Vanguard S&P 500 ETF", "index", "Tracks S&P 500 index"));
        symbols.add(createEtf("IVV", "iShares Core S&P 500 ETF", "index", "Tracks S&P 500 index"));

        // Major Index ETFs (Nasdaq/Tech)
        symbols.add(createEtf("QQQ", "Invesco QQQ Trust", "index", "Tracks Nasdaq 100"));
        symbols.add(createEtf("VGT", "Vanguard Information Technology ETF", "sector", "Technology sector"));
        symbols.add(createEtf("XLK", "Technology Select Sector SPDR", "sector", "Technology sector"));

        // Major Index ETFs (Small/Mid Cap)
        symbols.add(createEtf("IWM", "iShares Russell 2000 ETF", "index", "Small cap stocks"));
        symbols.add(createEtf("IJH", "iShares Core S&P Mid-Cap ETF", "index", "Mid cap stocks"));
        symbols.add(createEtf("MDY", "SPDR S&P MidCap 400 ETF Trust", "index", "Mid cap stocks"));

        // Major Index ETFs (Dow)
        symbols.add(createEtf("DIA", "SPDR Dow Jones Industrial Average ETF", "index", "Tracks Dow Jones"));

        // Major Index ETFs (Total Market)
        symbols.add(createEtf("VTI", "Vanguard Total Stock Market ETF", "index", "Total US stock market"));
        symbols.add(createEtf("ITOT", "iShares Core S&P Total U.S. Stock Market ETF", "index", "Total US stock market"));

        // Sector ETFs
        symbols.add(createEtf("XLF", "Financial Select Sector SPDR", "sector", "Financial sector"));
        symbols.add(createEtf("XLE", "Energy Select Sector SPDR", "sector", "Energy sector"));
        symbols.add(createEtf("XLV", "Health Care Select Sector SPDR", "sector", "Healthcare sector"));
        symbols.add(createEtf("XLI", "Industrial Select Sector SPDR", "sector", "Industrial sector"));
        symbols.add(createEtf("XLP", "Consumer Staples Select Sector SPDR", "sector", "Consumer staples sector"));
        symbols.add(createEtf("XLY", "Consumer Discretionary Select Sector SPDR", "sector", "Consumer discretionary sector"));
        symbols.add(createEtf("XLU", "Utilities Select Sector SPDR", "sector", "Utilities sector"));
        symbols.add(createEtf("XLRE", "Real Estate Select Sector SPDR", "sector", "Real estate sector"));

        // Bond ETFs
        symbols.add(createEtf("AGG", "iShares Core U.S. Aggregate Bond ETF", "bond", "US investment grade bonds"));
        symbols.add(createEtf("BND", "Vanguard Total Bond Market ETF", "bond", "US investment grade bonds"));
        symbols.add(createEtf("TLT", "iShares 20+ Year Treasury Bond ETF", "bond", "Long-term treasuries"));
        symbols.add(createEtf("IEF", "iShares 7-10 Year Treasury Bond ETF", "bond", "Intermediate treasuries"));
        symbols.add(createEtf("LQD", "iShares iBoxx Investment Grade Corporate Bond ETF", "bond", "Corporate bonds"));

        // International ETFs
        symbols.add(createEtf("EFA", "iShares MSCI EAFE ETF", "international", "Developed markets ex-US"));
        symbols.add(createEtf("VEA", "Vanguard FTSE Developed Markets ETF", "international", "Developed markets ex-US"));
        symbols.add(createEtf("IEMG", "iShares Core MSCI Emerging Markets ETF", "international", "Emerging markets"));
        symbols.add(createEtf("VWO", "Vanguard FTSE Emerging Markets ETF", "international", "Emerging markets"));
        symbols.add(createEtf("EEM", "iShares MSCI Emerging Markets ETF", "international", "Emerging markets"));

        // Commodity ETFs
        symbols.add(createEtf("GLD", "SPDR Gold Shares", "commodity", "Gold"));
        symbols.add(createEtf("SLV", "iShares Silver Trust", "commodity", "Silver"));
        symbols.add(createEtf("USO", "United States Oil Fund", "commodity", "Oil"));
        symbols.add(createEtf("UNG", "United States Natural Gas Fund", "commodity", "Natural gas"));

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
        entity.setPrimaryDataSource("ALPACA");  // Use Alpaca for crypto (supports /v1beta3/crypto/us/bars)
        entity.setTimeframes(List.of("1m", "30m", "1h", "4h", "1D"));  // Crypto supports same timeframes as stocks
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
        entity.setPrimaryDataSource("ALPACA");  // Use Alpaca for stocks
        entity.setTimeframes(List.of("1m", "30m", "1h", "4h", "1D"));
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
        entity.setPrimaryDataSource("ALPACA");  // Use Alpaca for ETFs
        entity.setTimeframes(List.of("1m", "30m", "1h", "4h", "1D"));
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
