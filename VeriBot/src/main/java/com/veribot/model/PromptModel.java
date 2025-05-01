package com.veribot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PromptModel {
	private String text;
    
    // Getters y setters
    public String getText() {
        return text;
    }
    
    public void setText(String query) {
        this.text = query;
    }
}
