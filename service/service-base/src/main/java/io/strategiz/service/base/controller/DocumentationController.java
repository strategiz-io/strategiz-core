package io.strategiz.service.base.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for serving documentation-related endpoints.
 * Uses clean architecture - no wrapper dependencies.
 */
@Controller("serviceDocumentationController")
@RequestMapping("/api/docs")
public class DocumentationController {
    // Using specific bean name to avoid conflict with api-base DocumentationController

    /**
     * Redirect to the architecture diagram HTML page
     * 
     * @return Redirect to the HTML page
     */
    @GetMapping("/architecture")
    public String viewArchitectureDiagram() {
        return "redirect:/docs/strategiz_architecture_diagram.html";
    }

}
