package com.veribot.service;

import com.veribot.config.SerpApiConfig;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.dankito.readability4j.Readability4J;
import net.dankito.readability4j.Article;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.net.URI;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to search the web for news articles and information using Azure Bing Search API.
 */
public class NewsSearchService {
    private static final Logger logger = LoggerFactory.getLogger(NewsSearchService.class);
    private static final int DEFAULT_MAX_RESULTS = 5;
    
    private final String apiKey;
    private final HttpClient httpClient;
    
    private static final String baseUrl = "https://serpapi.com/search.json";

    /**
     * Creates a new NewsSearchService with the provided configuration.
     *
     * @param config the Serp Api Configuration.
     */
    public NewsSearchService(SerpApiConfig config) {
        this.apiKey = config.getApiKey();
        this.httpClient = HttpClient.newHttpClient();
        logger.info("NewsSearchService initialized with SerpApi");
    }

    /**
     * Searches for news articles and information using the provided query and maximum results.
     *
     * @param query the search query
     * @param maxResults the maximum number of results to return
     * @return a list of documents containing relevant information
     */
    public List<Document> searchNews(String query, String country, String language) {
    	int maxResults = DEFAULT_MAX_RESULTS;
        logger.info("Searching for news with query: {}", query);
        try {
        	String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        	
            String requestUrl = baseUrl +
            					"?engine=google&q=" + encodedQuery +
                                "&api_key=" + apiKey +
                                "&gl=" + country +          // Country
                                "&hl=" +language+          // Language
                                "&tbm=nws"+        // News search only
                                "&num=" + maxResults;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseSerpApiResponse(response.body(), maxResults);
            } else {
            	logger.error("SerpAPI returned error: {} - {}", response.statusCode(), response.body());
                return new ArrayList<>();
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error searching for news: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }
    
    /**
     * Parses the Serp Api response and converts it to Document objects.
     * 
     * @param responseBody the JSON response from the SerpApi
     * @return a list of Document objects
     */
    private List<Document> parseSerpApiResponse(String responseBody, int maxResults) {
    	List<Document> results = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            String link,text,title,source,date = "";
            
            // We first tried to get news from "people_also_search_for"
            if (rootNode.has("people_also_search_for")) {
                JsonNode pasf = rootNode.get("people_also_search_for");
                
                for (JsonNode item : pasf) {
                    if (item.has("news_results")) {
                        JsonNode newsResults = item.get("news_results");
                        for (JsonNode news : newsResults) {
                        	text="";
                            if (news.has("link")) {       
                                
                            	link = news.get("link").asText();
                            	text = textOfPage(link);
                            	if(text!="") {
                            		title = news.has("title") ? news.get("title").asText() : "";
                                    source = news.has("source") ? news.get("source").asText() : "";
                                    date = news.has("date") ? news.get("date").asText() : "";
                                    
                                    
                                    StringBuilder contentBuilder = new StringBuilder();
                                    Map<String, String> metadataMap = new HashMap<>();
                                    contentBuilder.append("Title: ").append(title);
                                    contentBuilder.append(" - Date: ").append(date);
                                    contentBuilder.append(" - Source: ").append(source);
                                    contentBuilder.append(" - Text: ").append(text);

                                    metadataMap.put("date", date);
                                    metadataMap.put("source", source);
                                    metadataMap.put("link", link);
                                    
                                    Metadata metadata = new Metadata(metadataMap);
                                    Document doc = Document.from(contentBuilder.toString(), metadata);
                                    
                                    results.add(doc);
                                    if (results.size() >= maxResults) {
                                        return results;
                                    }
                            	}                                
                            }
                        }
                    }
                }
            }
            
            // If there aren't enough results or "people_also_search_for" wasn't found,
            // we search in "news_results"
            if (results.size() < maxResults && rootNode.has("news_results")) {
                JsonNode newsResults = rootNode.get("news_results");
                for (JsonNode news : newsResults) {
                	text="";
                    if (news.has("link")) {
                        link = news.get("link").asText();
                        text = textOfPage(link);
                        
                        if(text!="") {
	                        title = news.has("title") ? news.get("title").asText() : "";
	                        source = news.has("source") ? news.get("source").asText() : "";
	                        date = news.has("date") ? news.get("date").asText() : "";
	                        
	                        
	                        StringBuilder contentBuilder = new StringBuilder();
	                        Map<String, String> metadataMap = new HashMap<>();
	                        contentBuilder.append("Title: ").append(title);
	                        contentBuilder.append(" - Date: ").append(date);
	                        contentBuilder.append(" - Source: ").append(source);
	                        contentBuilder.append(" - Text: ").append(text);
	
	                        metadataMap.put("date", date);
	                        metadataMap.put("source", source);
	                        metadataMap.put("link", link);
	                        
	                        Metadata metadata = new Metadata(metadataMap);
	                        Document doc = Document.from(contentBuilder.toString(), metadata);
	                        
	                        results.add(doc);
	                        if (results.size() >= maxResults) {
	                            break;
	                        }
                        }
                    }
                }
            }
            
            logger.info("Parsed {} news results from SerpAPI response", results.size());
            return results;
            
        } catch (JsonProcessingException e) {
            logger.error("Error parsing SerpAPI JSON response: {}", e.getMessage(), e);
            return results;
        }
    }
    
    /**
     * Extracts the text from a URL.
     *
     * @param url the URL to extract from
     * @return the plain text
     */
    private String textOfPage(String URL) {
    	try {
	    	String html = Jsoup.connect(URL)
	                .userAgent("Mozilla/5.0")
	                .timeout(10000)
	                .get()
	                .html();
	
			Readability4J readability = new Readability4J(URL, html);
			Article article = readability.parse();
			
			return article.getTextContent();
		
		}catch (IOException e) {
			System.err.println("Error al leer la URL: " + URL);
			e.printStackTrace();
			return "";
		}

	}
}