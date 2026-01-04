package io.strategiz.service.labs.controller;

import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.business.aichat.LLMRouter;
import io.strategiz.service.labs.model.AIStrategyResponse;
import io.strategiz.service.labs.service.AIStrategyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AI Strategy Parsing functionality.
 * Tests the /parse-code endpoint with various Python code patterns to ensure
 * correct extraction of visual rules, AND/OR logic, and field values.
 *
 * Test Cases based on improved CODE_TO_VISUAL_PROMPT examples:
 * 1. Simple RSI strategy (value extraction)
 * 2. MACD with AND logic (multi-condition, boolean logic)
 * 3. Crossover detection (special comparator)
 * 4. OR logic (alternative conditions)
 */
@ExtendWith(MockitoExtension.class)
class AIStrategyParsingIntegrationTest {

	@Mock
	private LLMRouter llmRouter;

	@InjectMocks
	private AIStrategyService aiStrategyService;

	@InjectMocks
	private AIStrategyController controller;

	@BeforeEach
	void setUp() {
		// Ensure controller uses the service with mocked LLM
		controller = new AIStrategyController(aiStrategyService);
	}

	// ========================
	// Test Case 1: Simple RSI Strategy
	// ========================

	@Test
	void testParseCode_SimpleRSI_ExtractsCorrectValues() {
		// Arrange
		String pythonCode = """
				if rsi.iloc[-1] < 30:
				    return 'BUY'
				elif rsi.iloc[-1] > 70:
				    return 'SELL'
				""";

		String mockLLMResponse = """
				{
				  "visualConfig": {
				    "symbol": "AAPL",
				    "name": "RSI Strategy",
				    "description": "Buy when RSI below 30, sell when above 70",
				    "rules": [
				      {
				        "id": "rule_1",
				        "type": "entry",
				        "action": "BUY",
				        "logic": "AND",
				        "conditions": [
				          {
				            "id": "cond_1",
				            "indicator": "rsi",
				            "comparator": "lt",
				            "value": 30,
				            "valueType": "number"
				          }
				        ]
				      },
				      {
				        "id": "rule_2",
				        "type": "exit",
				        "action": "SELL",
				        "logic": "AND",
				        "conditions": [
				          {
				            "id": "cond_2",
				            "indicator": "rsi",
				            "comparator": "gt",
				            "value": 70,
				            "valueType": "number"
				          }
				        ]
				      }
				    ],
				    "riskSettings": {
				      "stopLoss": 5.0,
				      "takeProfit": 10.0,
				      "positionSize": 5,
				      "maxPositions": 1
				    }
				  },
				  "canRepresent": true,
				  "extractedIndicators": ["rsi"]
				}
				""";

		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse)));

		Map<String, String> request = Map.of("code", pythonCode, "visualEditorSchema", "");

		// Act
		ResponseEntity<AIStrategyResponse> response = controller.parseCode(request);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().isSuccess());

		Map<String, Object> visualConfig = response.getBody().getVisualConfig();
		assertNotNull(visualConfig);

		// Verify rules array exists
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rules = (List<Map<String, Object>>) visualConfig.get("rules");
		assertNotNull(rules, "Rules array should not be null");
		assertEquals(2, rules.size(), "Should have 2 rules (entry and exit)");

		// Verify entry rule (BUY)
		Map<String, Object> entryRule = rules.get(0);
		assertEquals("entry", entryRule.get("type"));
		assertEquals("BUY", entryRule.get("action"));
		assertEquals("AND", entryRule.get("logic"));

		// Verify entry condition - RSI < 30
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> entryConditions = (List<Map<String, Object>>) entryRule.get("conditions");
		assertEquals(1, entryConditions.size());

		Map<String, Object> rsiCondition = entryConditions.get(0);
		assertEquals("rsi", rsiCondition.get("indicator"));
		assertEquals("lt", rsiCondition.get("comparator"));
		assertEquals(30, rsiCondition.get("value"));
		assertEquals("number", rsiCondition.get("valueType"));

		// Verify exit rule (SELL)
		Map<String, Object> exitRule = rules.get(1);
		assertEquals("exit", exitRule.get("type"));
		assertEquals("SELL", exitRule.get("action"));

		// Verify exit condition - RSI > 70
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> exitConditions = (List<Map<String, Object>>) exitRule.get("conditions");
		Map<String, Object> rsiExitCondition = exitConditions.get(0);
		assertEquals(70, rsiExitCondition.get("value"));
		assertEquals("gt", rsiExitCondition.get("comparator"));
	}

	// ========================
	// Test Case 2: MACD with AND Logic
	// ========================

	@Test
	void testParseCode_MACD_AND_Logic_ExtractsMultipleConditions() {
		// Arrange
		String pythonCode = """
				if macd.iloc[-1] > macd_signal.iloc[-1] and macd.iloc[-1] < 0:
				    return 'BUY'
				""";

		String mockLLMResponse = """
				{
				  "visualConfig": {
				    "rules": [
				      {
				        "id": "rule_1",
				        "type": "entry",
				        "action": "BUY",
				        "logic": "AND",
				        "conditions": [
				          {
				            "id": "cond_1",
				            "indicator": "macd",
				            "comparator": "gt",
				            "value": "macd_signal",
				            "valueType": "indicator",
				            "secondaryIndicator": "macd_signal"
				          },
				          {
				            "id": "cond_2",
				            "indicator": "macd",
				            "comparator": "lt",
				            "value": 0,
				            "valueType": "number"
				          }
				        ]
				      }
				    ]
				  },
				  "canRepresent": true
				}
				""";

		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse)));

		Map<String, String> request = Map.of("code", pythonCode);

		// Act
		ResponseEntity<AIStrategyResponse> response = controller.parseCode(request);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().isSuccess());

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rules = (List<Map<String, Object>>) response.getBody().getVisualConfig().get("rules");
		Map<String, Object> rule = rules.get(0);

		// Verify AND logic
		assertEquals("AND", rule.get("logic"), "Should detect AND logic from Python 'and' operator");

		// Verify two conditions
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.get("conditions");
		assertEquals(2, conditions.size(), "Should extract both conditions from AND expression");

		// Verify first condition - indicator comparison
		Map<String, Object> cond1 = conditions.get(0);
		assertEquals("macd", cond1.get("indicator"));
		assertEquals("gt", cond1.get("comparator"));
		assertEquals("macd_signal", cond1.get("value"));
		assertEquals("indicator", cond1.get("valueType"));
		assertEquals("macd_signal", cond1.get("secondaryIndicator"));

		// Verify second condition - numeric comparison
		Map<String, Object> cond2 = conditions.get(1);
		assertEquals("macd", cond2.get("indicator"));
		assertEquals("lt", cond2.get("comparator"));
		assertEquals(0, cond2.get("value"));
		assertEquals("number", cond2.get("valueType"));
	}

	// ========================
	// Test Case 3: Crossover Detection
	// ========================

	@Test
	void testParseCode_Crossover_DetectsCrossAboveComparator() {
		// Arrange
		String pythonCode = """
				# SMA crossover
				if sma_20.iloc[-1] > sma_50.iloc[-1] and sma_20.iloc[-2] <= sma_50.iloc[-2]:
				    return 'BUY'
				""";

		String mockLLMResponse = """
				{
				  "visualConfig": {
				    "rules": [
				      {
				        "id": "rule_1",
				        "type": "entry",
				        "action": "BUY",
				        "logic": "AND",
				        "conditions": [
				          {
				            "id": "cond_1",
				            "indicator": "sma",
				            "comparator": "crossAbove",
				            "value": "sma_50",
				            "valueType": "indicator",
				            "secondaryIndicator": "sma_50"
				          }
				        ]
				      }
				    ]
				  },
				  "canRepresent": true
				}
				""";

		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse)));

		Map<String, String> request = Map.of("code", pythonCode);

		// Act
		ResponseEntity<AIStrategyResponse> response = controller.parseCode(request);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().isSuccess());

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rules = (List<Map<String, Object>>) response.getBody().getVisualConfig().get("rules");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> conditions = (List<Map<String, Object>>) rules.get(0).get("conditions");

		Map<String, Object> condition = conditions.get(0);
		assertEquals("crossAbove", condition.get("comparator"),
				"Should detect crossover pattern and use 'crossAbove' comparator");
		assertEquals("sma", condition.get("indicator"));
		assertEquals("sma_50", condition.get("value"));
		assertEquals("indicator", condition.get("valueType"));
	}

	// ========================
	// Test Case 4: OR Logic
	// ========================

	@Test
	void testParseCode_OR_Logic_DetectsOROperator() {
		// Arrange
		String pythonCode = """
				if rsi.iloc[-1] < 30 or stoch_k.iloc[-1] < 20:
				    return 'BUY'
				""";

		String mockLLMResponse = """
				{
				  "visualConfig": {
				    "rules": [
				      {
				        "id": "rule_1",
				        "type": "entry",
				        "action": "BUY",
				        "logic": "OR",
				        "conditions": [
				          {
				            "id": "cond_1",
				            "indicator": "rsi",
				            "comparator": "lt",
				            "value": 30,
				            "valueType": "number"
				          },
				          {
				            "id": "cond_2",
				            "indicator": "stoch_k",
				            "comparator": "lt",
				            "value": 20,
				            "valueType": "number"
				          }
				        ]
				      }
				    ]
				  },
				  "canRepresent": true
				}
				""";

		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse)));

		Map<String, String> request = Map.of("code", pythonCode);

		// Act
		ResponseEntity<AIStrategyResponse> response = controller.parseCode(request);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().isSuccess());

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rules = (List<Map<String, Object>>) response.getBody().getVisualConfig().get("rules");
		Map<String, Object> rule = rules.get(0);

		// Verify OR logic
		assertEquals("OR", rule.get("logic"), "Should detect OR logic from Python 'or' operator");

		// Verify both conditions exist
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.get("conditions");
		assertEquals(2, conditions.size());

		// Both should be numeric comparisons
		assertEquals("number", conditions.get(0).get("valueType"));
		assertEquals("number", conditions.get(1).get("valueType"));
	}

	// ========================
	// Test Case 5: Complex Code (canRepresent=false)
	// ========================

	@Test
	void testParseCode_ComplexLoop_ReturnsCanRepresentFalse() {
		// Arrange
		String pythonCode = """
				for i in range(len(data)):
				    if complex_logic(data[i]):
				        signals.append('BUY')
				""";

		String mockLLMResponse = """
				{
				  "visualConfig": {},
				  "canRepresent": false,
				  "warning": "Code uses loops which are not supported by the visual editor. Please use the code editor for this strategy."
				}
				""";

		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse)));

		Map<String, String> request = Map.of("code", pythonCode);

		// Act
		ResponseEntity<AIStrategyResponse> response = controller.parseCode(request);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().isSuccess());
		assertFalse(response.getBody().canRepresentVisually(),
				"Should set canRepresent=false for code with loops");
		assertNotNull(response.getBody().getWarning(),
				"Should provide warning explaining why code can't be represented");
		assertTrue(response.getBody().getWarning().contains("loops"));
	}

	// ========================
	// Test Case 6: Missing Code (Error Handling)
	// ========================

	@Test
	void testParseCode_MissingCode_ReturnsBadRequest() {
		// Arrange
		Map<String, String> request = Map.of(); // Empty request

		// Act
		ResponseEntity<AIStrategyResponse> response = controller.parseCode(request);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertFalse(response.getBody().isSuccess());
		assertNotNull(response.getBody().getError());
	}

	// ========================
	// Test Case 7: Schema Passed to LLM
	// ========================

	@Test
	void testParseCode_WithSchema_PassesSchemaToPrompt() {
		// Arrange
		String pythonCode = "if rsi < 30: return 'BUY'";
		String schema = "AVAILABLE INDICATORS: rsi, macd, sma...";

		String mockLLMResponse = """
				{
				  "visualConfig": {"rules": []},
				  "canRepresent": true
				}
				""";

		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse)));

		Map<String, String> request = Map.of("code", pythonCode, "visualEditorSchema", schema);

		// Act
		controller.parseCode(request);

		// Assert - Capture and verify the prompt includes schema
		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(llmRouter).generateContent(promptCaptor.capture(), anyList(), anyString());

		String capturedPrompt = promptCaptor.getValue();
		assertTrue(capturedPrompt.contains(schema),
				"Prompt should include the visual editor schema for better AI guidance");
	}

}
