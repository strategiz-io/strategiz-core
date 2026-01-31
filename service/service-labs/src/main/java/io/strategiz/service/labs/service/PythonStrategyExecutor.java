package io.strategiz.service.labs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.service.labs.model.ExecuteStrategyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for executing Python trading strategies Handles subprocess execution, code
 * injection, and result parsing
 */
@Service
public class PythonStrategyExecutor {

	private static final Logger logger = LoggerFactory.getLogger(PythonStrategyExecutor.class);

	private static final int EXECUTION_TIMEOUT_SECONDS = 30;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Execute Python strategy code with market data
	 * @param userCode User's Python strategy code
	 * @param marketDataJson Market data as JSON string
	 * @return ExecuteStrategyResponse with indicators and signals
	 */
	public ExecuteStrategyResponse executePythonCode(String userCode, String marketDataJson) {
		long startTime = System.currentTimeMillis();
		ExecuteStrategyResponse response = new ExecuteStrategyResponse();
		response.setErrors(new ArrayList<>());
		response.setLogs(new ArrayList<>());

		try {
			// Read the Python wrapper template
			String wrapperScript = loadPythonWrapper();

			// Inject user code into wrapper
			String finalScript = wrapperScript.replace("USER_CODE_PLACEHOLDER", userCode);

			// Create temporary file for the script
			File tempScript = Files.createTempFile("strategy_", ".py").toFile();
			tempScript.deleteOnExit();
			Files.writeString(tempScript.toPath(), finalScript, StandardCharsets.UTF_8);

			// Build Python command
			ProcessBuilder processBuilder = new ProcessBuilder("python3", tempScript.getAbsolutePath());

			processBuilder.redirectErrorStream(false);

			logger.info("Executing Python strategy...");

			// Start process
			Process process = processBuilder.start();

			// Write market data to stdin
			try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(),
					StandardCharsets.UTF_8)) {
				writer.write(marketDataJson);
				writer.flush();
			}

			// Read stdout (execution results)
			String stdout = readStream(process.getInputStream());

			// Read stderr (errors/logs)
			String stderr = readStream(process.getErrorStream());

			// Wait for process completion with timeout
			boolean completed = process.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

			if (!completed) {
				process.destroyForcibly();
				response.getErrors().add("Execution timeout after " + EXECUTION_TIMEOUT_SECONDS + " seconds");
				response.setExecutionTime(System.currentTimeMillis() - startTime);
				return response;
			}

			int exitCode = process.exitValue();

			if (exitCode != 0) {
				logger.error("Python execution failed with exit code: {}", exitCode);
				response.getErrors().add("Python execution failed with exit code: " + exitCode);
				if (!stderr.isEmpty()) {
					response.getErrors().add("Error output: " + stderr);
				}
				response.setExecutionTime(System.currentTimeMillis() - startTime);
				return response;
			}

			// Parse JSON output from Python script
			if (!stdout.isEmpty()) {
				parseExecutionResults(stdout, response);
			}
			else {
				response.getErrors().add("No output received from Python execution");
			}

			// Add stderr as logs if present
			if (!stderr.isEmpty()) {
				response.getLogs().add(stderr);
			}

			logger.info("Python execution completed successfully in {} ms", System.currentTimeMillis() - startTime);

		}
		catch (IOException e) {
			logger.error("IO error during Python execution", e);
			response.getErrors().add("IO error: " + e.getMessage());
		}
		catch (InterruptedException e) {
			logger.error("Python execution was interrupted", e);
			response.getErrors().add("Execution interrupted: " + e.getMessage());
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			logger.error("Unexpected error during Python execution", e);
			response.getErrors().add("Unexpected error: " + e.getMessage());
		}

		response.setExecutionTime(System.currentTimeMillis() - startTime);
		return response;
	}

	/**
	 * Load Python wrapper script from resources
	 */
	private String loadPythonWrapper() throws IOException {
		ClassPathResource resource = new ClassPathResource("python/strategy_executor.py");
		try (InputStream inputStream = resource.getInputStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Read all content from an input stream
	 */
	private String readStream(InputStream inputStream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
			return sb.toString().trim();
		}
	}

	/**
	 * Parse JSON results from Python execution
	 */
	private void parseExecutionResults(String jsonOutput, ExecuteStrategyResponse response) {
		try {
			JsonNode root = objectMapper.readTree(jsonOutput);

			// Parse indicators
			if (root.has("indicators")) {
				List<ExecuteStrategyResponse.Indicator> indicators = new ArrayList<>();
				for (JsonNode indicatorNode : root.get("indicators")) {
					ExecuteStrategyResponse.Indicator indicator = parseIndicator(indicatorNode);
					indicators.add(indicator);
				}
				response.setIndicators(indicators);
			}

			// Parse signals
			if (root.has("signals")) {
				List<ExecuteStrategyResponse.Signal> signals = new ArrayList<>();
				for (JsonNode signalNode : root.get("signals")) {
					ExecuteStrategyResponse.Signal signal = parseSignal(signalNode);
					signals.add(signal);
				}
				response.setSignals(signals);
			}

			// Parse errors
			if (root.has("errors")) {
				for (JsonNode errorNode : root.get("errors")) {
					response.getErrors().add(errorNode.asText());
				}
			}

		}
		catch (Exception e) {
			logger.error("Failed to parse Python execution results", e);
			response.getErrors().add("Failed to parse results: " + e.getMessage());
			response.getLogs().add("Raw output: " + jsonOutput);
		}
	}

	/**
	 * Parse indicator from JSON node
	 */
	private ExecuteStrategyResponse.Indicator parseIndicator(JsonNode node) {
		ExecuteStrategyResponse.Indicator indicator = new ExecuteStrategyResponse.Indicator();
		indicator.setName(node.get("name").asText());
		indicator.setColor(node.get("color").asText());
		indicator.setLinewidth(node.has("linewidth") ? node.get("linewidth").asInt() : 2);
		indicator.setOverlay(node.has("overlay") ? node.get("overlay").asBoolean() : true);
		indicator.setType("line"); // Default type

		// Parse data points
		List<ExecuteStrategyResponse.Indicator.DataPoint> dataPoints = new ArrayList<>();
		if (node.has("data")) {
			for (JsonNode dataNode : node.get("data")) {
				ExecuteStrategyResponse.Indicator.DataPoint dataPoint = new ExecuteStrategyResponse.Indicator.DataPoint();
				dataPoint.setTime(dataNode.get("time").asText());
				dataPoint.setValue(dataNode.get("value").asDouble());
				dataPoints.add(dataPoint);
			}
		}
		indicator.setData(dataPoints);

		return indicator;
	}

	/**
	 * Parse signal from JSON node
	 */
	private ExecuteStrategyResponse.Signal parseSignal(JsonNode node) {
		ExecuteStrategyResponse.Signal signal = new ExecuteStrategyResponse.Signal();
		signal.setType(node.get("type").asText());
		signal.setTimestamp(node.get("timestamp").asText());
		signal.setPrice(node.get("price").asDouble());
		signal.setText(node.has("text") ? node.get("text").asText() : signal.getType());
		signal.setShape(node.has("shape") ? node.get("shape").asText() : "circle");

		return signal;
	}

}
