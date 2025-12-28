Feature: AI Strategy Natural Language Constant Extraction
  As a user of the AI Strategy Generator
  I want to describe strategies in natural language
  So that the AI automatically extracts trading constants (SYMBOL, TIMEFRAME, STOP_LOSS, TAKE_PROFIT, POSITION_SIZE)

  Background:
    Given the AI Strategy Generator is available
    And the user is authenticated

  Scenario: Minimal input with intelligent defaults
    Given the user provides the prompt "MACD crossover strategy"
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "SPY"
    And the extracted TIMEFRAME should be "1D"
    And the extracted STOP_LOSS should be 3.0
    And the extracted TAKE_PROFIT should be 9.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Crypto strategy with Bitcoin keyword
    Given the user provides the prompt "Create a Bitcoin momentum strategy using RSI oversold/overbought levels"
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "BTC"
    And the extracted TIMEFRAME should be "1D"
    And the extracted STOP_LOSS should be 3.0
    And the extracted TAKE_PROFIT should be 9.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Intraday scalping with 15-minute timeframe
    Given the user provides the prompt "Scalping strategy on 15 minute chart for TSLA. Buy when price breaks above VWAP. Target 2% profit with 1% stop."
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "TSLA"
    And the extracted TIMEFRAME should be "15Min"
    And the extracted STOP_LOSS should be 1.0
    And the extracted TAKE_PROFIT should be 2.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Hourly timeframe with decimal stop loss
    Given the user provides the prompt "Trade GOOGL on the 4 hour chart. Buy when SMA 20 crosses above SMA 50. Set stop at 2.5 percent."
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "GOOGL"
    And the extracted TIMEFRAME should be "4H"
    And the extracted STOP_LOSS should be 2.5
    And the extracted TAKE_PROFIT should be 7.5
    And the extracted POSITION_SIZE should be 5

  Scenario: Weekly swing trade with explicit risk/reward
    Given the user provides the prompt "Weekly swing trade for SPY. Enter when RSI drops below 30, exit when it rises above 70. Risk 5% with 15% profit target."
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "SPY"
    And the extracted TIMEFRAME should be "1W"
    And the extracted STOP_LOSS should be 5.0
    And the extracted TAKE_PROFIT should be 15.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Mean reversion strategy with tighter defaults
    Given the user provides the prompt "Mean reversion strategy for NVDA. Buy when Bollinger Band lower is touched, sell at middle band."
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "NVDA"
    And the extracted TIMEFRAME should be "1D"
    And the extracted STOP_LOSS should be 2.0
    And the extracted TAKE_PROFIT should be 6.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Ethereum breakout on hourly chart
    Given the user provides the prompt "Ethereum breakout strategy on 1 hour timeframe. Buy when price breaks resistance with volume spike. 4% stop loss, 12% take profit."
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "ETH"
    And the extracted TIMEFRAME should be "1H"
    And the extracted STOP_LOSS should be 4.0
    And the extracted TAKE_PROFIT should be 12.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Take profit only with default stop loss
    Given the user provides the prompt "Buy AMZN when MACD histogram turns positive. Target 10% gain on daily chart."
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "AMZN"
    And the extracted TIMEFRAME should be "1D"
    And the extracted STOP_LOSS should be 3.0
    And the extracted TAKE_PROFIT should be 10.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Alternative phrasing with company name
    Given the user provides the prompt "I want to trade Microsoft stock. Enter long when Stochastic is oversold. Cut losses at three percent. Book profits at nine percent. Work on hourly basis."
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "MSFT"
    And the extracted TIMEFRAME should be "1H"
    And the extracted STOP_LOSS should be 3.0
    And the extracted TAKE_PROFIT should be 9.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Multiple symbols with primary selection
    Given the user provides the prompt "Buy QQQ when it's stronger than SPY. Use 1 day timeframe. Stop at 2%."
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "QQQ"
    And the extracted TIMEFRAME should be "1D"
    And the extracted STOP_LOSS should be 2.0
    And the extracted TAKE_PROFIT should be 6.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Extreme minimal input
    Given the user provides the prompt "RSI strategy"
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "SPY"
    And the extracted TIMEFRAME should be "1D"
    And the extracted STOP_LOSS should be 3.0
    And the extracted TAKE_PROFIT should be 9.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Solana crypto with higher risk/reward
    Given the user provides the prompt "Solana momentum play on 4H chart when volume increases 2x average. Risk 5% for 20% target."
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "SOL"
    And the extracted TIMEFRAME should be "4H"
    And the extracted STOP_LOSS should be 5.0
    And the extracted TAKE_PROFIT should be 20.0
    And the extracted POSITION_SIZE should be 5

  Scenario: Full example from commit message
    Given the user provides the prompt "Create a MACD momentum strategy: Buy AAPL when MACD line crosses above the signal line while both are below zero. Sell AAPL when MACD line crosses below the signal line while both are above zero. Set 3% stop loss and 8% take profit, work on timeframe of 1 day"
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "AAPL"
    And the extracted TIMEFRAME should be "1D"
    And the extracted STOP_LOSS should be 3.0
    And the extracted TAKE_PROFIT should be 8.0
    And the extracted POSITION_SIZE should be 5

  # Edge cases for timeframe extraction
  Scenario Outline: Timeframe format variations
    Given the user provides a prompt containing "<timeframe_phrase>"
    When the AI generates the strategy code
    Then the extracted TIMEFRAME should be "<expected_format>"

    Examples:
      | timeframe_phrase      | expected_format |
      | 1 minute             | 1Min            |
      | 5 min                | 5Min            |
      | 15 minute chart      | 15Min           |
      | hourly               | 1H              |
      | 1 hour               | 1H              |
      | 4H chart             | 4H              |
      | daily                | 1D              |
      | 1 day                | 1D              |
      | weekly               | 1W              |
      | 1 week               | 1W              |
      | monthly              | 1M              |

  # Edge cases for symbol extraction
  Scenario Outline: Symbol extraction from natural language
    Given the user provides a prompt containing "<symbol_phrase>"
    When the AI generates the strategy code
    Then the extracted SYMBOL should be "<expected_symbol>"

    Examples:
      | symbol_phrase           | expected_symbol |
      | Buy AAPL                | AAPL            |
      | trade Bitcoin           | BTC             |
      | for MSFT stock          | MSFT            |
      | Tesla                   | TSLA            |
      | Microsoft stock         | MSFT            |
      | Ethereum                | ETH             |
      | Solana                  | SOL             |
      | GOOGL when              | GOOGL           |
      | SPY entry               | SPY             |
      | NVDA position           | NVDA            |

  # Edge cases for stop loss extraction
  Scenario Outline: Stop loss phrase variations
    Given the user provides a prompt containing "<stop_loss_phrase>"
    When the AI generates the strategy code
    Then the extracted STOP_LOSS should be <expected_value>

    Examples:
      | stop_loss_phrase        | expected_value |
      | 3% stop loss            | 3.0            |
      | stop at 5%              | 5.0            |
      | cut losses at 2.5%      | 2.5            |
      | Risk 4 percent          | 4.0            |
      | stop loss of 1.5%       | 1.5            |
      | set stop at three percent | 3.0          |

  # Edge cases for take profit extraction
  Scenario Outline: Take profit phrase variations
    Given the user provides a prompt containing "<take_profit_phrase>"
    When the AI generates the strategy code
    Then the extracted TAKE_PROFIT should be <expected_value>

    Examples:
      | take_profit_phrase      | expected_value |
      | 8% take profit          | 8.0            |
      | target 10%              | 10.0           |
      | 15% profit target       | 15.0           |
      | book profits at 12%     | 12.0           |
      | exit at 20% gain        | 20.0           |
      | take profit of nine percent | 9.0        |

  # Performance benchmarks
  Scenario: Response time for simple strategy
    Given the user provides the prompt "RSI strategy"
    When the AI generates the strategy code
    Then the response time should be less than 15 seconds

  Scenario: Response time for standard strategy
    Given the user provides the prompt "Buy AAPL when MACD crosses above signal. 3% stop, 9% target on daily chart."
    When the AI generates the strategy code
    Then the response time should be less than 18 seconds

  Scenario: Response time for complex strategy
    Given the user provides the prompt "Solana momentum play on 4H chart when volume increases 2x average. Risk 5% for 20% target."
    When the AI generates the strategy code
    Then the response time should be less than 22 seconds

  # Validation rules
  Scenario: All constants must be present
    Given the user provides any valid strategy prompt
    When the AI generates the strategy code
    Then the generated code must contain SYMBOL constant
    And the generated code must contain TIMEFRAME constant
    And the generated code must contain STOP_LOSS constant
    And the generated code must contain TAKE_PROFIT constant
    And the generated code must contain POSITION_SIZE constant

  Scenario: Constants must use correct format
    Given the user provides any valid strategy prompt
    When the AI generates the strategy code
    Then SYMBOL must be a string in single quotes
    And TIMEFRAME must be a string in single quotes
    And STOP_LOSS must be a float (percentage, not decimal)
    And TAKE_PROFIT must be a float (percentage, not decimal)
    And POSITION_SIZE must be an integer
