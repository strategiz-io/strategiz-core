# AI Strategy Natural Language Extraction - BDD Tests

## Overview

These Behavior-Driven Development (BDD) tests verify that the AI Strategy Generator correctly extracts trading constants from natural language prompts.

## What These Tests Verify

The AI must extract **5 required constants** from user prompts:

1. **SYMBOL** - Trading instrument (e.g., 'AAPL', 'BTC', 'SPY')
2. **TIMEFRAME** - Chart timeframe (e.g., '1Min', '1H', '1D', '1W')
3. **STOP_LOSS** - Risk percentage (e.g., 3.0 for 3%)
4. **TAKE_PROFIT** - Target percentage (e.g., 9.0 for 9%)
5. **POSITION_SIZE** - Position sizing (e.g., 5 for 5%)

## Running the Tests

### Run All BDD Tests
```bash
mvn test -Dtest=CucumberTestRunner
```

### Run Specific Scenarios by Tag
```bash
# Run smoke tests only
mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@smoke"

# Run performance tests only
mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@performance"
```

### Run from IntelliJ IDEA
1. Right-click on `CucumberTestRunner.java`
2. Select "Run 'CucumberTestRunner'"
3. Or right-click on any `.feature` file and select "Run Feature"

## Test Reports

After running tests, reports are generated in:
- **HTML Report**: `target/cucumber-reports/cucumber.html` (human-readable)
- **JSON Report**: `target/cucumber-reports/cucumber.json` (for CI/CD tools)
- **JUnit XML**: `target/cucumber-reports/cucumber.xml` (for Jenkins, etc.)

## Test Scenarios

### 1. Minimal Input Tests
Verify AI applies intelligent defaults when minimal information is provided.

**Example:**
```gherkin
Given the user provides the prompt "MACD crossover strategy"
Then the extracted SYMBOL should be "SPY"
And the extracted TIMEFRAME should be "1D"
And the extracted STOP_LOSS should be 3.0
```

### 2. Explicit Value Tests
Verify AI extracts explicitly mentioned values correctly.

**Example:**
```gherkin
Given the user provides the prompt "Buy AAPL on 15 minute chart. 2% stop, 6% target."
Then the extracted SYMBOL should be "AAPL"
And the extracted TIMEFRAME should be "15Min"
And the extracted STOP_LOSS should be 2.0
And the extracted TAKE_PROFIT should be 6.0
```

### 3. Natural Language Variation Tests
Verify AI handles different phrasings for the same concept.

**Example:**
```gherkin
# All these should extract STOP_LOSS = 3.0
- "3% stop loss"
- "stop at 3%"
- "cut losses at 3 percent"
- "risk three percent"
```

### 4. Symbol Extraction Tests
Verify AI correctly identifies symbols from various phrasings.

**Example:**
```gherkin
# All these should extract correct symbols
- "Buy AAPL" → SYMBOL = 'AAPL'
- "trade Bitcoin" → SYMBOL = 'BTC'
- "Microsoft stock" → SYMBOL = 'MSFT'
- "Ethereum" → SYMBOL = 'ETH'
```

### 5. Timeframe Conversion Tests
Verify AI converts natural language to standard format.

**Example:**
```gherkin
# Timeframe conversions
- "1 minute" → TIMEFRAME = '1Min'
- "hourly" → TIMEFRAME = '1H'
- "daily" → TIMEFRAME = '1D'
- "4H chart" → TIMEFRAME = '4H'
```

### 6. Strategy-Type Recognition
Verify AI applies appropriate defaults based on strategy type.

**Example:**
```gherkin
# Mean reversion strategies get tighter stops (2.0 vs 3.0)
Given the user provides the prompt "Mean reversion strategy for NVDA"
Then the extracted STOP_LOSS should be 2.0
```

### 7. Performance Tests
Verify response times are within acceptable ranges.

**Example:**
```gherkin
Given the user provides the prompt "RSI strategy"
When the AI generates the strategy code
Then the response time should be less than 15 seconds
```

### 8. Validation Tests
Verify all generated code meets quality standards.

**Example:**
```gherkin
Then SYMBOL must be a string in single quotes
And STOP_LOSS must be a float (percentage, not decimal)
And TAKE_PROFIT must be a float (percentage, not decimal)
```

## Test Data Coverage

### Symbols Tested
- **Stocks**: AAPL, MSFT, GOOGL, TSLA, NVDA, AMZN, SPY, QQQ
- **Crypto**: BTC, ETH, SOL
- **Company Names**: Microsoft → MSFT, Tesla → TSLA, etc.

### Timeframes Tested
- **Intraday**: 1Min, 5Min, 15Min, 1H, 4H
- **Swing**: 1D (daily), 1W (weekly), 1M (monthly)

### Strategy Types Tested
- **Momentum**: MACD, RSI, Stochastic
- **Mean Reversion**: Bollinger Bands
- **Breakout**: Volume-based breakouts
- **Scalping**: VWAP, short timeframes

## Expected Test Results

### Success Criteria
✅ All 13 main scenarios pass
✅ All 45+ scenario outline examples pass
✅ Response times < 20 seconds for all tests
✅ 100% constant extraction accuracy

### Common Failure Modes
If tests fail, check for:
- ❌ Missing constants in generated code
- ❌ Wrong format (e.g., '1 day' instead of '1D')
- ❌ Percentage as decimal (e.g., 0.03 instead of 3.0)
- ❌ Company name not converted to ticker (e.g., 'Microsoft' instead of 'MSFT')
- ❌ Response time exceeding threshold

## Debugging Failed Tests

### 1. Enable Verbose Logging
```bash
mvn test -Dtest=CucumberTestRunner -Dlogging.level.io.strategiz=DEBUG
```

### 2. Run Single Scenario
Add line number to feature file:
```bash
mvn test -Dtest=CucumberTestRunner -Dcucumber.options="src/test/resources/features/ai-strategy-extraction.feature:15"
```

### 3. Inspect Generated Code
Failed tests print the actual generated code in assertions:
```
Expected SYMBOL='AAPL' but found 'SPY' in generated code
```

### 4. Check AI Model Behavior
If extraction is consistently wrong:
1. Verify AIStrategyPrompts.java has latest extraction rules
2. Check if AI model version changed (gemini-2.5-flash vs gemini-2.0-flash)
3. Review LLMRouter model selection logic

## CI/CD Integration

### GitHub Actions
```yaml
- name: Run BDD Tests
  run: mvn test -Dtest=CucumberTestRunner

- name: Publish Test Results
  uses: EnricoMi/publish-unit-test-result-action@v2
  with:
    files: target/cucumber-reports/cucumber.xml
```

### Jenkins
```groovy
stage('BDD Tests') {
    steps {
        sh 'mvn test -Dtest=CucumberTestRunner'
        cucumber reportTitle: 'AI Strategy Extraction',
                 fileIncludePattern: '**/cucumber.json',
                 sortingMethod: 'ALPHABETICAL'
    }
}
```

## Future Test Additions

### Planned Scenarios
- [ ] Multi-leg strategies (e.g., spreads, straddles)
- [ ] Conditional exits (trailing stops, time-based)
- [ ] Portfolio-level risk (max positions, correlation)
- [ ] Exotic timeframes (3Min, 2H, etc.)
- [ ] Non-English prompts (Spanish, French, etc.)

### Performance Benchmarks
- [ ] Concurrent request handling (10 requests simultaneously)
- [ ] Cache hit rate for repeated prompts
- [ ] Token usage per request (cost optimization)

## Maintenance

### Updating Test Cases
1. Edit `ai-strategy-extraction.feature`
2. Run tests to verify syntax: `mvn test -Dtest=CucumberTestRunner`
3. Implement new step definitions in `AIStrategyExtractionSteps.java` if needed
4. Commit both feature file and step definitions

### Version Compatibility
- Cucumber: 7.20.1
- Spring Boot: 3.5.7
- JUnit: 5.x
- Java: 21

## Support

For questions or issues:
1. Check test output in `target/cucumber-reports/cucumber.html`
2. Review step definitions in `AIStrategyExtractionSteps.java`
3. Consult AIStrategyPrompts.java for extraction rules
4. File bug report with scenario name and expected vs actual output
