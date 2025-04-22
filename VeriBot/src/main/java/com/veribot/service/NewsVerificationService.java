package com.veribot.service;

import com.veribot.config.AzureOpenAIConfig;
import com.veribot.model.NewsVerificationResult;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for verifying news content and analyzing its truthfulness.
 */
public class NewsVerificationService {
    private static final Logger logger = LoggerFactory.getLogger(NewsVerificationService.class);
    private final NewsSearchService searchService;
    private final String endpoint;
    private final String apiKey;
    private final String deploymentName;
    private final String apiVersion;
    private final HttpClient httpClient;

    /**
     * Creates a new NewsVerificationService with the provided configurations.
     *
     * @param openAIConfig the Azure OpenAI configuration
     * @param searchService the news search service
     */
    public NewsVerificationService(AzureOpenAIConfig openAIConfig, NewsSearchService searchService) {
        this.endpoint = openAIConfig.getEndpoint();
        this.apiKey = openAIConfig.getApiKey();
        this.deploymentName = openAIConfig.getDeploymentName();
        this.apiVersion = openAIConfig.getApiVersion();
        this.searchService = searchService;
        this.httpClient = HttpClient.newHttpClient();
        
        logger.info("NewsVerificationService initialized with Azure OpenAI");
    }

    /**
     * Verifies a news query by searching for information and analyzing the results.
     *
     * @param userQuery the user's query about a news item
     * @return a NewsVerificationResult containing the analysis
     */
    public NewsVerificationResult verifyNews(String userQuery) {
        // 1. Validate input for news-related content
        if (!isNewsRelatedQuery(userQuery)) {
            return createInvalidQueryResponse();
        }

        // 2. Generate search query
        String searchQuery = generateSearchQuery(userQuery);
        
        // 3. Search for relevant information
        List<Document> searchResults = searchService.searchNews(searchQuery);
        
        if (searchResults.isEmpty()) {
            return createNoResultsResponse(userQuery);
        }

        // 4. Analyze the search results
        return analyzeNewsContent(userQuery, searchResults);
    }

    /**
     * Determines if the user query is related to news content.
     *
     * @param query the user's query
     * @return true if the query is news-related, false otherwise
     */
    private boolean isNewsRelatedQuery(String query) {
        String promptTemplate = """
            Determine if the following query is related to news content, fact verification, 
            or current events that could be found in news sources.
                        
            Query: %s
                        
            Answer only with YES if it's news related, or NO if it's not news related.
            """;
        
        String prompt = String.format(promptTemplate, query);
        String response = generateAzureOpenAIResponse(prompt, 0.0);
        
        logger.debug("News validation response: {}", response);
        return response.trim().toUpperCase().contains("YES");
    }

    /**
     * Generates an optimized search query based on the user's input.
     *
     * @param userQuery the user's original query
     * @return an optimized search query for news search
     */
    private String generateSearchQuery(String userQuery) {
        String promptTemplate = """
            Convert the following user query about news or current events into an optimized search query.
            Focus on key facts, dates, places, or persons that would be useful for a web search.
                        
            User query: %s
                        
            Return ONLY the optimized search query, nothing else.
            """;
        
        String prompt = String.format(promptTemplate, userQuery);
        String response = generateAzureOpenAIResponse(prompt, 0.0);
        
        logger.debug("Generated search query: {}", response);
        return response.trim();
    }

    /**
     * Creates a response for when the user query is not related to news.
     *
     * @return a NewsVerificationResult with an appropriate message
     */
    private NewsVerificationResult createInvalidQueryResponse() {
        return new NewsVerificationResult(
                "This query does not appear to be related to news or factual content that can be verified.",
                0,
                "The query doesn't match expected input criteria for news verification.",
                List.of()
        );
    }

    /**
     * Creates a response for when no search results are found.
     *
     * @param query the original user query
     * @return a NewsVerificationResult with an appropriate message
     */
    private NewsVerificationResult createNoResultsResponse(String query) {
        return new NewsVerificationResult(
                "No relevant information found for this query.",
                0,
                "Unable to verify due to lack of information sources.",
                List.of()
        );
    }

    /**
     * Analyzes news content from search results to verify truthfulness.
     *
     * @param query the user's original query
     * @param documents the search results to analyze
     * @return a NewsVerificationResult with the analysis
     */
    private NewsVerificationResult analyzeNewsContent(String query, List<Document> documents) {
        // Extract text content from documents
        String contentToAnalyze = documents.stream()
                .map(Document::text)
                .collect(Collectors.joining("\n\n"));
        
        // Extract source information from documents
        List<String> sources = extractSources(documents);
        
        String systemPrompt = """
            You are an expert news verification agent that verifies information based on multiple sources.
            Analyze the news content provided and determine:
            1. A clear, objective, and concise summary (2-3 sentences)
            2. The percentage of truthfulness (0-100%) based on consistency across sources and factual support
            3. A brief justification for your truthfulness rating
                        
            Format your response exactly as follows (JSON format):
            {
              "summary": "your summary here",
              "truthfulness_percentage": number between 0-100,
              "justification": "your justification here"
            }
            """;
        
        String userPrompt = """
            System: %s
            
            Query: %s
                        
            Content to verify:
            %s
            """;
        
        String formattedPrompt = String.format(userPrompt, systemPrompt, query, contentToAnalyze);
        String response = generateAzureOpenAIResponse(formattedPrompt, 0.0);
        
        logger.debug("Verification analysis response: {}", response);
        
        return parseVerificationResponse(response, sources);
    }

    /**
     * Extracts source information from documents.
     *
     * @param documents the search result documents
     * @return a list of source names
     */
    private List<String> extractSources(List<Document> documents) {
        List<String> sources = new ArrayList<>();
        
        for (Document doc : documents) {
            Metadata metadata = doc.metadata();
            
            // Check for source in metadata
            String source = metadata.get("source");
            if (source != null && !source.isEmpty()) {
                sources.add(source);
            }
            
            // Check for URL in metadata to extract domain
            String url = metadata.get("url");
            if (url != null && !url.isEmpty()) {
                try {
                    String domain = extractDomainFromUrl(url);
                    if (domain != null && !domain.isEmpty() && !sources.contains(domain)) {
                        sources.add(domain);
                    }
                } catch (Exception e) {
                    logger.warn("Error extracting domain from URL: {}", url);
                }
            }
        }
        
        return sources.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Extracts the domain name from a URL.
     *
     * @param url the URL to extract from
     * @return the domain name
     */
    private String extractDomainFromUrl(String url) {
        Pattern pattern = Pattern.compile("https?://(?:www\\.)?([^/]+)");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * Generates a response from Azure OpenAI API.
     *
     * @param prompt the prompt to send to the API
     * @param temperature the temperature to use for generation (0.0-1.0)
     * @return the generated response text
     */
    private String generateAzureOpenAIResponse(String prompt, double temperature) {
        try {
            // Build the API URL
            String apiUrl = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s", 
                    endpoint.replaceAll("/$", ""), 
                    deploymentName, 
                    apiVersion);
            
            // Create the request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", 800);
            
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);
            
            requestBody.put("messages", messages);
            
            // Build the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            
            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Process the response
            if (response.statusCode() == 200) {
                JSONObject responseJson = new JSONObject(response.body());
                
                // Extract the generated content
                if (responseJson.has("choices") && responseJson.getJSONArray("choices").length() > 0) {
                    JSONObject firstChoice = responseJson.getJSONArray("choices").getJSONObject(0);
                    
                    if (firstChoice.has("message") && firstChoice.getJSONObject("message").has("content")) {
                        return firstChoice.getJSONObject("message").getString("content");
                    }
                }
                
                logger.error("Unexpected response format from Azure OpenAI: {}", response.body());
                return "Error: Unexpected response format";
            } else {
                logger.error("Azure OpenAI API returned error: {} - {}", response.statusCode(), response.body());
                return "Error: " + response.statusCode();
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Azure OpenAI API: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Parses the verification response from the language model.
     *
     * @param response the raw response from the language model
     * @param sources the list of sources used
     * @return a structured NewsVerificationResult
     */
    private NewsVerificationResult parseVerificationResponse(String response, List<String> sources) {
        try {
            // First, try to parse as a well-formed JSON
            try {
                JSONObject jsonResponse = extractJsonObject(response);
                
                String summary = jsonResponse.getString("summary");
                int truthfulnessPercentage = jsonResponse.getInt("truthfulness_percentage");
                String justification = jsonResponse.getString("justification");
                
                return new NewsVerificationResult(
                        summary,
                        truthfulnessPercentage,
                        justification,
                        sources
                );
            } catch (Exception e) {
                // If JSON parsing fails, fall back to regex extraction
                logger.debug("Failed to parse JSON response, falling back to regex: {}", e.getMessage());
                
                // Extract summary
                Pattern summaryPattern = Pattern.compile("\"summary\"\\s*:\\s*\"([^\"]+)\"");
                Matcher summaryMatcher = summaryPattern.matcher(response);
                String summary = summaryMatcher.find() ? summaryMatcher.group(1) : "Summary not available";
                
                // Extract truthfulness percentage
                Pattern truthfulnessPattern = Pattern.compile("\"truthfulness_percentage\"\\s*:\\s*(\\d+)");
                Matcher truthfulnessMatcher = truthfulnessPattern.matcher(response);
                int truthfulnessPercentage = truthfulnessMatcher.find() ? 
                        Integer.parseInt(truthfulnessMatcher.group(1)) : 0;
                
                // Extract justification
                Pattern justificationPattern = Pattern.compile("\"justification\"\\s*:\\s*\"([^\"]+)\"");
                Matcher justificationMatcher = justificationPattern.matcher(response);
                String justification = justificationMatcher.find() ? 
                        justificationMatcher.group(1) : "Justification not available";
                
                return new NewsVerificationResult(
                        summary,
                        truthfulnessPercentage,
                        justification,
                        sources
                );
            }
        } catch (Exception e) {
            logger.error("Error parsing verification response: {}", e.getMessage(), e);
            
            return new NewsVerificationResult(
                    "Error analyzing content",
                    0,
                    "An error occurred while processing the verification results.",
                    sources
            );
        }
    }
    
    /**
     * Extracts a JSON object from a string that might contain additional text.
     * 
     * @param text the text that contains a JSON object
     * @return the extracted JSON object
     */
    private JSONObject extractJsonObject(String text) {
        // Look for patterns like: ```json { ... } ``` or just { ... }
        Pattern jsonPattern = Pattern.compile("\\{[^}]*\\}");
        Matcher matcher = jsonPattern.matcher(text);
        
        if (matcher.find()) {
            String jsonStr = matcher.group(0);
            return new JSONObject(jsonStr);
        }
        
        // If no JSON-like pattern was found, try with the whole text
        return new JSONObject(text);
    }
}