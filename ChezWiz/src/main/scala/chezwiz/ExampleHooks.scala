package chezwiz.agent.examples

import chezwiz.agent.*
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable

/**
 * Example hook implementations demonstrating common use cases:
 * - Metrics collection
 * - Request/response logging
 * - OpenTelemetry-style tracing
 * - Error monitoring
 * - Conversation analytics
 */

// ============================================================================
// Metrics Collection Hooks
// ============================================================================

/** Simple metrics collector using atomic counters */
class MetricsCollectorHook extends PreRequestHook with PostResponseHook with ErrorHook {
  private val requestCount = new AtomicLong(0)
  private val successCount = new AtomicLong(0)
  private val errorCount = new AtomicLong(0)
  private val totalDuration = new AtomicLong(0)
  private val modelUsage = mutable.Map[String, AtomicLong]()

  override def onPreRequest(context: PreRequestContext): Unit = {
    requestCount.incrementAndGet()
    modelUsage.getOrElseUpdate(context.model, new AtomicLong(0)).incrementAndGet()
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

  // Metrics accessors
  def getRequestCount: Long = requestCount.get()
  def getSuccessCount: Long = successCount.get()
  def getErrorCount: Long = errorCount.get()
  def getAverageDuration: Double = {
    val count = getSuccessCount
    if (count > 0) totalDuration.get().toDouble / count else 0.0
  }
  def getSuccessRate: Double = {
    val total = getRequestCount
    if (total > 0) getSuccessCount.toDouble / total else 0.0
  }
  def getModelUsage: Map[String, Long] = {
    modelUsage.view.mapValues(_.get()).toMap
  }

  def printMetrics(): Unit = {
    println(s"=== Agent Metrics ===")
    println(s"Total Requests: ${getRequestCount}")
    println(s"Successful Requests: ${getSuccessCount}")
    println(s"Failed Requests: ${getErrorCount}")
    println(s"Success Rate: ${(getSuccessRate * 100).round}%")
    println(s"Average Duration: ${getAverageDuration.round}ms")
    println(s"Model Usage: ${getModelUsage}")
  }
}

// ============================================================================
// Logging Hooks
// ============================================================================

/** Comprehensive logging hook for requests, responses, and operations */
class LoggingHook extends PreRequestHook with PostResponseHook with PreObjectRequestHook 
    with PostObjectResponseHook with ErrorHook with HistoryHook {

  override def onPreRequest(context: PreRequestContext): Unit = {
    println(s"[${Instant.ofEpochMilli(context.timestamp)}] PRE-REQUEST: Agent '${context.agentName}' " +
      s"using model '${context.model}' with ${context.request.messages.size} messages " +
      s"(tenant: ${context.metadata.tenantId.getOrElse("_")}, " +
      s"user: ${context.metadata.userId.getOrElse("_")}, " +
      s"conversation: ${context.metadata.conversationId.getOrElse("_")})")
  }

  override def onPostResponse(context: PostResponseContext): Unit = {
    val status = context.response match {
      case Right(response) => s"SUCCESS (${context.duration}ms, ${response.usage.map(_.totalTokens).getOrElse(0)} tokens)"
      case Left(error) => s"ERROR (${context.duration}ms): $error"
    }
    println(s"[${Instant.ofEpochMilli(context.responseTimestamp)}] POST-RESPONSE: Agent '${context.agentName}' - $status")
  }

  override def onPreObjectRequest(context: PreObjectRequestContext): Unit = {
    println(s"[${Instant.ofEpochMilli(context.timestamp)}] PRE-OBJECT-REQUEST: Agent '${context.agentName}' " +
      s"generating ${context.targetType} with ${context.request.messages.size} messages")
  }

  override def onPostObjectResponse(context: PostObjectResponseContext): Unit = {
    val status = context.response match {
      case Right(response) => s"SUCCESS (${context.duration}ms, generated ${context.targetType})"
      case Left(error) => s"ERROR (${context.duration}ms): $error"
    }
    println(s"[${Instant.ofEpochMilli(context.responseTimestamp)}] POST-OBJECT-RESPONSE: Agent '${context.agentName}' - $status")
  }

  override def onError(context: ErrorContext): Unit = {
    println(s"[${Instant.ofEpochMilli(context.timestamp)}] ERROR: Agent '${context.agentName}' " +
      s"operation '${context.operation}' failed: ${context.error}")
  }

  override def onHistoryChange(context: HistoryContext): Unit = {
    val operation = context.operation match {
      case HistoryOperation.Add => s"ADDED message (${context.message.map(_.role).getOrElse("?")})"
      case HistoryOperation.Clear => "CLEARED history"
      case HistoryOperation.ClearAll => "CLEARED ALL histories"
    }
    println(s"[${Instant.ofEpochMilli(context.timestamp)}] HISTORY: Agent '${context.agentName}' " +
      s"$operation - History size: ${context.historySize}")
  }
}

// ============================================================================
// OpenTelemetry-Style Tracing Hook
// ============================================================================

/** Simple tracing hook that tracks operation spans */
case class Span(
    traceId: String,
    spanId: String,
    operationName: String,
    startTime: Long,
    endTime: Option[Long] = None,
    attributes: Map[String, String] = Map.empty,
    status: String = "pending"
) {
  def duration: Long = endTime.getOrElse(System.currentTimeMillis()) - startTime
  def isComplete: Boolean = endTime.isDefined
}

class TracingHook extends PreRequestHook with PostResponseHook with PreObjectRequestHook 
    with PostObjectResponseHook with ErrorHook {
  
  private val activeSpans = mutable.Map[String, Span]()
  private val completedSpans = mutable.ArrayBuffer[Span]()

  private def generateTraceId(): String = java.util.UUID.randomUUID().toString
  private def generateSpanId(): String = java.util.UUID.randomUUID().toString.take(16)

  override def onPreRequest(context: PreRequestContext): Unit = {
    val spanId = generateSpanId()
    val span = Span(
      traceId = generateTraceId(),
      spanId = spanId,
      operationName = "generateText",
      startTime = context.timestamp,
      attributes = Map(
        "agent.name" -> context.agentName,
        "model" -> context.model,
        "tenant.id" -> context.metadata.tenantId.getOrElse("_"),
        "user.id" -> context.metadata.userId.getOrElse("_"),
        "conversation.id" -> context.metadata.conversationId.getOrElse("_"),
        "message.count" -> context.request.messages.size.toString
      )
    )
    activeSpans(spanId) = span
  }

  override def onPostResponse(context: PostResponseContext): Unit = {
    // Find the corresponding span (simplified - in real tracing you'd use trace context)
    activeSpans.values.find(span => span.operationName == "generateText" && !span.isComplete).foreach { span =>
      val status = context.response match {
        case Right(response) => "success"
        case Left(_) => "error"
      }
      val updatedSpan = span.copy(
        endTime = Some(context.responseTimestamp),
        status = status,
        attributes = span.attributes ++ Map(
          "duration.ms" -> context.duration.toString,
          "tokens.total" -> context.response.toOption.flatMap(_.usage.map(_.totalTokens.toString)).getOrElse("0")
        )
      )
      activeSpans.remove(span.spanId)
      completedSpans += updatedSpan
    }
  }

  override def onPreObjectRequest(context: PreObjectRequestContext): Unit = {
    val spanId = generateSpanId()
    val span = Span(
      traceId = generateTraceId(),
      spanId = spanId,
      operationName = "generateObject",
      startTime = context.timestamp,
      attributes = Map(
        "agent.name" -> context.agentName,
        "model" -> context.model,
        "target.type" -> context.targetType,
        "tenant.id" -> context.metadata.tenantId.getOrElse("_"),
        "user.id" -> context.metadata.userId.getOrElse("_"),
        "conversation.id" -> context.metadata.conversationId.getOrElse("_")
      )
    )
    activeSpans(spanId) = span
  }

  override def onPostObjectResponse(context: PostObjectResponseContext): Unit = {
    activeSpans.values.find(span => span.operationName == "generateObject" && !span.isComplete).foreach { span =>
      val status = context.response match {
        case Right(_) => "success"
        case Left(_) => "error"
      }
      val updatedSpan = span.copy(
        endTime = Some(context.responseTimestamp),
        status = status,
        attributes = span.attributes ++ Map(
          "duration.ms" -> context.duration.toString
        )
      )
      activeSpans.remove(span.spanId)
      completedSpans += updatedSpan
    }
  }

  override def onError(context: ErrorContext): Unit = {
    // Mark any active spans as errored
    activeSpans.values.foreach { span =>
      if (!span.isComplete) {
        val updatedSpan = span.copy(
          endTime = Some(context.timestamp),
          status = "error",
          attributes = span.attributes + ("error.message" -> context.error.toString)
        )
        activeSpans.remove(span.spanId)
        completedSpans += updatedSpan
      }
    }
  }

  def getCompletedSpans: List[Span] = completedSpans.toList
  def getActiveSpans: List[Span] = activeSpans.values.toList

  def printTraces(): Unit = {
    println("=== Completed Traces ===")
    completedSpans.foreach { span =>
      println(s"Trace ${span.traceId.take(8)}: ${span.operationName} - ${span.duration}ms (${span.status})")
      span.attributes.foreach { case (key, value) => println(s"  $key: $value") }
      println()
    }
  }
}

// ============================================================================
// Error Monitoring Hook
// ============================================================================

/** Hook for monitoring and alerting on errors */
class ErrorMonitoringHook extends ErrorHook {
  private val errors = mutable.ArrayBuffer[ErrorContext]()
  private val errorCounts = mutable.Map[String, AtomicLong]()

  override def onError(context: ErrorContext): Unit = {
    errors += context
    val errorType = context.error.getClass.getSimpleName
    errorCounts.getOrElseUpdate(errorType, new AtomicLong(0)).incrementAndGet()
    
    // In a real implementation, you might send alerts here
    if (shouldAlert(context)) {
      sendAlert(context)
    }
  }

  private def shouldAlert(context: ErrorContext): Boolean = {
    // Simple alerting logic - alert on certain error types
    context.error match {
      case _: ChezError.NetworkError => true
      case _: ChezError.ApiError => true
      case _ => false
    }
  }

  private def sendAlert(context: ErrorContext): Unit = {
    println(s"🚨 ALERT: Error in agent '${context.agentName}' operation '${context.operation}': ${context.error}")
  }

  def getErrorCount: Int = errors.size
  def getErrorsByType: Map[String, Long] = errorCounts.view.mapValues(_.get()).toMap
  def getRecentErrors(minutes: Int = 10): List[ErrorContext] = {
    val cutoff = System.currentTimeMillis() - (minutes * 60 * 1000)
    errors.filter(_.timestamp >= cutoff).toList
  }

  def printErrorSummary(): Unit = {
    println("=== Error Summary ===")
    println(s"Total Errors: ${getErrorCount}")
    println("Error Types:")
    getErrorsByType.foreach { case (errorType, count) =>
      println(s"  $errorType: $count")
    }
    val recent = getRecentErrors()
    if (recent.nonEmpty) {
      println(s"Recent Errors (last 10 minutes): ${recent.size}")
    }
  }
}

// ============================================================================
// Conversation Analytics Hook
// ============================================================================

/** Hook for analyzing conversation patterns and usage */
class ConversationAnalyticsHook extends PostResponseHook with HistoryHook {
  private val conversationStats = mutable.Map[String, ConversationStats]()

  case class ConversationStats(
      var messageCount: Int = 0,
      var totalTokens: Int = 0,
      var averageResponseTime: Double = 0.0,
      var lastActivity: Long = 0,
      var errorCount: Int = 0
  )

  private def getScopeKey(metadata: RequestMetadata): String = {
    s"${metadata.tenantId.getOrElse("_")}:${metadata.userId.getOrElse("_")}:${metadata.conversationId.getOrElse("_")}"
  }

  override def onPostResponse(context: PostResponseContext): Unit = {
    val scopeKey = getScopeKey(context.metadata)
    val stats = conversationStats.getOrElseUpdate(scopeKey, ConversationStats())
    
    context.response match {
      case Right(response) =>
        stats.messageCount += 1
        stats.totalTokens += response.usage.map(_.totalTokens).getOrElse(0)
        // Update average response time
        val newAvg = (stats.averageResponseTime * (stats.messageCount - 1) + context.duration) / stats.messageCount
        stats.averageResponseTime = newAvg
        stats.lastActivity = context.responseTimestamp
      case Left(_) =>
        stats.errorCount += 1
    }
  }

  override def onHistoryChange(context: HistoryContext): Unit = {
    val scopeKey = getScopeKey(context.metadata)
    val stats = conversationStats.getOrElseUpdate(scopeKey, ConversationStats())
    stats.lastActivity = context.timestamp
  }

  def getConversationStats(metadata: RequestMetadata): Option[ConversationStats] = {
    conversationStats.get(getScopeKey(metadata))
  }

  def getAllConversationStats: Map[String, ConversationStats] = conversationStats.toMap

  def printAnalytics(): Unit = {
    println("=== Conversation Analytics ===")
    println(s"Active Conversations: ${conversationStats.size}")
    
    if (conversationStats.nonEmpty) {
      val totalMessages = conversationStats.values.map(_.messageCount).sum
      val totalTokens = conversationStats.values.map(_.totalTokens).sum
      val avgResponseTime = conversationStats.values.map(_.averageResponseTime).sum / conversationStats.size
      
      println(s"Total Messages: $totalMessages")
      println(s"Total Tokens: $totalTokens")
      println(s"Average Response Time: ${avgResponseTime.round}ms")
      
      // Top active conversations
      val topConversations = conversationStats.toSeq
        .sortBy(-_._2.messageCount)
        .take(5)
      
      println("Top Active Conversations:")
      topConversations.foreach { case (scope, stats) =>
        println(s"  $scope: ${stats.messageCount} messages, ${stats.totalTokens} tokens")
      }
    }
  }
}

// ============================================================================
// Example Usage
// ============================================================================

object HookExamples {
  
  /** Example showing how to set up multiple hooks on an agent */
  def createAgentWithHooks(): Unit = {
    // Create hook instances
    val metrics = new MetricsCollectorHook()
    val logging = new LoggingHook()
    val tracing = new TracingHook()
    val errorMonitoring = new ErrorMonitoringHook()
    val analytics = new ConversationAnalyticsHook()

    // Configure hook registry
    val hooks = HookRegistry.empty
      .addPreRequestHook(metrics)
      .addPostResponseHook(metrics)
      .addErrorHook(metrics)
      .addPreRequestHook(logging)
      .addPostResponseHook(logging)
      .addPreObjectRequestHook(logging)
      .addPostObjectResponseHook(logging)
      .addErrorHook(logging)
      .addHistoryHook(logging)
      .addPreRequestHook(tracing)
      .addPostResponseHook(tracing)
      .addPreObjectRequestHook(tracing)
      .addPostObjectResponseHook(tracing)
      .addErrorHook(tracing)
      .addErrorHook(errorMonitoring)
      .addPostResponseHook(analytics)
      .addHistoryHook(analytics)

    // Create agent with hooks (example with mock API key)
    val agentResult = AgentFactory.createOpenAIAgent(
      name = "Monitored Agent",
      instructions = "You are a helpful assistant with comprehensive monitoring.",
      apiKey = "sk-mock-key",
      model = "gpt-4o-mini",
      hooks = hooks
    )

    agentResult match {
      case Right(agent) =>
        println("Agent created with comprehensive monitoring hooks!")
        
        // Example usage that would trigger hooks
        val metadata = RequestMetadata(
          tenantId = Some("demo-company"),
          userId = Some("demo-user"),
          conversationId = Some("demo-conversation")
        )
        
        // This would trigger all the hooks
        // agent.generateText("Hello, how are you?", metadata)
        
        // Print metrics after some usage
        // metrics.printMetrics()
        // tracing.printTraces()
        // errorMonitoring.printErrorSummary()
        // analytics.printAnalytics()
        
      case Left(error) =>
        println(s"Failed to create agent: $error")
    }
  }
}