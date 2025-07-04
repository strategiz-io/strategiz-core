package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.auth.config.AuthOAuthConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugOAuthController {

    @Autowired
    private AuthOAuthConfig authOAuthConfig;

    @GetMapping("/oauth-config")
    public Map<String, Object> getOAuthConfig() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("providers", authOAuthConfig.getProviders());
        response.put("google", authOAuthConfig.getGoogle());
        response.put("facebook", authOAuthConfig.getFacebook());
        
        if (authOAuthConfig.getGoogle() != null) {
            Map<String, String> googleConfig = new HashMap<>();
            googleConfig.put("clientId", authOAuthConfig.getGoogle().getClientId());
            googleConfig.put("clientSecret", authOAuthConfig.getGoogle().getClientSecret() != null ? "***" : "null");
            googleConfig.put("redirectUri", authOAuthConfig.getGoogle().getRedirectUri());
            googleConfig.put("authUrl", authOAuthConfig.getGoogle().getAuthUrl());
            googleConfig.put("tokenUrl", authOAuthConfig.getGoogle().getTokenUrl());
            googleConfig.put("userInfoUrl", authOAuthConfig.getGoogle().getUserInfoUrl());
            googleConfig.put("scope", authOAuthConfig.getGoogle().getScope());
            response.put("googleDetails", googleConfig);
        }
        
        return response;
    }
} 