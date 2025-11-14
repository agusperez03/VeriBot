# VeriBot - News Verification and Summarization Agent

VeriBot is an AI-powered news verification and summarization agent built in Java using LangChain4j and Azure OpenAI. It helps users understand and verify news content by providing concise summaries and truthfulness assessments.

## Features

- **News Verification**: Analyzes news content and provides an estimated truthfulness percentage
- **Content Summarization**: Creates brief, understandable summaries of news articles
- **Source Tracking**: Lists the sources used during the verification process
- **Input Validation**: Detects and redirects non-news-related queries
- **RAG (Retrieval-Augmented Generation)**: Embeds news articles and provides enhanced responses for follow-up questions
- **Conversation Context**: Maintains context across multiple questions about the same news event

## How It Works

VeriBot follows a structured process:

1. **Input Validation**: Verifies that the user's query is related to news content
2. **Search Query Generation**: Creates an optimized search query from the user's input
3. **Information Retrieval**: Searches trusted sources using Azure Bing Search API
4. **Content Analysis**: Processes the retrieved information using Azure OpenAI LLMs
5. **Result Generation**: Provides a summary, truthfulness percentage, and justification

## Requirements

- Java 21 or higher
- Maven
- Azure OpenAI API access
- SerpAPI access (for news search)
- Docker (for ChromaDB - RAG feature)

## Setup

### 1. Prerequisites

Install required software:
- Java 21+
- Maven
- Docker (for ChromaDB)

### 2. Environment Configuration

1. Clone the repository
2. Create a `.env` file in the project root with the following variables:

```
# Azure OpenAI Configuration
AZURE_OPENAI_ENDPOINT=https://your-azure-openai-instance.openai.azure.com/
AZURE_OPENAI_API_KEY=your-azure-openai-api-key
AZURE_OPENAI_DEPLOYMENT_NAME=your-deployment-name
AZURE_OPENAI_API_VERSION=2023-05-15

# SerpAPI Configuration (for news search)
SERPAPI_API_KEY=your-serpapi-key
```

### 3. Start ChromaDB (for RAG feature)

```powershell
cd VeriBot
docker-compose up -d
```

For detailed RAG setup instructions, see [RAG_SETUP.md](RAG_SETUP.md).

### 4. Build the Project

3. Build the project using Maven:

```
mvn clean package
```

## Usage

Run the application using:

```
java -jar target/veribot-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Example queries:
- "What happened yesterday in New York?"
- "Is it true that [some claim]?"
- "Give me the latest news on climate change"
- "What's going on with the elections in Brazil?"

**Follow-up questions** (uses RAG to retrieve relevant context):
- "What were the main points?"
- "Who was involved?"
- "What happened next?"
- "Were there any controversies?"

## Sample Output

```json
{
  "summary": "The news is about...",
  "truthfulness": "83%",
  "justification": "Matches reports from Reuters and AFP, though lacks detail on X.",
  "sources_used": ["Reuters", "AFP", "Snopes"]
}
```

## Project Structure

- `config/`: Configuration classes for Azure services
- `model/`: Data models including NewsVerificationResult, ConversationSession, UserContext
- `service/`: Core services for searching, verifying news content, and RAG operations
  - `NewsSearchService`: Searches for news using SerpAPI
  - `NewsVerificationService`: Analyzes and verifies news content
  - `RAGService`: Handles document embedding and retrieval using ChromaDB
  - `ConversationService`: Manages user conversations and context
- `VeriBot.java`: Main application entry point with CLI interface
- `docker-compose.yml`: ChromaDB container configuration
- `RAG_SETUP.md`: Detailed RAG setup and usage guide

## Truthfulness Scale

VeriBot uses a four-tier truthfulness scale:
- **High (90-100%)**: Content is well-supported by multiple reliable sources
- **Moderate (60-89%)**: Content is mostly accurate with some uncertainties
- **Doubtful (30-59%)**: Content has significant inconsistencies or lacks verification
- **Low (0-29%)**: Content is mostly unsupported or contradicted by reliable sources

## Development Guidelines

- Follow clean code principles and SOLID design
- Document public APIs with Javadoc
- Use proper error handling and logging
- Write comprehensive tests for services and models

## License

[MIT License](LICENSE)