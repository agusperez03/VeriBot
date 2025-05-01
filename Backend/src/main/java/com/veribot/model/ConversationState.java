package com.veribot.model;

/**
 * Represents the state of a conversation in the news verification system.
 */
public enum ConversationState {
    /**
     * The conversation is about a specific news event that has already been discussed.
     */
    DISCUSSING_CURRENT_EVENT,
    
    /**
     * The conversation is looking for a new news event to discuss.
     */
    LOOKING_FOR_NEW_EVENT,
    
    /**
     * The conversation is about an irrelevant topic (not news-related).
     */
    IRRELEVANT_TOPIC
}