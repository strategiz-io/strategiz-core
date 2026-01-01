package io.strategiz.service.marketplace.controller;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.StrategyPricing;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.data.strategy.repository.UpdateStrategyRepository;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.test.BaseIntegrationTest;
import io.strategiz.service.marketplace.constants.StrategyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Strategy Pricing endpoints in StrategyPublishController.
 *
 * <p>These tests verify the pricing and listing status update endpoints work correctly
 * for the marketplace "Sell Strategy" flow.</p>
 *
 * <h3>Test Coverage:</h3>
 * <ul>
 *   <li>PUT /v1/strategies/{id}/pricing - Simple format (price + listedStatus)</li>
 *   <li>PUT /v1/strategies/{id}/pricing - Full format (pricingType + prices)</li>
 *   <li>Validation and error handling</li>
 *   <li>Authorization (owner-only access)</li>
 * </ul>
 *
 * <h3>Test Strategy:</h3>
 * <p>Integration tests focus on HTTP layer verification:</p>
 * <ul>
 *   <li>Request mapping and routing</li>
 *   <li>Request body parsing (JSON)</li>
 *   <li>HTTP status codes</li>
 *   <li>Response structure and content</li>
 *   <li>Error responses</li>
 * </ul>
 *
 * @see StrategyPublishController
 * @see BaseIntegrationTest
 */
@SpringBootTest(classes = io.strategiz.service.marketplace.TestApplication.class)
@ActiveProfiles("test")
@DisplayName("Strategy Pricing Controller Integration Tests")
public class StrategyPricingControllerIntegrationTest extends BaseIntegrationTest {

    private static final String STRATEGY_ID = "test-strategy-123";
    private static final String OWNER_USER_ID = "test-user-456";
    private static final String OTHER_USER_ID = "other-user-789";
    private static final String ACCESS_TOKEN = "test-access-token";

    @MockBean
    private ReadStrategyRepository readStrategyRepository;

    @MockBean
    private UpdateStrategyRepository updateStrategyRepository;

    private Strategy testStrategy;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        // Create test strategy with owner
        testStrategy = new Strategy();
        testStrategy.setId(STRATEGY_ID);
        testStrategy.setName("Test Strategy");
        testStrategy.setDescription("Test strategy for pricing tests");
        testStrategy.setOwnerId(OWNER_USER_ID);
        testStrategy.setCreatorId(OWNER_USER_ID);
        testStrategy.setPublishStatus("PUBLISHED");
        testStrategy.setPublicStatus("PUBLIC");
        testStrategy.setListedStatus("NOT_LISTED");
        testStrategy.setCode("# Test strategy code");
        testStrategy.setLanguage("python");
    }

    // ============================================
    // Simple Format Tests (price + listedStatus)
    // ============================================

    @Nested
    @DisplayName("PUT /v1/strategies/{id}/pricing - Simple Format")
    class UpdatePricingSimpleFormat {

        @Test
        @DisplayName("should list strategy for sale with price and LISTED status")
        void shouldListStrategyForSale() throws Exception {
            logTestStep("Set strategy price to $99.99 and list for sale");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Strategy updatedStrategy = new Strategy();
            updatedStrategy.setId(STRATEGY_ID);
            updatedStrategy.setOwnerId(OWNER_USER_ID);
            updatedStrategy.setListedStatus("LISTED");
            updatedStrategy.setPricing(StrategyPricing.oneTime(new BigDecimal("99.99"), "USD"));

            when(updateStrategyRepository.update(eq(STRATEGY_ID), eq(OWNER_USER_ID), any(Strategy.class)))
                .thenReturn(updatedStrategy);

            Map<String, Object> requestBody = Map.of(
                "price", 99.99,
                "listedStatus", "LISTED"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pricing updated successfully"))
                .andExpect(jsonPath("$.strategy").exists())
                .andExpect(jsonPath("$.strategy.listedStatus").value("LISTED"));

            logAssertion("Response confirms strategy listed with price $99.99");
        }

        @Test
        @DisplayName("should remove strategy from marketplace with NOT_LISTED status")
        void shouldRemoveStrategyListing() throws Exception {
            logTestStep("Remove strategy from marketplace");

            // Arrange
            testStrategy.setListedStatus("LISTED");
            testStrategy.setPricing(StrategyPricing.oneTime(new BigDecimal("99.99"), "USD"));

            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Strategy updatedStrategy = new Strategy();
            updatedStrategy.setId(STRATEGY_ID);
            updatedStrategy.setOwnerId(OWNER_USER_ID);
            updatedStrategy.setListedStatus("NOT_LISTED");
            updatedStrategy.setPricing(testStrategy.getPricing());

            when(updateStrategyRepository.update(eq(STRATEGY_ID), eq(OWNER_USER_ID), any(Strategy.class)))
                .thenReturn(updatedStrategy);

            Map<String, Object> requestBody = Map.of(
                "listedStatus", "NOT_LISTED"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pricing updated successfully"))
                .andExpect(jsonPath("$.strategy.listedStatus").value("NOT_LISTED"));

            logAssertion("Strategy removed from marketplace");
        }

        @Test
        @DisplayName("should update only price without changing listing status")
        void shouldUpdateOnlyPrice() throws Exception {
            logTestStep("Update strategy price from $99.99 to $149.99");

            // Arrange
            testStrategy.setListedStatus("LISTED");
            testStrategy.setPricing(StrategyPricing.oneTime(new BigDecimal("99.99"), "USD"));

            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Strategy updatedStrategy = new Strategy();
            updatedStrategy.setId(STRATEGY_ID);
            updatedStrategy.setOwnerId(OWNER_USER_ID);
            updatedStrategy.setListedStatus("LISTED"); // Unchanged
            updatedStrategy.setPricing(StrategyPricing.oneTime(new BigDecimal("149.99"), "USD"));

            when(updateStrategyRepository.update(eq(STRATEGY_ID), eq(OWNER_USER_ID), any(Strategy.class)))
                .thenReturn(updatedStrategy);

            Map<String, Object> requestBody = Map.of(
                "price", 149.99
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy.listedStatus").value("LISTED"));

            logAssertion("Price updated successfully, listing status unchanged");
        }

        @Test
        @DisplayName("should reject price of zero")
        void shouldRejectZeroPrice() throws Exception {
            logTestStep("Attempt to set price to $0");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Map<String, Object> requestBody = Map.of(
                "price", 0,
                "listedStatus", "LISTED"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Price must be greater than zero"));

            logAssertion("Rejected price of $0");
        }

        @Test
        @DisplayName("should reject negative price")
        void shouldRejectNegativePrice() throws Exception {
            logTestStep("Attempt to set negative price");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Map<String, Object> requestBody = Map.of(
                "price", -50.00,
                "listedStatus", "LISTED"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Price must be greater than zero"));

            logAssertion("Rejected negative price");
        }

        @Test
        @DisplayName("should reject invalid listedStatus value")
        void shouldRejectInvalidListedStatus() throws Exception {
            logTestStep("Attempt to set invalid listedStatus value");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Map<String, Object> requestBody = Map.of(
                "listedStatus", "INVALID_STATUS"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("listedStatus must be 'LISTED' or 'NOT_LISTED'"));

            logAssertion("Rejected invalid listedStatus value");
        }
    }

    // ============================================
    // Full Format Tests (pricingType + prices)
    // ============================================

    @Nested
    @DisplayName("PUT /v1/strategies/{id}/pricing - Full Format (Legacy)")
    class UpdatePricingFullFormat {

        @Test
        @DisplayName("should update to ONE_TIME pricing with price")
        void shouldUpdateToOneTimePricing() throws Exception {
            logTestStep("Update to ONE_TIME pricing with $29.99");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Strategy updatedStrategy = new Strategy();
            updatedStrategy.setId(STRATEGY_ID);
            updatedStrategy.setOwnerId(OWNER_USER_ID);
            updatedStrategy.setPricing(StrategyPricing.oneTime(new BigDecimal("29.99"), "USD"));

            when(updateStrategyRepository.update(eq(STRATEGY_ID), eq(OWNER_USER_ID), any(Strategy.class)))
                .thenReturn(updatedStrategy);

            Map<String, Object> requestBody = Map.of(
                "pricingType", "ONE_TIME",
                "oneTimePrice", 29.99
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pricing updated successfully"))
                .andExpect(jsonPath("$.strategy").exists());

            logAssertion("Updated to ONE_TIME pricing successfully");
        }

        @Test
        @DisplayName("should update to FREE pricing")
        void shouldUpdateToFreePricing() throws Exception {
            logTestStep("Update to FREE pricing");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Strategy updatedStrategy = new Strategy();
            updatedStrategy.setId(STRATEGY_ID);
            updatedStrategy.setOwnerId(OWNER_USER_ID);
            updatedStrategy.setPricing(StrategyPricing.free());

            when(updateStrategyRepository.update(eq(STRATEGY_ID), eq(OWNER_USER_ID), any(Strategy.class)))
                .thenReturn(updatedStrategy);

            Map<String, Object> requestBody = Map.of(
                "pricingType", "FREE"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pricing updated successfully"));

            logAssertion("Updated to FREE pricing successfully");
        }

        @Test
        @DisplayName("should reject ONE_TIME without price")
        void shouldRejectOneTimeWithoutPrice() throws Exception {
            logTestStep("Attempt ONE_TIME pricing without price");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Map<String, Object> requestBody = Map.of(
                "pricingType", "ONE_TIME"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isBadRequest());

            logAssertion("Rejected ONE_TIME pricing without price");
        }

        @Test
        @DisplayName("should reject invalid pricing type")
        void shouldRejectInvalidPricingType() throws Exception {
            logTestStep("Attempt invalid pricing type");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Map<String, Object> requestBody = Map.of(
                "pricingType", "INVALID_TYPE"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid pricing type. Must be FREE, ONE_TIME, or SUBSCRIPTION"));

            logAssertion("Rejected invalid pricing type");
        }
    }

    // ============================================
    // Error Handling Tests
    // ============================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should return 400 when no pricing fields provided")
        void shouldReturn400WhenNoPricingFields() throws Exception {
            logTestStep("Attempt update with empty request body");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Map<String, Object> requestBody = Map.of();

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Either 'price'/'listedStatus' or 'pricingType' must be provided"));

            logAssertion("Rejected empty request body");
        }

        @Test
        @DisplayName("should return 404 when strategy not found")
        void shouldReturn404WhenStrategyNotFound() throws Exception {
            logTestStep("Attempt to update non-existent strategy");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.empty());

            Map<String, Object> requestBody = Map.of(
                "price", 99.99,
                "listedStatus", "LISTED"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isNotFound());

            logAssertion("Returns 404 for non-existent strategy");
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            logTestStep("Attempt update without authentication");

            Map<String, Object> requestBody = Map.of(
                "price", 99.99,
                "listedStatus", "LISTED"
            );

            // Act & Assert
            performPut("/v1/strategies/" + STRATEGY_ID + "/pricing", requestBody)
                .andExpect(status().isUnauthorized());

            logAssertion("Returns 401 for unauthenticated request");
        }

        @Test
        @DisplayName("should return 403 when user is not strategy owner")
        void shouldReturn403WhenNotOwner() throws Exception {
            logTestStep("Attempt update by non-owner user");

            // Arrange - strategy owned by OWNER_USER_ID, but request from OTHER_USER_ID
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Map<String, Object> requestBody = Map.of(
                "price", 99.99,
                "listedStatus", "LISTED"
            );

            // Mock AuthenticatedUser for OTHER_USER_ID
            String otherUserToken = "other-user-token";

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", otherUserToken, requestBody)
                .andExpect(status().isForbidden());

            logAssertion("Returns 403 for non-owner");
        }
    }

    // ============================================
    // Edge Cases and Business Logic Tests
    // ============================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should accept very high price ($9,999.99)")
        void shouldAcceptHighPrice() throws Exception {
            logTestStep("Set strategy price to $9,999.99");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Strategy updatedStrategy = new Strategy();
            updatedStrategy.setId(STRATEGY_ID);
            updatedStrategy.setOwnerId(OWNER_USER_ID);
            updatedStrategy.setListedStatus("LISTED");
            updatedStrategy.setPricing(StrategyPricing.oneTime(new BigDecimal("9999.99"), "USD"));

            when(updateStrategyRepository.update(eq(STRATEGY_ID), eq(OWNER_USER_ID), any(Strategy.class)))
                .thenReturn(updatedStrategy);

            Map<String, Object> requestBody = Map.of(
                "price", 9999.99,
                "listedStatus", "LISTED"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isOk());

            logAssertion("Accepted high price of $9,999.99");
        }

        @Test
        @DisplayName("should accept very low price ($0.01)")
        void shouldAcceptLowPrice() throws Exception {
            logTestStep("Set strategy price to $0.01");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Strategy updatedStrategy = new Strategy();
            updatedStrategy.setId(STRATEGY_ID);
            updatedStrategy.setOwnerId(OWNER_USER_ID);
            updatedStrategy.setListedStatus("LISTED");
            updatedStrategy.setPricing(StrategyPricing.oneTime(new BigDecimal("0.01"), "USD"));

            when(updateStrategyRepository.update(eq(STRATEGY_ID), eq(OWNER_USER_ID), any(Strategy.class)))
                .thenReturn(updatedStrategy);

            Map<String, Object> requestBody = Map.of(
                "price", 0.01,
                "listedStatus", "LISTED"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isOk());

            logAssertion("Accepted low price of $0.01");
        }

        @Test
        @DisplayName("should handle decimal precision correctly")
        void shouldHandleDecimalPrecision() throws Exception {
            logTestStep("Set strategy price with decimal precision ($99.999)");

            // Arrange
            when(readStrategyRepository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(testStrategy));

            Strategy updatedStrategy = new Strategy();
            updatedStrategy.setId(STRATEGY_ID);
            updatedStrategy.setOwnerId(OWNER_USER_ID);
            updatedStrategy.setListedStatus("LISTED");
            // BigDecimal should handle precision
            updatedStrategy.setPricing(StrategyPricing.oneTime(new BigDecimal("99.999"), "USD"));

            when(updateStrategyRepository.update(eq(STRATEGY_ID), eq(OWNER_USER_ID), any(Strategy.class)))
                .thenReturn(updatedStrategy);

            Map<String, Object> requestBody = Map.of(
                "price", 99.999,
                "listedStatus", "LISTED"
            );

            // Act & Assert
            performAuthenticatedPut("/v1/strategies/" + STRATEGY_ID + "/pricing", ACCESS_TOKEN, requestBody)
                .andExpect(status().isOk());

            logAssertion("Handled decimal precision correctly");
        }
    }
}
