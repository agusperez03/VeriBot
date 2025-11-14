# RAG Setup Guide for VeriBot

This guide explains how to set up and use the RAG (Retrieval-Augmented Generation) feature in VeriBot using ChromaDB.

## Overview

VeriBot now includes a RAG workflow that:
1. **Embeds scraped news articles** into a vector database (ChromaDB)
2. **Retrieves relevant context** when users ask follow-up questions
3. **Enhances responses** with detailed information from the original articles

## Prerequisites

- Docker installed on your system
- Java 21
- Maven
- Running VeriBot application

## Quick Start

### 1. Start ChromaDB

VeriBot uses ChromaDB as the vector database. You can run it using Docker:

#### Using Docker Compose (Recommended)

```powershell
# Navigate to the VeriBot directory
cd VeriBot

# Start ChromaDB
docker-compose up -d

# Check if it's running
docker ps
```

You should see the `veribot-chromadb` container running on port 8000.

#### Using Docker Run (Alternative)

```powershell
docker run -d --name veribot-chromadb -p 8000:8000 -v chroma_data:/chroma/chroma chromadb/chroma:latest
```

### 2. Verify ChromaDB is Running

You can check if ChromaDB is accessible:

```powershell
curl http://localhost:8000/api/v1/heartbeat
```

Or open in browser: http://localhost:8000

### 3. Build and Run VeriBot

```powershell
# Build the project
mvn clean package

# Run VeriBot
.\run-veribot.bat
```

## How RAG Works in VeriBot

### Document Embedding Flow

1. **User queries news**: "What happened with the recent election?"
2. **VeriBot searches for news** using SerpAPI
3. **Scraped articles are embedded** into ChromaDB with:
   - Text content from the articles
   - Metadata (source, URL, session ID, timestamp)
   - Vector embeddings (using AllMiniLM-L6-v2 model)

### Retrieval Flow

1. **User asks follow-up question**: "What were the main controversies?"
2. **RAG retrieves relevant passages** from embedded articles
3. **Context is added to the prompt** for the LLM
4. **Enhanced response** is generated using retrieved information

## Architecture

```
User Query
    ↓
NewsVerificationService
    ↓
NewsSearchService (SerpAPI)
    ↓
RAGService.storeNewsDocuments()
    ↓
ChromaDB (Vector Store)
    ↓
[Follow-up Question]
    ↓
RAGService.retrieveRelevantContext()
    ↓
Enhanced Response with Retrieved Context
```

## Configuration

### ChromaDB Connection

The RAG service connects to ChromaDB with these default settings:

```java
// In RAGService.java
private static final String CHROMA_BASE_URL = "http://localhost:8000";
private static final String COLLECTION_NAME = "news_verification";
```

To customize, you can modify these values or create a constructor overload.

### Embedding Model

VeriBot uses **AllMiniLM-L6-v2** for embeddings:
- Fast and efficient
- Runs locally (no API key required)
- Good quality for semantic search
- Multilingual support

### Retrieval Parameters

```java
// In RAGService.java
private static final int MAX_RESULTS = 5;        // Maximum documents to retrieve
private static final double MIN_SCORE = 0.6;     // Minimum similarity score
```

## Managing ChromaDB

### Stop ChromaDB

```powershell
docker-compose down
```

### Stop and Remove Data

```powershell
docker-compose down -v
```

### View Logs

```powershell
docker logs veribot-chromadb
```

### Access ChromaDB Admin (if available)

Some ChromaDB versions include a web UI. Check at: http://localhost:8000

## Testing RAG

### Example Conversation

1. **Initial Query**:
   ```
   User: "Tell me about the recent climate summit"
   VeriBot: [Searches news, embeds articles, provides summary]
   ```

2. **Follow-up Questions** (uses RAG):
   ```
   User: "What countries participated?"
   VeriBot: [Retrieves relevant context from embedded articles]
   
   User: "What were the main agreements?"
   VeriBot: [Retrieves specific details about agreements]
   
   User: "Were there any controversies?"
   VeriBot: [Finds relevant passages about controversies]
   ```

## Troubleshooting

### ChromaDB Connection Error

**Error**: `Failed to connect to ChromaDB`

**Solution**:
- Ensure ChromaDB is running: `docker ps`
- Check port 8000 is available: `netstat -an | findstr :8000`
- Restart ChromaDB: `docker-compose restart`

### Embedding Model Download

**Error**: First run might download the embedding model

**Solution**: Be patient, the AllMiniLM-L6-v2 model will be downloaded automatically on first use (~80MB)

### Out of Memory

**Error**: Java heap space error with large documents

**Solution**: Increase JVM memory:
```powershell
set JAVA_OPTS=-Xmx2g -Xms512m
mvn spring-boot:run
```

## Advanced Configuration

### Custom ChromaDB Settings

Edit `docker-compose.yml` to customize:

```yaml
environment:
  - IS_PERSISTENT=TRUE          # Persist data across restarts
  - ANONYMIZED_TELEMETRY=FALSE  # Disable telemetry
  - CHROMA_SERVER_AUTH_CREDENTIALS=admin:password  # Add authentication
```

### Using Remote ChromaDB

To use a remote ChromaDB instance, update the `UserContext.java`:

```java
this.ragService = new RAGService("http://your-chroma-server:8000", "news_verification");
```

## Performance Tips

1. **Document Chunking**: Large articles are stored as-is. For very long articles, consider implementing chunking in `RAGService.storeNewsDocuments()`

2. **Batch Processing**: The service already batches embeddings for efficiency

3. **Cache Embeddings**: ChromaDB persists embeddings, so they're available across application restarts

4. **Session Cleanup**: Implement periodic cleanup of old sessions to manage storage

## API Reference

### RAGService Methods

```java
// Store news documents
int storeNewsDocuments(List<Document> documents, String sessionId)

// Retrieve relevant context
List<String> retrieveRelevantContext(String query, String sessionId)
List<String> retrieveRelevantContext(String query, String sessionId, int maxResults)

// Format context for prompts
String formatContextForPrompt(List<String> contexts)

// Clear session documents
void clearSessionDocuments(String sessionId)
```

## Integration Points

- **NewsVerificationService**: Stores documents after news search
- **ConversationSession**: Tracks embedded documents
- **UserContext**: Initializes RAG service per session

## Future Enhancements

Potential improvements for the RAG system:

1. **Document Chunking**: Split large articles into smaller chunks
2. **Metadata Filtering**: Filter by date, source, or topic
3. **Hybrid Search**: Combine vector search with keyword search
4. **Reranking**: Use cross-encoder for better relevance
5. **Session Cleanup**: Automatic cleanup of old embeddings
6. **Analytics**: Track which documents are most relevant

## Resources

- [ChromaDB Documentation](https://docs.trychroma.com/)
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [AllMiniLM-L6-v2 Model](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)

## Support

For issues or questions:
1. Check ChromaDB logs: `docker logs veribot-chromadb`
2. Check VeriBot logs in `logs/` directory
3. Verify ChromaDB health: http://localhost:8000/api/v1/heartbeat
