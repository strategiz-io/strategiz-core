# Strategy Execution Engine Design

## Architecture Overview

### 1. Core Components

#### ExecutionEngineService
```java
@Service
public class ExecutionEngineService {
    private final Map<String, LanguageExecutor> executors;
    private final ProviderDataService providerDataService;
    private final LLMAssistantService llmAssistant;
    
    public ExecutionResult executeStrategy(ExecutionRequest request) {
        // 1. Get provider data
        MarketData data = providerDataService.getData(
            request.getProviderId(), 
            request.getSymbol(), 
            request.getTimeframe()
        );
        
        // 2. Prepare execution context
        ExecutionContext context = buildContext(request, data);
        
        // 3. Execute strategy
        LanguageExecutor executor = executors.get(request.getLanguage());
        return executor.execute(request.getCode(), context);
    }
}
```

### 2. Language Executors

#### Python Executor (using GraalVM or Jython)
```java
@Component
public class PythonExecutor implements LanguageExecutor {
    public ExecutionResult execute(String code, ExecutionContext context) {
        // Option 1: GraalVM Polyglot
        try (Context graalContext = Context.newBuilder("python")
                .allowAllAccess(true)
                .build()) {
            
            // Inject market data
            graalContext.getBindings("python").putMember("data", context.getData());
            graalContext.getBindings("python").putMember("indicators", new TechnicalIndicators());
            
            // Execute strategy
            Value result = graalContext.eval("python", code);
            return parseResult(result);
        }
        
        // Option 2: Process-based execution (more secure)
        // - Write code to temp file
        // - Execute in Docker container
        // - Parse JSON output
    }
}
```

#### JavaScript Executor (using Nashorn/GraalVM)
```java
@Component
public class JavaScriptExecutor implements LanguageExecutor {
    public ExecutionResult execute(String code, ExecutionContext context) {
        // Similar to Python but for JS
    }
}
```

### 3. Provider Data Integration

```java
@Service
public class ProviderDataService {
    private final Map<String, MarketDataProvider> providers;
    
    @PostConstruct
    public void init() {
        providers.put("kraken", new KrakenDataProvider());
        providers.put("binance", new BinanceDataProvider());
        providers.put("alpaca", new AlpacaDataProvider());
        // Add more providers
    }
    
    public MarketData getData(String providerId, String symbol, Timeframe timeframe) {
        MarketDataProvider provider = providers.get(providerId);
        return provider.getMarketData(symbol, timeframe);
    }
}
```

### 4. LLM Integration

```java
@Service
public class LLMAssistantService {
    private final LLMProviderFactory llmFactory;
    
    public StrategyAssistance analyzeStrategy(String code, String language) {
        LLMProvider llm = llmFactory.getProvider("openai"); // or "anthropic", "google"
        
        String prompt = buildAnalysisPrompt(code, language);
        LLMResponse response = llm.complete(prompt);
        
        return StrategyAssistance.builder()
            .suggestions(response.getSuggestions())
            .risks(response.getRisks())
            .optimizations(response.getOptimizations())
            .build();
    }
    
    public String generateStrategy(StrategyRequirements requirements) {
        // Use LLM to generate strategy code based on requirements
    }
}
```

### 5. Execution Request/Response Models

```java
public class ExecutionRequest {
    private String strategyId;
    private String code;
    private String language;
    private String providerId; // Which data provider to use
    private String symbol;
    private Timeframe timeframe;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ExecutionMode mode; // BACKTEST, PAPER, LIVE
    private Map<String, Object> parameters;
    private LLMConfig llmConfig; // Optional LLM assistance
}

public class ExecutionResult {
    private List<Signal> signals;
    private PerformanceMetrics metrics;
    private List<Trade> trades;
    private Map<String, TimeSeries> indicators;
    private LLMAnalysis llmAnalysis; // If LLM was used
}
```

### 6. Security Considerations

1. **Sandboxed Execution**
   - Run strategies in Docker containers
   - Resource limits (CPU, memory, time)
   - No network access except to approved APIs

2. **Code Validation**
   - Static analysis before execution
   - Banned imports/functions
   - Complexity limits

3. **Provider API Key Management**
   - User's own API keys stored in Vault
   - Encrypted at rest
   - Rate limiting per user

### 7. Technical Indicator Library

```java
public class TechnicalIndicators {
    public double sma(List<Double> prices, int period);
    public double ema(List<Double> prices, int period);
    public RSIResult rsi(List<Double> prices, int period);
    public MACDResult macd(List<Double> prices, int fast, int slow, int signal);
    public BollingerBands bollinger(List<Double> prices, int period, double stdDev);
    // ... more indicators
}
```

## Implementation Recommendations

1. **Start with Python support** - Most popular for algo trading
2. **Use GraalVM** for polyglot execution (supports Python, JS, R)
3. **Implement provider adapters** incrementally (start with 2-3)
4. **Add LLM integration** as an optional enhancement feature
5. **Use Redis** for caching market data
6. **Consider Apache Kafka** for real-time data streaming

## Example Strategy Execution Flow

1. User writes strategy in Python
2. Selects Kraken as data provider
3. Chooses to backtest on BTC/USD, 1h timeframe
4. Optionally enables LLM analysis
5. System:
   - Fetches historical data from Kraken
   - Executes strategy in sandboxed environment
   - LLM analyzes code for improvements
   - Returns signals, metrics, and suggestions