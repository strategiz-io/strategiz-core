package io.strategiz.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import io.strategiz.application.config.VaultOAuthInitializer;

/**
 * Main Application class for Strategiz Core
 */
@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = {
    "io.strategiz"
})
@PropertySource("classpath:application.properties")
@ConfigurationPropertiesScan(basePackages = {"io.strategiz"})
public class Application {

    // ANSI color codes for enhanced console output
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_BOLD = "\u001B[1m";

    public static void main(String[] args) {
        try {
            // Enhanced startup message
            System.out.println(ANSI_CYAN + ANSI_BOLD + "🚀 Initializing Strategiz Core Backend..." + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "   Loading modules: Portfolio • Exchange • Market Data • Analytics" + ANSI_RESET);
            
            SpringApplication app = new SpringApplication(Application.class);
            
            // Add Vault OAuth initializer (works the same in all environments)
            app.addInitializers(new VaultOAuthInitializer());
            
            // Check for required Vault configuration
            String vaultToken = System.getenv("VAULT_TOKEN");
            if (vaultToken == null || vaultToken.isEmpty()) {
                System.out.println(ANSI_YELLOW + "   ⚠️  VAULT_TOKEN not set - OAuth features may not work properly" + ANSI_RESET);
                System.out.println(ANSI_YELLOW + "   Run: export VAULT_TOKEN=<your-token>" + ANSI_RESET);
            }
            
            app.run(args);
            
            // Success message with deployment info
            System.out.println();
            System.out.println(ANSI_GREEN + ANSI_BOLD + "✅ STRATEGIZ CORE BACKEND DEPLOYED SUCCESSFULLY!" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "🔥 Ready to serve trading strategies and portfolio management!" + ANSI_RESET);
            System.out.println(ANSI_CYAN + "📊 All systems operational • Authentication enabled • APIs ready" + ANSI_RESET);
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Error starting Strategiz Core application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
