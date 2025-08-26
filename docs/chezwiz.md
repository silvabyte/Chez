# ChezWiz: LLM Agent Library for Scala 3

**ChezWiz** is a type-safe LLM agent library for Scala 3 that provides structured text generation and conversation management with multi-provider support (OpenAI, Anthropic) and built-in JSON schema validation using the Chez library.

## Features

✅ **Multi-provider LLM support** (OpenAI, Anthropic, Custom Endpoints)  
✅ **Custom endpoint support** for local LLMs (LM Studio, Ollama) and OpenAI-compatible APIs  
✅ **Type-safe structured object generation**  
✅ **Vector embeddings support** for semantic search and similarity  
✅ **Scoped conversation history** (tenant/user/conversation isolation)  
✅ **Built-in JSON schema validation** via Chez integration  
✅ **Conversation persistence and management**  
✅ **Error handling with comprehensive error types**  
✅ **Configurable temperature and token limits**  
✅ **Hooks and metrics** for request/response monitoring

## Quick Start

### Basic Agent Creation

```scala
import chezwiz.agent.*

// Create an OpenAI agent
val openAIAgent = AgentFactory.createOpenAIAgent(
  name = "MyAssistant",
  instructions = "You are a helpful assistant that provides concise answers.",
  apiKey = "your-openai-api-key",
  model = "gpt-4o",
  temperature = Some(0.7),
  maxTokens = Some(1000)
) match {
  case Right(agent) => agent
  case Left(error) => throw new RuntimeException(s"Failed to create agent: $error")
}

// Create an Anthropic agent
val anthropicAgent = AgentFactory.createAnthropicAgent(
  name = "ClaudeAssistant",
  instructions = "You are Claude, an AI assistant.",
  apiKey = "your-anthropic-api-key",
  model = "claude-3-5-sonnet-20241022",
  temperature = Some(0.5)
)
```

For custom providers and local LLMs, see the [Custom Providers Guide](custom-providers.md).

### Text Generation

All ChezWiz methods require metadata for conversation scoping:

```scala
// Create metadata for your conversation scope
val metadata = RequestMetadata(
  tenantId = Some("acme-corp"),
  userId = Some("john-doe"),
  conversationId = Some("support-chat-123")
)

// Simple text generation with conversation history
val response1 = agent.generateText("What is the capital of France?", metadata)
val response2 = agent.generateText("What's the population of that city?", metadata)

// Text generation without history (stateless)
val response3 = agent.generateTextWithoutHistory("Translate 'hello' to Spanish", metadata)

response1 match {
  case Right(chatResponse) =>
    println(s"Response: ${chatResponse.content}")
    chatResponse.usage.foreach(u => println(s"Tokens used: ${u.totalTokens}")
  case Left(error) =>
    println(s"Error: $error")
}
```

### Structured Object Generation

```scala
import chez.derivation.Schema

// Define your data structure
case class WeatherReport(
  @Schema.description("The city name")
  city: String,

  @Schema.description("Temperature in Celsius")
  @Schema.minimum(-50)
  @Schema.maximum(60)
  temperature: Int,

  @Schema.description("Weather conditions")
  conditions: String,

  @Schema.description("Humidity percentage")
  @Schema.minimum(0)
  @Schema.maximum(100)
  humidity: Int
) derives Schema

// Generate structured data
val weatherResponse = agent.generateObject[WeatherReport](
  "What's the current weather in Tokyo? Respond with structured data.",
  metadata
)

weatherResponse match {
  case Right(objectResponse) =>
    val weather = objectResponse.data
    println(s"Weather in ${weather.city}: ${weather.temperature}°C, ${weather.conditions}")
  case Left(error) =>
    println(s"Failed to generate weather data: $error")
}
```

## Scoped Conversation History

ChezWiz supports multi-tenant conversation scoping using metadata to isolate conversations by tenant, user, and conversation ID.

### Basic Scoping

```scala
import chezwiz.agent.RequestMetadata

// Create metadata for scoping
val metadata = RequestMetadata(
  tenantId = Some("acme-corp"),
  userId = Some("john-doe"),
  conversationId = Some("support-chat-123")
)

// Each scope maintains separate conversation history
val response1 = agent.generateText("Hello, I need help with my account", metadata)
val response2 = agent.generateText("What was my previous question?", metadata)

// Different scope = different conversation
val otherMetadata = RequestMetadata(
  tenantId = Some("acme-corp"),
  userId = Some("jane-smith"),
  conversationId = Some("sales-inquiry-456")
)

val response3 = agent.generateText("Hello, I'm interested in your products", otherMetadata)
```

### Partial Scoping

You can provide partial metadata - missing fields are filled with `"_"`:

```scala
// Only tenant and user, no specific conversation
val metadata = RequestMetadata(
  tenantId = Some("acme-corp"),
  userId = Some("john-doe"),
  conversationId = None  // Will use "_"
)

// Only user scoping
val userOnlyMetadata = RequestMetadata(
  userId = Some("john-doe")
  // tenantId and conversationId will be "_"
)
```

### History Management

```scala
// Get conversation history for a specific scope
val history = agent.getConversationHistory(metadata)

// Clear history for a specific scope
agent.clearHistory(metadata)

// Clear all histories across all scopes
agent.clearAllHistories()

// Add messages manually to a specific scope
agent.addChatMessage(
  ChatMessage(Role.User, "Custom message"),
  metadata
)
```

## Conversation Scope Keys

The library creates hierarchical scope keys from metadata:

- `"default"` - No metadata provided (backward compatibility)
- `"acme:john:chat123"` - Full scoping: tenant=acme, user=john, conversation=chat123
- `"acme:_:chat123"` - Partial: tenant=acme, no user, conversation=chat123
- `"_:john:_"` - Only user provided: user=john
- `"acme:john:_"` - Tenant and user, no specific conversation

## Error Handling

ChezWiz provides comprehensive error types:

```scala
import chezwiz.agent.ChezError

val result = agent.generateText("Hello world")

result match {
  case Right(response) =>
    // Success
    println(response.content)

  case Left(ChezError.NetworkError(message, statusCode)) =>
    println(s"Network error: $message (status: $statusCode)")

  case Left(ChezError.ApiError(message, code, statusCode)) =>
    println(s"API error: $message")

  case Left(ChezError.ParseError(message, cause)) =>
    println(s"Parse error: $message")

  case Left(ChezError.ModelNotSupported(model, provider, supportedModels)) =>
    println(s"Model $model not supported by $provider. Supported: ${supportedModels.mkString(", ")}")

  case Left(ChezError.SchemaConversionError(message, targetType)) =>
    println(s"Schema conversion failed for $targetType: $message")

  case Left(ChezError.ConfigurationError(message)) =>
    println(s"Configuration error: $message")
}
```

## Supported Models

### OpenAI Models

- `gpt-4o`
- `gpt-4o-mini`
- `gpt-4-turbo`
- `gpt-3.5-turbo`

### Anthropic Models

- `claude-3-5-sonnet-20241022`
- `claude-3-5-haiku-20241022`
- `claude-3-opus-20240229`
- `claude-3-sonnet-20240229`
- `claude-3-haiku-20240307`

## Advanced Usage

### Custom System Instructions

```scala
// Change system instructions for a new agent instance
val customAgent = agent.withSystemChatMessage(
  "You are a technical documentation expert. Provide detailed, accurate answers."
)
```

### Conversation Management

```scala
// Get current conversation history
val messages = agent.getConversationHistory()

// Check message roles and content
messages.foreach { message =>
  println(s"${message.role}: ${message.content}")
}

// Clear conversation and start fresh
agent.clearHistory()
```

### Usage Tracking

```scala
val response = agent.generateText("Hello")
response.foreach { chatResponse =>
  chatResponse.usage.foreach { usage =>
    println(s"Prompt tokens: ${usage.promptTokens}")
    println(s"Completion tokens: ${usage.completionTokens}")
    println(s"Total tokens: ${usage.totalTokens}")
  }
}
```

## Integration with Chez Schema System

ChezWiz leverages the Chez schema system for structured generation:

```scala
// Complex nested structures work seamlessly
case class Address(
  @Schema.description("Street address")
  street: String,
  city: String,
  @Schema.pattern("^[0-9]{5}$")
  zipCode: String
) derives Schema

case class Person(
  @Schema.description("Full name")
  @Schema.minLength(1)
  name: String,

  @Schema.description("Age in years")
  @Schema.minimum(0)
  @Schema.maximum(150)
  age: Int,

  address: Address,

  @Schema.description("Email addresses")
  emails: List[String]
) derives Schema

// Generate complex structured data
val personResponse = agent.generateObject[Person](
  "Create a person profile for a software engineer in San Francisco",
  metadata
)
```

## Hook System for Observability and Custom Logic

ChezWiz provides a comprehensive hook system that allows you to inject custom logic at key points in the agent lifecycle for observability, logging, metrics collection, and business logic.

### Available Hook Types

All hooks extend the base `AgentHook` trait:

- **PreRequestHook** - Before sending requests to LLM providers
- **PostResponseHook** - After receiving responses from LLM providers
- **PreObjectRequestHook** - Before sending object generation requests
- **PostObjectResponseHook** - After receiving object generation responses
- **ErrorHook** - When errors occur in any operation
- **HistoryHook** - When conversation history changes
- **ScopeChangeHook** - When conversation scope changes

### Basic Hook Usage

```scala
import chezwiz.agent.*

// Create a simple logging hook
class LoggingHook extends AgentHook with PreRequestHook with PostResponseHook with ErrorHook {
  override def onPreRequest(context: PreRequestContext): Unit = {
    println(s"[${context.agentName}] Starting request with ${context.request.messages.size} messages")
  }

  override def onPostResponse(context: PostResponseContext): Unit = {
    context.response match {
      case Right(response) =>
        println(s"[${context.agentName}] Success: ${context.duration}ms, ${response.usage.map(_.totalTokens).getOrElse(0)} tokens")
      case Left(error) =>
        println(s"[${context.agentName}] Error: $error")
    }
  }

  override def onError(context: ErrorContext): Unit = {
    println(s"[${context.agentName}] Operation '${context.operation}' failed: ${context.error}")
  }
}

// Register hooks with agent
val hooks = HookRegistry.empty
  .addPreRequestHook(new LoggingHook())
  .addPostResponseHook(new LoggingHook())
  .addErrorHook(new LoggingHook())

val agent = AgentFactory.createOpenAIAgent(
  name = "Monitored Agent",
  instructions = "You are a helpful assistant.",
  apiKey = "your-api-key",
  model = "gpt-4o-mini",
  hooks = hooks
).toOption.get
```

### Metrics Collection Hook

```scala
import java.util.concurrent.atomic.AtomicLong

class MetricsHook extends AgentHook with PreRequestHook with PostResponseHook with ErrorHook {
  private val requestCount = new AtomicLong(0)
  private val successCount = new AtomicLong(0)
  private val errorCount = new AtomicLong(0)
  private val totalDuration = new AtomicLong(0)

  override def onPreRequest(context: PreRequestContext): Unit = {
    requestCount.incrementAndGet()
  }

  override def onPostResponse(context: PostResponseContext): Unit = {
    context.response match {
      case Right(_) =>
        successCount.incrementAndGet()
        totalDuration.addAndGet(context.duration)
      case Left(_) =>
        errorCount.incrementAndGet()
    }
  }

  override def onError(context: ErrorContext): Unit = {
    errorCount.incrementAndGet()
  }

  def getStats: String = {
    val requests = requestCount.get()
    val successes = successCount.get()
    val errors = errorCount.get()
    val avgDuration = if (successes > 0) totalDuration.get().toDouble / successes else 0.0

    s"Requests: $requests, Success: $successes, Errors: $errors, Avg Duration: ${avgDuration.round}ms"
  }
}

// Usage
val metricsHook = new MetricsHook()
val hooks = HookRegistry.empty
  .addPreRequestHook(metricsHook)
  .addPostResponseHook(metricsHook)
  .addErrorHook(metricsHook)

// After some agent usage...
println(metricsHook.getStats)
```

### OpenTelemetry-Style Tracing Hook

```scala
case class Span(
  traceId: String,
  operationName: String,
  startTime: Long,
  endTime: Option[Long] = None,
  attributes: Map[String, String] = Map.empty
)

class TracingHook extends AgentHook with PreRequestHook with PostResponseHook {
  private val spans = mutable.ArrayBuffer[Span]()

  override def onPreRequest(context: PreRequestContext): Unit = {
    val span = Span(
      traceId = java.util.UUID.randomUUID().toString,
      operationName = "generateText",
      startTime = context.timestamp,
      attributes = Map(
        "agent.name" -> context.agentName,
        "model" -> context.model,
        "tenant.id" -> context.metadata.tenantId.getOrElse("_")
      )
    )
    spans += span
  }

  override def onPostResponse(context: PostResponseContext): Unit = {
    // Find and complete the span
    spans.lastOption.foreach { span =>
      val completedSpan = span.copy(
        endTime = Some(context.responseTimestamp),
        attributes = span.attributes + ("duration.ms" -> context.duration.toString)
      )
      spans(spans.length - 1) = completedSpan
    }
  }

  def getTraces: List[Span] = spans.toList
}
```

### History Analytics Hook

```scala
class HistoryAnalyticsHook extends AgentHook with HistoryHook {
  private val operationCounts = mutable.Map[HistoryOperation, Int]()

  override def onHistoryChange(context: HistoryContext): Unit = {
    val currentCount = operationCounts.getOrElse(context.operation, 0)
    operationCounts(context.operation) = currentCount + 1

    println(s"History ${context.operation} for ${context.agentName}: size=${context.historySize}")
  }

  def getAnalytics: Map[HistoryOperation, Int] = operationCounts.toMap
}
```

### Multiple Hooks and Composition

```scala
// You can register multiple hooks of the same type
val hooks = HookRegistry.empty
  .addPreRequestHook(new LoggingHook())
  .addPreRequestHook(new MetricsHook())
  .addPreRequestHook(new TracingHook())
  .addPostResponseHook(new LoggingHook())
  .addPostResponseHook(new MetricsHook())
  .addPostResponseHook(new TracingHook())
  .addErrorHook(new LoggingHook())
  .addErrorHook(new MetricsHook())
  .addHistoryHook(new HistoryAnalyticsHook())

// Hooks are executed in the order they were registered
val agent = Agent(
  name = "Comprehensive Agent",
  instructions = "You are a monitored assistant.",
  provider = new OpenAIProvider("your-api-key"),
  model = "gpt-4o-mini",
  hooks = hooks
)
```

### Hook Context Data

Each hook receives rich context data:

```scala
// PreRequestContext provides:
context.agentName        // Agent name
context.model           // Model being used
context.request         // Full ChatRequest object
context.metadata        // RequestMetadata with tenant/user/conversation info
context.timestamp       // Request timestamp

// PostResponseContext provides:
context.response        // Either[ChezError, ChatResponse]
context.duration        // Response time in milliseconds
context.requestTimestamp // When request started
context.responseTimestamp // When response completed

// ErrorContext provides:
context.error          // The ChezError that occurred
context.operation      // Operation that failed (e.g., "generateText")
context.request        // Optional request that caused the error

// HistoryContext provides:
context.operation      // HistoryOperation (Add, Clear, ClearAll)
context.message        // Optional ChatMessage (for Add operations)
context.historySize    // Current history size
context.metadata       // Scope metadata
```

### Hook Safety and Error Handling

Hooks are designed to be safe and non-intrusive:

- **Hook failures don't break agent functionality** - If a hook throws an exception, it's caught and logged, but the agent continues working
- **Hooks execute in order** - Multiple hooks of the same type execute in registration order
- **Zero overhead when unused** - If no hooks are registered, there's minimal performance impact

```scala
// Even if this hook always fails, the agent will continue working
class FailingHook extends AgentHook with PreRequestHook {
  override def onPreRequest(context: PreRequestContext): Unit = {
    throw new RuntimeException("This hook always fails!")
  }
}

val agent = Agent(
  name = "Resilient Agent",
  instructions = "I work even with failing hooks.",
  provider = provider,
  model = "gpt-4o-mini",
  hooks = HookRegistry.empty.addPreRequestHook(new FailingHook())
)

// This will still work - the hook failure is logged but doesn't break the agent
agent.generateText("Hello", metadata) // ✅ Works fine
```

For more comprehensive examples, see [ExampleHooks.scala](../ChezWiz/src/main/scala/chezwiz/ExampleHooks.scala).

## Vector Embeddings

ChezWiz provides comprehensive support for vector embeddings, enabling semantic search, similarity analysis, and document clustering capabilities. Embeddings are supported through both local providers (LM Studio) and cloud providers (OpenAI, upcoming).

### Quick Start with Embeddings

```scala
import chezwiz.agent.*

// Create agent with embedding-capable provider
val provider = new LMStudioProvider(
  baseUrl = "http://localhost:1234/v1",
  modelId = "text-embedding-qwen3-embedding-8b"
)

val agent = Agent(
  name = "EmbeddingAgent",
  instructions = "Generate embeddings for semantic analysis",
  provider = provider,
  model = "text-embedding-qwen3-embedding-8b"
)

// Generate single embedding
val text = "ChezWiz is a type-safe LLM agent library for Scala 3"
agent.generateEmbedding(text) match {
  case Right(response) =>
    println(s"Generated ${response.dimensions}-dimensional embedding")
    println(s"Tokens used: ${response.usage.map(_.totalTokens).getOrElse(0)}")
  case Left(error) =>
    println(s"Error: $error")
}
```

### Batch Embeddings

Process multiple texts efficiently in a single request:

```scala
val texts = List(
  "Scala is a functional programming language",
  "Type safety prevents runtime errors",
  "LLM agents can generate structured data"
)

agent.generateEmbeddings(texts) match {
  case Right(response) =>
    println(s"Generated ${response.embeddings.size} embeddings")
    response.embeddings.zip(texts).foreach { case (embedding, text) =>
      println(s"Text: '${text.take(30)}...' -> ${embedding.values.size} dimensions")
    }
  case Left(error) =>
    println(s"Error: $error")
}
```

### Semantic Similarity Search

Find the most similar texts using cosine similarity:

```scala
// Generate embeddings for documents
val documents = List(
  "Machine learning models require training data",
  "Neural networks learn patterns from examples",
  "Scala combines object-oriented and functional paradigms",
  "Type inference reduces boilerplate code"
)

val queryText = "deep learning algorithms"

// Generate embeddings
val docsResponse = agent.generateEmbeddings(documents)
val queryResponse = agent.generateEmbedding(queryText)

(docsResponse, queryResponse) match {
  case (Right(docs), Right(query)) =>
    val queryEmbedding = query.embeddings.head.values

    // Calculate similarities
    val similarities = docs.embeddings.zip(documents).map {
      case (docEmbedding, text) =>
        val similarity = agent.cosineSimilarity(queryEmbedding, docEmbedding.values)
        (text, similarity)
    }

    // Find most similar
    val topMatches = similarities.sortBy(-_._2).take(3)
    println("Top 3 most similar documents:")
    topMatches.foreach { case (text, score) =>
      println(f"  Score: $score%.3f - ${text.take(50)}...")
    }

  case _ => println("Error generating embeddings")
}
```

### Document Clustering

Create similarity matrices for document clustering:

```scala
val documents = List(
  "Machine learning requires data",
  "Neural networks learn patterns",
  "Scala functional programming",
  "Type safety in Scala"
)

agent.generateEmbeddings(documents) match {
  case Right(response) =>
    // Build similarity matrix
    val embeddings = response.embeddings.map(_.values)

    println("Similarity Matrix:")
    for (i <- embeddings.indices) {
      for (j <- embeddings.indices) {
        val similarity = if (i == j) 1.0
          else agent.cosineSimilarity(embeddings(i), embeddings(j))
        print(f"$similarity%.2f ")
      }
      println()
    }

    // Find clusters (documents with similarity > threshold)
    val threshold = 0.7
    for {
      i <- documents.indices
      j <- (i + 1) until documents.size
      similarity = agent.cosineSimilarity(embeddings(i), embeddings(j))
      if similarity > threshold
    } {
      println(f"Cluster found (similarity: $similarity%.3f):")
      println(s"  - ${documents(i)}")
      println(s"  - ${documents(j)}")
    }

  case Left(error) => println(s"Error: $error")
}
```

### Embedding Message Types

ChezWiz uses type-safe message types for embeddings:

```scala
import chezwiz.agent.{EmbeddingInput, EmbeddingRequest, EmbeddingResponse}

// Single text input
val singleInput = EmbeddingInput.Single("Text to embed")

// Multiple texts input
val multipleInput = EmbeddingInput.Multiple(List("Text 1", "Text 2", "Text 3"))

// Full request with options
val request = EmbeddingRequest(
  input = singleInput,
  model = "text-embedding-qwen3-embedding-8b",
  dimensions = Some(1024),  // Optional: specify dimensions
  encodingFormat = "float",  // "float" or "base64"
  metadata = Some(RequestMetadata(
    tenantId = Some("my-tenant"),
    userId = Some("user-123")
  ))
)
```

### Embedding Hooks

Monitor and track embedding operations with hooks:

```scala
import chezwiz.agent.*

class EmbeddingMonitorHook extends AgentHook
  with PreEmbeddingHook
  with PostEmbeddingHook {

  override def onPreEmbedding(context: PreEmbeddingContext): Unit = {
    println(s"[${context.agentName}] Generating embedding for ${context.inputSize} texts")
    println(s"  Model: ${context.model}")
    println(s"  Tenant: ${context.metadata.tenantId.getOrElse("default")}")
  }

  override def onPostEmbedding(context: PostEmbeddingContext): Unit = {
    context.response match {
      case Right(response) =>
        println(s"[${context.agentName}] Generated ${response.embeddings.size} embeddings")
        println(s"  Dimensions: ${response.dimensions}")
        println(s"  Duration: ${context.duration}ms")
        response.usage.foreach { usage =>
          println(s"  Tokens: ${usage.totalTokens}")
        }
      case Left(error) =>
        println(s"[${context.agentName}] Embedding failed: $error")
    }
  }
}

// Register hooks with agent
val hooks = HookRegistry.empty
  .addPreEmbeddingHook(new EmbeddingMonitorHook())
  .addPostEmbeddingHook(new EmbeddingMonitorHook())

val agent = Agent(
  name = "MonitoredEmbeddingAgent",
  instructions = "Generate embeddings with monitoring",
  provider = provider,
  model = "text-embedding-qwen3-embedding-8b",
  hooks = hooks
)
```

### Supported Embedding Models

#### LM Studio (Local)

- Any embedding model loaded in LM Studio
- Recommended: `text-embedding-qwen3-embedding-8b`
- Other options: `nomic-embed-text`, `bge-large`, `e5-large`

#### OpenAI (Coming Soon)

- `text-embedding-3-large` (3072 dimensions)
- `text-embedding-3-small` (1536 dimensions)
- `text-embedding-ada-002` (1536 dimensions)

### Configuration

Configure embedding providers via environment variables or `.env` file:

```bash
# LM Studio configuration
LM_STUDIO_URL=http://localhost:1234/v1
LM_STUDIO_EMBEDDING_MODEL=text-embedding-qwen3-embedding-8b

# OpenAI configuration (coming soon)
OPENAI_API_KEY=your-api-key
OPENAI_EMBEDDING_MODEL=text-embedding-3-large
```

### Best Practices

1. **Batch Processing**: Use `generateEmbeddings` for multiple texts to reduce API calls
2. **Caching**: Cache embeddings for frequently accessed texts
3. **Dimension Selection**: Choose appropriate dimensions based on your use case
4. **Model Selection**: Use smaller models for speed, larger for accuracy
5. **Normalization**: Embeddings are pre-normalized for cosine similarity
6. **Token Limits**: Be aware of model token limits when embedding long texts

### Performance Considerations

```scala
// Efficient batch processing
val largeBatch = (1 to 1000).map(i => s"Document $i").toList

// Process in chunks to avoid token limits
val chunkSize = 100
val chunks = largeBatch.grouped(chunkSize)

val allEmbeddings = chunks.flatMap { chunk =>
  agent.generateEmbeddings(chunk) match {
    case Right(response) => Some(response.embeddings)
    case Left(error) =>
      println(s"Error processing chunk: $error")
      None
  }
}.flatten.toList

println(s"Processed ${allEmbeddings.size} embeddings")
```

### Example: Building a Semantic Search System

```scala
case class Document(
  id: String,
  content: String,
  embedding: Option[Vector[Float]] = None
)

class SemanticSearchIndex(agent: Agent) {
  private var documents = Map[String, Document]()

  def addDocument(id: String, content: String): Unit = {
    agent.generateEmbedding(content) match {
      case Right(response) =>
        val embedding = response.embeddings.head.values
        documents = documents + (id -> Document(id, content, Some(embedding)))
        println(s"Added document $id to index")
      case Left(error) =>
        println(s"Failed to index document $id: $error")
    }
  }

  def search(query: String, topK: Int = 5): List[(Document, Float)] = {
    agent.generateEmbedding(query) match {
      case Right(response) =>
        val queryEmbedding = response.embeddings.head.values

        documents.values.toList
          .filter(_.embedding.isDefined)
          .map { doc =>
            val similarity = agent.cosineSimilarity(
              queryEmbedding,
              doc.embedding.get
            )
            (doc, similarity)
          }
          .sortBy(-_._2)
          .take(topK)

      case Left(error) =>
        println(s"Search failed: $error")
        List.empty
    }
  }
}

// Usage
val searchIndex = new SemanticSearchIndex(agent)

// Add documents
searchIndex.addDocument("doc1", "Scala is a functional programming language")
searchIndex.addDocument("doc2", "Machine learning requires large datasets")
searchIndex.addDocument("doc3", "Type safety helps prevent bugs")

// Search
val results = searchIndex.search("functional programming", topK = 2)
results.foreach { case (doc, score) =>
  println(f"Score: $score%.3f - ${doc.content}")
}
```

## Built-in Metrics System

ChezWiz provides a comprehensive, production-ready metrics system out of the box. Get detailed observability into your agent performance with zero configuration.

### Quick Start with Metrics

```scala
import chezwiz.agent.*

// Create an agent with automatic metrics collection
val (agent, metrics) = MetricsFactory.createOpenAIAgentWithMetrics(
  name = "ProductionAgent",
  instructions = "You are a helpful assistant.",
  apiKey = "your-api-key",
  model = "gpt-4o"
).toOption.get

val metadata = RequestMetadata(
  tenantId = Some("my-company"),
  userId = Some("user-123"),
  conversationId = Some("support-chat")
)

// Use the agent normally - metrics are collected automatically
agent.generateText("Hello, how can I help?", metadata)
agent.generateObject[MyDataClass]("Generate some data", metadata)

// Get comprehensive metrics
val snapshot = metrics.getSnapshot("ProductionAgent").get
println(snapshot.summary)

// Export for monitoring systems
println(snapshot.toPrometheusFormat) // Prometheus metrics
println(snapshot.toJson)             // JSON format
```

### Metrics Data Available

The metrics system tracks:

- **Request Counts**: Total, successful, failed requests
- **Performance**: Average, min, max response times
- **Token Usage**: By model and operation type
- **Error Analysis**: Error types, frequencies, recent errors
- **Scope Analytics**: Usage by tenant/user/conversation
- **Model Usage**: Requests and tokens per model
- **Rate Information**: Recent request and error rates

### Using Global Metrics

```scala
// All agents created with MetricsFactory share a global metrics instance
val (agent1, _) = MetricsFactory.createOpenAIAgentWithMetrics(
  name = "Agent1", instructions = "First agent", apiKey = "key", model = "gpt-4o"
).toOption.get

val (agent2, _) = MetricsFactory.createAnthropicAgentWithMetrics(
  name = "Agent2", instructions = "Second agent", apiKey = "key", model = "claude-3-5-sonnet-20241022"
).toOption.get

// Get metrics for all agents
val allMetrics = MetricsFactory.getAllMetrics
allMetrics.foreach { case (agentName, snapshot) =>
  println(s"$agentName: ${snapshot.totalRequests} requests")
}

// Export all metrics for monitoring
val prometheusMetrics = MetricsFactory.exportPrometheus
val jsonMetrics = MetricsFactory.exportJson
```

### Custom Metrics Instance

```scala
// Create your own metrics instance for isolation
val customMetrics = new DefaultAgentMetrics()

val (agent, _) = MetricsFactory.createOpenAIAgentWithMetrics(
  name = "IsolatedAgent",
  instructions = "This agent has separate metrics",
  apiKey = "your-api-key",
  model = "gpt-4o",
  customMetrics = Some(customMetrics)
).toOption.get

// This metrics instance is separate from the global one
val snapshot = customMetrics.getSnapshot("IsolatedAgent")
```

### Advanced Metrics Usage

```scala
// Get detailed breakdown
val snapshot = metrics.getSnapshot("MyAgent").get

// Operation-specific metrics
println(s"Text Generation: ${snapshot.textGenerations.count} requests")
println(s"  Success Rate: ${snapshot.textGenerations.successRate}")
println(s"  Avg Duration: ${snapshot.textGenerations.averageDuration}ms")

println(s"Object Generation: ${snapshot.objectGenerations.count} requests")
println(s"  Token Usage: ${snapshot.objectGenerations.totalTokens}")

// Model breakdown
snapshot.modelMetrics.foreach { case (model, metrics) =>
  println(s"Model $model: ${metrics.requestCount} requests, ${metrics.totalTokens} tokens")
}

// Scope analysis (tenant/user/conversation)
snapshot.scopeMetrics.foreach { case (scope, metrics) =>
  println(s"Scope $scope: ${metrics.messageCount} messages, ${metrics.totalTokens} tokens")
}

// Error analysis
snapshot.errorMetrics.foreach { case (errorType, metrics) =>
  println(s"$errorType: ${metrics.count} occurrences")
}

// Recent activity
println(s"Recent request rate: ${snapshot.recentRequestRate} req/min")
println(s"Collection time: ${(snapshot.snapshotTime - snapshot.collectionStartTime) / 1000}s")
```

### Prometheus Integration

```scala
// Export metrics in Prometheus format
val prometheusMetrics = snapshot.toPrometheusFormat

// Example output:
// # HELP chezwiz_agent_requests_total Total number of requests
// # TYPE chezwiz_agent_requests_total counter
// chezwiz_agent_requests_total{agent="MyAgent"} 1543
//
// # HELP chezwiz_agent_success_rate Request success rate (0-1)
// # TYPE chezwiz_agent_success_rate gauge
// chezwiz_agent_success_rate{agent="MyAgent"} 0.987

// Set up HTTP endpoint for Prometheus scraping
import cask.main.*

@cask.get("/metrics")
def metrics() = {
  cask.Response(
    data = MetricsFactory.exportPrometheus,
    headers = Seq("Content-Type" -> "text/plain")
  )
}
```

### JSON Export for Dashboards

```scala
// Export as JSON for custom dashboards
val jsonData = snapshot.toJson

// Example structure:
// {
//   "agentName": "MyAgent",
//   "totalRequests": 1543,
//   "successfulRequests": 1523,
//   "failedRequests": 20,
//   "averageDuration": 247.5,
//   "successRate": 0.987,
//   "modelMetrics": {
//     "gpt-4o": {
//       "requestCount": 1200,
//       "totalTokens": 45000
//     }
//   },
//   "scopeMetrics": { ... },
//   "errorMetrics": { ... }
// }
```

### Combining Metrics with Custom Hooks

```scala
// Add custom hooks alongside automatic metrics
class CustomAlertHook extends AgentHook with ErrorHook {
  override def onError(context: ErrorContext): Unit = {
    if (context.error.isInstanceOf[ChezError.NetworkError]) {
      // Send alert to your monitoring system
      sendSlackAlert(s"Network error in ${context.agentName}")
    }
  }
}

val customHooks = HookRegistry.empty.addErrorHook(new CustomAlertHook())

val (agent, metrics) = MetricsFactory.createOpenAIAgentWithMetrics(
  name = "MonitoredAgent",
  instructions = "You are a monitored assistant.",
  apiKey = "your-api-key",
  model = "gpt-4o",
  additionalHooks = customHooks // Your custom hooks + automatic metrics
).toOption.get
```

### Thread Safety and Performance

The metrics system is designed for production use:

- **Thread-safe**: Uses atomic operations and concurrent collections
- **High performance**: Minimal overhead with lock-free operations
- **Memory efficient**: Bounded collections with automatic cleanup
- **Zero-overhead**: No performance impact when metrics aren't accessed

### Metrics Reset and Management

```scala
// Reset metrics for a specific agent
metrics.reset("MyAgent")

// Reset all metrics
metrics.resetAll()

// Check if agent has metrics
val hasMetrics = metrics.getSnapshot("MyAgent").isDefined
```

## Best Practices

1. **Always provide metadata** - All ChezWiz methods require `RequestMetadata` for conversation scoping
2. **Use scoped conversations** for multi-tenant applications to ensure data isolation
3. **Handle errors gracefully** using the comprehensive error types
4. **Set appropriate temperature and token limits** based on your use case
5. **Use structured generation** when you need predictable output formats
6. **Clear conversation history** when starting new logical conversations
7. **Monitor token usage** to manage costs effectively
8. **Use built-in metrics** - Use `MetricsFactory` for instant observability with Prometheus and JSON export
9. **Monitor key metrics** - Track success rates, response times, token usage, and error patterns
10. **Use hooks for custom logic** - Implement logging, tracing, and business logic hooks alongside metrics
11. **Keep hooks lightweight** - Hooks should be fast and non-blocking to avoid impacting agent performance
12. **Handle hook failures gracefully** - Don't let hook implementation bugs break your core agent functionality

## Examples

See the [Examples.scala](../ChezWiz/src/main/scala/chezwiz/Examples/Examples.scala) file for more comprehensive usage examples.
