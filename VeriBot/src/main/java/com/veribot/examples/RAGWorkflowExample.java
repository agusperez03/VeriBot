package com.veribot.examples;

import com.veribot.config.AzureOpenAIConfig;
import com.veribot.config.SerpApiConfig;
import com.veribot.model.NewsVerificationResult;
import com.veribot.service.NewsSearchService;
import com.veribot.service.NewsVerificationService;
import com.veribot.service.RAGService;

/**
 * Example demonstrating the RAG workflow in VeriBot.
 * 
 * This example shows:
 * 1. Initial news verification with document embedding
 * 2. Follow-up questions using RAG retrieval
 * 3. How context is maintained across conversation
 */
public class RAGWorkflowExample {
    
    public static void main(String[] args) {
        System.out.println("=== VeriBot RAG Workflow Example ===\n");
        
        try {
            // Initialize configurations
            AzureOpenAIConfig openAIConfig = new AzureOpenAIConfig();
            SerpApiConfig serpApiConfig = new SerpApiConfig();
            
            // Initialize services
            RAGService ragService = new RAGService();
            NewsSearchService searchService = new NewsSearchService(serpApiConfig);
            String sessionId = "example-session-123";
            NewsVerificationService verificationService = new NewsVerificationService(
                openAIConfig, searchService, ragService, sessionId
            );
            
            System.out.println("✓ Services initialized successfully\n");
            
            // ==========================================
            // Step 1: Initial News Query
            // ==========================================
            System.out.println("--- Step 1: Initial News Query ---");
            String initialQuery = "What happened with the recent climate summit?";
            System.out.println("User: " + initialQuery);
            System.out.println("\nProcessing...");
            System.out.println("- Searching for news articles");
            System.out.println("- Embedding documents into ChromaDB");
            System.out.println("- Analyzing content");
            
            NewsVerificationResult result1 = verificationService.verifyNews(initialQuery);
            
            System.out.println("\nVeriBot Response:");
            System.out.println("Summary: " + result1.summary());
            System.out.println("Truthfulness: " + result1.truthfulnessPercentage() + "%");
            System.out.println("Justification: " + result1.justification());
            System.out.println("Sources: " + result1.sourcesUsed());
            System.out.println("\n✓ Documents embedded in vector database for this session");
            
            // ==========================================
            // Step 2: Follow-up Question (uses RAG)
            // ==========================================
            System.out.println("\n--- Step 2: Follow-up Question ---");
            String followUpQuery1 = "What countries participated?";
            System.out.println("User: " + followUpQuery1);
            System.out.println("\nProcessing...");
            System.out.println("- Detected same event (no new search needed)");
            System.out.println("- Retrieving relevant context from vector database");
            System.out.println("- Generating enhanced response with RAG");
            
            NewsVerificationResult result2 = verificationService.verifyNews(followUpQuery1);
            
            System.out.println("\nVeriBot Response:");
            System.out.println(result2.summary());
            System.out.println("\n✓ RAG retrieved relevant passages about participating countries");
            
            // ==========================================
            // Step 3: Another Follow-up Question
            // ==========================================
            System.out.println("\n--- Step 3: Another Follow-up Question ---");
            String followUpQuery2 = "What were the main agreements?";
            System.out.println("User: " + followUpQuery2);
            System.out.println("\nProcessing...");
            System.out.println("- Retrieving context about agreements from vector database");
            System.out.println("- Generating response");
            
            NewsVerificationResult result3 = verificationService.verifyNews(followUpQuery2);
            
            System.out.println("\nVeriBot Response:");
            System.out.println(result3.summary());
            System.out.println("\n✓ RAG found specific details about agreements");
            
            // ==========================================
            // Step 4: New Topic (triggers new search)
            // ==========================================
            System.out.println("\n--- Step 4: New Topic Query ---");
            String newTopicQuery = "What's happening with the tech industry layoffs?";
            System.out.println("User: " + newTopicQuery);
            System.out.println("\nProcessing...");
            System.out.println("- Detected new topic");
            System.out.println("- Starting new search");
            System.out.println("- Embedding new documents");
            
            NewsVerificationResult result4 = verificationService.verifyNews(newTopicQuery);
            
            System.out.println("\nVeriBot Response:");
            System.out.println("Summary: " + result4.summary());
            System.out.println("Truthfulness: " + result4.truthfulnessPercentage() + "%");
            System.out.println("\n✓ New documents embedded, old context still available");
            
            System.out.println("\n=== Example Complete ===");
            System.out.println("\nKey Points:");
            System.out.println("1. Initial queries trigger web search + document embedding");
            System.out.println("2. Follow-up questions use RAG to retrieve relevant context");
            System.out.println("3. All documents are stored in ChromaDB with session ID");
            System.out.println("4. New topics trigger new searches while maintaining old data");
            
        } catch (Exception e) {
            System.err.println("Error running example: " + e.getMessage());
            e.printStackTrace();
            
            System.err.println("\nMake sure:");
            System.err.println("1. ChromaDB is running (docker-compose up -d)");
            System.err.println("2. Environment variables are set (.env file)");
            System.err.println("3. All dependencies are installed (mvn clean install)");
        }
    }
}
