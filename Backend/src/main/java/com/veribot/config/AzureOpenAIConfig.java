package com.veribot.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Provides configuration for Azure OpenAI services.
 */
public class AzureOpenAIConfig {
    private final String endpoint;
    private final String apiKey;
    private final String deploymentName;
    private final String apiVersion;

    /**
     * Creates a new AzureOpenAIConfig from environment variables.
     */
    public AzureOpenAIConfig() {
        Dotenv dotenv = Dotenv.load();
        this.endpoint = dotenv.get("AZURE_OPENAI_ENDPOINT");
        this.apiKey = dotenv.get("AZURE_OPENAI_API_KEY");
        this.deploymentName = dotenv.get("AZURE_OPENAI_DEPLOYMENT_NAME");
        this.apiVersion = dotenv.get("AZURE_OPENAI_API_VERSION");
        
        validateConfig();
    }

    /**
     * Validates that all required configuration is present.
     */
    private void validateConfig() {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalStateException("AZURE_OPENAI_ENDPOINT environment variable is required");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("AZURE_OPENAI_API_KEY environment variable is required");
        }
        if (deploymentName == null || deploymentName.isEmpty()) {
            throw new IllegalStateException("AZURE_OPENAI_DEPLOYMENT_NAME environment variable is required");
        }
        if (apiVersion == null || apiVersion.isEmpty()) {
            throw new IllegalStateException("AZURE_OPENAI_API_VERSION environment variable is required");
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getApiVersion() {
        return apiVersion;
    }
}