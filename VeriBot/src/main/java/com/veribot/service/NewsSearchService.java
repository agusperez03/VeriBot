package com.veribot.service;

import com.veribot.config.AzureBingSearchConfig;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to search the web for news articles and information using Azure Bing Search API.
 */
public class NewsSearchService {
    private static final Logger logger = LoggerFactory.getLogger(NewsSearchService.class);
    private static final int DEFAULT_MAX_RESULTS = 5;
    
    private final String apiKey;
    private final String endpoint;
    private final HttpClient httpClient;

    /**
     * Creates a new NewsSearchService with the provided configuration.
     *
     * @param config the Azure Bing Search configuration
     */
    public NewsSearchService(AzureBingSearchConfig config) {
        this.apiKey = config.getApiKey();
        this.endpoint = config.getEndpoint();
        this.httpClient = HttpClient.newHttpClient();
        logger.info("NewsSearchService initialized with Azure Bing Search");
    }

    /**
     * Searches for news articles and information using the provided query.
     *
     * @param query the search query
     * @return a list of documents containing relevant information
     */
    public List<Document> searchNews(String query) {
        return searchNews(query, DEFAULT_MAX_RESULTS);
    }

    /**
     * Searches for news articles and information using the provided query and maximum results.
     *
     * @param query the search query
     * @param maxResults the maximum number of results to return
     * @return a list of documents containing relevant information
     */
    public List<Document> searchNews(String query, int maxResults) {
        logger.info("Searching for news with query: {}", query);
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String requestUrl = endpoint + "?q=" + encodedQuery + "&count=" + maxResults;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseBingSearchResponse(response.body());
            } else {
                logger.error("Bing Search API returned error: {} - {}", response.statusCode(), response.body());
                return new ArrayList<>();
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error searching for news: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }
    
    /**
     * Parses the Bing Search API response and converts it to Document objects.
     * 
     * @param responseBody the JSON response from the Bing Search API
     * @return a list of Document objects
     */
    private List<Document> parseBingSearchResponse(String responseBody) {
        List<Document> documents = new ArrayList<>();
        
        try {
            JSONObject responseJson = new JSONObject(responseBody);
            
            // Check if webPages section exists
            if (responseJson.has("webPages") && responseJson.getJSONObject("webPages").has("value")) {
                JSONArray results = responseJson.getJSONObject("webPages").getJSONArray("value");
                
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    
                    String name = result.optString("name", "");
                    String url = result.optString("url", "");
                    String snippet = result.optString("snippet", "");
                    
                    // Extract domain as source
                    String source = extractDomainFromUrl(url);
                    
                    // Combine name and snippet for the document text
                    StringBuilder contentBuilder = new StringBuilder();
                    contentBuilder.append("Title: ").append(name).append("\n\n");
                    contentBuilder.append("Content: ").append(snippet);
                    
                    // Create metadata using a Map
                    Map<String, String> metadataMap = new HashMap<>();
                    metadataMap.put("source", source);
                    metadataMap.put("url", url);
                    metadataMap.put("title", name);
                    
                    Metadata metadata = new Metadata(metadataMap);
                    
                    Document document = Document.from(contentBuilder.toString(), metadata);
                    documents.add(document);
                }
            }
            
            // Check if news section exists
            if (responseJson.has("news") && responseJson.getJSONObject("news").has("value")) {
                JSONArray newsResults = responseJson.getJSONObject("news").getJSONArray("value");
                
                for (int i = 0; i < newsResults.length(); i++) {
                    JSONObject result = newsResults.getJSONObject(i);
                    
                    String name = result.optString("name", "");
                    String url = result.optString("url", "");
                    String description = result.optString("description", "");
                    
                    // Extract provider (source)
                    String source = "";
                    if (result.has("provider") && result.getJSONArray("provider").length() > 0) {
                        source = result.getJSONArray("provider").getJSONObject(0).optString("name", "");
                    }
                    
                    if (source.isEmpty()) {
                        source = extractDomainFromUrl(url);
                    }
                    
                    // Combine name and description for the document text
                    StringBuilder contentBuilder = new StringBuilder();
                    contentBuilder.append("News Title: ").append(name).append("\n\n");
                    contentBuilder.append("Content: ").append(description);
                    
                    // Create metadata using a Map
                    Map<String, String> metadataMap = new HashMap<>();
                    metadataMap.put("source", source);
                    metadataMap.put("url", url);
                    metadataMap.put("title", name);
                    
                    Metadata metadata = new Metadata(metadataMap);
                    
                    Document document = Document.from(contentBuilder.toString(), metadata);
                    documents.add(document);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error parsing Bing Search results: {}", e.getMessage(), e);
        }
        
        return documents;
    }
    
    /**
     * Extracts the domain name from a URL.
     *
     * @param url the URL to extract from
     * @return the domain name
     */
    private String extractDomainFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        
        // Remove protocol if present
        String domain = url.replaceAll("^https?://", "");
        
        // Remove www. if present
        domain = domain.replaceAll("^www\\.", "");
        
        // Get domain up to the first /
        int slashIndex = domain.indexOf('/');
               if (slashIndex != -1) {
            domain = domain.substring(0, slashIndex);
        }
        
        return domain;
    }
}