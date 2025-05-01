# VeriBot Project Documentation
![veribot_2](https://github.com/user-attachments/assets/94d512bf-017d-4a87-97ce-ffb05b0cba5d)

## Short Description

VeriBot is an AI-powered news verification and summarization agent that helps users validate the truthfulness of news content. It analyzes information from multiple sources, provides concise summaries, and assigns truthfulness ratings on a scale from 0-100%. The system is designed to combat misinformation by providing users with factual, source-verified information about current events.

## Architecture and Implementation Overview

![architecture](https://github.com/user-attachments/assets/f16ff77a-2785-4789-89eb-57a17ec9bc0b)

VeriBot is a news verification and fact-checking application built with a multi-layered architecture:

1. **Presentation Layer**:
    - HTML/JavaScript frontend for web interaction (`main.html`)
    - RESTful API endpoints via Spring Boot (`VeribotController`)
    - Cross-origin configuration to allow web access (`WebConfig`)
2. **Service Layer**:
    - Conversation management and session handling (`ConversationService`)
    - News verification logic (`NewsVerificationService`)
    - External search capabilities (`NewsSearchService`)
3. **Data Layer**:
    - Models for requests/responses (`PromptModel`, `NewsVerificationResult`)
    - User context and session state management (`UserContext`, `ConversationSession`)
    - Configuration classes for external services (`AzureOpenAIConfig`, `SerpApiConfig`)

The application implements a stateful conversation model that maintains context between user interactions, allowing for follow-up questions on the same news topic.

## Data Sources Used

VeriBot integrates with multiple data sources to verify news content:

1. **SerpAPI**: Used for searching online news sources across different countries and languages, providing the initial results for verification.
2. **Web Content**: The system uses Readability4J and Jsoup to extract and parse content from news websites, enabling deeper analysis of full articles.
3. **Azure OpenAI**: Provides advanced natural language understanding capabilities for analyzing news content, determining relevance, and assessing truthfulness.
4. **Location Database**: The application includes a comprehensive google_countries.JSON database with country and language mappings to localize search results based on the user's query context.

## Technologies Used

VeriBot leverages several key technologies:

1. **Spring Boot**: Provides the web framework, dependency injection, and REST API capabilities.
2. **LangChain4j**: Used for AI interactions and document processing.
3. **Azure OpenAI**: Powers the natural language understanding, content analysis, and truthfulness assessment components.
4. **SerpAPI**: Provides the search functionality to find relevant news sources.
5. **Readability4J/Jsoup**: Used to extract clean, readable text content from web pages.
6. **Maven**: Manages dependencies and builds the application.
7. **Log4j2**: Provides logging capabilities throughout the application.
8. **JSON Processing Libraries**: Used for working with API responses and generating structured outputs.

## Target Audience

VeriBot is designed for several audience segments:

1. **General Public**: Individuals seeking to verify news stories and claims they encounter online.
2. **Researchers and Students**: Those who need to validate information sources for academic or research purposes.
3. **Journalists and Content Creators**: Professionals who need to fact-check information before including it in their work.
4. **Information Literacy Educators**: Teachers and trainers who help others develop skills to identify reliable information.
5. **International Users**: The system supports multiple languages and country-specific searches, making it accessible to a global audience.

## Conclusion and Future Works

VeriBot demonstrates a practical approach to combating misinformation through AI-powered verification. The system successfully integrates search capabilities, content extraction, and language understanding to provide users with factual assessments of news content.

### Potential Future Enhancements:

1. **Source Credibility Analysis**: Implementing reputation scoring for news sources to weight information based on source reliability.
2. **Multi-Modal Verification**: Expanding beyond text to verify images, videos, and audio content.
3. **User Feedback Loop**: Incorporating user feedback to improve verification algorithms and address evolving misinformation tactics.
4. **Expanded Language Support**: Enhancing support for more languages and dialects to reach a truly global audience.
5. **Real-Time Monitoring**: Adding capabilities to monitor breaking news and proactively verify trending stories.
6. **Explainable AI**: Improving the transparency of verification processes by providing more detailed explanations of how truthfulness determinations are made.
7. **Mobile Applications**: Developing dedicated mobile apps for easier access on smartphones and tablets.
8. **API Integrations**: Creating APIs for third-party applications to leverage VeriBot's verification capabilities.

The VeriBot project establishes a solid foundation for automated news verification that can be expanded to address the evolving challenges of misinformation in our digital information ecosystem.
