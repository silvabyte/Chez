# Custom Providers in ChezWiz

**ChezWiz** provides dedicated support for LM Studio (local LLM server) and custom OpenAI-compatible endpoints through specialized providers.

## LM Studio Provider

LM Studio is a desktop application for running LLMs locally. ChezWiz includes a dedicated `LMStudioProvider` optimized for local model serving.

### Features

✅ **No authentication required** - Runs locally without API keys  
✅ **HTTP/1.1 by default** - Optimized for local network compatibility  
✅ **Structured output support** - Uses OpenAI's `json_schema` format  
✅ **Automatic model validation** - Accepts any model loaded in LM Studio  
✅ **Simple configuration** - Minimal setup required

### Quick Start

```scala
import chezwiz.agent.{Agent, RequestMetadata}
import chezwiz.agent.providers.LMStudioProvider

// Create LM Studio provider with default settings
val provider = LMStudioProvider()  // Defaults to http://localhost:1234/v1

// Or specify custom URL and model
val customProvider = LMStudioProvider(
  baseUrl = "http://localhost:8080/v1",
  modelId = "qwen2.5-coder-7b-instruct"
)

// Create an agent
val agent = Agent(
  name = "LocalAssistant",
  instructions = "You are a helpful AI assistant running locally.",
  provider = provider,
  model = "local-model"
)

// Use the agent
val metadata = RequestMetadata(userId = Some("user123"))

agent.generateText("Hello! Tell me about yourself.", metadata) match {
  case Right(response) => println(response.content)
  case Left(error) => println(s"Error: $error")
}
```

### Configuration

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `baseUrl` | `String` | LM Studio server URL | `"http://localhost:1234/v1"` |
| `modelId` | `String` | Default model ID | `"local-model"` |

### Structured Output

LM Studio supports OpenAI's `json_schema` format for structured output:

```scala
import upickle.default.*
import chez.derivation.*

case class Analysis(
  @Schema.description("Summary of the text")
  summary: String,
  
  @Schema.description("Sentiment: positive, negative, or neutral")
  sentiment: String,
  
  @Schema.description("Key topics identified")
  topics: List[String],
  
  @Schema.description("Confidence score from 0.0 to 1.0")
  @Schema.minimum(0.0)
  @Schema.maximum(1.0)
  confidence: Double
) derives Schema, ReadWriter

val provider = LMStudioProvider()
val agent = Agent(
  name = "Analyzer",
  instructions = "You analyze text and provide structured insights.",
  provider = provider,
  model = "local-model"
)

agent.generateObject[Analysis](
  "Analyze this: The new product launch was incredibly successful!",
  RequestMetadata()
) match {
  case Right(response) =>
    val analysis = response.data
    println(s"Sentiment: ${analysis.sentiment}")
    println(s"Confidence: ${analysis.confidence}")
    println(s"Topics: ${analysis.topics.mkString(", ")}")
  case Left(error) =>
    println(s"Error: $error")
}
```

## Custom Endpoint Provider

For other OpenAI-compatible APIs, use the `CustomEndpointProvider`:

### Features

✅ **OpenAI-compatible API support**  
✅ **Configurable authentication**  
✅ **Custom headers support**  
✅ **HTTP/2 by default** with HTTP/1.1 option  
✅ **Model whitelisting**

### Examples

#### Basic Usage

```scala
import chezwiz.agent.providers.CustomEndpointProvider

// Create provider for OpenAI-compatible API
val provider = CustomEndpointProvider(
  baseUrl = "https://api.example.com/v1",
  apiKey = "your-api-key"
)

val agent = Agent(
  name = "Assistant",
  instructions = "You are a helpful assistant.",
  provider = provider,
  model = "gpt-3.5-turbo"
)
```

#### With Custom Headers

```scala
val provider = CustomEndpointProvider(
  baseUrl = "https://api.company.com/v1",
  apiKey = sys.env("API_KEY"),
  customHeaders = Map(
    "X-Organization" -> "my-org",
    "X-Project" -> "my-project"
  )
)
```

#### Model Whitelisting

```scala
val provider = CustomEndpointProvider(
  baseUrl = "https://api.example.com/v1",
  apiKey = "key",
  supportedModels = List("model-a", "model-b", "model-c")
)

// This will succeed
agent.generateText("Hello", RequestMetadata(), model = "model-a")

// This will fail with ModelNotSupported error
agent.generateText("Hello", RequestMetadata(), model = "model-x")
```

#### Local Network with HTTP/1.1

```scala
val provider = CustomEndpointProvider(
  baseUrl = "http://192.168.1.100:8080/v1",
  apiKey = "", // No auth for local endpoint
  httpVersion = HttpVersion.Http11
)
```

### Configuration

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `baseUrl` | `String` | API endpoint base URL (required) | - |
| `apiKey` | `String` | API key for authentication (required) | - |
| `supportedModels` | `List[String]` | Allowed models (empty = any) | `List.empty` |
| `customHeaders` | `Map[String, String]` | Additional request headers | `Map.empty` |
| `httpVersion` | `HttpVersion` | HTTP protocol version | `Http2` |

## Complete Examples

### Example 1: Chat with Conversation History

```scala
val provider = LMStudioProvider()
val agent = Agent(
  name = "ChatBot",
  instructions = "You are a friendly conversational assistant.",
  provider = provider,
  model = "local-model"
)

val metadata = RequestMetadata(
  userId = Some("user123"),
  conversationId = Some("chat-001")
)

// First message
agent.generateText("My name is Alice", metadata)

// Follow-up uses conversation history
agent.generateText("What's my name?", metadata) match {
  case Right(response) => 
    println(response.content) // Should remember "Alice"
  case Left(error) => 
    println(s"Error: $error")
}
```

### Example 2: Code Generation with Custom Endpoint

```scala
val provider = CustomEndpointProvider(
  baseUrl = "https://api.codegen.com/v1",
  apiKey = sys.env("CODEGEN_API_KEY"),
  supportedModels = List("codegen-16b", "codegen-6b")
)

val agent = Agent(
  name = "CodeGenerator",
  instructions = "You are an expert programmer.",
  provider = provider,
  model = "codegen-16b",
  temperature = Some(0.2),
  maxTokens = Some(2000)
)

agent.generateText(
  "Write a Scala function to merge two sorted lists",
  RequestMetadata()
) match {
  case Right(response) => 
    println(s"Generated code:\n${response.content}")
  case Left(error) => 
    println(s"Error: $error")
}
```

### Example 3: Monitoring with Hooks

```scala
import chezwiz.agent.*

// Create monitoring hooks
val requestLogger = new PreRequestHook {
  def execute(context: PreRequestContext): Unit = {
    println(s"[${context.timestamp}] Request to ${context.provider}")
  }
}

val responseLogger = new PostResponseHook {
  def execute(context: PostResponseContext): Unit = {
    context.response match {
      case Right(_) => 
        println(s"[${context.timestamp}] Success in ${context.durationMs}ms")
      case Left(error) => 
        println(s"[${context.timestamp}] Failed: $error")
    }
  }
}

val hooks = HookRegistry.empty
  .withPreRequestHook(requestLogger)
  .withPostResponseHook(responseLogger)

// Create agent with hooks
val provider = LMStudioProvider()
val agent = Agent(
  name = "MonitoredAgent",
  instructions = "You are a helpful assistant.",
  provider = provider,
  model = "local-model",
  hooks = hooks
)

// Requests will be logged automatically
agent.generateText("Hello", RequestMetadata())
```

## Error Handling

Both providers return detailed error information:

```scala
agent.generateText("Hello", metadata) match {
  case Right(response) => 
    println(s"Success: ${response.content}")
    
  case Left(error) => error match {
    case ChezError.NetworkError(msg, statusCode) =>
      println(s"Network error: $msg")
      statusCode.foreach(code => println(s"HTTP Status: $code"))
      
    case ChezError.ParseError(msg) =>
      println(s"Failed to parse response: $msg")
      
    case ChezError.ApiError(msg, code, statusCode) =>
      println(s"API error: $msg")
      code.foreach(c => println(s"Error code: $c"))
      
    case ChezError.ModelNotSupported(model, provider, supported) =>
      println(s"Model '$model' not supported by $provider")
      println(s"Supported models: ${supported.mkString(", ")}")
      
    case _ =>
      println(s"Unexpected error: $error")
  }
}
```

## Best Practices

### 1. Environment Variables
```scala
val provider = LMStudioProvider(
  baseUrl = sys.env.getOrElse("LM_STUDIO_URL", "http://localhost:1234/v1"),
  modelId = sys.env.getOrElse("LM_STUDIO_MODEL", "local-model")
)
```

### 2. Resource Management
```scala
// Use the same agent instance for multiple requests
val agent = Agent(name = "Assistant", provider = provider, model = "model")

// Reuse for efficiency
(1 to 10).foreach { i =>
  agent.generateText(s"Question $i", metadata)
}
```

### 3. Error Recovery
```scala
def retryWithBackoff[T](
  fn: => Either[ChezError, T], 
  maxRetries: Int = 3
): Either[ChezError, T] = {
  def attempt(retriesLeft: Int, delay: Long): Either[ChezError, T] = {
    fn match {
      case Right(result) => Right(result)
      case Left(error: ChezError.NetworkError) if retriesLeft > 0 =>
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

## Troubleshooting

### LM Studio Connection Issues

**Problem**: `NetworkError("Failed to connect to localhost:1234")`

**Solutions**:
- Verify LM Studio is running and server is started
- Check the correct port in LM Studio settings
- Ensure no firewall is blocking the connection
- Try `curl http://localhost:1234/v1/models` to test

**Problem**: `ParseError("Failed to parse LM Studio response")`

**Solutions**:
- Update LM Studio to the latest version
- Check if the model is fully loaded
- Verify the response format in LM Studio logs

### Custom Endpoint Issues

**Problem**: `ModelNotSupported("gpt-4", "CustomEndpoint", ["gpt-3.5"])`

**Solutions**:
- Check supported models with provider configuration
- Leave `supportedModels` empty to allow any model
- Verify model availability at the endpoint

**Problem**: `ApiError("Invalid API key", code = "invalid_api_key")`

**Solutions**:
- Verify API key is correct
- Check if API key needs specific format (Bearer token, etc.)
- Ensure API key has required permissions

## Migration from CustomEndpointProvider.forLMStudio

If you were using the old API:

```scala
// Old way
val provider = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://localhost:1234/v1",
  modelId = "local-model"
)

// New way
val provider = LMStudioProvider(
  baseUrl = "http://localhost:1234/v1",
  modelId = "local-model"
)
```

The new `LMStudioProvider` is simpler and optimized specifically for LM Studio.

## Supported LLM Servers

### Works with LMStudioProvider
- **LM Studio** - Primary target, fully supported

### Works with CustomEndpointProvider
- **Ollama** (with OpenAI compatibility layer)
- **LocalAI**
- **Text Generation WebUI** (with API)
- **vLLM**
- **FastChat**
- **Any OpenAI-compatible API**

## Advanced Topics

### Custom Response Parsing

If you need to handle non-standard responses, you can extend the providers:

```scala
class MyCustomProvider(baseUrl: String, apiKey: String) 
    extends CustomEndpointProvider(baseUrl, apiKey) {
  
  override protected def parseResponse(responseBody: String): Either[ChezError, ChatResponse] = {
    // Custom parsing logic
    super.parseResponse(responseBody)
  }
}
```

### Performance Optimization

For high-throughput scenarios:

```scala
// Use connection pooling with custom endpoint
val provider = CustomEndpointProvider(
  baseUrl = "https://api.example.com/v1",
  apiKey = "key",
  httpVersion = HttpVersion.Http2 // Better for concurrent requests
)

// Process requests in parallel
val requests = (1 to 100).map { i =>
  Future {
    agent.generateText(s"Request $i", RequestMetadata())
  }
}

val results = Await.result(Future.sequence(requests), 5.minutes)
```