package io.strategiz.api.base.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for serving documentation-related endpoints
 */
@Controller
@RequestMapping("/api/docs")
public class DocumentationController extends BaseApiController {

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
