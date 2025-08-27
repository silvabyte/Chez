# OpenAI-Compatible Providers in ChezWiz

ChezWiz provides a unified `OpenAICompatibleProvider` for connecting to any OpenAI-compatible API, including local model servers like LM Studio, LLamaCPP, Ollama, and cloud services.

## Installation

### Mill

```scala
mvn"com.silvabyte::chez:0.2.0"
mvn"com.silvabyte::chezwiz:0.2.0"
```

### SBT

```scala
libraryDependencies ++= Seq(
  "com.silvabyte" %% "chez" % "0.2.0",
  "com.silvabyte" %% "chezwiz" % "0.2.0"
)
```

## Quick Start

```scala
import chezwiz.agent.{Agent, RequestMetadata}
import chezwiz.agent.providers.OpenAICompatibleProvider

// LM Studio (local, no auth)
val lmStudioProvider = OpenAICompatibleProvider(
  baseUrl = "http://localhost:1234/v1",
  modelId = "local-model",
  strictModelValidation = false
)

// Custom cloud endpoint (with auth)
val cloudProvider = OpenAICompatibleProvider(
  baseUrl = "https://api.example.com/v1",
  apiKey = "your-api-key",
  modelId = "gpt-3.5-turbo"
)

// Create agent
val agent = Agent(
  name = "Assistant",
  instructions = "You are a helpful AI assistant.",
  provider = lmStudioProvider,
  model = "local-model"
)

// Use it
agent.generateText("Hello!", RequestMetadata()) match {
  case Right(response) => println(response.content)
  case Left(error) => println(s"Error: $error")
}
```

## Configuration Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `baseUrl` | `String` | Required | API endpoint URL |
| `apiKey` | `String` | `""` | API key (empty for no auth) |
| `modelId` | `String` | `"local-model"` | Default model to use |
| `supportedModels` | `List[String]` | `List.empty` | Allowed models (empty = any) |
| `customHeaders` | `Map[String, String]` | `Map.empty` | Additional headers |
| `enableEmbeddings` | `Boolean` | `false` | Enable embedding support |
| `strictModelValidation` | `Boolean` | `true` | Enforce model validation |
| `httpVersion` | `HttpVersion` | `Http2` | HTTP protocol version |
| `timeouts` | `ProviderTimeouts` | Default | Connection/request timeouts |

## Common Use Cases

### Local Model Servers

```scala
// LM Studio
val lmStudio = OpenAICompatibleProvider(
  baseUrl = "http://localhost:1234/v1",
  modelId = "qwen2.5-coder-7b",
  strictModelValidation = false  // Local models may have custom names
)

// LLamaCPP
val llamaCpp = OpenAICompatibleProvider(
  baseUrl = "http://localhost:8080/v1",
  modelId = "llama-3.2-3b",
  strictModelValidation = false,
  httpVersion = HttpVersion.Http11  // Some local servers prefer HTTP/1.1
)

// Ollama (with OpenAI compatibility)
val ollama = OpenAICompatibleProvider(
  baseUrl = "http://localhost:11434/v1",
  modelId = "llama3.2",
  strictModelValidation = false
)
```

### Cloud Endpoints

```scala
// Custom endpoint with authentication
val customCloud = OpenAICompatibleProvider(
  baseUrl = "https://api.company.com/v1",
  apiKey = sys.env("API_KEY"),
  modelId = "company-model-v2",
  customHeaders = Map(
    "X-Organization" -> "my-org",
    "X-Project" -> "my-project"
  )
)

// With model validation
val restrictedEndpoint = OpenAICompatibleProvider(
  baseUrl = "https://api.example.com/v1",
  apiKey = "key",
  supportedModels = List("model-a", "model-b"),
  strictModelValidation = true  // Only allow listed models
)
```

## Structured Output

Generate typed responses using Chez schemas:

```scala
import upickle.default.*
import chez.derivation.*

case class Analysis(
  @Schema.description("Brief summary")
  summary: String,
  
  @Schema.description("Sentiment: positive, negative, or neutral")
  sentiment: String,
  
  @Schema.description("Confidence from 0.0 to 1.0")
  @Schema.minimum(0.0)
  @Schema.maximum(1.0)
  confidence: Double
) derives Schema, ReadWriter

val agent = Agent(
  name = "Analyzer",
  instructions = "Analyze text and provide structured insights.",
  provider = OpenAICompatibleProvider("http://localhost:1234/v1"),
  model = "local-model"
)

agent.generateObject[Analysis](
  "The product launch exceeded all expectations!",
  RequestMetadata()
) match {
  case Right(response) =>
    println(s"Sentiment: ${response.data.sentiment}")
    println(s"Confidence: ${response.data.confidence}")
  case Left(error) =>
    println(s"Error: $error")
}
```

## Embeddings

For providers that support embeddings:

```scala
val embeddingProvider = OpenAICompatibleProvider(
  baseUrl = "http://localhost:1234/v1",
  modelId = "text-embedding-model",
  enableEmbeddings = true
)

embeddingProvider.generateEmbedding(
  EmbeddingRequest(
    input = EmbeddingInput.Text("Vector databases are useful"),
    model = "text-embedding-model"
  )
) match {
  case Right(response) =>
    println(s"Embedding dimension: ${response.data.head.embedding.length}")
  case Left(error) =>
    println(s"Error: $error")
}
```

## Error Handling

```scala
agent.generateText("Hello", metadata) match {
  case Right(response) => 
    println(response.content)
    
  case Left(error) => error match {
    case ChezError.NetworkError(msg, statusCode) =>
      println(s"Network error: $msg (status: ${statusCode.getOrElse("unknown")})")
      
    case ChezError.ParseError(msg) =>
      println(s"Failed to parse response: $msg")
      
    case ChezError.ApiError(msg, code, _) =>
      println(s"API error: $msg (code: ${code.getOrElse("unknown")})")
      
    case ChezError.ModelNotSupported(model, provider, supported) =>
      println(s"Model '$model' not supported. Available: ${supported.mkString(", ")}")
      
    case _ => println(s"Unexpected error: $error")
  }
}
```

## Advanced Examples

### Conversation with History

```scala
val agent = Agent(
  name = "ChatBot",
  instructions = "You are a friendly conversational assistant.",
  provider = OpenAICompatibleProvider("http://localhost:1234/v1"),
  model = "local-model"
)

val metadata = RequestMetadata(
  userId = Some("user123"),
  conversationId = Some("chat-001")  // Enables conversation history
)

// Messages automatically maintain context
agent.generateText("My name is Alice", metadata)
agent.generateText("What's my name?", metadata) match {
  case Right(response) => println(response.content)  // Should remember "Alice"
  case Left(error) => println(s"Error: $error")
}
```

### With Monitoring Hooks

```scala
import chezwiz.agent.*

val requestLogger = new PreRequestHook {
  def execute(context: PreRequestContext): Unit = {
    println(s"[${context.timestamp}] Request to ${context.provider}")
  }
}

val responseLogger = new PostResponseHook {
  def execute(context: PostResponseContext): Unit = {
    val duration = context.durationMs
    context.response match {
      case Right(_) => println(s"Success in ${duration}ms")
      case Left(error) => println(s"Failed after ${duration}ms: $error")
    }
  }
}

val agent = Agent(
  name = "MonitoredAgent",
  instructions = "You are a helpful assistant.",
  provider = OpenAICompatibleProvider("http://localhost:1234/v1"),
  model = "local-model",
  hooks = HookRegistry.empty
    .withPreRequestHook(requestLogger)
    .withPostResponseHook(responseLogger)
)
```

### Retry with Backoff

```scala
def retryWithBackoff[T](
  fn: => Either[ChezError, T], 
  maxRetries: Int = 3
): Either[ChezError, T] = {
  def attempt(retriesLeft: Int, delay: Long): Either[ChezError, T] = {
    fn match {
      case Right(result) => Right(result)
      case Left(_: ChezError.NetworkError) if retriesLeft > 0 =>
        Thread.sleep(delay)
        attempt(retriesLeft - 1, delay * 2)
      case Left(error) => Left(error)
    }
  }
  attempt(maxRetries, 1000)
}

// Use with retry
retryWithBackoff {
  agent.generateText("Hello", metadata)
}
```

## Supported Servers

The `OpenAICompatibleProvider` works with:

- **LM Studio** - Local model server
- **LLamaCPP** - High-performance local inference
- **Ollama** - Local model management
- **LocalAI** - OpenAI-compatible local API
- **vLLM** - High-throughput serving
- **Text Generation WebUI** - With API enabled
- **FastChat** - Multi-model serving
- Any OpenAI-compatible API endpoint

## Migration Guide

If upgrading from older versions:

```scala
// Old approach (deprecated)
import chezwiz.agent.providers.{LMStudioProvider, CustomEndpointProvider}

val lmStudio = LMStudioProvider(baseUrl, modelId)
val custom = CustomEndpointProvider(baseUrl, apiKey)

// New unified approach
import chezwiz.agent.providers.OpenAICompatibleProvider

val provider = OpenAICompatibleProvider(
  baseUrl = baseUrl,
  apiKey = apiKey,  // Optional, defaults to ""
  modelId = modelId,
  strictModelValidation = false  // For local models
)
```

## Troubleshooting

### Connection Issues
- Verify server is running: `curl http://localhost:1234/v1/models`
- Check firewall settings
- For local servers, use `httpVersion = HttpVersion.Http11` if needed

### Model Errors
- Set `strictModelValidation = false` for local models with custom names
- Leave `supportedModels` empty to allow any model
- Check available models at `/v1/models` endpoint

### Authentication
- Local servers typically don't need `apiKey` (leave as `""`)
- Cloud endpoints may require Bearer token format
- Use `customHeaders` for non-standard auth schemes

## Best Practices

1. **Environment Configuration**
   ```scala
   val provider = OpenAICompatibleProvider(
     baseUrl = sys.env.getOrElse("LLM_URL", "http://localhost:1234/v1"),
     apiKey = sys.env.getOrElse("LLM_API_KEY", ""),
     modelId = sys.env.getOrElse("LLM_MODEL", "local-model")
   )
   ```

2. **Reuse Agents**
   ```scala
   // Create once, use many times
   val agent = Agent(name = "Assistant", provider = provider, model = "model")
   
   // Efficient for multiple requests
   val responses = requests.map(req => agent.generateText(req, metadata))
   ```

3. **Handle Errors Gracefully**
   ```scala
   agent.generateText(prompt, metadata) match {
     case Right(response) => processResponse(response)
     case Left(ChezError.NetworkError(_, _)) => fallbackToCache()
     case Left(error) => logError(error)
   }
   ```