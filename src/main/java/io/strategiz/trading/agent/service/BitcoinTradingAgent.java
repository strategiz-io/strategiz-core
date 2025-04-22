package io.strategiz.trading.agent.service;

import io.strategiz.coinbase.client.CoinbaseClient;
import io.strategiz.coinbase.client.exception.CoinbaseApiException;
import io.strategiz.trading.agent.model.HistoricalPriceData;
import io.strategiz.trading.agent.model.TradingSignal;
import io.strategiz.trading.agent.model.TradingSignal.SignalStrength;
import io.strategiz.trading.agent.model.TradingSignal.SignalType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Bitcoin Trading Agent Service
 * Uses real Coinbase API data to analyze BTC price trends and generate trading signals
 */
@Slf4j
@Service
public class BitcoinTradingAgent {

    private final CoinbaseClient coinbaseClient;
    private final RestTemplate restTemplate;
    
    private static final String BTC_SYMBOL = "BTC";
    private static final String QUOTE_CURRENCY = "USD";
    private static final List<String> TIMEFRAMES = Arrays.asList("1d", "6h", "1h", "15m");
    
    @Autowired
    public BitcoinTradingAgent(CoinbaseClient coinbaseClient) {
        this.coinbaseClient = coinbaseClient;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Generate a trading signal based on historical price analysis
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key
     * @param passphrase Coinbase API passphrase
     * @param timeframe Timeframe for analysis (e.g., "1d", "6h", "1h", "15m")
     * @return Trading signal with buy/sell recommendation
     */
    public TradingSignal generateTradingSignal(String apiKey, String privateKey, String passphrase, String timeframe) {
        log.info("Generating trading signal for BTC on {} timeframe", timeframe);
        
        // Validate inputs
        if (!TIMEFRAMES.contains(timeframe)) {
            log.error("Invalid timeframe: {}", timeframe);
            throw new IllegalArgumentException("Invalid timeframe. Supported timeframes: " + String.join(", ", TIMEFRAMES));
        }
        
        try {
            // Fetch real historical price data from Coinbase
            List<HistoricalPriceData> historicalData = fetchHistoricalData(apiKey, privateKey, passphrase, timeframe);
            if (historicalData.isEmpty()) {
                log.error("No historical data found");
                throw new RuntimeException("No historical price data available");
            }
            
            // Get the current price (most recent close price)
            double currentPrice = historicalData.get(0).getClose();
            
            // Calculate technical indicators
            Map<String, Object> indicators = calculateIndicators(historicalData);
            
            // Determine signal type based on indicator values
            SignalType signalType = determineSignalType(indicators);
            
            // Calculate signal strength based on indicator agreement
            SignalStrength signalStrength = calculateSignalStrength(indicators);
            
            // Calculate target price if it's a buy signal
            double targetPrice = calculateTargetPrice(signalType, currentPrice, indicators);
            
            // Generate rationale for the signal
            String rationale = generateRationale(signalType, indicators);
            
            // Create and return the trading signal
            return TradingSignal.builder()
                .assetSymbol(BTC_SYMBOL)
                .signalType(signalType)
                .strength(signalStrength)
                .timestamp(LocalDateTime.now())
                .currentPrice(currentPrice)
                .targetPrice(targetPrice)
                .rationale(rationale)
                .timeframe(timeframe)
                .additionalMetrics(indicators)
                .build();
                
        } catch (Exception e) {
            log.error("Error generating trading signal: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate trading signal: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fetch historical price data from Coinbase API
     */
    private List<HistoricalPriceData> fetchHistoricalData(String apiKey, String privateKey, String passphrase, String timeframe) throws CoinbaseApiException {
        log.info("Fetching historical BTC price data from Coinbase API");
        
        // Determine how many data points to fetch based on timeframe
        int dataPoints = getDataPointsForTimeframe(timeframe);
        
        // Prepare parameters for Coinbase API request
        String endpoint = "/products/BTC-USD/candles";
        Map<String, String> params = new HashMap<>();
        params.put("granularity", convertTimeframeToSeconds(timeframe)); // granularity is in seconds
        
        // Make authenticated request to get candle data
        List<Object[]> candleData = coinbaseClient.signedRequest(
            HttpMethod.GET,
            endpoint,
            params,
            apiKey,
            privateKey,
            passphrase,
            new ParameterizedTypeReference<List<Object[]>>() {}
        );
        
        // Convert candle data to HistoricalPriceData objects
        return candleData.stream()
            .map(this::convertCandleToHistoricalData)
            .limit(dataPoints)
            .collect(Collectors.toList());
    }
    
    /**
     * Convert Coinbase candle data to HistoricalPriceData
     */
    private HistoricalPriceData convertCandleToHistoricalData(Object[] candle) {
        // Coinbase API returns candles in format [timestamp, low, high, open, close, volume]
        long timestamp = ((Number) candle[0]).longValue();
        double low = ((Number) candle[1]).doubleValue();
        double high = ((Number) candle[2]).doubleValue();
        double open = ((Number) candle[3]).doubleValue();
        double close = ((Number) candle[4]).doubleValue();
        double volume = ((Number) candle[5]).doubleValue();
        
        return HistoricalPriceData.builder()
            .assetSymbol(BTC_SYMBOL)
            .timestamp(LocalDateTime.ofInstant(new Date(timestamp * 1000).toInstant(), ZoneId.systemDefault()))
            .low(low)
            .high(high)
            .open(open)
            .close(close)
            .volume(volume)
            .quoteVolume(volume * close) // Approximate quote volume
            .build();
    }
    
    /**
     * Calculate various technical indicators for signal generation
     */
    private Map<String, Object> calculateIndicators(List<HistoricalPriceData> data) {
        Map<String, Object> indicators = new HashMap<>();
        
        // Simple Moving Averages (SMA)
        indicators.put("sma7", calculateSMA(data, 7));
        indicators.put("sma25", calculateSMA(data, 25));
        indicators.put("sma99", calculateSMA(data, 99));
        
        // Exponential Moving Averages (EMA)
        indicators.put("ema12", calculateEMA(data, 12));
        indicators.put("ema26", calculateEMA(data, 26));
        
        // Moving Average Convergence Divergence (MACD)
        double ema12 = (double) indicators.get("ema12");
        double ema26 = (double) indicators.get("ema26");
        double macd = ema12 - ema26;
        indicators.put("macd", macd);
        
        // Relative Strength Index (RSI)
        indicators.put("rsi14", calculateRSI(data, 14));
        
        // Bollinger Bands
        Map<String, Double> bollingerBands = calculateBollingerBands(data, 20, 2.0);
        indicators.putAll(bollingerBands);
        
        // Current price position relative to indicators
        double currentPrice = data.get(0).getClose();
        indicators.put("priceVsSMA7", currentPrice / (double) indicators.get("sma7"));
        indicators.put("priceVsSMA25", currentPrice / (double) indicators.get("sma25"));
        indicators.put("priceVsSMA99", currentPrice / (double) indicators.get("sma99"));
        
        // Volatility metrics
        indicators.put("volatility", calculateVolatility(data, 14));
        
        // Market trend
        indicators.put("uptrend", isUptrend(data, 5));
        
        return indicators;
    }
    
    /**
     * Calculate Simple Moving Average (SMA)
     */
    private double calculateSMA(List<HistoricalPriceData> data, int period) {
        if (data.size() < period) {
            return data.get(0).getClose();
        }
        
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += data.get(i).getClose();
        }
        
        return sum / period;
    }
    
    /**
     * Calculate Exponential Moving Average (EMA)
     */
    private double calculateEMA(List<HistoricalPriceData> data, int period) {
        if (data.size() < period) {
            return data.get(0).getClose();
        }
        
        // Start with SMA for the first EMA value
        double ema = calculateSMA(data.subList(data.size() - period, data.size()), period);
        double multiplier = 2.0 / (period + 1);
        
        // Calculate EMA backwards from oldest to newest
        for (int i = data.size() - period - 1; i >= 0; i--) {
            ema = (data.get(i).getClose() - ema) * multiplier + ema;
        }
        
        return ema;
    }
    
    /**
     * Calculate Relative Strength Index (RSI)
     */
    private double calculateRSI(List<HistoricalPriceData> data, int period) {
        if (data.size() < period + 1) {
            return 50; // Neutral RSI if not enough data
        }
        
        double sumGain = 0;
        double sumLoss = 0;
        
        // Calculate initial average gain and loss
        for (int i = 1; i <= period; i++) {
            double change = data.get(i-1).getClose() - data.get(i).getClose();
            if (change >= 0) {
                sumGain += change;
            } else {
                sumLoss += Math.abs(change);
            }
        }
        
        double avgGain = sumGain / period;
        double avgLoss = sumLoss / period;
        
        // Calculate RSI using Wilder's smoothing method
        for (int i = period; i > 0; i--) {
            double change = data.get(i-1).getClose() - data.get(i).getClose();
            if (change >= 0) {
                avgGain = ((avgGain * (period - 1)) + change) / period;
                avgLoss = (avgLoss * (period - 1)) / period;
            } else {
                avgGain = (avgGain * (period - 1)) / period;
                avgLoss = ((avgLoss * (period - 1)) + Math.abs(change)) / period;
            }
        }
        
        if (avgLoss == 0) {
            return 100;
        }
        
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
    
    /**
     * Calculate Bollinger Bands
     */
    private Map<String, Double> calculateBollingerBands(List<HistoricalPriceData> data, int period, double deviation) {
        Map<String, Double> result = new HashMap<>();
        
        // Calculate SMA
        double sma = calculateSMA(data, period);
        result.put("bband_middle", sma);
        
        // Calculate standard deviation
        double sum = 0;
        for (int i = 0; i < period; i++) {
            double deviation_value = data.get(i).getClose() - sma;
            sum += deviation_value * deviation_value;
        }
        
        double standardDeviation = Math.sqrt(sum / period);
        
        // Calculate upper and lower bands
        result.put("bband_upper", sma + (standardDeviation * deviation));
        result.put("bband_lower", sma - (standardDeviation * deviation));
        
        return result;
    }
    
    /**
     * Calculate price volatility
     */
    private double calculateVolatility(List<HistoricalPriceData> data, int period) {
        if (data.size() < period) {
            return 0;
        }
        
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < period; i++) {
            double todayPrice = data.get(i-1).getClose();
            double yesterdayPrice = data.get(i).getClose();
            returns.add(Math.log(todayPrice / yesterdayPrice));
        }
        
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        double sum = 0;
        for (double r : returns) {
            sum += Math.pow(r - mean, 2);
        }
        
        return Math.sqrt(sum / returns.size()) * Math.sqrt(252); // Annualized volatility
    }
    
    /**
     * Determine if the market is in an uptrend
     */
    private boolean isUptrend(List<HistoricalPriceData> data, int period) {
        if (data.size() < period + 1) {
            return false;
        }
        
        double sma = calculateSMA(data, period);
        return data.get(0).getClose() > sma && data.get(0).getClose() > data.get(1).getClose();
    }
    
    /**
     * Determine the signal type based on technical indicators
     */
    private SignalType determineSignalType(Map<String, Object> indicators) {
        int bullishSignals = 0;
        int bearishSignals = 0;
        
        // MACD signal
        double macd = (double) indicators.get("macd");
        if (macd > 0) bullishSignals++;
        else bearishSignals++;
        
        // RSI signals
        double rsi = (double) indicators.get("rsi14");
        if (rsi < 30) bullishSignals += 2; // Oversold - strong buy signal
        else if (rsi < 40) bullishSignals++;
        else if (rsi > 70) bearishSignals += 2; // Overbought - strong sell signal
        else if (rsi > 60) bearishSignals++;
        
        // Moving average signals
        double priceVsSMA7 = (double) indicators.get("priceVsSMA7");
        double priceVsSMA25 = (double) indicators.get("priceVsSMA25");
        
        if (priceVsSMA7 > 1.0 && priceVsSMA25 > 1.0) bullishSignals++;
        else if (priceVsSMA7 < 1.0 && priceVsSMA25 < 1.0) bearishSignals++;
        
        // Golden/Death Cross
        if (priceVsSMA7 > priceVsSMA25) bullishSignals++;
        else bearishSignals++;
        
        // Bollinger Bands
        double currentPrice = indicators.get("bband_middle") != null ? 
            (double) indicators.getOrDefault("currentPrice", 0.0) : 0.0;
        double upperBand = (double) indicators.get("bband_upper");
        double lowerBand = (double) indicators.get("bband_lower");
        
        if (currentPrice <= lowerBand) bullishSignals += 2; // Price at lower band is a buy signal
        else if (currentPrice >= upperBand) bearishSignals += 2; // Price at upper band is a sell signal
        
        // Uptrend check
        boolean uptrend = (boolean) indicators.get("uptrend");
        if (uptrend) bullishSignals++;
        else bearishSignals++;
        
        // Determine signal type based on indicator counts
        if (bullishSignals > bearishSignals + 2) {
            return SignalType.BUY;
        } else if (bearishSignals > bullishSignals + 2) {
            return SignalType.SELL;
        } else {
            return SignalType.HOLD;
        }
    }
    
    /**
     * Calculate signal strength based on indicator agreement
     */
    private SignalStrength calculateSignalStrength(Map<String, Object> indicators) {
        // Count how many indicators agree with the signal
        int totalIndicators = 7; // MACD, RSI, SMA cross, price vs SMAs, Bollinger Bands, uptrend
        int agreeingIndicators = 0;
        
        // Logic to count agreeing indicators
        double macd = (double) indicators.get("macd");
        double rsi = (double) indicators.get("rsi14");
        double priceVsSMA7 = (double) indicators.get("priceVsSMA7");
        double priceVsSMA25 = (double) indicators.get("priceVsSMA25");
        boolean uptrend = (boolean) indicators.get("uptrend");
        
        // Check for buy signals
        if (macd > 0) agreeingIndicators++;
        if (rsi < 40) agreeingIndicators++;
        if (priceVsSMA7 > 1.0) agreeingIndicators++;
        if (priceVsSMA25 > 1.0) agreeingIndicators++;
        if (priceVsSMA7 > priceVsSMA25) agreeingIndicators++;
        if (uptrend) agreeingIndicators++;
        
        // Determine strength based on percentage of agreeing indicators
        double agreement = (double) agreeingIndicators / totalIndicators;
        
        if (agreement >= 0.8) return SignalStrength.VERY_STRONG;
        else if (agreement >= 0.6) return SignalStrength.STRONG;
        else if (agreement >= 0.4) return SignalStrength.MODERATE;
        else return SignalStrength.WEAK;
    }
    
    /**
     * Calculate target price based on signal type and indicators
     */
    private double calculateTargetPrice(SignalType signalType, double currentPrice, Map<String, Object> indicators) {
        if (signalType == SignalType.BUY) {
            // For buy signals, target price is higher
            double upperBand = (double) indicators.get("bband_upper");
            return upperBand; // Use Bollinger upper band as target
        } else if (signalType == SignalType.SELL) {
            // For sell signals, target price is lower
            double lowerBand = (double) indicators.get("bband_lower");
            return lowerBand; // Use Bollinger lower band as target
        } else {
            // For hold signals, target is current price
            return currentPrice;
        }
    }
    
    /**
     * Generate a human-readable rationale for the signal
     */
    private String generateRationale(SignalType signalType, Map<String, Object> indicators) {
        StringBuilder rationale = new StringBuilder();
        
        double macd = (double) indicators.get("macd");
        double rsi = (double) indicators.get("rsi14");
        double sma7 = (double) indicators.get("sma7");
        double sma25 = (double) indicators.get("sma25");
        
        if (signalType == SignalType.BUY) {
            rationale.append("Buy signal based on: ");
            
            if (macd > 0) {
                rationale.append("Positive MACD (").append(String.format("%.2f", macd)).append("), ");
            }
            
            if (rsi < 40) {
                rationale.append("Oversold RSI (").append(String.format("%.2f", rsi)).append("), ");
            }
            
            if (sma7 > sma25) {
                rationale.append("Golden cross (SMA7 > SMA25), ");
            }
            
            if ((boolean) indicators.get("uptrend")) {
                rationale.append("Market in uptrend, ");
            }
            
        } else if (signalType == SignalType.SELL) {
            rationale.append("Sell signal based on: ");
            
            if (macd < 0) {
                rationale.append("Negative MACD (").append(String.format("%.2f", macd)).append("), ");
            }
            
            if (rsi > 60) {
                rationale.append("Overbought RSI (").append(String.format("%.2f", rsi)).append("), ");
            }
            
            if (sma7 < sma25) {
                rationale.append("Death cross (SMA7 < SMA25), ");
            }
            
            if (!(boolean) indicators.get("uptrend")) {
                rationale.append("Market in downtrend, ");
            }
            
        } else {
            rationale.append("Hold signal: Indicators are mixed or neutral. ");
            
            if (rsi >= 40 && rsi <= 60) {
                rationale.append("RSI is neutral (").append(String.format("%.2f", rsi)).append("), ");
            }
            
            if (Math.abs(macd) < 0.5) {
                rationale.append("MACD is close to zero (").append(String.format("%.2f", macd)).append("), ");
            }
        }
        
        // Add volatility assessment
        double volatility = (double) indicators.get("volatility");
        if (volatility > 0.5) {
            rationale.append("Market volatility is high (").append(String.format("%.2f", volatility)).append(").");
        } else {
            rationale.append("Market volatility is normal (").append(String.format("%.2f", volatility)).append(").");
        }
        
        return rationale.toString();
    }
    
    /**
     * Get the number of data points to fetch based on timeframe
     */
    private int getDataPointsForTimeframe(String timeframe) {
        switch (timeframe) {
            case "1d": return 100;  // 100 days
            case "6h": return 100;  // 100 6-hour candles
            case "1h": return 200;  // 200 1-hour candles
            case "15m": return 300; // 300 15-minute candles
            default: return 100;    // Default to 100 data points
        }
    }
    
    /**
     * Convert timeframe string to granularity in seconds for Coinbase API
     */
    private String convertTimeframeToSeconds(String timeframe) {
        switch (timeframe) {
            case "1d": return "86400";  // 60 * 60 * 24
            case "6h": return "21600";  // 60 * 60 * 6
            case "1h": return "3600";   // 60 * 60
            case "15m": return "900";   // 60 * 15
            default: return "3600";     // Default to 1 hour
        }
    }
}
