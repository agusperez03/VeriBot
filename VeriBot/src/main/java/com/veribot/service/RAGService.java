package com.veribot.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for RAG (Retrieval-Augmented Generation) operations.
 * Handles embedding news articles and retrieving relevant context for queries.
 */
public class RAGService {
    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);
    
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    
    // Configuration
    private static final String CHROMA_BASE_URL = "http://localhost:8000";
    private static final String COLLECTION_NAME = "news_verification";
    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.6;
    
    /**
     * Creates a new RAGService with ChromaDB as the vector store.
     */
    public RAGService() {
        this(CHROMA_BASE_URL, COLLECTION_NAME);
    }
    
    /**
     * Creates a new RAGService with custom ChromaDB configuration.
     * 
     * @param chromaBaseUrl The base URL for ChromaDB server
     * @param collectionName The name of the collection to use
     */
    public RAGService(String chromaBaseUrl, String collectionName) {
        logger.info("Initializing RAG Service with Chroma at: {}", chromaBaseUrl);
        
        // Initialize embedding model (local, no API key required)
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        
        // Initialize Chroma vector store
        this.embeddingStore = ChromaEmbeddingStore.builder()
                .baseUrl(chromaBaseUrl)
                .collectionName(collectionName)
                .timeout(Duration.ofSeconds(30))
                .build();
        
        logger.info("RAG Service initialized successfully");
    }
    
    /**
     * Stores news documents in the vector database after embedding them.
     * 
     * @param documents List of news documents to store
     * @param sessionId Session identifier to associate with these documents
     * @return Number of documents successfully stored
     */
    public int storeNewsDocuments(List<Document> documents, String sessionId) {
        if (documents == null || documents.isEmpty()) {
            logger.warn("No documents to store");
            return 0;
        }
        
        logger.info("Storing {} news documents for session: {}", documents.size(), sessionId);
        
        try {
            List<TextSegment> segments = new ArrayList<>();
            
            for (Document doc : documents) {
                // Create metadata with session ID and original metadata
                Metadata metadata = new Metadata();
                metadata.put("sessionId", sessionId);
                metadata.put("timestamp", System.currentTimeMillis());
                
                // Add original metadata from document
                if (doc.metadata() != null) {
                    doc.metadata().toMap().forEach((key, value) -> {
                        if (value != null) {
                            metadata.put(key, value.toString());
                        }
                    });
                }
                
                // Create text segment
                TextSegment segment = TextSegment.from(doc.text(), metadata);
                segments.add(segment);
            }
            
            // Embed and store all segments
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
            
            logger.info("Successfully stored {} documents", segments.size());
            return segments.size();
            
        } catch (Exception e) {
            logger.error("Error storing documents: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Retrieves relevant news content based on a user query.
     * 
     * @param query User's question or query
     * @param sessionId Session identifier to filter results
     * @param maxResults Maximum number of results to return
     * @return List of relevant text segments with their content
     */
    public List<String> retrieveRelevantContext(String query, String sessionId, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            logger.warn("Empty query provided");
            return new ArrayList<>();
        }
        
        logger.info("Retrieving context for query: '{}' (session: {})", query, sessionId);
        
        try {
            // Embed the query
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // Create filter for session ID
            Filter sessionFilter = new IsEqualTo("sessionId", sessionId);
            
            // Create search request with filter
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(MIN_SCORE)
                    .filter(sessionFilter)
                    .build();
            
            // Search for similar documents
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            
            // Extract text from matches
            List<String> relevantTexts = searchResult.matches().stream()
                    .map(match -> {
                        logger.debug("Match score: {}", match.score());
                        return match.embedded().text();
                    })
                    .collect(Collectors.toList());
            
            logger.info("Retrieved {} relevant documents", relevantTexts.size());
            return relevantTexts;
            
        } catch (Exception e) {
            logger.error("Error retrieving context: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Retrieves relevant news content based on a user query with default max results.
     * 
     * @param query User's question or query
     * @param sessionId Session identifier to filter results
     * @return List of relevant text segments with their content
     */
    public List<String> retrieveRelevantContext(String query, String sessionId) {
        return retrieveRelevantContext(query, sessionId, MAX_RESULTS);
    }
    
    /**
     * Clears all documents associated with a specific session.
     * Note: This requires custom implementation or manual cleanup in ChromaDB.
     * 
     * @param sessionId Session identifier
     */
    public void clearSessionDocuments(String sessionId) {
        logger.info("Clearing documents for session: {}", sessionId);
        // Note: ChromaEmbeddingStore doesn't have a built-in method to delete by metadata
        // This would require a custom implementation or recreating the collection
        logger.warn("Session-specific document deletion not yet implemented");
    }
    
    /**
     * Formats retrieved context into a single string for use in prompts.
     * 
     * @param contexts List of context strings
     * @return Formatted context string
     */
    public String formatContextForPrompt(List<String> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "No additional context available.";
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append("Relevant information from previous news articles:\n\n");
        
        for (int i = 0; i < contexts.size(); i++) {
            formatted.append(String.format("[Source %d]\n%s\n\n", i + 1, contexts.get(i)));
        }
        
        return formatted.toString();
    }
}
