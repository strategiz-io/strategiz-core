package io.strategiz.framework.apidocs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for accessing API documentation
 */
@Controller
@RequestMapping("/api/docs")
@Tag(name = "API Documentation", description = "Access to API specifications and documentation")
public class ApiDocsController {

    @Value("${strategiz.api.docs.enabled:true}")
    private boolean apiDocsEnabled;

    /**
     * Redirect to the Swagger UI
     * 
     * @return Redirect to the Swagger UI
     */
    @Operation(summary = "View API documentation", 
              description = "Redirects to the Swagger UI with complete API documentation")
    @GetMapping("/api")
    public RedirectView viewApiDocumentation() {
        return new RedirectView("/swagger-ui/index.html");
    }
    
    /**
     * Redirect to the OpenAPI specification
     * 
     * @return Redirect to the OpenAPI specification JSON
     */
    @Operation(summary = "Download OpenAPI specification", 
              description = "Redirects to the raw OpenAPI specification in JSON format")
    @GetMapping("/openapi")
    public RedirectView viewOpenApiSpec() {
        return new RedirectView("/v3/api-docs");
    }
    
    /**
     * Redirect to a specific API group's documentation
     * 
     * @return Redirect to the specific group documentation
     */
    @Operation(summary = "View specific API group documentation", 
              description = "Redirects to documentation for a specific API group")
    @GetMapping("/group/auth")
    public RedirectView viewAuthApiDocs() {
        return new RedirectView("/swagger-ui/index.html?urls.primaryName=Auth%20API");
    }
    
    /**
     * Redirect to the portfolio API group documentation
     * 
     * @return Redirect to the portfolio group documentation
     */
    @Operation(summary = "View portfolio API documentation", 
              description = "Redirects to documentation for portfolio-related APIs")
    @GetMapping("/group/portfolio")
    public RedirectView viewPortfolioApiDocs() {
        return new RedirectView("/swagger-ui/index.html?urls.primaryName=Portfolio%20API");
    }
    
    /**
     * Redirect to the strategy API group documentation
     * 
     * @return Redirect to the strategy group documentation
     */
    @Operation(summary = "View strategy API documentation", 
              description = "Redirects to documentation for strategy-related APIs")
    @GetMapping("/group/strategy")
    public RedirectView viewStrategyApiDocs() {
        return new RedirectView("/swagger-ui/index.html?urls.primaryName=Strategy%20API");
    }
}
