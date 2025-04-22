package com.veribot;

import com.veribot.config.AzureBingSearchConfig;
import com.veribot.config.AzureOpenAIConfig;
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

    /**
     * Main method to run the VeriBot news verification agent.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.info("Starting VeriBot News Verification Agent");

        try {
            // Initialize configurations
            AzureOpenAIConfig openAIConfig = new AzureOpenAIConfig();
            AzureBingSearchConfig bingSearchConfig = new AzureBingSearchConfig();

            // Initialize services
            NewsSearchService searchService = new NewsSearchService(bingSearchConfig);
            NewsVerificationService verificationService = new NewsVerificationService(openAIConfig, searchService);

            // Start the user interaction loop
            runInteractiveMode(verificationService);
        } catch (Exception e) {
            logger.error("Error initializing VeriBot: {}", e.getMessage(), e);
            System.err.println("Failed to start VeriBot: " + e.getMessage());
            
            if (e instanceof IllegalStateException && e.getMessage().contains("environment variable")) {
                System.err.println("\nPlease ensure you have set up the required environment variables.");
                System.err.println("Create a .env file in the project root with the following variables:");
                System.err.println("AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_API_KEY, AZURE_OPENAI_DEPLOYMENT_NAME, AZURE_OPENAI_API_VERSION");
                System.err.println("AZURE_BING_SEARCH_API_KEY, AZURE_BING_SEARCH_ENDPOINT");
            }
        }
    }

    /**
     * Runs the interactive command-line interface for the VeriBot agent.
     *
     * @param verificationService the service to use for verifying news
     */
    private static void runInteractiveMode(NewsVerificationService verificationService) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        printWelcomeMessage();

        while (running) {
            System.out.print("\nEnter a news-related query (or type 'exit' to quit): ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit") || 
                userInput.equalsIgnoreCase("quit") || 
                userInput.equalsIgnoreCase("q")) {
                running = false;
                continue;
            }

            if (userInput.isEmpty()) {
                System.out.println("Please enter a valid query.");
                continue;
            }

            try {
                System.out.println("\nAnalyzing news content... Please wait.");
                NewsVerificationResult result = verificationService.verifyNews(userInput);
                printVerificationResult(result);
            } catch (Exception e) {
                logger.error("Error processing query: {}", e.getMessage(), e);
                System.err.println("Error processing your query: " + e.getMessage());
            }
        }

        System.out.println("Thank you for using VeriBot. Goodbye!");
        scanner.close();
    }

    /**
     * Prints a welcome message with instructions for using VeriBot.
     */
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
        System.out.println("\nType 'exit' at any time to quit the application.");
        System.out.println("-".repeat(80));
    }

    /**
     * Prints the verification result in a formatted way.
     *
     * @param result the verification result to print
     */
    private static void printVerificationResult(NewsVerificationResult result) {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("VERIFICATION RESULTS");
        System.out.println("-".repeat(80));
        
        System.out.println("\nSUMMARY:");
        System.out.println(result.summary());
        
        System.out.println("\nTRUTHFULNESS: " + result.truthfulnessPercentage() + "% - " + result.getTruthfulnessLevel());
        
        System.out.println("\nJUSTIFICATION:");
        System.out.println(result.justification());
        
        if (!result.sourcesUsed().isEmpty()) {
            System.out.println("\nSOURCES USED:");
            for (String source : result.sourcesUsed()) {
                System.out.println("- " + source);
            }
        }
        
        System.out.println("\n" + "-".repeat(80));
        System.out.println("JSON OUTPUT:");
        System.out.println(result.toJson());
        System.out.println("-".repeat(80));
    }
}