package com.veribot.controllers;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veribot.model.PromptModel;
import com.veribot.service.ConversationService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/veribot")
public class VeribotController {

    @Autowired
	ConversationService convServ; 
    
    @PostMapping
    public ResponseEntity<Map<String, String>> processVeribotRequest(@RequestBody PromptModel request, 
    		HttpSession session) {
    	
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Query cannot be empty"));
        }
        
        try {
            String response = convServ.processQuery(request, session.getId());
            return ResponseEntity.ok(Collections.singletonMap("response", response));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", "Failed to process request: " + e.getMessage()));
        }
    }

} 
