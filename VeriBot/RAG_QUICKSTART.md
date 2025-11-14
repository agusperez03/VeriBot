# RAG Quick Start Guide

## Prerequisites
```powershell
# 1. Start ChromaDB
cd VeriBot
docker-compose up -d

# 2. Verify ChromaDB is running
curl http://localhost:8000/api/v1/heartbeat
```

## How It Works

### Architecture Flow
```
User Query
    ↓
NewsVerificationService
    ↓
┌─────────────────────────────────────┐
│ If NEW topic:                       │
│  1. Search news (SerpAPI)           │
│  2. Embed documents (ChromaDB)      │
│  3. Generate summary                │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ If FOLLOW-UP question:              │
│  1. Retrieve context (RAG)          │
│  2. Enhance prompt with context     │
│  3. Generate enhanced response      │
└─────────────────────────────────────┘
```

## Example Conversation

### Initial Query
```
User: "Tell me about the recent climate summit"

VeriBot:
- Searches web for news articles
- Embeds 5 articles into ChromaDB
- Analyzes content
- Returns: Summary, Truthfulness %, Justification
```

### Follow-up Questions (uses RAG)
```
User: "What countries participated?"

VeriBot:
- Detects same topic (no new search)
- Queries vector DB for relevant passages
- Retrieves top 5 most relevant chunks
- Generates answer using retrieved context

User: "What were the main agreements?"

VeriBot:
- Again uses RAG to find specific details
- Returns detailed answer from embedded articles
```

## Code Integration Points

### 1. UserContext.java
```java
// RAG service is initialized per user session
this.ragService = new RAGService();
this.verificationService = new NewsVerificationService(
    openAIConfig, searchService, ragService, sessionId
);
```

### 2. NewsVerificationService.java
```java
// After searching for news, embed documents
List<Document> searchResults = searchService.searchNews(...);
conversationSession.setCurrentNewsDocuments(searchResults);
int storedCount = ragService.storeNewsDocuments(searchResults, sessionId);
conversationSession.markDocumentsAsEmbedded();
```

### 3. Follow-up Response with RAG
```java
// Retrieve relevant context
List<String> contexts = ragService.retrieveRelevantContext(userQuery, sessionId);
String relevantContext = ragService.formatContextForPrompt(contexts);

// Add to prompt
String prompt = String.format(
    "Context: %s\nQuestion: %s\nAnswer:",
    relevantContext, userQuery
);
```

## Configuration

### ChromaDB Settings
```java
// In RAGService.java
private static final String CHROMA_BASE_URL = "http://localhost:8000";
private static final String COLLECTION_NAME = "news_verification";
```

### Retrieval Parameters
```java
private static final int MAX_RESULTS = 5;        // Top-K results
private static final double MIN_SCORE = 0.6;     // Similarity threshold
```

## Testing RAG

### 1. Check ChromaDB
```powershell
# View logs
docker logs veribot-chromadb

# Verify container is running
docker ps | findstr chromadb
```

### 2. Test Embedding
```java
// Documents stored with metadata
Metadata metadata = new Metadata();
metadata.put("sessionId", sessionId);
metadata.put("timestamp", System.currentTimeMillis());
TextSegment segment = TextSegment.from(doc.text(), metadata);
```

### 3. Test Retrieval
```java
// Query returns top similar documents for session
List<String> contexts = ragService.retrieveRelevantContext(
    "What were the controversies?",
    "session-123"
);
```

## Troubleshooting

### ChromaDB Not Running
```powershell
# Error: Connection refused
# Solution:
docker-compose up -d
```

### No Context Retrieved
```
# Check if documents were embedded
logger.info("Stored {} documents", storedCount);

# Verify session ID matches
logger.info("Retrieving context for session: {}", sessionId);
```

### Low Quality Retrieval
```java
// Adjust similarity threshold
private static final double MIN_SCORE = 0.5;  // Lower = more results

// Increase max results
private static final int MAX_RESULTS = 10;
```

## Key Classes

| Class | Responsibility |
|-------|---------------|
| `RAGService` | Embedding & retrieval operations |
| `NewsVerificationService` | Orchestrates RAG workflow |
| `ConversationSession` | Tracks embedded documents |
| `UserContext` | Manages per-user RAG service |

## Benefits

✅ **Better Context**: Follow-up questions have access to full article details  
✅ **No Re-searching**: Save API calls by using cached embeddings  
✅ **Accurate Answers**: Retrieve exact passages relevant to questions  
✅ **Conversation Flow**: Natural back-and-forth about news events  
✅ **Session Isolation**: Each user has separate embedded documents  

## Next Steps

1. Start ChromaDB: `docker-compose up -d`
2. Run VeriBot: `.\run-veribot.bat`
3. Ask about news: "What happened at the G20 summit?"
4. Ask follow-ups: "Who attended?", "What was discussed?"
5. Observe RAG in action in the logs!
