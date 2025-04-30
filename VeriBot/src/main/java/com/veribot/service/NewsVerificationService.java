package com.veribot.service;

import com.veribot.config.AzureOpenAIConfig;
import com.veribot.model.ConversationSession;
import com.veribot.model.ConversationState;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
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
    
    // Conversation timeout in minutes
    private static final int CONVERSATION_TIMEOUT_MINUTES = 30;
    
    // Store the current conversation session
    private ConversationSession conversationSession;

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
        this.conversationSession = new ConversationSession();
        
        logger.info("NewsVerificationService initialized with Azure OpenAI");
    }

	/**
     * Verifies a news query by searching for information and analyzing the results.
     * This method integrates conversation context to determine if the query is related
     * to a previously discussed news event.
     *
     * @param userQuery the user's query about a news item
     * @return a NewsVerificationResult containing the analysis
     */
    public NewsVerificationResult verifyNews(String userQuery) {
        // Check if the conversation has expired due to inactivity
        if (conversationSession.hasExpired(CONVERSATION_TIMEOUT_MINUTES)) {
            logger.info("Conversation session expired, starting new session");
            conversationSession.startNewEvent();
        }
        
        conversationSession.updateLastInteractionTime();
        
        // If we're already discussing a news event, determine if this query is related
        if (conversationSession.getState() == ConversationState.DISCUSSING_CURRENT_EVENT) {
            String queryIntent = classifyQueryIntent(userQuery, conversationSession.getCurrentEvent());
            
            if (queryIntent.equals("SAME_EVENT")) {
                // Query is about the same event, respond with the existing information
                logger.info("Query is about the same event: {}", conversationSession.getCurrentEvent());
                return createFollowUpResponse(userQuery);
            } else if (queryIntent.equals("NEW_EVENT")) {
                // Query is about a new event, initiate a new search
                logger.info("Query is about a new event, initiating search");
                conversationSession.startNewEvent();
                // Continue with normal verification process
            } else {
                // Query is not news-related
                return createIrrelevantQueryResponse();
            }
        }
        
        // If we don't have a current event or the query is about a new event
        // 1. Validate input for news-related content
        if (!isNewsRelatedQuery(userQuery)) {
            return createInvalidQueryResponse();
        }

        // 2. Generate search query and user's country
        String[] searchQueryAndCountry = generateSearchQuery(userQuery);
        String searchQuery = searchQueryAndCountry[0];
        String countryName = searchQueryAndCountry[1];

        String countryCode = "ar";    //Default values
        String languageCode = "es";
        
        String[] countryLang = CountryLanguageUtils.findCountryAndLanguage(countryName);
        if (countryLang != null) {
            countryCode = countryLang[0];
            languageCode = countryLang[1];
        }
        
        // 3. Search for relevant information
        List<Document> searchResults = searchService.searchNews(searchQuery, countryCode, languageCode);
        
        if (searchResults.isEmpty()) {
            return createNoResultsResponse(userQuery);
        }

        // 4. Analyze the search results
        NewsVerificationResult result = analyzeNewsContent(userQuery, searchResults, languageCode);
        
        
        
        return result;
    }
    
    /**
     * Classifies a user query to determine if it's about the current event,
     * a new event, or an irrelevant topic.
     * 
     * @param userQuery The user's query
     * @param currentEvent The current event being discussed
     * @return A string indicating the intent ("SAME_EVENT", "NEW_EVENT", or "IRRELEVANT")
     */
    private String classifyQueryIntent(String userQuery, String currentEvent) {
        if (currentEvent == null || currentEvent.isEmpty()) {
            return "NEW_EVENT";
        }
        
        String promptTemplate = """
            The conversation is currently about this news topic:
            "%s"
            
            The user has asked:
            "%s"
            
            Determine if the user's question is:
            1. About the same news topic we're already discussing
            2. About a new, different news topic
            3. Not related to any news topic at all
            
            Respond ONLY with one of these exact phrases:
            "SAME_EVENT" if it's about the same topic
            "NEW_EVENT" if it's about a different news topic
            "IRRELEVANT" if it's not about news
            """;
        
        String prompt = String.format(promptTemplate, currentEvent, userQuery);
        String response = generateAzureOpenAIResponse(prompt, 0.0);
        
        response = response.trim().toUpperCase();
        logger.debug("Query intent classification: {}", response);
        
        if (response.contains("SAME_EVENT")) {
            return "SAME_EVENT";
        } else if (response.contains("NEW_EVENT")) {
            return "NEW_EVENT";
        } else {
            return "IRRELEVANT";
        }
    }
    
    /**
     * Creates a response for follow-up questions about the current event.
     * 
     * @param userQuery The user's follow-up question
     * @return A NewsVerificationResult containing information from the current session
     */
    private NewsVerificationResult createFollowUpResponse(String userQuery) {
        // Generate a response specific to the follow-up question
        String promptTemplate = """
            You are answering a follow-up question about a news event.
            
            The news event summary: "%s"
            Truthfulness rating: %d%%
            Justification: "%s"
            
            The user is now asking: "%s"
            
            Based on the information above, provide a direct answer to the user's follow-up question.
            Answer in plain text without any special formatting or labeling.
            Keep your answer conversational, helpful, and relevant to the question.
            """;
        
        String prompt = String.format(
            promptTemplate,
            conversationSession.getCurrentEventSummary(),
            conversationSession.getTruthfulnessPercentage(),
            conversationSession.getJustification(),
            userQuery
        );
        
        String response = generateAzureOpenAIResponse(prompt, 0.0);
        logger.debug("Follow-up response: {}", response);
        
        // Simply use the plain text response directly
        return new NewsVerificationResult(
            response.trim(),
            conversationSession.getTruthfulnessPercentage(),
            conversationSession.getJustification(),
            new ArrayList<>() // Empty sources since we're using cached info
        );
    }
    
    /**
     * Creates a response for when the user query is irrelevant (not news-related).
     *
     * @return a NewsVerificationResult with an appropriate message
     */
    private NewsVerificationResult createIrrelevantQueryResponse() {
        conversationSession.setState(ConversationState.IRRELEVANT_TOPIC);
        
        return new NewsVerificationResult(
                "I can only help with questions about news events and factual information.",
                0,
                "Your question doesn't appear to be about a news event or topic that can be verified.",
                List.of()
        );
    }

    /**
     * Determines if the user query is related to news content.
     *
     * @param query the user's query
     * @return true if the query is news-related, false otherwise
     */
    private boolean isNewsRelatedQuery(String query) {
        String promptTemplate = """
            Classify the following query for its likelihood to be related to news content:

            - LIKELY: It can reasonably appear in news articles (even local news).
            - UNLIKELY: It is technical, informational, or clearly unrelated to news.

            Query: %s

            Respond only with LIKELY or UNLIKELY.
            """;
        
        String prompt = String.format(promptTemplate, query);
        String response = generateAzureOpenAIResponse(prompt, 0.0);
        
        logger.debug("News validation response: {}", response);
        return response.trim().toUpperCase().contains("LIKELY");
    }

    /**
     * Generates an optimized search query and infers the user's country based on the input.
     *
     * @param userQuery the user's original query
     * @return a String array where [0] is the optimized search query, and [1] is the inferred country name
     */
    private String[] generateSearchQuery(String userQuery) {
        LocalDate currentDate = LocalDate.now();
        String promptTemplate = """
            Given the following user query, do two things:
            1. Convert it into an optimized search query, focusing on key facts, dates, places, or persons. If the user did not specify a date, append this date %s.The date must be in this format mm/dd/aaaa . The generated search query must be in the language of the user's query 
            2. Guess the country related to the query or the user, based on the context (e.g., if the query mentions a country, city, or uses a specific language).
            
            Return the answer ONLY in this exact JSON format:
            {
            "search_query": "your optimized search query here",
            "country": "guessed country name here"
            }
            
            User query: %s
            """;

        String prompt = String.format(promptTemplate, currentDate, userQuery);
        String response = generateAzureOpenAIResponse(prompt, 0.0);

        logger.debug("Generated search query and country: {}", response);

        try {
            JSONObject jsonResponse = extractJsonObject(response);
            String searchQuery = jsonResponse.getString("search_query").trim();
            String country = jsonResponse.getString("country").trim();
            return new String[]{searchQuery, country};
        } catch (Exception e) {
            logger.error("Error parsing search query and country: {}", e.getMessage(), e);
            // Fallback in case of parsing error
            return new String[]{userQuery, "Unknown"};
        }
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
     * @param languageCode the ISO 639-1 language code to respond in (e.g., "es" for Spanish)
     * @return a NewsVerificationResult with the analysis
     */
    private NewsVerificationResult analyzeNewsContent(String query, List<Document> documents, String languageCode) {
        // Extract text content from documents
        String contentToAnalyze = documents.stream()
                .map(Document::text)
                .collect(Collectors.joining("\n\n"));

        // Extract source information from documents
        List<String> sources = extractSources(documents);

        String languageInstruction = getLanguageInstruction(languageCode);

        String systemPrompt = String.format("""
            You are an expert news verification agent that verifies information based on multiple sources.
            Analyze the news content provided and determine:
            1. A clear, objective, and concise summary
            2. The percentage of truthfulness (0-100%%) based on consistency across sources and factual support
            3. A brief justification for your truthfulness rating

            Respond %s.

            Format your response exactly as follows (JSON format):
            {
            "summary": "your summary here",
            "truthfulness_percentage": number between 0-100,
            "justification": "your justification here"
            }
            """, languageInstruction);

        String userPrompt = String.format("""
            System: %s
            
            Query: %s
                        
            Content to verify:
            %s
            """, systemPrompt, query, contentToAnalyze);

        String response = generateAzureOpenAIResponse(userPrompt, 0.0);

        logger.debug("Verification analysis response: {}", response);

        return parseVerificationResponse(response, sources);
    }

    private String getLanguageInstruction(String languageCode) {
        return switch (languageCode) {
            case "es" -> "in Spanish";
            case "en" -> "in English";
            case "fr" -> "in French";
            case "ar" -> "in Arabic";
            case "de" -> "in German";
            case "it" -> "in Italian";
            case "pt" -> "in Portuguese";
            case "ru" -> "in Russian";
            case "zh" -> "in Chinese";
            default -> "in the most appropriate language"; // fallback general
        };
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
            requestBody.put("max_completion_tokens", 800);
            
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

    public class CountryLanguageUtils {

        /**
         * Finds the country and language code for a given country name.
         *
         * @param countryName the name of the country
         * @return a String array: [countryCode, languageCode], or null if not found
         */
        public static String[] findCountryAndLanguage(String countryName) {
            try {
                String path = "/home/roman7978/Documentos/VeriBot/VeriBot/google_countries.JSON"; // ajustalo si tu ruta es distinta
                String content = Files.readString(Paths.get(path));
                JSONArray countriesArray = new JSONArray(content);

                for (int i = 0; i < countriesArray.length(); i++) {
                    JSONObject obj = countriesArray.getJSONObject(i);
                    String name = obj.getString("country_name").trim();

                    if (name.equalsIgnoreCase(countryName.trim())) {
                        String countryCode = obj.getString("country_code");
                        String languageCode = obj.getString("language_code");
                        return new String[]{countryCode, languageCode};
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null; // si no encuentra nada
        }
    }


    /**
     * Get ConversationSession object.
     *
     * @return ConversationSession.
     */
    public ConversationSession getConversationSession() {
		return conversationSession;
	}
    
}