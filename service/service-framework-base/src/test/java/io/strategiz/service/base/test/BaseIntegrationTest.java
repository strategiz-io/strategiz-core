package io.strategiz.service.base.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.framework.exception.StandardErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Base class for integration tests that test actual REST API endpoints.
 *
 * <p>Integration tests verify that:</p>
 * <ul>
 *   <li>Controllers correctly handle HTTP requests</li>
 *   <li>Request validation works as expected</li>
 *   <li>Services are properly integrated</li>
 *   <li>Error handling returns correct HTTP status codes</li>
 *   <li>Response JSON structure is correct</li>
 * </ul>
 *
 * <p>These tests use Spring Boot's test framework with MockMvc to invoke
 * actual controller endpoints without starting a full HTTP server.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @SpringBootTest(classes = AuthApplication.class)
 * public class GoogleOAuthControllerIntegrationTest extends BaseIntegrationTest {
 *
 *     @Test
 *     public void testOAuthRedirect() throws Exception {
 *         performGet("/v1/auth/oauth/google/signin")
 *             .andExpect(status().is3xxRedirection())
 *             .andExpect(header().exists("Location"));
 *     }
 *
 *     @Test
 *     public void testInvalidCallback() throws Exception {
 *         Map<String, String> params = Map.of(
 *             "code", "invalid-code",
 *             "state", "test-state"
 *         );
 *
 *         performGet("/v1/auth/oauth/google/callback", params)
 *             .andExpect(status().isBadRequest())
 *             .andExpectError("OAUTH_ERROR");
 *     }
 * }
 * }</pre>
 *
 * @see MockMvc
 * @see SpringBootTest
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Setup method called before each test.
     * Override in subclasses for test-specific setup.
     */
    @BeforeEach
    public void setUp() {
        log.info("Setting up test: {}", this.getClass().getSimpleName());
    }

    // ============================================
    // HTTP Request Helpers
    // ============================================

    /**
     * Perform GET request to the specified endpoint.
     *
     * @param endpoint The API endpoint (e.g., "/v1/auth/login")
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performGet(String endpoint) throws Exception {
        log.debug("GET {}", endpoint);
        return mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print());
    }

    /**
     * Perform GET request with query parameters.
     *
     * @param endpoint The API endpoint
     * @param params Query parameters as key-value map
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performGet(String endpoint, Map<String, String> params) throws Exception {
        log.debug("GET {} with params: {}", endpoint, params);
        MockHttpServletRequestBuilder request = get(endpoint)
            .contentType(MediaType.APPLICATION_JSON);

        params.forEach(request::param);

        return mockMvc.perform(request).andDo(print());
    }

    /**
     * Perform POST request with JSON body.
     *
     * @param endpoint The API endpoint
     * @param requestBody The request body object (will be serialized to JSON)
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performPost(String endpoint, Object requestBody) throws Exception {
        String json = objectMapper.writeValueAsString(requestBody);
        log.debug("POST {} with body: {}", endpoint, json);

        return mockMvc.perform(post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print());
    }

    /**
     * Perform POST request without body.
     *
     * @param endpoint The API endpoint
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performPost(String endpoint) throws Exception {
        log.debug("POST {}", endpoint);
        return mockMvc.perform(post(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print());
    }

    /**
     * Perform PUT request with JSON body.
     *
     * @param endpoint The API endpoint
     * @param requestBody The request body object
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performPut(String endpoint, Object requestBody) throws Exception {
        String json = objectMapper.writeValueAsString(requestBody);
        log.debug("PUT {} with body: {}", endpoint, json);

        return mockMvc.perform(put(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print());
    }

    /**
     * Perform DELETE request.
     *
     * @param endpoint The API endpoint
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performDelete(String endpoint) throws Exception {
        log.debug("DELETE {}", endpoint);
        return mockMvc.perform(delete(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print());
    }

    /**
     * Perform PATCH request with JSON body.
     *
     * @param endpoint The API endpoint
     * @param requestBody The request body object
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performPatch(String endpoint, Object requestBody) throws Exception {
        String json = objectMapper.writeValueAsString(requestBody);
        log.debug("PATCH {} with body: {}", endpoint, json);

        return mockMvc.perform(patch(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print());
    }

    // ============================================
    // Authenticated Request Helpers
    // ============================================

    /**
     * Perform authenticated GET request with access token.
     *
     * @param endpoint The API endpoint
     * @param accessToken The access token (PASETO token)
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performAuthenticatedGet(String endpoint, String accessToken) throws Exception {
        log.debug("GET {} (authenticated)", endpoint);
        return mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken))
            .andDo(print());
    }

    /**
     * Perform authenticated POST request with access token and JSON body.
     *
     * @param endpoint The API endpoint
     * @param accessToken The access token
     * @param requestBody The request body object
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performAuthenticatedPost(String endpoint, String accessToken, Object requestBody) throws Exception {
        String json = objectMapper.writeValueAsString(requestBody);
        log.debug("POST {} (authenticated) with body: {}", endpoint, json);

        return mockMvc.perform(post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(json))
            .andDo(print());
    }

    /**
     * Perform authenticated PUT request with access token and JSON body.
     *
     * @param endpoint The API endpoint
     * @param accessToken The access token
     * @param requestBody The request body object
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performAuthenticatedPut(String endpoint, String accessToken, Object requestBody) throws Exception {
        String json = objectMapper.writeValueAsString(requestBody);
        log.debug("PUT {} (authenticated) with body: {}", endpoint, json);

        return mockMvc.perform(put(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(json))
            .andDo(print());
    }

    /**
     * Perform authenticated DELETE request with access token.
     *
     * @param endpoint The API endpoint
     * @param accessToken The access token
     * @return ResultActions for fluent assertion chaining
     * @throws Exception if request fails
     */
    protected ResultActions performAuthenticatedDelete(String endpoint, String accessToken) throws Exception {
        log.debug("DELETE {} (authenticated)", endpoint);
        return mockMvc.perform(delete(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken))
            .andDo(print());
    }

    // ============================================
    // Response Assertion Helpers
    // ============================================

    /**
     * Assert that response contains an error with the specified error code.
     *
     * @param actions The result actions from a request
     * @param expectedErrorCode The expected error code
     * @return ResultActions for further chaining
     * @throws Exception if assertion fails
     */
    protected ResultActions andExpectError(ResultActions actions, String expectedErrorCode) throws Exception {
        return actions.andExpect(jsonPath("$.errorCode").value(expectedErrorCode));
    }

    /**
     * Assert that response contains a specific field with expected value.
     *
     * @param actions The result actions from a request
     * @param jsonPath The JSON path to the field (e.g., "$.userId")
     * @param expectedValue The expected value
     * @return ResultActions for further chaining
     * @throws Exception if assertion fails
     */
    protected ResultActions andExpectField(ResultActions actions, String jsonPath, Object expectedValue) throws Exception {
        return actions.andExpect(jsonPath(jsonPath).value(expectedValue));
    }

    /**
     * Assert that response contains a specific field.
     *
     * @param actions The result actions from a request
     * @param jsonPath The JSON path to the field
     * @return ResultActions for further chaining
     * @throws Exception if assertion fails
     */
    protected ResultActions andExpectFieldExists(ResultActions actions, String jsonPath) throws Exception {
        return actions.andExpect(jsonPath(jsonPath).exists());
    }

    /**
     * Extract response body as a specific type.
     *
     * @param result The MVC result
     * @param responseType The class type to deserialize to
     * @return Deserialized response object
     * @throws Exception if deserialization fails
     */
    protected <T> T extractResponse(MvcResult result, Class<T> responseType) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        log.debug("Response body: {}", responseBody);
        return objectMapper.readValue(responseBody, responseType);
    }

    /**
     * Extract error response.
     *
     * @param result The MVC result
     * @return StandardErrorResponse object
     * @throws Exception if deserialization fails
     */
    protected StandardErrorResponse extractErrorResponse(MvcResult result) throws Exception {
        return extractResponse(result, StandardErrorResponse.class);
    }

    // ============================================
    // Test Data Helpers
    // ============================================

    /**
     * Generate a unique test email.
     *
     * @return Test email address
     */
    protected String generateTestEmail() {
        return "test-" + System.currentTimeMillis() + "@test.strategiz.io";
    }

    /**
     * Generate a unique test phone number.
     *
     * @return Test phone number
     */
    protected String generateTestPhone() {
        return "+1555" + (System.currentTimeMillis() % 10000000);
    }

    /**
     * Generate a random test string.
     *
     * @param prefix The prefix for the string
     * @return Random test string
     */
    protected String generateTestString(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    /**
     * Log test step for better test output readability.
     *
     * @param step The test step description
     */
    protected void logTestStep(String step) {
        log.info(">>> TEST STEP: {}", step);
    }

    /**
     * Log test assertion for debugging.
     *
     * @param assertion The assertion being made
     */
    protected void logAssertion(String assertion) {
        log.debug("    Asserting: {}", assertion);
    }
}
