package io.strategiz.service.auth.controller.provider;

import io.strategiz.data.auth.model.provider.CreateProviderIntegrationRequest;
import io.strategiz.service.auth.model.provider.CreateProviderIntegrationResponse;
import io.strategiz.service.auth.service.provider.CreateProviderService;
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

/**
 * Controller for creating provider integrations during signup flow
 * Handles provider connection for exchanges and brokerages in step 3 of signup
 */
@RestController("authCreateProviderController")
@RequestMapping("/v1/users/provider-integrations")
@Tag(name = "Provider Integrations", description = "Create provider integrations during signup")
@SecurityRequirement(name = "bearerAuth")
public class CreateProviderController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(CreateProviderController.class);

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private final CreateProviderService createProviderService;

    public CreateProviderController(CreateProviderService createProviderService) {
        this.createProviderService = createProviderService;
    }

    @Operation(summary = "Create provider integration", 
               description = "Create a new provider integration during signup step 3")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Provider integration created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or connection failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "409", description = "Provider already integrated"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<CreateProviderIntegrationResponse> createProviderIntegration(
            @Valid @RequestBody CreateProviderIntegrationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userId = userDetails.getUsername();
        logRequest("createProviderIntegration", userId, request.getProviderId());
        
        try {
            // Validate required parameters
            validateRequiredParam("providerId", request.getProviderId());
            validateRequiredParam("credentials", request.getCredentials());
            validateRequiredParam("apiKey", request.getCredentials().getApiKey());
            validateRequiredParam("apiSecret", request.getCredentials().getApiSecret());
            
            // Create the provider integration
            CreateProviderIntegrationResponse response = createProviderService.createProviderIntegration(request, userId);
            
            if (response.isSuccess()) {
                logRequestSuccess("createProviderIntegration", userId, response, 
                    "Provider: " + request.getProviderId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                log.warn("Provider integration failed for user: {}, provider: {}, message: {}", 
                    userId, request.getProviderId(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid provider integration request for user: {}, provider: {}, error: {}", 
                userId, request.getProviderId(), e.getMessage());
            
            CreateProviderIntegrationResponse errorResponse = CreateProviderIntegrationResponse.builder()
                .success(false)
                .providerId(request.getProviderId())
                .status("failed")
                .message(e.getMessage())
                .build();
                
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("Error creating provider integration for user: {}, provider: {}", 
                userId, request.getProviderId(), e);
            
            CreateProviderIntegrationResponse errorResponse = CreateProviderIntegrationResponse.builder()
                .success(false)
                .providerId(request.getProviderId())
                .status("error")
                .message("Failed to create provider integration")
                .build();
                
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(summary = "Test provider connection", 
               description = "Test provider credentials without creating integration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connection test completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/test-connection")
    public ResponseEntity<CreateProviderIntegrationResponse> testProviderConnection(
            @Valid @RequestBody CreateProviderIntegrationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userId = userDetails.getUsername();
        logRequest("testProviderConnection", userId, request.getProviderId());
        
        try {
            // Validate required parameters
            validateRequiredParam("providerId", request.getProviderId());
            validateRequiredParam("credentials", request.getCredentials());
            
            // Test the connection
            CreateProviderIntegrationResponse response = createProviderService.testProviderConnection(request, userId);
            
            logRequestSuccess("testProviderConnection", userId, response,
                "Provider: " + request.getProviderId() + ", Success: " + response.isSuccess());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid provider test request for user: {}, provider: {}, error: {}", 
                userId, request.getProviderId(), e.getMessage());
            
            CreateProviderIntegrationResponse errorResponse = CreateProviderIntegrationResponse.builder()
                .success(false)
                .providerId(request.getProviderId())
                .status("test_failed")
                .message(e.getMessage())
                .build();
                
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("Error testing provider connection for user: {}, provider: {}", 
                userId, request.getProviderId(), e);
            
            CreateProviderIntegrationResponse errorResponse = CreateProviderIntegrationResponse.builder()
                .success(false)
                .providerId(request.getProviderId())
                .status("test_error")
                .message("Connection test failed")
                .build();
                
            return ResponseEntity.ok(errorResponse);
        }
    }
}