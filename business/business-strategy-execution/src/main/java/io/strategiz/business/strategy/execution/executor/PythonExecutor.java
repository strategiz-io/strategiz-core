package io.strategiz.business.strategy.execution.executor;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Python strategy executor using GraalVM Polyglot
 *
 * Executes user-provided Python trading strategies in a secure sandbox environment.
 * Provides market data context and captures execution output including trading signals.
 */
@Component
public class PythonExecutor {

	private static final Logger logger = LoggerFactory.getLogger(PythonExecutor.class);

	// Execution constraints
	private static final long EXECUTION_TIMEOUT_SECONDS = 30;

	private static final String DEFAULT_RESULT = "{\"signal\": \"HOLD\", \"reason\": \"No signal generated\"}";

	public PythonExecutor() {
		logger.info("Python executor initialized with GraalVM Polyglot");
	}

	/**
	 * Execute Python strategy code with market data context
	 * @param code The Python code to execute
	 * @return Execution result as JSON string
	 */
	public String execute(String code) {
		return executeWithContext(code, new HashMap<>());
	}

	/**
	 * Execute Python strategy code with market data context
	 * @param code The Python code to execute
	 * @param marketData Market data to inject (price, volume, etc.)
	 * @return Execution result as JSON string containing signal and indicators
	 */
	public String executeWithContext(String code, Map<String, Object> marketData) {
		logger.info("Executing Python strategy code with GraalVM");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		try (Context context = Context.newBuilder("python")
			.allowExperimentalOptions(true)
			.option("python.EmulateJython", "true")
			.option("python.WarnOptions", "ignore")
			.out(out)
			.err(err)
			.build()) {

			// Set execution timeout
			context.enter();

			// Inject market data as global variables
			Value bindings = context.getBindings("python");
			for (Map.Entry<String, Object> entry : marketData.entrySet()) {
				bindings.putMember(entry.getKey(), entry.getValue());
			}

			// Add helper function to create signal output
			String helperCode = """
					import json

					def create_signal(signal_type, price=None, quantity=None, reason=None, indicators=None):
					    '''
					    Create a trading signal output.

					    Args:
					        signal_type: 'BUY', 'SELL', or 'HOLD'
					        price: Target price (optional)
					        quantity: Suggested quantity (optional)
					        reason: Reason for the signal (optional)
					        indicators: Dict of indicator values like {'rsi': 28, 'macd': 1.5}

					    Returns:
					        JSON string with signal data
					    '''
					    result = {
					        'signal': signal_type.upper(),
					        'price': price,
					        'quantity': quantity,
					        'reason': reason or 'Strategy logic triggered',
					        'indicators': indicators or {}
					    }
					    return json.dumps(result)

					# Make sure result variable exists
					result = None
					""";

			context.eval("python", helperCode);

			// Execute user's strategy code
			Value executionResult = context.eval("python", code);

			// Get the 'result' variable if it was set by the user
			Value resultVar = bindings.getMember("result");

			context.leave();

			// Parse output
			String output = out.toString().trim();
			String errors = err.toString().trim();

			if (!errors.isEmpty()) {
				logger.warn("Python execution warnings/errors: {}", errors);
			}

			// Try to get result in order of preference:
			// 1. User set 'result' variable
			// 2. Last expression return value
			// 3. Stdout output
			String finalResult = null;

			if (resultVar != null && !resultVar.isNull()) {
				finalResult = resultVar.asString();
			}
			else if (executionResult != null && !executionResult.isNull() && executionResult.isString()) {
				finalResult = executionResult.asString();
			}
			else if (!output.isEmpty()) {
				// Check if output is already JSON
				if (output.startsWith("{") && output.contains("signal")) {
					finalResult = output;
				}
				else {
					logger.debug("Strategy output (not JSON): {}", output);
				}
			}

			// Validate result is valid JSON with signal
			if (finalResult != null && finalResult.contains("\"signal\"")) {
				logger.info("Strategy executed successfully, signal generated");
				return finalResult;
			}
			else {
				logger.info("Strategy executed but no signal generated, defaulting to HOLD");
				return DEFAULT_RESULT;
			}

		}
		catch (PolyglotException e) {
			logger.error("Python execution error: {}", e.getMessage());
			String errorOutput = err.toString();
			return createErrorResult(
					"Execution error: " + e.getMessage() + (errorOutput.isEmpty() ? "" : "\n" + errorOutput));

		}
		catch (Exception e) {
			logger.error("Unexpected error during Python execution", e);
			return createErrorResult("Unexpected error: " + e.getMessage());
		}
	}

	/**
	 * Validate Python code syntax
	 * @param code The Python code to validate
	 * @return true if valid, false otherwise
	 */
	public boolean validateCode(String code) {
		if (code == null || code.trim().isEmpty()) {
			return false;
		}

		try (Context context = Context.newBuilder("python")
			.allowExperimentalOptions(true)
			.option("python.EmulateJython", "true")
			.build()) {

			// Try to parse the code (doesn't execute, just validates syntax)
			context.parse("python", code);
			return true;

		}
		catch (PolyglotException e) {
			logger.warn("Python code validation failed: {}", e.getMessage());
			return false;
		}
		catch (Exception e) {
			logger.error("Error validating Python code", e);
			return false;
		}
	}

	/**
	 * Create error result in JSON format
	 */
	private String createErrorResult(String errorMessage) {
		return String.format("{\"signal\": \"HOLD\", \"reason\": \"Error: %s\", \"error\": true}",
				errorMessage.replace("\"", "\\\""));
	}

}