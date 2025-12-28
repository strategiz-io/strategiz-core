package io.strategiz.service.labs.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.strategiz.service.labs.model.AIStrategyRequest;
import io.strategiz.service.labs.model.AIStrategyResponse;
import io.strategiz.service.labs.service.AIStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for AI Strategy Natural Language Extraction BDD tests.
 *
 * These tests verify that the AI can correctly extract trading constants
 * (SYMBOL, TIMEFRAME, STOP_LOSS, TAKE_PROFIT, POSITION_SIZE) from natural
 * language prompts.
 */
@SpringBootTest
public class AIStrategyExtractionSteps {

    @Autowired
    private AIStrategyService aiStrategyService;

    private String userPrompt;
    private AIStrategyResponse generatedStrategy;
    private long startTime;
    private long responseTime;
    private Exception thrownException;

    @Given("the AI Strategy Generator is available")
    public void theAIStrategyGeneratorIsAvailable() {
        assertNotNull(aiStrategyService, "AIStrategyService should be available");
    }

    @Given("the user is authenticated")
    public void theUserIsAuthenticated() {
        // In BDD tests, we bypass authentication
        // In real integration tests, this would set up auth context
    }

    @Given("the user provides the prompt {string}")
    public void theUserProvidesThePrompt(String prompt) {
        this.userPrompt = prompt;
    }

    @Given("the user provides a prompt containing {string}")
    public void theUserProvidesAPromptContaining(String phrase) {
        // For scenario outlines testing specific phrases
        this.userPrompt = "Create a trading strategy " + phrase + " with standard settings";
    }

    @Given("the user provides any valid strategy prompt")
    public void theUserProvidesAnyValidStrategyPrompt() {
        // Default test prompt
        this.userPrompt = "Buy AAPL when RSI is oversold. 3% stop loss, 9% take profit on daily chart.";
    }

    @When("the AI generates the strategy code")
    public void theAIGeneratesTheStrategyCode() {
        try {
            startTime = System.currentTimeMillis();

            AIStrategyRequest request = new AIStrategyRequest();
            request.setPrompt(userPrompt);
            request.setRequestType(AIStrategyRequest.RequestType.GENERATE);
            request.setModel("gemini-2.5-flash"); // Default model for testing

            generatedStrategy = aiStrategyService.generateStrategy(request);

            responseTime = System.currentTimeMillis() - startTime;

            assertNotNull(generatedStrategy, "Generated strategy should not be null");
            assertNotNull(generatedStrategy.getPythonCode(), "Generated code should not be null");

        } catch (Exception e) {
            thrownException = e;
        }
    }

    @Then("the extracted SYMBOL should be {string}")
    public void theExtractedSYMBOLShouldBe(String expectedSymbol) {
        String generatedCode = generatedStrategy.getPythonCode();
        String extractedSymbol = extractConstant(generatedCode, "SYMBOL");

        assertEquals(expectedSymbol, extractedSymbol,
            String.format("Expected SYMBOL='%s' but found '%s' in generated code", expectedSymbol, extractedSymbol));
    }

    @Then("the extracted TIMEFRAME should be {string}")
    public void theExtractedTIMEFRAMEShouldBe(String expectedTimeframe) {
        String generatedCode = generatedStrategy.getPythonCode();
        String extractedTimeframe = extractConstant(generatedCode, "TIMEFRAME");

        assertEquals(expectedTimeframe, extractedTimeframe,
            String.format("Expected TIMEFRAME='%s' but found '%s' in generated code", expectedTimeframe, extractedTimeframe));
    }

    @Then("the extracted STOP_LOSS should be {double}")
    public void theExtractedSTOP_LOSSShouldBe(Double expectedStopLoss) {
        String generatedCode = generatedStrategy.getPythonCode();
        Double extractedStopLoss = extractNumericConstant(generatedCode, "STOP_LOSS");

        assertEquals(expectedStopLoss, extractedStopLoss, 0.01,
            String.format("Expected STOP_LOSS=%.1f but found %.1f in generated code", expectedStopLoss, extractedStopLoss));
    }

    @Then("the extracted TAKE_PROFIT should be {double}")
    public void theExtractedTAKE_PROFITShouldBe(Double expectedTakeProfit) {
        String generatedCode = generatedStrategy.getPythonCode();
        Double extractedTakeProfit = extractNumericConstant(generatedCode, "TAKE_PROFIT");

        assertEquals(expectedTakeProfit, extractedTakeProfit, 0.01,
            String.format("Expected TAKE_PROFIT=%.1f but found %.1f in generated code", expectedTakeProfit, extractedTakeProfit));
    }

    @Then("the extracted POSITION_SIZE should be {int}")
    public void theExtractedPOSITION_SIZEShouldBe(Integer expectedPositionSize) {
        String generatedCode = generatedStrategy.getPythonCode();
        Integer extractedPositionSize = extractIntegerConstant(generatedCode, "POSITION_SIZE");

        assertEquals(expectedPositionSize, extractedPositionSize,
            String.format("Expected POSITION_SIZE=%d but found %d in generated code", expectedPositionSize, extractedPositionSize));
    }

    @Then("the response time should be less than {int} seconds")
    public void theResponseTimeShouldBeLessThan(int maxSeconds) {
        long maxMillis = maxSeconds * 1000L;
        assertTrue(responseTime < maxMillis,
            String.format("Response time %dms exceeded maximum %dms (%ds)", responseTime, maxMillis, maxSeconds));
    }

    @Then("the generated code must contain SYMBOL constant")
    public void theGeneratedCodeMustContainSYMBOLConstant() {
        String generatedCode = generatedStrategy.getPythonCode();
        assertTrue(generatedCode.contains("SYMBOL =") || generatedCode.contains("SYMBOL="),
            "Generated code must contain SYMBOL constant");
    }

    @Then("the generated code must contain TIMEFRAME constant")
    public void theGeneratedCodeMustContainTIMEFRAMEConstant() {
        String generatedCode = generatedStrategy.getPythonCode();
        assertTrue(generatedCode.contains("TIMEFRAME =") || generatedCode.contains("TIMEFRAME="),
            "Generated code must contain TIMEFRAME constant");
    }

    @Then("the generated code must contain STOP_LOSS constant")
    public void theGeneratedCodeMustContainSTOP_LOSSConstant() {
        String generatedCode = generatedStrategy.getPythonCode();
        assertTrue(generatedCode.contains("STOP_LOSS =") || generatedCode.contains("STOP_LOSS="),
            "Generated code must contain STOP_LOSS constant");
    }

    @Then("the generated code must contain TAKE_PROFIT constant")
    public void theGeneratedCodeMustContainTAKE_PROFITConstant() {
        String generatedCode = generatedStrategy.getPythonCode();
        assertTrue(generatedCode.contains("TAKE_PROFIT =") || generatedCode.contains("TAKE_PROFIT="),
            "Generated code must contain TAKE_PROFIT constant");
    }

    @Then("the generated code must contain POSITION_SIZE constant")
    public void theGeneratedCodeMustContainPOSITION_SIZEConstant() {
        String generatedCode = generatedStrategy.getPythonCode();
        assertTrue(generatedCode.contains("POSITION_SIZE =") || generatedCode.contains("POSITION_SIZE="),
            "Generated code must contain POSITION_SIZE constant");
    }

    @Then("SYMBOL must be a string in single quotes")
    public void symbolMustBeAStringInSingleQuotes() {
        String generatedCode = generatedStrategy.getPythonCode();
        Pattern pattern = Pattern.compile("SYMBOL\\s*=\\s*'([^']+)'");
        Matcher matcher = pattern.matcher(generatedCode);
        assertTrue(matcher.find(), "SYMBOL must be a string in single quotes (e.g., SYMBOL = 'AAPL')");
    }

    @Then("TIMEFRAME must be a string in single quotes")
    public void timeframeMustBeAStringInSingleQuotes() {
        String generatedCode = generatedStrategy.getPythonCode();
        Pattern pattern = Pattern.compile("TIMEFRAME\\s*=\\s*'([^']+)'");
        Matcher matcher = pattern.matcher(generatedCode);
        assertTrue(matcher.find(), "TIMEFRAME must be a string in single quotes (e.g., TIMEFRAME = '1D')");
    }

    @Then("STOP_LOSS must be a float \\(percentage, not decimal)")
    public void stopLossMustBeAFloatPercentageNotDecimal() {
        String generatedCode = generatedStrategy.getPythonCode();
        Double stopLoss = extractNumericConstant(generatedCode, "STOP_LOSS");
        assertNotNull(stopLoss, "STOP_LOSS must be present");
        assertTrue(stopLoss >= 0.1 && stopLoss <= 100,
            "STOP_LOSS must be a percentage (e.g., 3.0 for 3%, not 0.03)");
    }

    @Then("TAKE_PROFIT must be a float \\(percentage, not decimal)")
    public void takeProfitMustBeAFloatPercentageNotDecimal() {
        String generatedCode = generatedStrategy.getPythonCode();
        Double takeProfit = extractNumericConstant(generatedCode, "TAKE_PROFIT");
        assertNotNull(takeProfit, "TAKE_PROFIT must be present");
        assertTrue(takeProfit >= 0.1 && takeProfit <= 200,
            "TAKE_PROFIT must be a percentage (e.g., 9.0 for 9%, not 0.09)");
    }

    @Then("POSITION_SIZE must be an integer")
    public void positionSizeMustBeAnInteger() {
        String generatedCode = generatedStrategy.getPythonCode();
        Pattern pattern = Pattern.compile("POSITION_SIZE\\s*=\\s*(\\d+)");
        Matcher matcher = pattern.matcher(generatedCode);
        assertTrue(matcher.find(), "POSITION_SIZE must be an integer (e.g., POSITION_SIZE = 5)");
    }

    // Helper methods to extract constants from generated Python code

    /**
     * Extract string constant value from Python code.
     * Example: SYMBOL = 'AAPL' → returns "AAPL"
     */
    private String extractConstant(String code, String constantName) {
        Pattern pattern = Pattern.compile(constantName + "\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract numeric constant value from Python code.
     * Example: STOP_LOSS = 3.0 → returns 3.0
     */
    private Double extractNumericConstant(String code, String constantName) {
        Pattern pattern = Pattern.compile(constantName + "\\s*=\\s*([0-9]+\\.?[0-9]*)");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return null;
    }

    /**
     * Extract integer constant value from Python code.
     * Example: POSITION_SIZE = 5 → returns 5
     */
    private Integer extractIntegerConstant(String code, String constantName) {
        Pattern pattern = Pattern.compile(constantName + "\\s*=\\s*(\\d+)");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }
}
