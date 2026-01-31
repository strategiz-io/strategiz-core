package io.strategiz.business.portfolio.enhancer.mapper;

import io.strategiz.business.portfolio.enhancer.model.AssetMetadata;
import io.strategiz.data.symbol.entity.SymbolEntity;
import io.strategiz.data.symbol.repository.SymbolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal mapper that provides human-readable names and metadata for standard symbols.
 * Provider-agnostic mappings that work across all exchanges.
 *
 * Now delegates to Firestore (via SymbolRepository) with fallback to static mapping.
 */
@Component
public class UniversalSymbolMapper {

	private static final Logger log = LoggerFactory.getLogger(UniversalSymbolMapper.class);

	private final SymbolRepository symbolRepository;

	// In-memory cache for frequently accessed symbols
	private final Map<String, AssetMetadata> metadataCache = new ConcurrentHashMap<>();

	// Static fallback mapping (used when symbol not in Firestore)
	private static final Map<String, AssetMetadata> FALLBACK_METADATA;

	static {
		FALLBACK_METADATA = new HashMap<>();

		// Major Cryptocurrencies
		addCrypto("BTC", "Bitcoin", "payment", "The first and largest cryptocurrency");
		addCrypto("ETH", "Ethereum", "platform", "Smart contract platform");
		addCrypto("BNB", "Binance Coin", "exchange", "Binance exchange token");
		addCrypto("XRP", "Ripple", "payment", "Cross-border payment system");
		addCrypto("ADA", "Cardano", "platform", "Proof-of-stake blockchain platform");
		addCrypto("SOL", "Solana", "platform", "High-performance blockchain");
		addCrypto("DOGE", "Dogecoin", "meme", "Popular meme cryptocurrency");
		addCrypto("DOT", "Polkadot", "platform", "Multi-chain protocol");
		addCrypto("MATIC", "Polygon", "layer2", "Ethereum scaling solution");
		addCrypto("POL", "Polygon", "layer2", "Polygon ecosystem token");
		addCrypto("AVAX", "Avalanche", "platform", "Smart contract platform");
		addCrypto("TRX", "TRON", "platform", "Content sharing platform");
		addCrypto("LINK", "Chainlink", "oracle", "Decentralized oracle network");
		addCrypto("UNI", "Uniswap", "defi", "Decentralized exchange protocol");
		addCrypto("ATOM", "Cosmos", "platform", "Internet of blockchains");
		addCrypto("LTC", "Litecoin", "payment", "Digital silver to Bitcoin's gold");
		addCrypto("XLM", "Stellar", "payment", "Cross-border transfer platform");
		addCrypto("XMR", "Monero", "privacy", "Privacy-focused cryptocurrency");

		// DeFi Tokens
		addCrypto("AAVE", "Aave", "defi", "Lending and borrowing protocol");
		addCrypto("MKR", "Maker", "defi", "DAI stablecoin governance");
		addCrypto("COMP", "Compound", "defi", "Algorithmic money market");
		addCrypto("CRV", "Curve", "defi", "Stablecoin DEX");
		addCrypto("SUSHI", "SushiSwap", "defi", "Decentralized exchange");
		addCrypto("YFI", "Yearn Finance", "defi", "Yield aggregator");
		addCrypto("SNX", "Synthetix", "defi", "Synthetic assets protocol");

		// Stablecoins
		addCrypto("USDT", "Tether", "stablecoin", "USD-pegged stablecoin");
		addCrypto("USDC", "USD Coin", "stablecoin", "USD-pegged stablecoin");
		addCrypto("DAI", "DAI", "stablecoin", "Decentralized stablecoin");
		addCrypto("BUSD", "Binance USD", "stablecoin", "USD-pegged stablecoin");

		// Gaming & Metaverse
		addCrypto("SAND", "The Sandbox", "gaming", "Virtual world game");
		addCrypto("MANA", "Decentraland", "metaverse", "Virtual reality platform");
		addCrypto("AXS", "Axie Infinity", "gaming", "Play-to-earn game");
		addCrypto("GALA", "Gala Games", "gaming", "Gaming ecosystem");
		addCrypto("ENJ", "Enjin Coin", "gaming", "Gaming ecosystem");

		// Layer 2 & Scaling
		addCrypto("ARB", "Arbitrum", "layer2", "Ethereum layer 2");
		addCrypto("OP", "Optimism", "layer2", "Ethereum layer 2");

		// Exchange Tokens
		addCrypto("CRO", "Crypto.com Coin", "exchange", "Crypto.com token");
		addCrypto("KCS", "KuCoin Token", "exchange", "KuCoin exchange token");
		addCrypto("FTT", "FTX Token", "exchange", "FTX exchange token");

		// Infrastructure
		addCrypto("FIL", "Filecoin", "storage", "Decentralized storage");
		addCrypto("GRT", "The Graph", "indexing", "Blockchain data indexing");
		addCrypto("OCEAN", "Ocean Protocol", "data", "Data exchange protocol");
		addCrypto("STORJ", "Storj", "storage", "Decentralized cloud storage");

		// From user's portfolio
		addCrypto("AKT", "Akash Network", "cloud", "Decentralized cloud computing");
		addCrypto("SEI", "Sei", "platform", "Trading-focused blockchain");
		addCrypto("INJ", "Injective", "defi", "Decentralized derivatives exchange");
		addCrypto("RENDER", "Render", "computing", "Distributed GPU rendering");
		addCrypto("BABY", "BabyDoge", "meme", "Meme cryptocurrency");

		// Additional popular tokens
		addCrypto("ALGO", "Algorand", "platform", "Pure proof-of-stake blockchain");
		addCrypto("FLOW", "Flow", "platform", "NFT-focused blockchain");
		addCrypto("NEAR", "NEAR Protocol", "platform", "Sharded blockchain");
		addCrypto("ICP", "Internet Computer", "platform", "Decentralized computing");
		addCrypto("VET", "VeChain", "enterprise", "Supply chain platform");
		addCrypto("THETA", "Theta", "video", "Decentralized video streaming");
		addCrypto("FTM", "Fantom", "platform", "DAG-based smart contract platform");
		addCrypto("HBAR", "Hedera", "platform", "Hashgraph consensus");
		addCrypto("ONE", "Harmony", "platform", "Sharding blockchain");
		addCrypto("ZEC", "Zcash", "privacy", "Privacy-preserving cryptocurrency");
		addCrypto("BAT", "Basic Attention Token", "advertising", "Digital advertising token");
		addCrypto("ZRX", "0x", "defi", "DEX infrastructure protocol");

		// Wrapped tokens
		addCrypto("WBTC", "Wrapped Bitcoin", "wrapped", "Bitcoin on Ethereum");
		addCrypto("WETH", "Wrapped Ether", "wrapped", "ERC-20 version of ETH");

		// Fiat currencies
		addFiat("USD", "US Dollar", "United States Dollar");
		addFiat("EUR", "Euro", "European Union currency");
		addFiat("GBP", "British Pound", "Pound Sterling");
		addFiat("JPY", "Japanese Yen", "Japanese currency");
		addFiat("CAD", "Canadian Dollar", "Canadian currency");
		addFiat("AUD", "Australian Dollar", "Australian currency");
		addFiat("CHF", "Swiss Franc", "Swiss currency");
		addFiat("CNY", "Chinese Yuan", "Chinese Renminbi");
		addFiat("KRW", "Korean Won", "South Korean currency");
	}

	@Autowired
	public UniversalSymbolMapper(SymbolRepository symbolRepository) {
		this.symbolRepository = symbolRepository;
		log.info("UniversalSymbolMapper initialized with Firestore backing");
	}

	private static void addCrypto(String symbol, String name, String category, String description) {
		AssetMetadata metadata = new AssetMetadata(symbol, name, "crypto");
		metadata.setCategory(category);
		metadata.setDescription(description);
		metadata.setDisplayName(name + " (" + symbol + ")");
		FALLBACK_METADATA.put(symbol, metadata);
	}

	private static void addFiat(String symbol, String name, String description) {
		AssetMetadata metadata = new AssetMetadata(symbol, name, "fiat");
		metadata.setCategory("currency");
		metadata.setDescription(description);
		metadata.setDisplayName(name);
		metadata.setDecimals(2);
		FALLBACK_METADATA.put(symbol, metadata);
	}

	/**
	 * Get asset metadata for a standard symbol. First checks Firestore, then falls back
	 * to static mapping.
	 */
	public AssetMetadata getMetadata(String symbol) {
		if (symbol == null || symbol.isEmpty()) {
			return null;
		}

		String upperSymbol = symbol.toUpperCase();

		// Check cache first
		if (metadataCache.containsKey(upperSymbol)) {
			return copyMetadata(metadataCache.get(upperSymbol));
		}

		// Try Firestore
		try {
			Optional<SymbolEntity> symbolEntity = symbolRepository.findById(upperSymbol);
			if (symbolEntity.isPresent()) {
				AssetMetadata metadata = convertToAssetMetadata(symbolEntity.get());
				metadataCache.put(upperSymbol, metadata);
				return copyMetadata(metadata);
			}
		}
		catch (Exception e) {
			log.debug("Error fetching symbol {} from Firestore, using fallback: {}", symbol, e.getMessage());
		}

		// Fallback to static mapping
		AssetMetadata fallback = FALLBACK_METADATA.get(upperSymbol);
		if (fallback != null) {
			metadataCache.put(upperSymbol, fallback);
			return copyMetadata(fallback);
		}

		// If not found anywhere, create basic metadata
		return createDefaultMetadata(symbol);
	}

	/**
	 * Convert SymbolEntity to AssetMetadata
	 */
	private AssetMetadata convertToAssetMetadata(SymbolEntity entity) {
		AssetMetadata metadata = new AssetMetadata();
		metadata.setSymbol(entity.getId());
		metadata.setName(entity.getName());
		metadata.setDisplayName(entity.getDisplayName());
		metadata.setAssetType(entity.getAssetType() != null ? entity.getAssetType().toLowerCase() : "unknown");
		metadata.setCategory(entity.getCategory());
		metadata.setDescription(entity.getDescription());
		metadata.setDecimals(entity.getDecimals());
		metadata.setIconUrl(entity.getIconUrl());
		return metadata;
	}

	/**
	 * Create a copy of metadata to prevent modification
	 */
	private AssetMetadata copyMetadata(AssetMetadata source) {
		AssetMetadata copy = new AssetMetadata();
		copy.setSymbol(source.getSymbol());
		copy.setName(source.getName());
		copy.setDisplayName(source.getDisplayName());
		copy.setAssetType(source.getAssetType());
		copy.setCategory(source.getCategory());
		copy.setDescription(source.getDescription());
		copy.setDecimals(source.getDecimals());
		copy.setIconUrl(source.getIconUrl());
		return copy;
	}

	/**
	 * Create default metadata for unknown symbols
	 */
	private AssetMetadata createDefaultMetadata(String symbol) {
		AssetMetadata metadata = new AssetMetadata();
		metadata.setSymbol(symbol);
		metadata.setName(symbol);
		metadata.setDisplayName(symbol);
		metadata.setAssetType("unknown");
		metadata.setDescription("Unknown asset");
		return metadata;
	}

	/**
	 * Check if a symbol is known (in Firestore or fallback)
	 */
	public boolean isKnownSymbol(String symbol) {
		if (symbol == null) {
			return false;
		}

		String upperSymbol = symbol.toUpperCase();

		// Check cache
		if (metadataCache.containsKey(upperSymbol)) {
			return true;
		}

		// Check Firestore
		try {
			if (symbolRepository.exists(upperSymbol)) {
				return true;
			}
		}
		catch (Exception e) {
			log.debug("Error checking symbol {} in Firestore: {}", symbol, e.getMessage());
		}

		// Check fallback
		return FALLBACK_METADATA.containsKey(upperSymbol);
	}

	/**
	 * Get human-readable name for a symbol
	 */
	public String getName(String symbol) {
		AssetMetadata metadata = getMetadata(symbol);
		return metadata != null ? metadata.getName() : symbol;
	}

	/**
	 * Clear the in-memory cache
	 */
	public void clearCache() {
		metadataCache.clear();
		log.info("UniversalSymbolMapper cache cleared");
	}

}