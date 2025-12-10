package boogieloops.ai
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}
import upickle.default.ReadWriter
import upickle.default.write
import scala.collection.concurrent.TrieMap
import scala.collection.mutable

/**
 * Comprehensive observability and metrics system for ChezWiz agents.
 * Provides out-of-the-box metrics collection with thread-safe operations
 * and multiple export formats.
 */

// ============================================================================
// Core Metrics Data Structures
// ============================================================================

/** Detailed metrics for individual operations */
case class OperationMetrics(
    count: Long = 0,
    successCount: Long = 0,
    errorCount: Long = 0,
    totalDuration: Long = 0,
    minDuration: Long = Long.MaxValue,
    maxDuration: Long = 0,
    totalTokens: Long = 0
) derives ReadWriter {
  def averageDuration: Double = if (successCount > 0) totalDuration.toDouble / successCount else 0.0
  def successRate: Double = if (count > 0) successCount.toDouble / count else 0.0
  def errorRate: Double = if (count > 0) errorCount.toDouble / count else 0.0
}

/** Metrics aggregated by model */
case class ModelMetrics(
    requestCount: Long = 0,
    totalTokens: Long = 0,
    totalCost: Double = 0.0, // If cost calculation is available
    operations: Map[String, OperationMetrics] = Map.empty
) derives ReadWriter

/** Metrics aggregated by scope (tenant/user/conversation) */
case class ScopeMetrics(
    scopeKey: String,
    requestCount: Long = 0,
    messageCount: Long = 0,
    totalTokens: Long = 0,
    averageResponseTime: Double = 0.0,
    lastActivity: Long = 0,
    errorCount: Long = 0,
    activeConversations: Int = 0
) derives ReadWriter

/** Error tracking information */
case class ErrorMetrics(
    errorType: String,
    count: Long,
    lastOccurrence: Long,
    sampleMessage: String
) derives ReadWriter

/** Comprehensive agent metrics snapshot */
case class AgentMetricsSnapshot(
    agentName: String,
    collectionStartTime: Long,
    snapshotTime: Long,

    // Overall metrics
    totalRequests: Long,
    successfulRequests: Long,
    failedRequests: Long,
    averageDuration: Double,
    successRate: Double,

    // Breakdown by operation type
    textGenerations: OperationMetrics,
    objectGenerations: OperationMetrics,

    // Model usage
    modelMetrics: Map[String, ModelMetrics],

    // Scope analytics
    scopeMetrics: Map[String, ScopeMetrics],

    // Error tracking
    errorMetrics: Map[String, ErrorMetrics],

    // Recent activity (last 24 hours)
    recentRequestRate: Double, // requests per minute
    recentErrorRate: Double // errors per minute
) derives ReadWriter {

  /** Export metrics in Prometheus format */
  def toPrometheusFormat: String = {
    val lines = mutable.ArrayBuffer[String]()
    val prefix = "boogieloops_ai"

    // Basic counters
    lines += s"# HELP ${prefix}_requests_total Total number of requests"
    lines += s"# TYPE ${prefix}_requests_total counter"
    lines += s"""${prefix}_requests_total{agent="$agentName"} $totalRequests"""

    lines += s"# HELP ${prefix}_requests_successful_total Total number of successful requests"
    lines += s"# TYPE ${prefix}_requests_successful_total counter"
    lines += s"""${prefix}_requests_successful_total{agent="$agentName"} $successfulRequests"""

    lines += s"# HELP ${prefix}_requests_failed_total Total number of failed requests"
    lines += s"# TYPE ${prefix}_requests_failed_total counter"
    lines += s"""${prefix}_requests_failed_total{agent="$agentName"} $failedRequests"""

    // Duration metrics
    lines += s"# HELP ${prefix}_request_duration_ms Average request duration in milliseconds"
    lines += s"# TYPE ${prefix}_request_duration_ms gauge"
    lines += s"""${prefix}_request_duration_ms{agent="$agentName"} $averageDuration"""

    // Success rate
    lines += s"# HELP ${prefix}_success_rate Request success rate (0-1)"
    lines += s"# TYPE ${prefix}_success_rate gauge"
    lines += s"""${prefix}_success_rate{agent="$agentName"} $successRate"""

    // Model metrics
    modelMetrics.foreach { case (model, metrics) =>
      lines += s"""${prefix}_model_requests_total{agent="$agentName",model="$model"} ${metrics.requestCount}"""
      lines += s"""${prefix}_model_tokens_total{agent="$agentName",model="$model"} ${metrics.totalTokens}"""
    }

    // Error metrics
    errorMetrics.foreach { case (errorType, metrics) =>
      lines += s"""${prefix}_errors_total{agent="$agentName",error_type="$errorType"} ${metrics.count}"""
    }

    lines.mkString("\n")
  }

  /** Export metrics as JSON */
  def toJson: String = write(this)

  /** Pretty print metrics summary */
  def summary: String = {
    val duration = (snapshotTime - collectionStartTime) / 1000.0 / 60.0 // minutes
    s"""
       |=== Agent Metrics Summary: $agentName ===
       |Collection Duration: ${duration.round} minutes
       |
       |Overall Performance:
       |  Total Requests: $totalRequests
       |  Successful: $successfulRequests (${(successRate * 100).round}%)
       |  Failed: $failedRequests
       |  Average Duration: ${averageDuration.round}ms
       |
       |Operations:
       |  Text Generation: ${textGenerations.count} requests, ${textGenerations.averageDuration.round}ms avg
       |  Object Generation: ${objectGenerations.count} requests, ${objectGenerations.averageDuration.round}ms avg
       |
       |Model Usage:
       |${modelMetrics.map { case (model, metrics) =>
        s"  $model: ${metrics.requestCount} requests, ${metrics.totalTokens} tokens"
      }.mkString("\n")}
       |
       |Active Scopes: ${scopeMetrics.size}
       |Error Types: ${errorMetrics.size}
       |
       |Recent Activity:
       |  Request Rate: ${recentRequestRate.round} req/min
       |  Error Rate: ${recentErrorRate.round} err/min
       |""".stripMargin
  }
}

// ============================================================================
// Metrics Interface
// ============================================================================

/** Core interface for agent metrics collection */
trait AgentMetrics {

  /** Record a request start */
  def recordRequestStart(
      agentName: String,
      model: String,
      operation: String,
      metadata: RequestMetadata
  ): Unit

  /** Record a successful response */
  def recordSuccess(
      agentName: String,
      model: String,
      operation: String,
      duration: Long,
      tokens: Int,
      metadata: RequestMetadata
  ): Unit

  /** Record an error */
  def recordError(
      agentName: String,
      model: String,
      operation: String,
      error: SchemaError,
      metadata: RequestMetadata
  ): Unit

  /** Record history operation */
  def recordHistoryOperation(
      agentName: String,
      operation: HistoryOperation,
      metadata: RequestMetadata
  ): Unit

  /** Get current metrics snapshot */
  def getSnapshot(agentName: String): Option[AgentMetricsSnapshot]

  /** Get all agent metrics */
  def getAllSnapshots: Map[String, AgentMetricsSnapshot]

  /** Reset metrics for an agent */
  def reset(agentName: String): Unit

  /** Reset all metrics */
  def resetAll(): Unit
}

// ============================================================================
// Default Implementation
// ============================================================================

/** Thread-safe, high-performance metrics implementation */
class DefaultAgentMetrics extends AgentMetrics {

  // Thread-safe collections for metrics storage
  private val agentStartTimes = TrieMap[String, Long]()
  private val requestCounts = TrieMap[String, AtomicLong]()
  private val successCounts = TrieMap[String, AtomicLong]()
  private val errorCounts = TrieMap[String, AtomicLong]()
  private val totalDurations = TrieMap[String, AtomicLong]()

  // Operation-specific metrics
  private val operationMetrics =
    TrieMap[String, TrieMap[String, AtomicReference[OperationMetrics]]]()

  // Model metrics
  private val modelMetrics = TrieMap[String, TrieMap[String, AtomicReference[ModelMetrics]]]()

  // Scope metrics
  private val scopeMetrics = TrieMap[String, AtomicReference[ScopeMetrics]]()

  // Error tracking
  private val errorTracking = TrieMap[String, TrieMap[String, AtomicReference[ErrorMetrics]]]()

  // Recent activity tracking (for rate calculations)
  private val recentActivity = TrieMap[String, mutable.ArrayBuffer[Long]]()

  private def getOrCreateAgent(agentName: String): Unit = {
    agentStartTimes.putIfAbsent(agentName, System.currentTimeMillis())
    requestCounts.putIfAbsent(agentName, new AtomicLong(0))
    successCounts.putIfAbsent(agentName, new AtomicLong(0))
    errorCounts.putIfAbsent(agentName, new AtomicLong(0))
    totalDurations.putIfAbsent(agentName, new AtomicLong(0))
    operationMetrics.putIfAbsent(agentName, TrieMap.empty)
    modelMetrics.putIfAbsent(agentName, TrieMap.empty)
    errorTracking.putIfAbsent(agentName, TrieMap.empty)
    recentActivity.putIfAbsent(agentName, mutable.ArrayBuffer.empty)
  }

  private def getScopeKey(metadata: RequestMetadata): String = {
    s"${metadata.tenantId.getOrElse("_")}:${metadata.userId.getOrElse("_")}:${metadata.conversationId.getOrElse("_")}"
  }

  override def recordRequestStart(
      agentName: String,
      model: String,
      operation: String,
      metadata: RequestMetadata
  ): Unit = {
    getOrCreateAgent(agentName)
    requestCounts(agentName).incrementAndGet()

    // Update model metrics
    val modelMap = modelMetrics(agentName)
    val modelRef = modelMap.getOrElseUpdate(model, new AtomicReference(ModelMetrics()))
    modelRef.updateAndGet(m => m.copy(requestCount = m.requestCount + 1))

    // Update scope metrics
    val scopeKey = getScopeKey(metadata)
    val scopeRef = scopeMetrics.getOrElseUpdate(
      scopeKey,
      new AtomicReference(
        ScopeMetrics(scopeKey = scopeKey, lastActivity = System.currentTimeMillis())
      )
    )
    scopeRef.updateAndGet(s =>
      s.copy(requestCount = s.requestCount + 1, lastActivity = System.currentTimeMillis())
    )

    // Track recent activity
    synchronized {
      val activity = recentActivity(agentName)
      val now = System.currentTimeMillis()
      activity += now
      // Keep only last 24 hours
      val cutoff = now - (24 * 60 * 60 * 1000)
      activity.filterInPlace(_ >= cutoff)
    }
  }

  override def recordSuccess(
      agentName: String,
      model: String,
      operation: String,
      duration: Long,
      tokens: Int,
      metadata: RequestMetadata
  ): Unit = {
    getOrCreateAgent(agentName)
    successCounts(agentName).incrementAndGet()
    totalDurations(agentName).addAndGet(duration)

    // Update operation metrics
    val opMap = operationMetrics(agentName)
    val opRef = opMap.getOrElseUpdate(operation, new AtomicReference(OperationMetrics()))
    opRef.updateAndGet { current =>
      current.copy(
        count = current.count + 1,
        successCount = current.successCount + 1,
        totalDuration = current.totalDuration + duration,
        minDuration = math.min(current.minDuration, duration),
        maxDuration = math.max(current.maxDuration, duration),
        totalTokens = current.totalTokens + tokens
      )
    }

    // Update model metrics
    val modelMap = modelMetrics(agentName)
    val modelRef = modelMap.getOrElseUpdate(model, new AtomicReference(ModelMetrics()))
    modelRef.updateAndGet { current =>
      val updatedOps = current.operations.get(operation) match {
        case Some(opMetrics) => current.operations.updated(
            operation,
            opMetrics.copy(
              count = opMetrics.count + 1,
              successCount = opMetrics.successCount + 1,
              totalDuration = opMetrics.totalDuration + duration,
              totalTokens = opMetrics.totalTokens + tokens
            )
          )
        case None => current.operations + (operation -> OperationMetrics(
            count = 1,
            successCount = 1,
            totalDuration = duration,
            totalTokens = tokens,
            minDuration = duration,
            maxDuration = duration
          ))
      }
      current.copy(totalTokens = current.totalTokens + tokens, operations = updatedOps)
    }

    // Update scope metrics
    val scopeKey = getScopeKey(metadata)
    val scopeRef = scopeMetrics.getOrElseUpdate(
      scopeKey,
      new AtomicReference(
        ScopeMetrics(scopeKey = scopeKey)
      )
    )
    scopeRef.updateAndGet { current =>
      val newMessageCount = current.messageCount + 1
      val newAvgTime =
        (current.averageResponseTime * (newMessageCount - 1) + duration) / newMessageCount
      current.copy(
        messageCount = newMessageCount,
        totalTokens = current.totalTokens + tokens,
        averageResponseTime = newAvgTime,
        lastActivity = System.currentTimeMillis()
      )
    }
  }

  override def recordError(
      agentName: String,
      model: String,
      operation: String,
      error: SchemaError,
      metadata: RequestMetadata
  ): Unit = {
    getOrCreateAgent(agentName)
    errorCounts(agentName).incrementAndGet()

    // Update operation metrics
    val opMap = operationMetrics(agentName)
    val opRef = opMap.getOrElseUpdate(operation, new AtomicReference(OperationMetrics()))
    opRef.updateAndGet(current =>
      current.copy(count = current.count + 1, errorCount = current.errorCount + 1)
    )

    // Track error details
    val errorType = error.getClass.getSimpleName
    val errorMap = errorTracking(agentName)
    val errorRef = errorMap.getOrElseUpdate(
      errorType,
      new AtomicReference(
        ErrorMetrics(errorType, 0, System.currentTimeMillis(), error.toString)
      )
    )
    errorRef.updateAndGet(current => {
      current.copy(
        count = current.count + 1,
        lastOccurrence = System.currentTimeMillis(),
        sampleMessage = error.toString
      )
    })

    // Update scope metrics
    val scopeKey = getScopeKey(metadata)
    val scopeRef = scopeMetrics.getOrElseUpdate(
      scopeKey,
      new AtomicReference(
        ScopeMetrics(scopeKey = scopeKey)
      )
    )
    scopeRef.updateAndGet(current => {
      current.copy(
        errorCount = current.errorCount + 1,
        lastActivity = System.currentTimeMillis()
      )
    })
  }

  override def recordHistoryOperation(
      agentName: String,
      operation: HistoryOperation,
      metadata: RequestMetadata
  ): Unit = {
    // Update scope metrics for history operations
    val scopeKey = getScopeKey(metadata)
    val scopeRef = scopeMetrics.getOrElseUpdate(
      scopeKey,
      new AtomicReference(
        ScopeMetrics(scopeKey = scopeKey)
      )
    )
    scopeRef.updateAndGet(current => current.copy(lastActivity = System.currentTimeMillis()))
  }

  override def getSnapshot(agentName: String): Option[AgentMetricsSnapshot] = {
    if (!agentStartTimes.contains(agentName)) return None

    val now = System.currentTimeMillis()
    val startTime = agentStartTimes(agentName)
    val totalRequests = requestCounts(agentName).get()
    val successfulRequests = successCounts(agentName).get()
    val failedRequests = errorCounts(agentName).get()
    val avgDuration = if (successfulRequests > 0)
      totalDurations(agentName).get().toDouble / successfulRequests
    else 0.0
    val successRate = if (totalRequests > 0) successfulRequests.toDouble / totalRequests else 0.0

    // Get operation metrics
    val opMetrics =
      operationMetrics.getOrElse(agentName, TrieMap.empty).view.mapValues(_.get()).toMap
    val textGen = opMetrics.getOrElse("generateText", OperationMetrics())
    val objGen = opMetrics.getOrElse("generateObject", OperationMetrics())

    // Get model metrics
    val modelMets = modelMetrics.getOrElse(agentName, TrieMap.empty).view.mapValues(_.get()).toMap

    // Get scope metrics
    val scopeMets = scopeMetrics.view.mapValues(_.get()).toMap

    // Get error metrics
    val errorMets = errorTracking.getOrElse(agentName, TrieMap.empty).view.mapValues(_.get()).toMap

    // Calculate recent rates
    val recentRequests = synchronized {
      recentActivity.getOrElse(agentName, mutable.ArrayBuffer.empty).toList
    }
    val recentMinutes = 60.0 // Last hour
    val cutoff = now - (recentMinutes * 60 * 1000).toLong
    val recentCount = recentRequests.count(_ >= cutoff)
    val recentRequestRate = recentCount / recentMinutes
    val recentErrorRate = 0.0 // Simplified for now

    Some(AgentMetricsSnapshot(
      agentName = agentName,
      collectionStartTime = startTime,
      snapshotTime = now,
      totalRequests = totalRequests,
      successfulRequests = successfulRequests,
      failedRequests = failedRequests,
      averageDuration = avgDuration,
      successRate = successRate,
      textGenerations = textGen,
      objectGenerations = objGen,
      modelMetrics = modelMets,
      scopeMetrics = scopeMets,
      errorMetrics = errorMets,
      recentRequestRate = recentRequestRate,
      recentErrorRate = recentErrorRate
    ))
  }

  override def getAllSnapshots: Map[String, AgentMetricsSnapshot] = {
    agentStartTimes.keys.flatMap(agentName => getSnapshot(agentName).map(agentName -> _)).toMap
  }

  override def reset(agentName: String): Unit = {
    agentStartTimes.put(agentName, System.currentTimeMillis())
    requestCounts.get(agentName).foreach(_.set(0))
    successCounts.get(agentName).foreach(_.set(0))
    errorCounts.get(agentName).foreach(_.set(0))
    totalDurations.get(agentName).foreach(_.set(0))
    operationMetrics.remove(agentName)
    modelMetrics.remove(agentName)
    errorTracking.remove(agentName)
    synchronized {
      recentActivity.get(agentName).foreach(_.clear())
    }
  }

  override def resetAll(): Unit = {
    agentStartTimes.keys.foreach(reset)
  }
}

// ============================================================================
// Metrics Hook Integration
// ============================================================================

/** Hook that integrates with AgentMetrics for automatic collection */
class MetricsHook(metrics: AgentMetrics) extends AgentHook with PreRequestHook with PostResponseHook
    with PreObjectRequestHook with PostObjectResponseHook with ErrorHook with HistoryHook {

  override def onPreRequest(context: PreRequestContext): Unit = {
    metrics.recordRequestStart(context.agentName, context.model, "generateText", context.metadata)
  }

  override def onPostResponse(context: PostResponseContext): Unit = {
    context.response match {
      case Right(response) =>
        val tokens = response.usage.map(_.totalTokens).getOrElse(0)
        metrics.recordSuccess(
          context.agentName,
          context.model,
          "generateText",
          context.duration,
          tokens,
          context.metadata
        )
      case Left(error) =>
        metrics.recordError(
          context.agentName,
          context.model,
          "generateText",
          error,
          context.metadata
        )
    }
  }

  override def onPreObjectRequest(context: PreObjectRequestContext): Unit = {
    metrics.recordRequestStart(context.agentName, context.model, "generateObject", context.metadata)
  }

  override def onPostObjectResponse(context: PostObjectResponseContext): Unit = {
    context.response match {
      case Right(response) =>
        val tokens = response.usage.map(_.totalTokens).getOrElse(0)
        metrics.recordSuccess(
          context.agentName,
          context.model,
          "generateObject",
          context.duration,
          tokens,
          context.metadata
        )
      case Left(error) =>
        metrics.recordError(
          context.agentName,
          context.model,
          "generateObject",
          error,
          context.metadata
        )
    }
  }

  override def onError(context: ErrorContext): Unit = {
    metrics.recordError(context.agentName, "", context.operation, context.error, context.metadata)
  }

  override def onHistoryChange(context: HistoryContext): Unit = {
    metrics.recordHistoryOperation(context.agentName, context.operation, context.metadata)
  }
}
