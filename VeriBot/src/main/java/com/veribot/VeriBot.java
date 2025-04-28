package com.veribot;

import com.veribot.config.AzureOpenAIConfig;
import com.veribot.config.SerpApiConfig;
import com.veribot.model.ConversationState;
import com.veribot.model.NewsVerificationResult;
import com.veribot.service.NewsSearchService;
import com.veribot.service.NewsVerificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Main entry point for the VeriBot news verification and summarization agent.
 */
public class VeriBot {
    private static final Logger logger = LoggerFactory.getLogger(VeriBot.class);
    private AzureOpenAIConfig openAIConfig;
    private SerpApiConfig serpApiConfig;
    private NewsSearchService searchService;
    private NewsVerificationService verificationService;

    public VeriBot () {
        logger.info("Starting VeriBot News Verification Agent");

        try {
            // Initialize configurations
            this.openAIConfig = new AzureOpenAIConfig();
            this.serpApiConfig = new SerpApiConfig();
            // Initialize services
            this.searchService = new NewsSearchService(serpApiConfig);
            this.verificationService = new NewsVerificationService(openAIConfig, searchService);
            
        } catch (Exception e) {
            logger.error("Error initializing VeriBot: {}", e.getMessage(), e);
            System.err.println("Failed to start VeriBot: " + e.getMessage());
            
            if (e instanceof IllegalStateException && e.getMessage().contains("environment variable")) {
                System.err.println("\nPlease ensure you have set up the required environment variables.");
                System.err.println("Create a .env file in the project root with the following variables:");
                System.err.println("AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_API_KEY, AZURE_OPENAI_DEPLOYMENT_NAME, AZURE_OPENAI_API_VERSION");
            }
        }
    }
    //             runInteractiveMode(verificationService);


    /**
     * Runs the interactive command-line interface for the VeriBot agent.
     *
     * @param verificationService the service to use for verifying news
     */
    public String run(String query) {
            try {
                System.out.println("\nProcessing your query... Please wait.");
                NewsVerificationResult result = this.verificationService.verifyNews(query);
                if(verificationService.getConversationSession().getState()==ConversationState.LOOKING_FOR_NEW_EVENT)
                    return GetVerificationResult(result, verificationService.getConversationSession().getState());
                else
                    return result.summary();
            } catch (Exception e) {
                logger.error("Error processing query: {}", e.getMessage(), e);
                return("Error processing your query: " + e.getMessage());

            }
    }
    private static String GetVerificationResult(NewsVerificationResult result, ConversationState conversationState) {
        String answer = "";
            answer.concat("VERIFICATION RESULTS");
            answer.concat("\nSUMMARY:");
            answer.concat(result.summary());
            answer.concat("\nTRUTHFULNESS: " + result.truthfulnessPercentage() + "% - " + result.getTruthfulnessLevel());
            answer.concat("\nJUSTIFICATION:");
            answer.concat(result.justification());
            
            if (!result.sourcesUsed().isEmpty()) {
                answer.concat("\nSOURCES USED:");
                for (String source : result.sourcesUsed()) {
                    answer.concat("- " + source);
                }
            }
        return answer;
    }
    /**
     * Prints a welcome message with instructions for using VeriBot.
     */
    /* 
    private static void printWelcomeMessage() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  Welcome to VeriBot - News Verification and Summarization Agent");
        System.out.println("=".repeat(80));
        System.out.println("\nVeriBot helps you verify and summarize news content from multiple sources.");
        System.out.println("\nExample queries:");
        System.out.println("  - What happened yesterday in New York?");
        System.out.println("  - Is it true that [some claim]?");
        System.out.println("  - Give me the latest news on climate change");
        System.out.println("  - What's going on with the elections in Brazil?");
        System.out.println("\nVeriBot now remembers context! You can ask follow-up questions about");
        System.out.println("the news topic you're discussing and get relevant answers.");
        System.out.println("\nType 'exit' at any time to quit the application.");
        System.out.println("-".repeat(80));
    }
    */

}