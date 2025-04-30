package com.veribot.model;

import java.time.LocalDateTime;

import com.veribot.config.AzureOpenAIConfig;
import com.veribot.config.SerpApiConfig;
import com.veribot.service.NewsSearchService;
import com.veribot.service.NewsVerificationService;

public class UserContext {
	private String sessionId;
    private LocalDateTime lastAccessed;
    private NewsSearchService searchService;
    private NewsVerificationService verificationService;
    private AzureOpenAIConfig openAIConfig;
    private SerpApiConfig serpApiConfig;
    
    public UserContext(String sessionId) {
        this.sessionId = sessionId;
        this.lastAccessed = LocalDateTime.now();
        try {
            // Initialize configurations
            this.openAIConfig = new AzureOpenAIConfig();
            this.serpApiConfig = new SerpApiConfig();
            // Initialize services
            this.searchService = new NewsSearchService(serpApiConfig);
            this.verificationService = new NewsVerificationService(openAIConfig, searchService);
            
        } catch (Exception e) {
            System.err.println("Failed to start VeriBot: " + e.getMessage());
            
            e.printStackTrace(); // << MOSTRÁ EL STACKTRACE
            
            if (e instanceof IllegalStateException && e.getMessage().contains("environment variable")) {
                System.err.println("\nPlease ensure you have set up the required environment variables.");
                System.err.println("Create a .env file in the project root with the following variables:");
                System.err.println("AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_API_KEY, AZURE_OPENAI_DEPLOYMENT_NAME, AZURE_OPENAI_API_VERSION");
            }
        }
    }
    
    public void updateLastAccessed() {
        this.lastAccessed = LocalDateTime.now();
    }
    
    public LocalDateTime getLastAccessed() {
        return lastAccessed;
    }
    
    
    public String getSessionId() {
        return sessionId;
    }

	public NewsSearchService getSearchService() {
		return searchService;
	}

	public NewsVerificationService getVerificationService() {
		return verificationService;
	}

	public AzureOpenAIConfig getOpenAIConfig() {
		return openAIConfig;
	}

	public SerpApiConfig getSerpApiConfig() {
		return serpApiConfig;
	}
    
}
