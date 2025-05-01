package com.veribot.service;
import com.veribot.VeriBot;

import com.veribot.model.PromptModel;
import com.veribot.model.UserContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;


@Service
public class ConversationService {
    // This class is responsible for managing the conversation state and interactions with the user.
    // It will handle the logic for processing user queries and generating responses.
    private final VeriBot veriBot;
    private final Map<String, UserContext> userContexts = new ConcurrentHashMap<>();
    private final long CONTEXT_EXPIRY_MINUTES = 30;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Autowired
    public ConversationService(VeriBot veriBot) {
        this.veriBot = veriBot;
        setupContextCleanupTask();
    }


	public String processQuery(PromptModel request, String sessionId) {
		UserContext context = userContexts.computeIfAbsent(
	            sessionId, id -> new UserContext(id)
	        );
	        
	        // Actualizar timestamp de último acceso
	        context.updateLastAccessed();
        return veriBot.run(request.getText(),context);
    }
	
	
    private void setupContextCleanupTask() {
		// TODO Auto-generated method stub
    	Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                this::cleanupOldContexts, 10, 10, TimeUnit.MINUTES
            );
	}
    
    private void cleanupOldContexts() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(CONTEXT_EXPIRY_MINUTES);
        
        userContexts.entrySet().removeIf(entry -> 
            entry.getValue().getLastAccessed().isBefore(expiryTime)
        );
    }

}
