package com.veribot.model;

import java.util.List;

/**
 * Represents the result of a news verification and summarization.
 */
public record NewsVerificationResult(
    String summary,
    int truthfulnessPercentage,
    String justification,
    List<String> sourcesUsed
) {
    /**
     * Returns a human-readable string representation of the truthfulness level.
     * 
     * @return A qualitative assessment of truthfulness
     */
    public String getTruthfulnessLevel() {
        if (truthfulnessPercentage >= 90) {
            return "High (90-100%)";
        } else if (truthfulnessPercentage >= 60) {
            return "Moderate (60-89%)";
        } else if (truthfulnessPercentage >= 30) {
            return "Doubtful (30-59%)";
        } else {
            return "Low (0-29%)";
        }
    }
    
    /**
     * Creates a formatted JSON string representation of this result.
     * 
     * @return A JSON string representing this result
     */
    public String toJson() {
        return """
            {
              "summary": "%s",
              "truthfulness": "%d%%",
              "justification": "%s",
              "sources_used": %s
            }
            """.formatted(
                summary,
                truthfulnessPercentage,
                justification,
                sourcesToJsonArray(sourcesUsed)
            );
    }
    
    private String sourcesToJsonArray(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return "[]";
        }
        
        return sources.stream()
                .map(source -> "\"" + source + "\"")
                .reduce((a, b) -> a + ", " + b)
                .map(result -> "[" + result + "]")
                .orElse("[]");
    }
}