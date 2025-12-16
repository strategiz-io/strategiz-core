# data-symbol Module

Symbol reference data and cross-exchange mapping for the Strategiz platform.

## Overview

This module provides centralized symbol metadata storage in Firestore, replacing static in-memory symbol mappings. It enables:

- **Cross-exchange symbol mapping**: Translate between provider-specific symbol formats (e.g., BTC-USD ↔ BTCUSDT)
- **Symbol metadata**: Store display names, categories, descriptions for consistent UI
- **Data collection tracking**: Track which symbols are being collected and when

## Firestore Collection

**Collection**: `symbols`

**Document ID**: Canonical symbol (uppercase, e.g., "BTC", "AAPL")

### Document Structure

```json
{
  "id": "BTC",
  "name": "Bitcoin",
  "displayName": "Bitcoin (BTC)",
  "assetType": "CRYPTO",
  "category": "payment",
  "description": "The first and largest cryptocurrency",
  "decimals": 8,
  "providerSymbols": {
    "YAHOO": "BTC-USD",
    "ALPACA": "BTC/USD",
    "COINBASE": "BTC-USD",
    "BINANCE": "BTCUSDT",
    "KRAKEN": "XXBTZUSD"
  },
  "collectionActive": true,
  "primaryDataSource": "YAHOO",
  "timeframes": ["1Day"],
  "lastCollectedAt": "2025-12-15T00:00:00Z",
  "status": "ACTIVE"
}
```

## Usage

### Finding a symbol

```java
@Autowired
private SymbolRepository symbolRepository;

// By canonical symbol
Optional<SymbolEntity> btc = symbolRepository.findById("BTC");

// By provider symbol (cross-exchange lookup)
Optional<SymbolEntity> symbol = symbolRepository.findByProviderSymbol("YAHOO", "BTC-USD");
```

### Getting provider-specific symbol

```java
SymbolEntity btc = symbolRepository.findById("BTC").orElseThrow();
String yahooSymbol = btc.getProviderSymbol("YAHOO");  // Returns "BTC-USD"
String binanceSymbol = btc.getProviderSymbol("BINANCE");  // Returns "BTCUSDT"
```

### Finding symbols for data collection

```java
// All active symbols
List<SymbolEntity> activeSymbols = symbolRepository.findActiveForCollection();

// Symbols for a specific data source
List<SymbolEntity> yahooSymbols = symbolRepository.findActiveForCollectionByDataSource("YAHOO");
```

## Module Dependencies

This module depends on:
- `data-base` - Base repository and entity classes
- `framework-exception` - Exception handling

Other modules can depend on `data-symbol` without pulling in market data dependencies:
- `business-portfolio` - For symbol metadata in portfolio display
- `business-marketdata` - For data collection symbol lists
- `data-watchlist` - For symbol lookup in watchlists
