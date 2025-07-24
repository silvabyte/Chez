# ChezWiz: LLM Agent Library for Scala 3

**ChezWiz** is a type-safe LLM agent library for Scala 3 that provides structured text generation and conversation management with multi-provider support (OpenAI, Anthropic) and built-in JSON schema validation using the Chez library.

## Features

✅ **Multi-provider LLM support** (OpenAI, Anthropic)  
✅ **Type-safe structured object generation**  
✅ **Scoped conversation history** (tenant/user/conversation isolation)  
✅ **Built-in JSON schema validation** via Chez integration  
✅ **Conversation persistence and management**  
✅ **Error handling with comprehensive error types**  
✅ **Configurable temperature and token limits**

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

## Best Practices

1. **Always provide metadata** - All ChezWiz methods require `RequestMetadata` for conversation scoping
2. **Use scoped conversations** for multi-tenant applications to ensure data isolation
3. **Handle errors gracefully** using the comprehensive error types
4. **Set appropriate temperature and token limits** based on your use case
5. **Use structured generation** when you need predictable output formats
6. **Clear conversation history** when starting new logical conversations
7. **Monitor token usage** to manage costs effectively

## Examples

See the [Examples.scala](../ChezWiz/src/main/scala/chezwiz/Examples/Examples.scala) file for more comprehensive usage examples.
