package com.veribot.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NewsVerificationResultTest {

    @Test
    void testGetTruthfulnessLevel() {
        // Test high truthfulness level
        NewsVerificationResult highResult = new NewsVerificationResult(
                "A test summary",
                95,
                "High truthfulness justification",
                List.of("Source1", "Source2")
        );
        assertEquals("High (90-100%)", highResult.getTruthfulnessLevel());

        // Test moderate truthfulness level
        NewsVerificationResult moderateResult = new NewsVerificationResult(
                "A test summary",
                75,
                "Moderate truthfulness justification",
                List.of("Source1", "Source2")
        );
        assertEquals("Moderate (60-89%)", moderateResult.getTruthfulnessLevel());

        // Test doubtful truthfulness level
        NewsVerificationResult doubtfulResult = new NewsVerificationResult(
                "A test summary",
                45,
                "Doubtful truthfulness justification",
                List.of("Source1", "Source2")
        );
        assertEquals("Doubtful (30-59%)", doubtfulResult.getTruthfulnessLevel());

        // Test low truthfulness level
        NewsVerificationResult lowResult = new NewsVerificationResult(
                "A test summary",
                15,
                "Low truthfulness justification",
                List.of("Source1", "Source2")
        );
        assertEquals("Low (0-29%)", lowResult.getTruthfulnessLevel());
    }

    @Test
    void testToJson() {
        // Create a result with sources
        NewsVerificationResult resultWithSources = new NewsVerificationResult(
                "Test summary with sources",
                80,
                "Test justification with sources",
                List.of("Reuters", "BBC")
        );
        
        String expectedJson = """
            {
              "summary": "Test summary with sources",
              "truthfulness": "80%",
              "justification": "Test justification with sources",
              "sources_used": ["Reuters", "BBC"]
            }
            """.trim();
        
        assertEquals(expectedJson, resultWithSources.toJson().trim());
        
        // Create a result without sources
        NewsVerificationResult resultWithoutSources = new NewsVerificationResult(
                "Test summary without sources",
                60,
                "Test justification without sources",
                List.of()
        );
        
        String expectedJsonNoSources = """
            {
              "summary": "Test summary without sources",
              "truthfulness": "60%",
              "justification": "Test justification without sources",
              "sources_used": []
            }
            """.trim();
        
        assertEquals(expectedJsonNoSources, resultWithoutSources.toJson().trim());
    }
}