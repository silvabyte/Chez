# Custom LLM Endpoints in ChezWiz

**ChezWiz** supports custom LLM endpoints through the `CustomEndpointProvider`, allowing you to connect to any OpenAI-compatible API, including local LLM servers like LM Studio, Ollama, or your own custom deployments.

## Features

✅ **OpenAI-compatible API support**  
✅ **Local LLM server integration** (LM Studio, Ollama, etc.)  
✅ **Configurable authentication** (API key optional)  
✅ **Custom headers support**  
✅ **HTTP/2 by default** with HTTP/1.1 support for local servers  
✅ **Model validation and whitelisting**  
✅ **Full integration with ChezWiz features** (structured output, conversation history, hooks)

## Quick Start

### Using LM Studio

```scala
import chezwiz.agent.{Agent, RequestMetadata}
import chezwiz.agent.providers.CustomEndpointProvider

// Create provider for LM Studio (runs locally without authentication)
// Note: LM Studio automatically uses HTTP/1.1 for better local compatibility
val provider = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://localhost:1234/v1",
  modelId = "qwen2.5-coder-7b-instruct"  // Optional, defaults to "local-model"
)

// Create an agent
val agent = Agent(
  name = "LocalAssistant",
  instructions = "You are a helpful AI assistant running locally.",
  provider = provider,
  model = "qwen2.5-coder-7b-instruct"
)

// Use the agent
val metadata = RequestMetadata(
  userId = Some("user123")
)

agent.generateText("Hello! Tell me about yourself.", metadata) match {
  case Right(response) => println(response.content)
  case Left(error) => println(s"Error: $error")
}
```

### Using OpenAI-Compatible Endpoints

```scala
// For authenticated OpenAI-compatible endpoints
val provider = CustomEndpointProvider.forOpenAICompatible(
  baseUrl = "https://your-api.com/v1",
  apiKey = "your-api-key",
  supportedModels = List("model1", "model2")
)

// For endpoints with custom headers
val providerWithHeaders = CustomEndpointProvider.forOpenAICompatible(
  baseUrl = "https://api.example.com/v1",
  apiKey = sys.env("CUSTOM_API_KEY"),
  supportedModels = List("gpt-3.5-turbo", "gpt-4"),
  customHeaders = Map(
    "X-Organization" -> "my-org",
    "X-Project" -> "my-project"
  )
)
```

## Configuration Options

### CustomEndpointProvider Parameters

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `apiKey` | `String` | API key for authentication | `""` |
| `baseUrl` | `String` | Base URL of the API endpoint (required) | - |
| `supportedModels` | `List[String]` | List of supported model IDs. Empty list allows any model | `List.empty` |
| `useOpenAIFormat` | `Boolean` | Use OpenAI request/response format | `true` |
| `requiresAuthentication` | `Boolean` | Whether API key is required | `true` |
| `customHeaders` | `Map[String, String]` | Additional headers to include in requests | `Map.empty` |
| `useJsonSchemaFormat` | `Boolean` | Use json_schema format for structured output (LM Studio style) | `false` |
| `httpVersion` | `HttpVersion` | HTTP protocol version (`Http11` or `Http2`). Use `Http11` for local servers | `Http2` |

### Factory Methods

#### `forLMStudio`

Creates a provider optimized for LM Studio with sensible defaults:

```scala
val provider = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://localhost:1234/v1",      // Required
  modelId = "mistral-7b-instruct"            // Optional, defaults to "local-model"
  // httpVersion defaults to Http11 for LM Studio (for local server compatibility)
)
```

**Default configuration:**
- No authentication required (`requiresAuthentication = false`)
- OpenAI-compatible format (`useOpenAIFormat = true`)
- HTTP/1.1 for better local network compatibility (override with `httpVersion` parameter if needed)
- Single model in supported models list
- Uses `json_schema` format for structured output (`useJsonSchemaFormat = true`)

#### `forOpenAICompatible`

Creates a provider for any OpenAI-compatible API:

```scala
val provider = CustomEndpointProvider.forOpenAICompatible(
  baseUrl = "https://api.example.com/v1",    // Required
  apiKey = "sk-...",                         // Required
  supportedModels = List("model1", "model2"), // Optional, defaults to empty (any model)
  customHeaders = Map("X-Org" -> "my-org"),  // Optional
  httpVersion = HttpVersion.Http2,           // Optional, defaults to Http2
  useJsonSchemaFormat = false                // Optional, defaults to false
)
```

**Default configuration:**
- Authentication required (`requiresAuthentication = true`)
- OpenAI-compatible format (`useOpenAIFormat = true`)
- HTTP/2 for better performance with remote endpoints (default for all providers except LM Studio)

## Examples

### Basic Chat with LM Studio

```scala
import chezwiz.agent.{Agent, RequestMetadata}
import chezwiz.agent.providers.{CustomEndpointProvider, HttpVersion}

val provider = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://localhost:1234/v1",
  modelId = "qwen2.5-coder-7b-instruct"
)

val agent = Agent(
  name = "CodingAssistant",
  instructions = "You are a helpful coding assistant specialized in Scala.",
  provider = provider,
  model = "qwen2.5-coder-7b-instruct",
  temperature = Some(0.7),
  maxTokens = Some(1000)
)

val metadata = RequestMetadata(
  userId = Some("developer1"),
  conversationId = Some("coding-session-1")
)

// Chat with history
agent.generateText("Write a Scala function to calculate factorial", metadata) match {
  case Right(response) => 
    println(s"Assistant: ${response.content}")
    response.usage.foreach(u => 
      println(s"Tokens - Prompt: ${u.promptTokens}, Completion: ${u.completionTokens}")
    )
  case Left(error) => 
    println(s"Error: $error")
}

// Follow-up question uses conversation history
agent.generateText("Now make it tail recursive", metadata) match {
  case Right(response) => println(s"Assistant: ${response.content}")
  case Left(error) => println(s"Error: $error")
}
```

### Structured Output Generation

ChezWiz supports structured output with custom endpoints. LM Studio and some other providers use OpenAI's `json_schema` format, which is automatically enabled for LM Studio providers.

```scala
import upickle.default.*
import chez.derivation.*

case class CodeReview(
  @Schema.description("Overall code quality score from 1-10")
  @Schema.minimum(1)
  @Schema.maximum(10)
  score: Int,
  
  @Schema.description("List of issues found")
  issues: List[String],
  
  @Schema.description("List of positive aspects")
  strengths: List[String],
  
  @Schema.description("Suggested improvements")
  suggestions: List[String]
) derives Schema, ReadWriter

val provider = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://localhost:1234/v1"
)

val agent = Agent(
  name = "CodeReviewer",
  instructions = "You are an expert code reviewer.",
  provider = provider,
  model = "local-model"
)

val code = """
def factorial(n: Int): Int = {
  if (n <= 0) 1
  else n * factorial(n - 1)
}
"""

agent.generateObject[CodeReview](
  s"Review this Scala code:\n\n$code",
  RequestMetadata()
) match {
  case Right(response) =>
    val review = response.data
    println(s"Code Score: ${review.score}/10")
    println(s"Issues: ${review.issues.mkString(", ")}")
    println(s"Strengths: ${review.strengths.mkString(", ")}")
    println(s"Suggestions: ${review.suggestions.mkString(", ")}")
  case Left(error) =>
    println(s"Error: $error")
}
```

### Using with Hooks and Metrics

```scala
import chezwiz.agent.*

// Create hooks for monitoring
val requestLogger = new PreRequestHook {
  def execute(context: PreRequestContext): Unit = {
    println(s"[${context.timestamp}] Sending to ${context.model} via custom endpoint")
  }
}

val responseLogger = new PostResponseHook {
  def execute(context: PostResponseContext): Unit = {
    context.response match {
      case Right(resp) => 
        println(s"[${context.timestamp}] Response received in ${context.durationMs}ms")
      case Left(error) => 
        println(s"[${context.timestamp}] Error after ${context.durationMs}ms: $error")
    }
  }
}

val hooks = HookRegistry.empty
  .withPreRequestHook(requestLogger)
  .withPostResponseHook(responseLogger)

val metrics = MetricsRegistry()

// Create provider and agent with hooks
val provider = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://localhost:1234/v1"
)

val agent = Agent(
  name = "MonitoredAgent",
  instructions = "You are a helpful assistant.",
  provider = provider,
  model = "local-model",
  hooks = hooks
)

// Make requests - hooks will log automatically
agent.generateText("Explain quantum computing", RequestMetadata()) match {
  case Right(response) =>
    println(response.content)
    println(s"Total requests: ${metrics.getTotalRequests()}")
    println(s"Success rate: ${metrics.getSuccessRate() * 100}%")
  case Left(error) =>
    println(s"Error: $error")
}
```

### HTTP Version Configuration

```scala
// Default behavior:
// - CustomEndpointProvider defaults to HTTP/2
// - forLMStudio defaults to HTTP/1.1 for local server compatibility
// - forOpenAICompatible defaults to HTTP/2

// LM Studio automatically uses HTTP/1.1
val lmStudioProvider = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://localhost:1234/v1"
  // httpVersion = HttpVersion.Http11 is the default for LM Studio
)

// Override LM Studio to use HTTP/2 (not recommended)
val lmStudioHttp2 = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://localhost:1234/v1",
  httpVersion = HttpVersion.Http2  // Override default
)

// OpenAI-compatible uses HTTP/2 by default
val remoteProvider = CustomEndpointProvider.forOpenAICompatible(
  baseUrl = "https://api.remote.com/v1",
  apiKey = "key"
  // httpVersion = HttpVersion.Http2 is the default
)

// Custom provider with explicit HTTP/1.1 for local network
val customProvider = new CustomEndpointProvider(
  baseUrl = "http://192.168.1.100:8080/v1",
  requiresAuthentication = false,
  httpVersion = HttpVersion.Http11  // Explicitly use HTTP/1.1
  // Without this, it would default to HTTP/2
)
```

## Error Handling

The CustomEndpointProvider provides detailed error information:

```scala
import chezwiz.agent.ChezError

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
      
    case ChezError.ConfigurationError(msg) =>
      println(s"Configuration error: $msg")
      
    case _ =>
      println(s"Unexpected error: $error")
  }
}
```

## Integration with AgentFactory

You can register custom providers with the AgentFactory:

```scala
import chezwiz.agent.AgentFactory

// Create your custom provider
val lmStudioProvider = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://localhost:1234/v1",
  modelId = "local-model"
)

// Create agent using the provider
val agent = AgentFactory.createAgent(
  name = "LocalAgent",
  instructions = "You are a helpful assistant.",
  provider = lmStudioProvider,
  model = "local-model"
) match {
  case Right(agent) => agent
  case Left(error) => throw new RuntimeException(s"Failed to create agent: $error")
}
```

## Best Practices

### 1. Model Validation
Always specify `supportedModels` to enable model validation:

```scala
val provider = CustomEndpointProvider.forOpenAICompatible(
  baseUrl = "https://api.example.com/v1",
  apiKey = "key",
  supportedModels = List("model-a", "model-b")  // Whitelist models
)
```

### 2. Environment Variables
Use environment variables for configuration:

```scala
val provider = CustomEndpointProvider.forLMStudio(
  baseUrl = sys.env.getOrElse("LM_STUDIO_URL", "http://localhost:1234/v1"),
  modelId = sys.env.getOrElse("LM_STUDIO_MODEL", "local-model")
)
```

### 3. Error Recovery
Implement retry logic for transient failures:

```scala
def retryRequest(prompt: String, maxRetries: Int = 3): Either[ChezError, ChatResponse] = {
  var attempts = 0
  var lastError: ChezError = null
  
  while (attempts < maxRetries) {
    agent.generateText(prompt, RequestMetadata()) match {
      case Right(response) => return Right(response)
      case Left(error: ChezError.NetworkError) if attempts < maxRetries - 1 =>
        lastError = error
        attempts += 1
        Thread.sleep(1000 * attempts)  // Exponential backoff
      case Left(error) => return Left(error)
    }
  }
  
  Left(lastError)
}
```

### 4. Local Network Configuration
For local LLM servers, HTTP/1.1 is often more compatible:

```scala
// LM Studio automatically uses HTTP/1.1
val lmStudioProvider = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://192.168.1.100:1234/v1"
  // No need to specify httpVersion, defaults to Http11
)

// For other local servers, explicitly set HTTP/1.1
val localProvider = new CustomEndpointProvider(
  baseUrl = "http://192.168.1.100:8080/v1",
  requiresAuthentication = false,
  httpVersion = HttpVersion.Http11  // Explicit for local compatibility
)
```

### 5. Security Considerations
- Never expose local LLM endpoints to the public internet
- Use HTTPS for remote endpoints
- Store API keys securely (environment variables, secret management)
- Validate SSL certificates for production use

## Structured Output Formats

ChezWiz supports two formats for structured output:

### 1. OpenAI `json_object` Format (Default)
Used by OpenAI and most providers. The model is instructed to return JSON but without strict schema validation.

### 2. OpenAI `json_schema` Format
Used by LM Studio and newer OpenAI models. Provides strict JSON schema validation ensuring the response conforms exactly to your schema.

```scala
// LM Studio automatically uses json_schema format
val lmStudioProvider = CustomEndpointProvider.forLMStudio(
  baseUrl = "http://localhost:1234/v1"
)

// For other providers, enable it explicitly
val customProvider = CustomEndpointProvider.forOpenAICompatible(
  baseUrl = "https://api.example.com/v1",
  apiKey = "key",
  useJsonSchemaFormat = true  // Enable json_schema format
)

// Or create a custom provider
val provider = new CustomEndpointProvider(
  baseUrl = "http://localhost:8080/v1",
  requiresAuthentication = false,
  useJsonSchemaFormat = true  // Enable json_schema format
)
```

When `useJsonSchemaFormat` is true, ChezWiz sends:
```json
{
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "structured_output",
      "strict": "true",
      "schema": { /* Your Chez schema converted to JSON Schema */ }
    }
  }
}
```

## Troubleshooting

### Connection Refused
```
Error: NetworkError("Failed to connect to localhost:1234: Connection refused")
```
**Solutions:**
- Verify LM Studio/server is running
- Check the correct port is used
- Ensure firewall allows connections

### Model Not Found
```
Error: ModelNotSupported("gpt-4", "CustomEndpoint", List("model1", "model2"))
```
**Solutions:**
- Check available models: `curl http://localhost:1234/v1/models`
- Update `supportedModels` list or leave empty to allow any model
- Ensure model ID matches exactly (case-sensitive)

### Timeout Errors
```
Error: NetworkError("Request to http://localhost:1234/v1/chat/completions timed out")
```
**Solutions:**
- Local models may need time to load initially
- Consider increasing timeout for first requests
- Check if model size is appropriate for your hardware

### Parse Errors
```
Error: ParseError("Failed to parse custom endpoint response: missing field 'choices'")
```
**Solutions:**
- Verify endpoint returns OpenAI-compatible format
- Check response structure matches expected format
- Enable debug logging to see raw responses

### HTTP Version Issues
```
Error: NetworkError("HTTP/2 connection failed")
```
**Solutions:**
- Try switching to HTTP/1.1 for local services:
  ```scala
  val provider = CustomEndpointProvider.forLMStudio(
    baseUrl = "http://localhost:1234/v1",
    httpVersion = HttpVersion.Http11
  )
  ```

## Supported LLM Servers

ChezWiz's CustomEndpointProvider works with any OpenAI-compatible API, including:

- **LM Studio** - Local LLM server with GUI
- **Ollama** - Command-line local LLM runner
- **LocalAI** - OpenAI compatible API for local models
- **Text Generation WebUI** - Gradio-based UI with API
- **vLLM** - High-throughput LLM serving
- **FastChat** - Training and serving platform
- **Any OpenAI-compatible API** - Custom deployments

## Advanced Configuration

### Custom Request/Response Handling

While currently only OpenAI format is supported, the provider is designed for extensibility:

```scala
// Future support for custom formats
val provider = new CustomEndpointProvider(
  baseUrl = "http://custom-api.com",
  useOpenAIFormat = false  // Currently throws NotImplementedError
)
```

### Local Network Detection

The provider includes utilities for local network detection:

```scala
import chezwiz.agent.providers.LocalNetworkHttpClient

// Check if URL is local
val isLocal = LocalNetworkHttpClient.isLocalAddress("http://localhost:1234")
// true for: localhost, 127.0.0.1, 192.168.x.x, 10.x.x.x, 172.16-31.x.x
```

This automatically uses optimized settings for local network requests.