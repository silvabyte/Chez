# Hooks & Metrics

ChezWiz provides a comprehensive hook system for observability and metrics collection, enabling you to monitor agent performance, log interactions, and implement custom business logic.

## Hook System

Hooks are lifecycle events that fire during agent operations. Each hook type receives specific context data:

### Available Hook Types

- **PreRequestHook**: Before sending requests to LLM providers
- **PostResponseHook**: After receiving responses from LLM providers
- **PreObjectRequestHook**: Before structured object generation requests
- **PostObjectResponseHook**: After structured object generation responses
- **ErrorHook**: When errors occur during operations
- **HistoryHook**: When conversation history changes
- **ScopeChangeHook**: When conversation scope changes
- **PreEmbeddingHook**: Before embedding requests
- **PostEmbeddingHook**: After embedding responses

### Creating Custom Hooks

Implement hook traits for custom functionality:

```scala
import chezwiz.agent.*

class LoggingHook extends PreRequestHook with PostResponseHook {
  def onPreRequest(context: PreRequestContext): Unit = {
    println(s"[${context.agentName}] Starting request: ${context.request.messages.last.content}")
  }

  def onPostResponse(context: PostResponseContext): Unit = {
    context.response match {
      case Right(response) =>
        println(s"[${context.agentName}] Success in ${context.duration}ms")
      case Left(error) =>
        println(s"[${context.agentName}] Error: ${error}")
    }
  }
}
```

### Registering Hooks

Use `HookRegistry` to combine multiple hooks:

```scala
val logging = new LoggingHook()
val metrics = new MetricsHook(new DefaultAgentMetrics())

val hooks = HookRegistry.empty
  .addPreRequestHook(logging)
  .addPostResponseHook(logging)
  .addPreRequestHook(metrics)
  .addPostResponseHook(metrics)

val agent = Agent("MyAgent", "You are helpful", provider, "gpt-4o", hooks = hooks)
```

## Built‑in Metrics

ChezWiz includes a comprehensive metrics system that tracks performance, usage, and errors automatically.

### Quick Setup with MetricsFactory

The easiest way to get metrics is using `MetricsFactory`:

```scala
import chezwiz.agent.MetricsFactory

val result = MetricsFactory.createOpenAIAgentWithMetrics(
  name = "ProductionAgent",
  instructions = "You are a helpful assistant",
  apiKey = sys.env("OPENAI_API_KEY"),
  model = "gpt-4o"
)

result match {
  case Right((agent, metrics)) =>
    // Use agent normally, metrics collected automatically
    agent.generateText("Hello world", metadata)

    // View metrics
    val snapshot = metrics.getSnapshot("ProductionAgent")
    println(snapshot.map(_.summary))
  case Left(error) => println(s"Setup failed: $error")
}
```

### Manual Metrics Setup

For more control, use `MetricsHook` directly:

```scala
val metrics = new DefaultAgentMetrics()
val metricsHook = new MetricsHook(metrics)

val hooks = HookRegistry.empty
  .addPreRequestHook(metricsHook)
  .addPostResponseHook(metricsHook)
  .addPreObjectRequestHook(metricsHook)
  .addPostObjectResponseHook(metricsHook)
  .addErrorHook(metricsHook)
  .addHistoryHook(metricsHook)

val agent = Agent("MyAgent", "Instructions", provider, "gpt-4o", hooks = hooks)
```

### Metrics Data

Collected metrics include:

- **Request counts**: Total, successful, failed
- **Performance**: Average/min/max duration, success rates
- **Usage**: Token consumption by model and operation
- **Scope analytics**: Per‑tenant/user/conversation metrics
- **Error tracking**: Error types, frequencies, samples
- **Recent activity**: Request rates and trends

### Exporting Metrics

Export metrics in multiple formats:

```scala
val snapshot = metrics.getSnapshot("MyAgent")

// Human‑readable summary
println(snapshot.map(_.summary))

// Prometheus format
println(snapshot.map(_.toPrometheusFormat))

// JSON export
println(snapshot.map(_.toJson))

// Global metrics export
println(MetricsFactory.exportPrometheus)
println(MetricsFactory.exportJson)
```

## Hook Context Data

Each hook receives rich context information:

### PreRequestContext

```scala
case class PreRequestContext(
  agentName: String,
  model: String,
  request: ChatRequest,
  metadata: RequestMetadata,
  timestamp: Long
)
```

### PostResponseContext

```scala
case class PostResponseContext(
  agentName: String,
  model: String,
  request: ChatRequest,
  response: Either[ChezError, ChatResponse],
  metadata: RequestMetadata,
  requestTimestamp: Long,
  responseTimestamp: Long
) {
  def duration: Long = responseTimestamp - requestTimestamp
}
```

### ErrorContext

```scala
case class ErrorContext(
  agentName: String,
  model: String,
  error: ChezError,
  metadata: RequestMetadata,
  operation: String,
  request: Option[Either[ChatRequest, ObjectRequest]],
  timestamp: Long
)
```

## Advanced Patterns

### Combining Multiple Metrics Systems

```scala
val customMetrics = new MyCustomMetrics()
val defaultMetrics = new DefaultAgentMetrics()

val customHook = new MetricsHook(customMetrics)
val defaultHook = new MetricsHook(defaultMetrics)

val hooks = HookRegistry.empty
  .addPreRequestHook(customHook)
  .addPostResponseHook(customHook)
  .addPreRequestHook(defaultHook)
  .addPostResponseHook(defaultHook)
```

### Conditional Hook Execution

```scala
class ConditionalLoggingHook extends PreRequestHook {
  def onPreRequest(context: PreRequestContext): Unit = {
    if (context.metadata.tenantId.contains("debug")) {
      println(s"Debug request: ${context.request}")
    }
  }
}
```

### Global Metrics Access

```scala
// Replace global metrics instance
MetricsFactory.setGlobalMetrics(new MyCustomMetrics())

// Access global metrics
val allMetrics = MetricsFactory.getAllMetrics
val agentMetrics = MetricsFactory.getMetrics("MyAgent")
```

