package com.veribot.config;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Provides configuration for SerpApiConfig.
 */
public class SerpApiConfig {
	private final String apiKey;
	/**
     * Creates a new AzureBingSearchConfig from environment variables.
     */
    public SerpApiConfig() {
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("SERPAPI_KEY");
        
        validateConfig();
    }
    
    /**
     * Validates that all required configuration is present.
     */
    private void validateConfig() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("SERPAPI_KEY environment variable is required");
        }
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    
    
}
