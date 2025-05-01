package com.veribot.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Provides configuration for Azure Bing Search services.
 */
public class AzureBingSearchConfig {
    private final String apiKey;
    private final String endpoint;

    /**
     * Creates a new AzureBingSearchConfig from environment variables.
     */
    public AzureBingSearchConfig() {
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("AZURE_BING_SEARCH_API_KEY");
        this.endpoint = dotenv.get("AZURE_BING_SEARCH_ENDPOINT");
        
        validateConfig();
    }

    /**
     * Validates that all required configuration is present.
     */
    private void validateConfig() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("AZURE_BING_SEARCH_API_KEY environment variable is required");
        }
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalStateException("AZURE_BING_SEARCH_ENDPOINT environment variable is required");
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }
}