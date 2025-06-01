package io.strategiz.application.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for serving documentation-related endpoints
 */
@Controller
@RequestMapping("/api/docs")
@Tag(name = "Documentation", description = "Access to project documentation and API specifications")
public class DocumentationController {

    /**
     * Redirect to the architecture diagram HTML page
     * 
     * @return Redirect to the HTML page
     */
    @Operation(summary = "View architecture diagram", 
              description = "Redirects to the interactive HTML architecture diagram showing the system design")
    @GetMapping("/architecture")
    public String viewArchitectureDiagram() {
        return "redirect:/docs/strategiz_architecture_diagram.html";
    }
    
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
}
