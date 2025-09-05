# Hooks & Metrics

ChezWiz provides a comprehensive hook system for observability and metrics collection, enabling you to monitor agent performance, log interactions, and implement custom behavior.

## Hook System

Hooks are lifecycle events that fire during agent operations. Each hook type receives specific context data.

### Available Hook Types

- PreRequestHook: Before sending requests to LLM providers
- PostResponseHook: After receiving responses from LLM providers
- PreObjectRequestHook: Before structured object generation requests
- PostObjectResponseHook: After structured object generation responses
- ErrorHook: When errors occur during operations
- HistoryHook: When conversation history changes
- ScopeChangeHook: When conversation scope changes
- PreEmbeddingHook: Before embedding requests
- PostEmbeddingHook: After embedding responses

### Creating Custom Hooks

```scala
import chezwiz.agent.*

class LoggingHook extends PreRequestHook with PostResponseHook {
  def onPreRequest(ctx: PreRequestContext): Unit =
    println(s"[${ctx.agentName}] → ${ctx.request.messages.last.content}")

  def onPostResponse(ctx: PostResponseContext): Unit = ctx.response match
    case Right(_) => println(s"[${ctx.agentName}] ✓ ${ctx.duration}ms")
    case Left(err) => println(s"[${ctx.agentName}] ✗ $err")
}
```

### Registering Hooks

Use `HookRegistry` to combine multiple hooks.

```scala
val logging = new LoggingHook()
val metrics = new DefaultAgentMetrics()
val metricsHook = new MetricsHook(metrics)

val hooks = HookRegistry.empty
  .addPreRequestHook(logging)
  .addPostResponseHook(logging)
  .addPreRequestHook(metricsHook)
  .addPostResponseHook(metricsHook)
  .addPreObjectRequestHook(metricsHook)
  .addPostObjectResponseHook(metricsHook)
  .addErrorHook(metricsHook)
  .addHistoryHook(metricsHook)

val agent = Agent("MyAgent", "You are helpful", provider, "gpt-4o", hooks = hooks)
```

## Built‑in Metrics

ChezWiz includes a metrics implementation that tracks performance, usage, and errors via hooks. There is no factory; wire metrics explicitly as above.

### What’s Collected

- Request counts: total, successful, failed
- Performance: avg/min/max duration, success rate
- Usage: tokens by model and operation
- Scope analytics: per tenant/user/conversation
- Error tracking: types, counts, sample message
- Recent activity: request rate (last hour)

### Accessing Metrics

Per‑agent snapshot:

```scala
metrics.getSnapshot("MyAgent").foreach { snap =>
  println(snap.summary)             // human-readable
  println(snap.toPrometheusFormat)  // Prometheus exposition
  println(snap.toJson)              // JSON
}
```

Collect all snapshots from a shared metrics instance (recommended when reusing a single `metrics` across agents):

```scala
val all: Map[String, AgentMetricsSnapshot] = metrics.getAllSnapshots
val prometheusAll = all.values.map(_.toPrometheusFormat).mkString("\n\n")
import upickle.default.write
val jsonAll = write(all)
```

### Share One Metrics Instance Across Agents

```scala
val metrics = new DefaultAgentMetrics()
def withMetrics(h: HookRegistry) = h
  .addPreRequestHook(new MetricsHook(metrics))
  .addPostResponseHook(new MetricsHook(metrics))
  .addPreObjectRequestHook(new MetricsHook(metrics))
  .addPostObjectResponseHook(new MetricsHook(metrics))
  .addErrorHook(new MetricsHook(metrics))
  .addHistoryHook(new MetricsHook(metrics))

val a1 = Agent("A1", "...", provider1, "model-1", hooks = withMetrics(HookRegistry.empty))
val a2 = Agent("A2", "...", provider2, "model-2", hooks = withMetrics(HookRegistry.empty))

// Export combined metrics
metrics.getAllSnapshots.values.foreach(s => println(s.summary))
```

## Hook Context Types

Core contexts passed to hooks:

```scala
case class PreRequestContext(
  agentName: String,
  model: String,
  request: ChatRequest,
  metadata: RequestMetadata,
  timestamp: Long
)

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

### Combining Multiple Metrics Backends

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
class ConditionalLoggingHook extends PreRequestHook:
  def onPreRequest(ctx: PreRequestContext): Unit =
    if ctx.metadata.tenantId.contains("debug") then
      println(s"Debug request: ${ctx.request}")
```
