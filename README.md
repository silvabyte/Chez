# Chez: JSON Schema for Scala 3

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/silvabyte/scalaschemaz)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Chez** is a comprehensive JSON Schema library for Scala 3 that provides TypeBox-like ergonomics with compile-time type safety and runtime JSON Schema compliance.

## Modules

- **[Chez](./docs/chez.md)**: Core JSON Schema generation, validation, and derivation library
- **[CaskChez](./docs/caskchez.md)**: Cask HTTP framework integration with automatic request/response validation  
- **[ChezWiz](./docs/chezwiz.md)**: Type-safe LLM agent library with multi-provider support and scoped conversation management

## Quick Start

```scala
import chez.derivation.Schema

// Annotation-based derivation (the main hotness!)
case class User(
  @Schema.description("User's full name")
  @Schema.minLength(1)
  name: String,

  @Schema.description("Email address")
  @Schema.format("email")
  email: String,

  @Schema.description("User's age")
  @Schema.minimum(0)
  @Schema.default(18)
  age: Int
) derives Schema

// Generate JSON Schema automatically
val userSchema = Schema[User]
val jsonSchema = userSchema.toJsonSchema

// Automatic validation
val validationResult = userSchema.validate(userData)

// HTTP endpoints with automatic validation
@CaskChez.post("/users", RouteSchema(body = Some(Schema[User])))
def createUser(validatedRequest: ValidatedRequest) = {
  // Request automatically validated against schema!
  validatedRequest.getBody[User] match {
    case Right(user) => processUser(user)
    case Left(error) => handleError(error)
  }
}

// LLM agents with structured generation and hooks for observability
import chezwiz.agent.*

// Simple logging hook for monitoring
class LoggingHook extends PreRequestHook with PostResponseHook {
  override def onPreRequest(context: PreRequestContext): Unit = 
    println(s"[${context.agentName}] Starting request...")
  override def onPostResponse(context: PostResponseContext): Unit = 
    println(s"[${context.agentName}] Completed in ${context.duration}ms")
}

val hooks = HookRegistry.empty
  .addPreRequestHook(new LoggingHook())
  .addPostResponseHook(new LoggingHook())

val agent = AgentFactory.createOpenAIAgent(
  name = "DataAgent",
  instructions = "Generate structured data based on user requests.",
  apiKey = "your-api-key",
  model = "gpt-4o",
  hooks = hooks
).toOption.get

// Generate structured data with conversation history and automatic metrics
val metadata = RequestMetadata(
  tenantId = Some("my-org"),
  userId = Some("user-123"),
  conversationId = Some("data-session")
)
val response = agent.generateObject[User]("Create a user profile for a software engineer", metadata)

// Get comprehensive metrics - all automatically collected
val (metricsAgent, metrics) = MetricsFactory.createOpenAIAgentWithMetrics(
  name = "ProductionAgent",
  instructions = "Production agent with built-in observability",
  apiKey = "your-api-key",
  model = "gpt-4o"
).toOption.get

// Export metrics for monitoring systems
println(metrics.getSnapshot("ProductionAgent").get.toPrometheusFormat)
println(metrics.getSnapshot("ProductionAgent").get.summary)
```

## Installation

TODO: cut a release and publish when ready...

```scala
// build.mill
def ivyDeps = Agg(
  ivy"com.lihaoyi::upickle:4.1.0",
  ivy"com.lihaoyi::cask:0.9.7", // For CaskChez
  // Add Chez modules when published
)
```

## Documentation

- **[Chez Core Library](./docs/chez.md)** - Schema creation, validation, composition, and annotation-based derivation
- **[CaskChez HTTP Integration](./docs/caskchez.md)** - HTTP framework integration, automatic validation, and OpenAPI generation
- **[ChezWiz LLM Agents](./docs/chezwiz.md)** - Type-safe LLM agents, structured generation, and scoped conversation management

## Examples

```bash
# Run all examples
make demo

# Run tests
make test                 # Run all tests
```

## Features

✅ **Full JSON Schema 2020-12 compliance**  
✅ **Scala 3 match types and union types**  
✅ **TypeBox-like ergonomics**  
✅ **Annotation-based schema derivation**  
✅ **Automatic HTTP request/response validation**  
✅ **OpenAPI specification generation**  
✅ **Zero boilerplate validation**  
✅ **Comprehensive validation for body, query, params, headers**  
✅ **Type-safe LLM agents with multi-provider support**  
✅ **Structured LLM response generation**  
✅ **Scoped conversation management (tenant/user/conversation)**  
✅ **Comprehensive hook system for observability and custom logic**  
✅ **Built-in metrics system with Prometheus and JSON export**

## License

MIT License. See [LICENSE](LICENSE) for details.
