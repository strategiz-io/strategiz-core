package io.strategiz.service.auth.controller.kraken;

import io.strategiz.client.kraken.auth.model.KrakenApiCredentials;
import io.strategiz.client.kraken.auth.service.KrakenCredentialService;
import io.strategiz.client.kraken.auth.service.KrakenPortfolioService;
import io.strategiz.service.auth.model.kraken.KrakenApiKeyRequest;
import io.strategiz.service.auth.model.kraken.KrakenApiKeyResponse;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing Kraken API key authentication
 */
@RestController
@RequestMapping("/v1/auth/kraken")
@Tag(name = "Kraken Authentication", description = "Endpoints for Kraken API key management")
@SecurityRequirement(name = "bearerAuth")
public class KrakenAuthController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(KrakenAuthController.class);

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private final KrakenCredentialService credentialService;
    private final KrakenPortfolioService portfolioService;

    public KrakenAuthController(KrakenCredentialService credentialService, KrakenPortfolioService portfolioService) {
        this.credentialService = credentialService;
        this.portfolioService = portfolioService;
    }

    @Operation(summary = "Store Kraken API credentials", 
               description = "Store user's Kraken API key and secret for portfolio access")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Credentials stored successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials or connection test failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/credentials")
    public ResponseEntity<KrakenApiKeyResponse> storeCredentials(
            @Valid @RequestBody KrakenApiKeyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Storing Kraken credentials for user: {}", userDetails.getUsername());
        
        try {
            // Build credentials object
            KrakenApiCredentials credentials = new KrakenApiCredentials(
                    request.getApiKey(),
                    request.getApiSecret(), 
                    request.getOtp(),
                    userDetails.getUsername()
            );
            
            // Test connection first
            boolean connectionSuccess = portfolioService.testConnection(credentials);
            if (!connectionSuccess) {
                return ResponseEntity
                        .badRequest()
                        .body(KrakenApiKeyResponse.builder()
                                .success(false)
                                .message("Failed to connect to Kraken API. Please verify your credentials.")
                                .build());
            }
            
            // Store credentials if test passed
            boolean storeSuccess = credentialService.storeCredentials(credentials);
            if (storeSuccess) {
                return ResponseEntity.ok(
                        KrakenApiKeyResponse.builder()
                                .success(true)
                                .message("Kraken credentials stored successfully")
                                .build()
                );
            } else {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(KrakenApiKeyResponse.builder()
                                .success(false)
                                .message("Failed to store credentials")
                                .build());
            }
            
        } catch (Exception e) {
            log.error("Error storing Kraken credentials for user: {}", userDetails.getUsername(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(KrakenApiKeyResponse.builder()
                            .success(false)
                            .message("Failed to store credentials")
                            .build());
        }
    }

    @Operation(summary = "Test Kraken API connection", 
               description = "Test if stored Kraken credentials are valid")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connection test result"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "No credentials found")
    })
    @GetMapping("/test-connection")
    public ResponseEntity<KrakenApiKeyResponse> testConnection(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Testing Kraken connection for user: {}", userDetails.getUsername());
        
        try {
            KrakenApiCredentials credentials = credentialService.getCredentials(userDetails.getUsername());
            if (credentials == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(KrakenApiKeyResponse.builder()
                                .success(false)
                                .message("No Kraken credentials found")
                                .build());
            }
            
            boolean success = portfolioService.testConnection(credentials);
            return ResponseEntity.ok(
                    KrakenApiKeyResponse.builder()
                            .success(success)
                            .message(success ? "Connection successful" : "Connection failed")
                            .build()
            );
            
        } catch (Exception e) {
            log.error("Error testing Kraken connection for user: {}", userDetails.getUsername(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(KrakenApiKeyResponse.builder()
                            .success(false)
                            .message("Failed to test connection")
                            .build());
        }
    }

    @Operation(summary = "Get Kraken portfolio", 
               description = "Fetch user's portfolio data from Kraken")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Portfolio data retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "No credentials found"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch portfolio")
    })
    @GetMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> getPortfolio(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Fetching Kraken portfolio for user: {}", userDetails.getUsername());
        
        try {
            Map<String, Object> portfolio = portfolioService.getUserPortfolio(userDetails.getUsername());
            if (portfolio == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(portfolio);
            
        } catch (Exception e) {
            log.error("Error fetching Kraken portfolio for user: {}", userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Update Kraken OTP", 
               description = "Update the OTP for Kraken 2FA if enabled")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "No credentials found")
    })
    @PutMapping("/otp")
    public ResponseEntity<KrakenApiKeyResponse> updateOtp(
            @RequestParam String otp,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Updating Kraken OTP for user: {}", userDetails.getUsername());
        
        try {
            boolean success = credentialService.updateOtp(userDetails.getUsername(), otp);
            if (success) {
                return ResponseEntity.ok(
                        KrakenApiKeyResponse.builder()
                                .success(true)
                                .message("OTP updated successfully")
                                .build()
                );
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(KrakenApiKeyResponse.builder()
                                .success(false)
                                .message("No credentials found")
                                .build());
            }
            
        } catch (Exception e) {
            log.error("Error updating Kraken OTP for user: {}", userDetails.getUsername(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(KrakenApiKeyResponse.builder()
                            .success(false)
                            .message("Failed to update OTP")
                            .build());
        }
    }

    @Operation(summary = "Delete Kraken credentials", 
               description = "Remove stored Kraken API credentials")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Credentials deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/credentials")
    public ResponseEntity<Void> deleteCredentials(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Deleting Kraken credentials for user: {}", userDetails.getUsername());
        
        try {
            credentialService.deleteCredentials(userDetails.getUsername());
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Error deleting Kraken credentials for user: {}", userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}