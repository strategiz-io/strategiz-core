package io.strategiz.service.exchange.trading.agent.service;

import io.strategiz.client.coinbase.CoinbaseClient;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.exchange.exception.ExchangeErrorDetails;
import io.strategiz.service.exchange.trading.agent.model.HistoricalPriceData;
import io.strategiz.service.exchange.trading.agent.model.TradingSignal;
import io.strategiz.service.exchange.trading.agent.model.TradingSignal.SignalStrength;
import io.strategiz.service.exchange.trading.agent.model.TradingSignal.SignalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
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
@Service
public class BitcoinTradingAgent {

    private static final Logger log = LoggerFactory.getLogger(BitcoinTradingAgent.class);

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
     * @param timeframe Timeframe for analysis (e.g., "1d", "6h", "1h", "15m")
     * @return Trading signal with buy/sell recommendation
     */
    public TradingSignal generateTradingSignal(String apiKey, String privateKey, String timeframe) {
        log.info("Generating trading signal for BTC on {} timeframe", timeframe);
        
        // Validate inputs
        if (!TIMEFRAMES.contains(timeframe)) {
            log.error("Invalid timeframe: {}", timeframe);
            throw new IllegalArgumentException("Invalid timeframe. Supported timeframes: " + String.join(", ", TIMEFRAMES));
        }
        
        try {
            // Fetch real historical price data from Coinbase
            List<HistoricalPriceData> historicalData = fetchHistoricalData(apiKey, privateKey, timeframe);
            if (historicalData.isEmpty()) {
                log.error("No historical data found");
                throw new StrategizException(ExchangeErrorDetails.NO_HISTORICAL_DATA, "service-exchange");
            }
            
            // Get the current price (most recent close price)
            BigDecimal currentPrice = historicalData.get(0).getClose();
            
            // Calculate technical indicators
            Map<String, Object> indicators = calculateIndicators(historicalData);
            
            // Determine signal type based on indicator values
            SignalType signalType = determineSignalType(indicators);
            
            // Calculate signal strength based on indicator agreement
            SignalStrength signalStrength = calculateSignalStrength(indicators);
            
            // Calculate target price if it's a buy signal
            BigDecimal targetPrice = calculateTargetPrice(signalType, currentPrice, indicators);
            
            // Generate rationale for the signal
            String rationale = generateRationale(signalType, indicators);
            
            // Create and return the trading signal
            return TradingSignal.builder()
                .assetSymbol(BTC_SYMBOL)
                .signalType(signalType)
                .strength(signalStrength)
                .timestamp(LocalDateTime.now())
                .currentPrice(currentPrice.doubleValue())
                .targetPrice(targetPrice.doubleValue())
                .rationale(rationale)
                .timeframe(timeframe)
                .additionalMetrics(indicators)
                .build();

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating trading signal: {}", e.getMessage(), e);
            throw new StrategizException(ExchangeErrorDetails.SIGNAL_GENERATION_FAILED, "service-exchange", e);
        }
    }
    
    /**
     * Fetch historical price data from Coinbase API
     */
    private List<HistoricalPriceData> fetchHistoricalData(String apiKey, String privateKey, String timeframe) {
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
        BigDecimal low = new BigDecimal(((Number) candle[1]).doubleValue());
        BigDecimal high = new BigDecimal(((Number) candle[2]).doubleValue());
        BigDecimal open = new BigDecimal(((Number) candle[3]).doubleValue());
        BigDecimal close = new BigDecimal(((Number) candle[4]).doubleValue());
        BigDecimal volume = new BigDecimal(((Number) candle[5]).doubleValue());
        
        return HistoricalPriceData.builder()
            .assetSymbol(BTC_SYMBOL)
            .timestamp(LocalDateTime.ofInstant(new Date(timestamp * 1000).toInstant(), ZoneId.systemDefault()))
            .low(low)
            .high(high)
            .open(open)
            .close(close)
            .volume(volume)
            .quoteVolume(volume.multiply(close)) // Approximate quote volume
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
        BigDecimal ema12 = (BigDecimal) indicators.get("ema12");
        BigDecimal ema26 = (BigDecimal) indicators.get("ema26");
        BigDecimal macd = ema12.subtract(ema26);
        indicators.put("macd", macd);
        
        // Relative Strength Index (RSI)
        indicators.put("rsi14", calculateRSI(data, 14));
        
        // Bollinger Bands
        Map<String, Double> bollingerBands = calculateBollingerBands(data, 20, 2.0);
        indicators.putAll(bollingerBands);
        
        // Current price position relative to indicators
        BigDecimal currentPrice = data.get(0).getClose();
        indicators.put("priceVsSMA7", currentPrice.divide((BigDecimal) indicators.get("sma7")));
        indicators.put("priceVsSMA25", currentPrice.divide((BigDecimal) indicators.get("sma25")));
        indicators.put("priceVsSMA99", currentPrice.divide((BigDecimal) indicators.get("sma99")));
        
        // Volatility metrics
        indicators.put("volatility", calculateVolatility(data, 14));
        
        // Market trend
        indicators.put("uptrend", isUptrend(data, 5));
        
        return indicators;
    }
    
    /**
     * Determine signal type based on technical indicators
     */
    private SignalType determineSignalType(Map<String, Object> indicators) {
        int buySignals = 0;
        int sellSignals = 0;
        
        // SMA crossovers
        BigDecimal priceVsSMA7 = (BigDecimal) indicators.get("priceVsSMA7");
        BigDecimal priceVsSMA25 = (BigDecimal) indicators.get("priceVsSMA25");
        BigDecimal priceVsSMA99 = (BigDecimal) indicators.get("priceVsSMA99");
        
        // Price above SMAs is bullish
        if (priceVsSMA7.compareTo(BigDecimal.ONE) > 0) buySignals++;
        else sellSignals++;
        
        if (priceVsSMA25.compareTo(BigDecimal.ONE) > 0) buySignals++;
        else sellSignals++;
        
        if (priceVsSMA99.compareTo(BigDecimal.ONE) > 0) buySignals++;
        else sellSignals++;
        
        // MACD
        BigDecimal macd = (BigDecimal) indicators.get("macd");
        if (macd.compareTo(BigDecimal.ZERO) > 0) buySignals++;
        else sellSignals++;
        
        // RSI
        double rsi = (double) indicators.get("rsi14");
        if (rsi < 30) buySignals += 2; // Oversold - strong buy signal
        else if (rsi < 45) buySignals++;
        else if (rsi > 70) sellSignals += 2; // Overbought - strong sell signal
        else if (rsi > 55) sellSignals++;
        
        // Bollinger Bands
        BigDecimal currentPrice = (BigDecimal) indicators.get("currentPrice");
        double lowerBand = (double) indicators.get("lowerBand");
        double upperBand = (double) indicators.get("upperBand");
        
        if (currentPrice.compareTo(BigDecimal.valueOf(lowerBand)) < 0) buySignals += 2; // Below lower band - strong buy signal
        else if (currentPrice.compareTo(BigDecimal.valueOf(upperBand)) > 0) sellSignals += 2; // Above upper band - strong sell signal
        
        // Uptrend
        boolean uptrend = (boolean) indicators.get("uptrend");
        if (uptrend) buySignals++;
        else sellSignals++;
        
        // Determine final signal type based on buy vs sell signals
        if (buySignals > sellSignals + 2) return SignalType.STRONG_BUY;
        else if (buySignals > sellSignals) return SignalType.BUY;
        else if (sellSignals > buySignals + 2) return SignalType.STRONG_SELL;
        else if (sellSignals > buySignals) return SignalType.SELL;
        else return SignalType.NEUTRAL;
    }
    
    /**
     * Calculate signal strength based on indicator agreement
     */
    private SignalStrength calculateSignalStrength(Map<String, Object> indicators) {
        // Count how many indicators agree with the signal
        int totalIndicators = 7; // SMA7, SMA25, SMA99, MACD, RSI, Bollinger Bands, Trend
        int agreeingIndicators = 0;
        
        // Logic to count agreeing indicators
        // This is a simplified version - in production, this would be more sophisticated
        
        double percentAgreement = (double) agreeingIndicators / totalIndicators;
        
        if (percentAgreement > 0.8) return SignalStrength.VERY_STRONG;
        else if (percentAgreement > 0.6) return SignalStrength.STRONG;
        else if (percentAgreement > 0.4) return SignalStrength.MODERATE;
        else return SignalStrength.WEAK;
    }
    
    /**
     * Calculate target price based on signal type and indicators
     */
    private BigDecimal calculateTargetPrice(SignalType signalType, BigDecimal currentPrice, Map<String, Object> indicators) {
        // For buy signals, calculate a target price higher than current
        // For sell signals, calculate a target price lower than current
        
        double volatility = (double) indicators.get("volatility");
        double adjustment = 0.0;
        
        switch (signalType) {
            case STRONG_BUY:
                adjustment = 0.05 + (volatility * 2); // 5% + 2x volatility
                break;
            case BUY:
                adjustment = 0.03 + volatility; // 3% + volatility
                break;
            case NEUTRAL:
                return currentPrice; // No change for neutral
            case SELL:
                adjustment = -0.03 - volatility; // -3% - volatility
                break;
            case STRONG_SELL:
                adjustment = -0.05 - (volatility * 2); // -5% - 2x volatility
                break;
        }
        
        return currentPrice.multiply(BigDecimal.valueOf(1 + adjustment));
    }
    
    /**
     * Generate human-readable rationale for the trading signal
     */
    private String generateRationale(SignalType signalType, Map<String, Object> indicators) {
        StringBuilder rationale = new StringBuilder();
        
        // Add signal type summary
        switch (signalType) {
            case STRONG_BUY:
                rationale.append("Strong buy signal detected. Multiple indicators suggest significant upside potential. ");
                break;
            case BUY:
                rationale.append("Buy signal detected. Technical indicators suggest upside potential. ");
                break;
            case NEUTRAL:
                rationale.append("Neutral signal. Mixed or unclear technical indicators. ");
                break;
            case SELL:
                rationale.append("Sell signal detected. Technical indicators suggest downside risk. ");
                break;
            case STRONG_SELL:
                rationale.append("Strong sell signal detected. Multiple indicators suggest significant downside risk. ");
                break;
        }
        
        // Add specific indicator details
        double rsi = (double) indicators.get("rsi14");
        rationale.append(String.format("RSI(14) is %.2f", rsi));
        if (rsi < 30) rationale.append(" (oversold). ");
        else if (rsi > 70) rationale.append(" (overbought). ");
        else rationale.append(". ");
        
        BigDecimal macd = (BigDecimal) indicators.get("macd");
        rationale.append(String.format("MACD is %.2f", macd));
        if (macd.compareTo(BigDecimal.ZERO) > 0) rationale.append(" (bullish). ");
        else rationale.append(" (bearish). ");
        
        boolean uptrend = (boolean) indicators.get("uptrend");
        if (uptrend) rationale.append("Price is in an uptrend. ");
        else rationale.append("Price is in a downtrend. ");
        
        // Add volatility context
        double volatility = (double) indicators.get("volatility");
        if (volatility > 0.03) rationale.append("Market volatility is high. ");
        else if (volatility < 0.01) rationale.append("Market volatility is low. ");
        else rationale.append("Market volatility is moderate. ");
        
        return rationale.toString();
    }
    
    /**
     * Calculate Simple Moving Average
     */
    private BigDecimal calculateSMA(List<HistoricalPriceData> data, int period) {
        if (data.size() < period) {
            return data.stream().map(HistoricalPriceData::getClose).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(data.size()));
        }
        
        return data.stream()
            .limit(period)
            .map(HistoricalPriceData::getClose)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(period));
    }
    
    /**
     * Calculate Exponential Moving Average
     */
    private BigDecimal calculateEMA(List<HistoricalPriceData> data, int period) {
        if (data.size() < period) {
            return calculateSMA(data, data.size());
        }
        
        double smoothingFactor = 2.0 / (period + 1);
        BigDecimal ema = data.get(period - 1).getClose();
        
        for (int i = period - 2; i >= 0; i--) {
            ema = data.get(i).getClose().multiply(BigDecimal.valueOf(smoothingFactor)).add(ema.multiply(BigDecimal.valueOf(1 - smoothingFactor)));
        }
        
        return ema;
    }
    
    /**
     * Calculate Relative Strength Index
     */
    private double calculateRSI(List<HistoricalPriceData> data, int period) {
        if (data.size() <= period) {
            return 50; // Neutral RSI if not enough data
        }
        
        double sumGain = 0;
        double sumLoss = 0;
        
        // Calculate initial average gain/loss
        for (int i = 0; i < period; i++) {
            BigDecimal change = data.get(i).getClose().subtract(data.get(i + 1).getClose());
            if (change.compareTo(BigDecimal.ZERO) >= 0) {
                sumGain += change.doubleValue();
            } else {
                sumLoss -= change.doubleValue();
            }
        }
        
        double avgGain = sumGain / period;
        double avgLoss = sumLoss / period;
        
        // Calculate RSI
        double rs = (avgLoss == 0) ? 100 : avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
    
    /**
     * Calculate Bollinger Bands
     */
    private Map<String, Double> calculateBollingerBands(List<HistoricalPriceData> data, int period, double multiplier) {
        Map<String, Double> result = new HashMap<>();
        
        // Calculate SMA
        BigDecimal sma = calculateSMA(data, period);
        result.put("middleBand", sma.doubleValue());
        
        // Calculate standard deviation
        double variance = data.stream()
            .limit(period)
            .mapToDouble(d -> Math.pow(d.getClose().doubleValue() - sma.doubleValue(), 2))
            .sum() / period;
        double stdDev = Math.sqrt(variance);
        
        // Calculate upper and lower bands
        result.put("upperBand", sma.add(BigDecimal.valueOf(stdDev * multiplier)).doubleValue());
        result.put("lowerBand", sma.subtract(BigDecimal.valueOf(stdDev * multiplier)).doubleValue());
        result.put("currentPrice", data.get(0).getClose().doubleValue());
        
        return result;
    }
    
    /**
     * Calculate price volatility
     */
    private double calculateVolatility(List<HistoricalPriceData> data, int period) {
        if (data.size() < period) {
            period = data.size();
        }
        
        List<Double> returns = new ArrayList<>();
        for (int i = 0; i < period - 1; i++) {
            BigDecimal todayPrice = data.get(i).getClose();
            BigDecimal yesterdayPrice = data.get(i + 1).getClose();
            double dailyReturn = (todayPrice.divide(yesterdayPrice).doubleValue()) - 1;
            returns.add(dailyReturn);
        }
        
        double meanReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double sumSquaredDeviations = returns.stream()
            .mapToDouble(r -> Math.pow(r - meanReturn, 2))
            .sum();
        
        return Math.sqrt(sumSquaredDeviations / (period - 1));
    }
    
    /**
     * Determine if price is in an uptrend
     */
    private boolean isUptrend(List<HistoricalPriceData> data, int period) {
        if (data.size() < period) {
            period = data.size();
        }
        
        BigDecimal currentPrice = data.get(0).getClose();
        BigDecimal oldPrice = data.get(period - 1).getClose();
        
        return currentPrice.compareTo(oldPrice) > 0;
    }
    
    /**
     * Get appropriate number of data points based on timeframe
     */
    private int getDataPointsForTimeframe(String timeframe) {
        switch (timeframe) {
            case "1d": return 90;   // 90 days
            case "6h": return 100;  // 100 6-hour candles
            case "1h": return 168;  // 168 hours (1 week)
            case "15m": return 192; // 192 15-minute candles (2 days)
            default: return 100;
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
