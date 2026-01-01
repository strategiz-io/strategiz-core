package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.base.test.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OAuthProviderController.
 *
 * <p>These tests verify the OAuth provider endpoints work correctly
 * by invoking actual controller methods with MockMvc.</p>
 *
 * <h3>Test Coverage:</h3>
 * <ul>
 *   <li>Authorization URL generation for signup/signin</li>
 *   <li>OAuth redirect flows</li>
 *   <li>OAuth callback handling</li>
 *   <li>Error handling for invalid providers</li>
 * </ul>
 *
 * <h3>Test Strategy:</h3>
 * <p>Integration tests focus on HTTP layer verification:</p>
 * <ul>
 *   <li>Request mapping and routing</li>
 *   <li>HTTP status codes</li>
 *   <li>Response structure and content</li>
 *   <li>Error responses</li>
 * </ul>
 *
 * <p>Business logic testing is done in service layer unit tests.</p>
 *
 * @see OAuthProviderController
 * @see BaseIntegrationTest
 */
@SpringBootTest(classes = io.strategiz.service.auth.TestApplication.class)
@ActiveProfiles("test")
@DisplayName("OAuth Provider Controller Integration Tests")
public class OAuthProviderControllerIntegrationTest extends BaseIntegrationTest {

    private static final String OAUTH_BASE_PATH = "/v1/auth/oauth/{provider}";

    // ============================================
    // Authorization URL Endpoints
    // ============================================

    @Nested
    @DisplayName("GET /v1/auth/oauth/{provider}/signup/authorization-url")
    class GetSignupAuthorizationUrl {

        @Test
        @DisplayName("should return authorization URL for Google signup")
        void shouldReturnAuthorizationUrlForGoogleSignup() throws Exception {
            logTestStep("Request Google OAuth signup authorization URL");

            performGet("/v1/auth/oauth/google/signup/authorization-url")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.state").exists());

            logAssertion("Response contains url and state fields");
        }

        @Test
        @DisplayName("should return authorization URL for Facebook signup")
        void shouldReturnAuthorizationUrlForFacebookSignup() throws Exception {
            logTestStep("Request Facebook OAuth signup authorization URL");

            performGet("/v1/auth/oauth/facebook/signup/authorization-url")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.state").exists());
        }

        @Test
        @DisplayName("should return 400 for unsupported provider")
        void shouldReturn400ForUnsupportedProvider() throws Exception {
            logTestStep("Request authorization URL for unsupported provider");

            performGet("/v1/auth/oauth/invalid-provider/signup/authorization-url")
                .andExpect(status().isBadRequest());

            logAssertion("Returns 400 Bad Request for invalid provider");
        }
    }

    @Nested
    @DisplayName("GET /v1/auth/oauth/{provider}/signin/authorization-url")
    class GetSigninAuthorizationUrl {

        @Test
        @DisplayName("should return authorization URL for Google signin")
        void shouldReturnAuthorizationUrlForGoogleSignin() throws Exception {
            logTestStep("Request Google OAuth signin authorization URL");

            performGet("/v1/auth/oauth/google/signin/authorization-url")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.state").exists());
        }

        @Test
        @DisplayName("should return authorization URL for Facebook signin")
        void shouldReturnAuthorizationUrlForFacebookSignin() throws Exception {
            logTestStep("Request Facebook OAuth signin authorization URL");

            performGet("/v1/auth/oauth/facebook/signin/authorization-url")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.state").exists());
        }

        @Test
        @DisplayName("should have different state for signup vs signin")
        void shouldHaveDifferentStateForSignupVsSignin() throws Exception {
            logTestStep("Compare state values for signup and signin");

            // Get signup state
            var signupResult = performGet("/v1/auth/oauth/google/signup/authorization-url")
                .andExpect(status().isOk())
                .andReturn();

            Map<?, ?> signupResponse = objectMapper.readValue(
                signupResult.getResponse().getContentAsString(),
                Map.class
            );
            String signupState = (String) signupResponse.get("state");

            // Get signin state
            var signinResult = performGet("/v1/auth/oauth/google/signin/authorization-url")
                .andExpect(status().isOk())
                .andReturn();

            Map<?, ?> signinResponse = objectMapper.readValue(
                signinResult.getResponse().getContentAsString(),
                Map.class
            );
            String signinState = (String) signinResponse.get("state");

            logAssertion("Signup and signin should have different state values");
            assert !signupState.equals(signinState) : "State values should be different for signup and signin";
        }
    }

    // ============================================
    // OAuth Redirect Endpoints
    // ============================================

    @Nested
    @DisplayName("GET /v1/auth/oauth/{provider}/signup/auth")
    class InitiateSignupOAuth {

        @Test
        @DisplayName("should redirect to Google OAuth for signup")
        void shouldRedirectToGoogleOAuthForSignup() throws Exception {
            logTestStep("Initiate Google OAuth signup redirect");

            performGet("/v1/auth/oauth/google/signup/auth")
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"));

            logAssertion("Returns 3xx redirect with Location header");
        }

        @Test
        @DisplayName("should redirect to Facebook OAuth for signup")
        void shouldRedirectToFacebookOAuthForSignup() throws Exception {
            logTestStep("Initiate Facebook OAuth signup redirect");

            performGet("/v1/auth/oauth/facebook/signup/auth")
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"));
        }
    }

    @Nested
    @DisplayName("GET /v1/auth/oauth/{provider}/signin/auth")
    class InitiateSigninOAuth {

        @Test
        @DisplayName("should redirect to Google OAuth for signin")
        void shouldRedirectToGoogleOAuthForSignin() throws Exception {
            logTestStep("Initiate Google OAuth signin redirect");

            performGet("/v1/auth/oauth/google/signin/auth")
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should redirect to Facebook OAuth for signin")
        void shouldRedirectToFacebookOAuthForSignin() throws Exception {
            logTestStep("Initiate Facebook OAuth signin redirect");

            performGet("/v1/auth/oauth/facebook/signin/auth")
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"));
        }
    }

    // ============================================
    // OAuth Callback Endpoints (POST - JSON)
    // ============================================

    @Nested
    @DisplayName("POST /v1/auth/oauth/{provider}/signup/callback")
    class HandleSignupCallbackJson {

        @Test
        @DisplayName("should handle missing code parameter")
        void shouldHandleMissingCodeParameter() throws Exception {
            logTestStep("Send signup callback without code parameter");

            Map<String, String> request = Map.of(
                "state", "test-state-123",
                "deviceId", "test-device"
            );

            performPost("/v1/auth/oauth/google/signup/callback", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());

            logAssertion("Returns error response for missing code");
        }

        @Test
        @DisplayName("should handle missing state parameter")
        void shouldHandleMissingStateParameter() throws Exception {
            logTestStep("Send signup callback without state parameter");

            Map<String, String> request = Map.of(
                "code", "test-auth-code",
                "deviceId", "test-device"
            );

            performPost("/v1/auth/oauth/google/signup/callback", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());

            logAssertion("Returns error response for missing state");
        }

        @Test
        @DisplayName("should handle invalid authorization code")
        void shouldHandleInvalidAuthorizationCode() throws Exception {
            logTestStep("Send signup callback with invalid code");

            Map<String, String> request = Map.of(
                "code", "invalid-code-12345",
                "state", "test-state-123",
                "deviceId", "test-device"
            );

            performPost("/v1/auth/oauth/google/signup/callback", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());

            logAssertion("Returns error response for invalid code");
        }

        @Test
        @DisplayName("should accept request without deviceId")
        void shouldAcceptRequestWithoutDeviceId() throws Exception {
            logTestStep("Send signup callback without deviceId");

            Map<String, String> request = Map.of(
                "code", "test-code",
                "state", "test-state"
            );

            // Should not throw validation error - deviceId is optional
            performPost("/v1/auth/oauth/google/signup/callback", request)
                .andExpect(status().isOk());

            logAssertion("Accepts request with optional deviceId omitted");
        }
    }

    @Nested
    @DisplayName("POST /v1/auth/oauth/{provider}/signin/callback")
    class HandleSigninCallbackJson {

        @Test
        @DisplayName("should handle invalid authorization code")
        void shouldHandleInvalidAuthorizationCode() throws Exception {
            logTestStep("Send signin callback with invalid code");

            Map<String, String> request = Map.of(
                "code", "invalid-code-12345",
                "state", "test-state-123",
                "deviceId", "test-device"
            );

            performPost("/v1/auth/oauth/facebook/signin/callback", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());

            logAssertion("Returns error response for invalid code");
        }

        @Test
        @DisplayName("should return success=false for failed authentication")
        void shouldReturnSuccessFalseForFailedAuth() throws Exception {
            logTestStep("Send signin callback with invalid credentials");

            Map<String, String> request = Map.of(
                "code", "invalid-code",
                "state", "invalid-state",
                "deviceId", "test-device"
            );

            performPost("/v1/auth/oauth/google/signin/callback", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

            logAssertion("Response includes success=false and error message");
        }
    }

    // ============================================
    // OAuth Callback Endpoints (GET - Redirects)
    // ============================================

    @Nested
    @DisplayName("GET /v1/auth/oauth/{provider}/signup/callback")
    class HandleSignupCallbackRedirect {

        @Test
        @DisplayName("should redirect to frontend with error for invalid code")
        void shouldRedirectToFrontendWithError() throws Exception {
            logTestStep("Request signup callback with invalid code");

            performGet("/v1/auth/oauth/google/signup/callback",
                Map.of("code", "invalid-code", "state", "test-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                    org.hamcrest.Matchers.containsString("error=")));

            logAssertion("Redirects to frontend with error parameter");
        }

        @Test
        @DisplayName("should require code parameter")
        void shouldRequireCodeParameter() throws Exception {
            logTestStep("Request signup callback without code parameter");

            performGet("/v1/auth/oauth/google/signup/callback",
                Map.of("state", "test-state"))
                .andExpect(status().is4xxClientError());

            logAssertion("Returns 4xx error for missing code parameter");
        }

        @Test
        @DisplayName("should require state parameter")
        void shouldRequireStateParameter() throws Exception {
            logTestStep("Request signup callback without state parameter");

            performGet("/v1/auth/oauth/google/signup/callback",
                Map.of("code", "test-code"))
                .andExpect(status().is4xxClientError());

            logAssertion("Returns 4xx error for missing state parameter");
        }
    }

    @Nested
    @DisplayName("GET /v1/auth/oauth/{provider}/signin/callback")
    class HandleSigninCallbackRedirect {

        @Test
        @DisplayName("should redirect to frontend with error for invalid code")
        void shouldRedirectToFrontendWithError() throws Exception {
            logTestStep("Request signin callback with invalid code");

            performGet("/v1/auth/oauth/facebook/signin/callback",
                Map.of("code", "invalid-code", "state", "test-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                    org.hamcrest.Matchers.containsString("error=")));

            logAssertion("Redirects to frontend with error parameter");
        }
    }

    // ============================================
    // Provider Support Tests
    // ============================================

    @Nested
    @DisplayName("Provider Support")
    class ProviderSupport {

        @Test
        @DisplayName("should support Google OAuth provider")
        void shouldSupportGoogle() throws Exception {
            logTestStep("Verify Google provider is supported");

            performGet("/v1/auth/oauth/google/signup/authorization-url")
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should support Facebook OAuth provider")
        void shouldSupportFacebook() throws Exception {
            logTestStep("Verify Facebook provider is supported");

            performGet("/v1/auth/oauth/facebook/signup/authorization-url")
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should reject unsupported provider names")
        void shouldRejectUnsupportedProviders() throws Exception {
            logTestStep("Test various unsupported provider names");

            String[] unsupportedProviders = {
                "twitter", "linkedin", "github", "microsoft",
                "invalid", "unknown", "test-provider"
            };

            for (String provider : unsupportedProviders) {
                performGet("/v1/auth/oauth/" + provider + "/signup/authorization-url")
                    .andExpect(status().is4xxClientError());
            }

            logAssertion("All unsupported providers return 4xx error");
        }
    }

    // ============================================
    // Request/Response Format Tests
    // ============================================

    @Nested
    @DisplayName("Request/Response Format")
    class RequestResponseFormat {

        @Test
        @DisplayName("authorization URL response should have required fields")
        void authorizationUrlResponseShouldHaveRequiredFields() throws Exception {
            logTestStep("Verify authorization URL response structure");

            performGet("/v1/auth/oauth/google/signup/authorization-url")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isString())
                .andExpect(jsonPath("$.state").isString())
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andExpect(jsonPath("$.state").isNotEmpty());

            logAssertion("Response has url and state as non-empty strings");
        }

        @Test
        @DisplayName("callback error response should have required fields")
        void callbackErrorResponseShouldHaveRequiredFields() throws Exception {
            logTestStep("Verify callback error response structure");

            Map<String, String> request = Map.of(
                "code", "invalid-code",
                "state", "test-state"
            );

            performPost("/v1/auth/oauth/google/signup/callback", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());

            logAssertion("Error response has success, error, and message fields");
        }

        @Test
        @DisplayName("should set Content-Type to application/json")
        void shouldSetContentTypeToJson() throws Exception {
            logTestStep("Verify Content-Type header");

            performGet("/v1/auth/oauth/google/signup/authorization-url")
                .andExpect(status().isOk())
                .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON));

            logAssertion("Response Content-Type is application/json");
        }
    }
}
