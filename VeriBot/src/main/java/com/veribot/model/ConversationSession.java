package com.veribot.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import dev.langchain4j.data.document.Document;

/**
 * Represents a conversation session with the user, maintaining context about the current news topic.
 */
public class ConversationSession {
    private String currentEvent;
    private String currentEventSummary;
    private int truthfulnessPercentage;
    private String justification;
    private ConversationState state;
    private LocalDateTime lastInteractionTime;
    private List<Document> currentNewsDocuments;
    private boolean documentsEmbedded;
    
    /**
     * Creates a new conversation session with default state.
     */
    public ConversationSession() {
        this.state = ConversationState.LOOKING_FOR_NEW_EVENT;
        this.lastInteractionTime = LocalDateTime.now();
        this.currentNewsDocuments = new ArrayList<>();
        this.documentsEmbedded = false;
    }
    
    /**
     * Updates the session with a new news verification result.
     * 
     * @param result The verification result containing news information
     */
    public void updateWithNewsResult(NewsVerificationResult result) {
        this.currentEvent = result.summary();
        this.currentEventSummary = result.summary();
        this.truthfulnessPercentage = result.truthfulnessPercentage();
        this.justification = result.justification();
        this.state = ConversationState.DISCUSSING_CURRENT_EVENT;
        updateLastInteractionTime();
    }
    
    /**
     * Updates the session with news documents for RAG.
     * 
     * @param documents The news documents to store
     */
    public void setCurrentNewsDocuments(List<Document> documents) {
        this.currentNewsDocuments = documents != null ? new ArrayList<>(documents) : new ArrayList<>();
        this.documentsEmbedded = false;
    }
    
    /**
     * Marks the current news documents as embedded in the vector store.
     */
    public void markDocumentsAsEmbedded() {
        this.documentsEmbedded = true;
    }
    
    /**
     * Sets the conversation state to indicate we're looking for a new event.
     */
    public void startNewEvent() {
        this.currentEvent = null;
        this.currentEventSummary = null;
        this.truthfulnessPercentage = 0;
        this.justification = null;
        this.currentNewsDocuments = new ArrayList<>();
        this.documentsEmbedded = false;
        this.state = ConversationState.LOOKING_FOR_NEW_EVENT;
        updateLastInteractionTime();
    }
    
    /**
     * Updates the timestamp of the last interaction.
     */
    public void updateLastInteractionTime() {
        this.lastInteractionTime = LocalDateTime.now();
    }
    
    /**
     * Checks if the session has expired (been inactive for too long).
     * 
     * @param timeoutMinutes Minutes of inactivity before a session is considered expired
     * @return true if the session has expired, false otherwise
     */
    public boolean hasExpired(int timeoutMinutes) {
        return lastInteractionTime.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
    }

    // Getters
    
    public String getCurrentEvent() {
        return currentEvent;
    }
    
    public String getCurrentEventSummary() {
        return currentEventSummary;
    }
    
    public int getTruthfulnessPercentage() {
        return truthfulnessPercentage;
    }
    
    public String getJustification() {
        return justification;
    }
    
    public ConversationState getState() {
        return state;
    }
    
    public void setState(ConversationState state) {
        this.state = state;
        updateLastInteractionTime();
    }
    
    public LocalDateTime getLastInteractionTime() {
        return lastInteractionTime;
    }
    
    public List<Document> getCurrentNewsDocuments() {
        return currentNewsDocuments;
    }
    
    public boolean areDocumentsEmbedded() {
        return documentsEmbedded;
    }
}