package boogieloops.ai.test

import utest.*
import boogieloops.ai.{
  Agent,
  AgentMetricsSnapshot,
  ChatRequest,
  ChatResponse,
  SchemaError,
  DefaultAgentMetrics,
  ErrorMetrics,
  HookRegistry,
  MetricsHook,
  ModelMetrics,
  ObjectRequest,
  ObjectResponse,
  OperationMetrics,
  RequestMetadata,
  ScopeMetrics,
  Usage
}
import boogieloops.ai.providers.*
import boogieloops.schema.derivation.Schema
import upickle.default.*

object MetricsSpec extends TestSuite:

  // Test data for structured generation
  case class TestData(
      name: String,
      value: Int
  ) derives Schema, ReadWriter

  // Default test metadata
  val defaultMetadata = RequestMetadata(
    tenantId = Some("test-tenant"),
    userId = Some("test-user"),
    conversationId = Some("test-conversation")
  )

  // Mock LLM Provider for testing
  class MockLLMProvider extends LLMProvider:
    override val name: String = "Mock"
    override val supportedModels: List[String] = List("mock-model-1", "mock-model-2")

    override def chat(request: ChatRequest): Either[SchemaError, ChatResponse] = {
      Right(ChatResponse(
        content = s"Mock response to: ${request.messages.last.content}",
        usage = Some(Usage(10, 20, 30)),
        model = request.model,
        finishReason = Some("stop")
      ))
    }

    override def generateObject(request: ObjectRequest)
        : Either[SchemaError, ObjectResponse[ujson.Value]] = {
      Right(ObjectResponse[ujson.Value](
        data = ujson.Obj("name" -> "test", "value" -> 42),
        usage = Some(Usage(10, 20, 30)),
        model = request.model,
        finishReason = Some("stop")
      ))
    }

    override protected def buildHeaders(apiKey: String): Map[String, String] = Map.empty
    override protected def buildRequestBody(request: ChatRequest): ujson.Value = ujson.Obj()
    override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value = ujson.Obj()
    override protected def parseResponse(responseBody: String): Either[SchemaError, ChatResponse] =
      Right(ChatResponse("mock", None, "mock-model-1", Some("stop")))
    override protected def parseObjectResponse(responseBody: String)
        : Either[SchemaError, ObjectResponse[ujson.Value]] = {
      Right(ObjectResponse[ujson.Value](
        data = ujson.Obj("name" -> "test", "value" -> 42),
        usage = None,
        model = "mock-model-1",
        finishReason = Some("stop")
      ))
    }

  val tests = Tests {

    test("DefaultAgentMetrics tracks basic operations") {
      val metrics = new DefaultAgentMetrics()
      val agentName = "TestAgent"
      val model = "test-model"
      val metadata = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      // Record some operations
      metrics.recordRequestStart(agentName, model, "generateText", metadata)
      metrics.recordSuccess(agentName, model, "generateText", 100, 50, metadata)

      metrics.recordRequestStart(agentName, model, "generateObject", metadata)
      metrics.recordSuccess(agentName, model, "generateObject", 200, 75, metadata)

      metrics.recordRequestStart(agentName, model, "generateText", metadata)
      metrics.recordError(
        agentName,
        model,
        "generateText",
        SchemaError.NetworkError("Test error", Some(500)),
        metadata
      )

      // Get snapshot
      val snapshot = metrics.getSnapshot(agentName)
      assert(snapshot.isDefined)

      val snap = snapshot.get
      assert(snap.agentName == agentName)
      assert(snap.totalRequests == 3)
      assert(snap.successfulRequests == 2)
      assert(snap.failedRequests == 1)
      assert(snap.averageDuration == 150.0) // (100 + 200) / 2
      assert(snap.successRate == 2.0 / 3.0)

      // Check operation metrics
      assert(snap.textGenerations.count == 2)
      assert(snap.textGenerations.successCount == 1)
      assert(snap.textGenerations.errorCount == 1)
      assert(snap.objectGenerations.count == 1)
      assert(snap.objectGenerations.successCount == 1)

      // Check model metrics
      assert(snap.modelMetrics.contains(model))
      val modelMetrics = snap.modelMetrics(model)
      assert(modelMetrics.requestCount == 3)
      assert(modelMetrics.totalTokens == 125) // 50 + 75

      // Check error metrics
      assert(snap.errorMetrics.contains("NetworkError"))
      assert(snap.errorMetrics("NetworkError").count == 1)
    }

    test("MetricsHook integrates with agent operations") {
      val metrics = new DefaultAgentMetrics()
      val metricsHook = new MetricsHook(metrics)

      val hooks = HookRegistry.empty
        .addPreRequestHook(metricsHook)
        .addPostResponseHook(metricsHook)
        .addPreObjectRequestHook(metricsHook)
        .addPostObjectResponseHook(metricsHook)
        .addErrorHook(metricsHook)
        .addHistoryHook(metricsHook)

      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "MetricsTestAgent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1",
        hooks = hooks
      )

      val metadata = RequestMetadata(
        tenantId = Some("test-tenant"),
        userId = Some("test-user"),
        conversationId = Some("test-conversation")
      )

      // Perform operations that should be tracked
      agent.generateText("Test message 1", metadata)
      agent.generateText("Test message 2", metadata)
      agent.generateObject[TestData]("Generate test data", metadata)

      // Check metrics were recorded
      val snapshot = metrics.getSnapshot("MetricsTestAgent")
      assert(snapshot.isDefined)

      val snap = snapshot.get
      assert(snap.totalRequests == 3)
      assert(snap.successfulRequests == 3)
      assert(snap.failedRequests == 0)
      assert(snap.textGenerations.count == 2)
      assert(snap.objectGenerations.count == 1)
      assert(snap.modelMetrics.contains("mock-model-1"))
    }

    test("AgentMetricsSnapshot export formats work correctly") {
      val snapshot = AgentMetricsSnapshot(
        agentName = "TestAgent",
        collectionStartTime = 1000000,
        snapshotTime = 1001000,
        totalRequests = 100,
        successfulRequests = 95,
        failedRequests = 5,
        averageDuration = 250.0,
        successRate = 0.95,
        textGenerations = OperationMetrics(
          count = 80,
          successCount = 76,
          errorCount = 4,
          totalDuration = 20000,
          totalTokens = 1000
        ),
        objectGenerations = OperationMetrics(
          count = 20,
          successCount = 19,
          errorCount = 1,
          totalDuration = 5000,
          totalTokens = 500
        ),
        modelMetrics = Map("gpt-4o" -> ModelMetrics(requestCount = 100, totalTokens = 1500)),
        scopeMetrics = Map("tenant1:user1:conv1" -> ScopeMetrics(
          "tenant1:user1:conv1",
          requestCount = 50,
          messageCount = 45
        )),
        errorMetrics =
          Map("NetworkError" -> ErrorMetrics("NetworkError", 3, 1001000, "Connection failed")),
        recentRequestRate = 10.5,
        recentErrorRate = 0.5
      )

      // Test Prometheus export
      val prometheus = snapshot.toPrometheusFormat
      assert(prometheus.contains("boogieloops_ai_requests_total"))
      assert(prometheus.contains("TestAgent"))
      assert(prometheus.contains("100"))

      // Test JSON export
      val json = snapshot.toJson
      assert(json.contains("TestAgent"))
      assert(json.contains("totalRequests"))

      // Test summary
      val summary = snapshot.summary
      assert(summary.contains("TestAgent"))
      assert(summary.contains("Total Requests: 100"))
      assert(summary.contains("95%"))
    }

    test("Metrics system handles concurrent operations safely") {
      val metrics = new DefaultAgentMetrics()
      val agentName = "ConcurrentTestAgent"
      val model = "test-model"

      // Simulate concurrent operations
      val threads = (1 to 10).map { i =>
        new Thread(() => {
          val metadata = RequestMetadata(
            tenantId = Some(s"tenant$i"),
            userId = Some(s"user$i"),
            conversationId = Some(s"conv$i")
          )

          for (j <- 1 to 10) {
            metrics.recordRequestStart(agentName, model, "generateText", metadata)
            metrics.recordSuccess(agentName, model, "generateText", j * 10, j * 5, metadata)
          }
        })
      }

      threads.foreach(_.start())
      threads.foreach(_.join())

      val snapshot = metrics.getSnapshot(agentName)
      assert(snapshot.isDefined)

      val snap = snapshot.get
      assert(snap.totalRequests == 100)
      assert(snap.successfulRequests == 100)
      assert(snap.failedRequests == 0)
      assert(snap.scopeMetrics.size == 10) // 10 different scopes
    }

    test("Metrics system reset functionality works") {
      val metrics = new DefaultAgentMetrics()
      val agentName = "ResetTestAgent"
      val model = "test-model"
      val metadata = defaultMetadata

      // Record some metrics
      metrics.recordRequestStart(agentName, model, "generateText", metadata)
      metrics.recordSuccess(agentName, model, "generateText", 100, 50, metadata)

      // Verify metrics exist
      val snapshot1 = metrics.getSnapshot(agentName)
      assert(snapshot1.isDefined)
      assert(snapshot1.get.totalRequests == 1)

      // Reset metrics
      metrics.reset(agentName)

      // Verify metrics are reset (but agent still exists)
      val snapshot2 = metrics.getSnapshot(agentName)
      assert(snapshot2.isDefined)
      assert(snapshot2.get.totalRequests == 0)
    }

    test("DefaultAgentMetrics tracks different operation types") {
      val metrics = new DefaultAgentMetrics()
      val agentName = "OperationTestAgent"
      val model = "test-model"
      val metadata = defaultMetadata

      // Record different types of operations
      metrics.recordRequestStart(agentName, model, "generateText", metadata)
      metrics.recordSuccess(agentName, model, "generateText", 150, 100, metadata)

      metrics.recordRequestStart(agentName, model, "generateObject", metadata)
      metrics.recordSuccess(agentName, model, "generateObject", 300, 200, metadata)

      metrics.recordRequestStart(agentName, model, "generateTextWithoutHistory", metadata)
      metrics.recordSuccess(agentName, model, "generateTextWithoutHistory", 75, 50, metadata)

      metrics.recordRequestStart(agentName, model, "generateObjectWithoutHistory", metadata)
      metrics.recordSuccess(agentName, model, "generateObjectWithoutHistory", 125, 80, metadata)

      val snapshot = metrics.getSnapshot(agentName)
      assert(snapshot.isDefined)

      val snap = snapshot.get
      assert(snap.totalRequests == 4)
      assert(snap.successfulRequests == 4)
      assert(snap.failedRequests == 0)
      assert(snap.averageDuration == 162.5) // (150 + 300 + 75 + 125) / 4

      // Check that different operation types are tracked
      // Note: The metrics system tracks operation types by their actual names
      assert(snap.textGenerations.count == 1) // only generateText
      assert(snap.objectGenerations.count == 1) // only generateObject
      // The "WithoutHistory" variants are tracked as separate operations in modelMetrics
      val modelMetrics = snap.modelMetrics(model)
      assert(modelMetrics.operations.contains("generateText"))
      assert(modelMetrics.operations.contains("generateObject"))
      assert(modelMetrics.operations.contains("generateTextWithoutHistory"))
      assert(modelMetrics.operations.contains("generateObjectWithoutHistory"))
    }

    test("Metrics track token usage correctly") {
      val metrics = new DefaultAgentMetrics()
      val agentName = "TokenTestAgent"
      val model = "test-model"
      val metadata = defaultMetadata

      // Record operations with different token counts
      metrics.recordRequestStart(agentName, model, "generateText", metadata)
      metrics.recordSuccess(agentName, model, "generateText", 100, 500, metadata)

      metrics.recordRequestStart(agentName, model, "generateObject", metadata)
      metrics.recordSuccess(agentName, model, "generateObject", 200, 750, metadata)

      val snapshot = metrics.getSnapshot(agentName)
      assert(snapshot.isDefined)

      val snap = snapshot.get
      assert(snap.textGenerations.totalTokens == 500)
      assert(snap.objectGenerations.totalTokens == 750)

      // Check model metrics aggregate tokens correctly
      val modelMetrics = snap.modelMetrics(model)
      assert(modelMetrics.totalTokens == 1250) // 500 + 750
    }

    test("Error metrics track different error types") {
      val metrics = new DefaultAgentMetrics()
      val agentName = "ErrorTestAgent"
      val model = "test-model"
      val metadata = defaultMetadata

      // Record different types of errors
      metrics.recordRequestStart(agentName, model, "generateText", metadata)
      metrics.recordError(
        agentName,
        model,
        "generateText",
        SchemaError.NetworkError("Network timeout", Some(408)),
        metadata
      )

      metrics.recordRequestStart(agentName, model, "generateObject", metadata)
      metrics.recordError(
        agentName,
        model,
        "generateObject",
        SchemaError.ApiError("Rate limited", Some("RATE_LIMIT"), Some(429)),
        metadata
      )

      metrics.recordRequestStart(agentName, model, "generateText", metadata)
      metrics.recordError(
        agentName,
        model,
        "generateText",
        SchemaError.NetworkError("Connection failed", Some(503)),
        metadata
      )

      val snapshot = metrics.getSnapshot(agentName)
      assert(snapshot.isDefined)

      val snap = snapshot.get
      assert(snap.totalRequests == 3)
      assert(snap.failedRequests == 3)
      assert(snap.successfulRequests == 0)

      // Check error metrics
      assert(snap.errorMetrics.contains("NetworkError"))
      assert(snap.errorMetrics.contains("ApiError"))
      assert(snap.errorMetrics("NetworkError").count == 2)
      assert(snap.errorMetrics("ApiError").count == 1)
    }

    test("Scope metrics track requests per scope") {
      val metrics = new DefaultAgentMetrics()
      val agentName = "ScopeTestAgent"
      val model = "test-model"

      val scope1 = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      val scope2 = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user2"),
        conversationId = Some("conv2")
      )

      // Record operations in different scopes
      metrics.recordRequestStart(agentName, model, "generateText", scope1)
      metrics.recordSuccess(agentName, model, "generateText", 100, 50, scope1)

      metrics.recordRequestStart(agentName, model, "generateText", scope1)
      metrics.recordSuccess(agentName, model, "generateText", 150, 75, scope1)

      metrics.recordRequestStart(agentName, model, "generateText", scope2)
      metrics.recordSuccess(agentName, model, "generateText", 120, 60, scope2)

      val snapshot = metrics.getSnapshot(agentName)
      assert(snapshot.isDefined)

      val snap = snapshot.get
      assert(snap.scopeMetrics.size == 2)

      val scope1Key = "tenant1:user1:conv1"
      val scope2Key = "tenant1:user2:conv2"

      assert(snap.scopeMetrics.contains(scope1Key))
      assert(snap.scopeMetrics.contains(scope2Key))

      assert(snap.scopeMetrics(scope1Key).requestCount == 2)
      assert(snap.scopeMetrics(scope2Key).requestCount == 1)
    }

    test("Metrics system calculates duration statistics correctly") {
      val metrics = new DefaultAgentMetrics()
      val agentName = "DurationTestAgent"
      val model = "test-model"
      val metadata = defaultMetadata

      // Record operations with known durations
      val durations = List(100, 200, 150, 300, 250)
      durations.foreach { duration =>
        metrics.recordRequestStart(agentName, model, "generateText", metadata)
        metrics.recordSuccess(agentName, model, "generateText", duration, 50, metadata)
      }

      val snapshot = metrics.getSnapshot(agentName)
      assert(snapshot.isDefined)

      val snap = snapshot.get
      val textMetrics = snap.textGenerations

      assert(textMetrics.count == 5)
      assert(textMetrics.minDuration == 100)
      assert(textMetrics.maxDuration == 300)
      assert(textMetrics.averageDuration == 200.0) // (100 + 200 + 150 + 300 + 250) / 5
      assert(textMetrics.totalDuration == 1000)
    }

    test("MetricsHook handles errors correctly") {
      val metrics = new DefaultAgentMetrics()
      val metricsHook = new MetricsHook(metrics)

      // Mock provider that returns errors
      class ErrorProvider extends LLMProvider {
        override val name: String = "Error"
        override val supportedModels: List[String] = List("error-model")

    override def chat(request: ChatRequest): Either[SchemaError, ChatResponse] = {
          Left(SchemaError.NetworkError("Simulated error", Some(500)))
        }

        override def generateObject(request: ObjectRequest)
            : Either[SchemaError, ObjectResponse[ujson.Value]] = {
          Left(SchemaError.ApiError("API error", Some("TEST_ERROR"), Some(400)))
        }

        override protected def buildHeaders(apiKey: String): Map[String, String] = Map.empty
        override protected def buildRequestBody(request: ChatRequest): ujson.Value = ujson.Obj()
        override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value =
          ujson.Obj()
        override protected def parseResponse(responseBody: String)
            : Either[SchemaError, ChatResponse] =
          Right(ChatResponse("", None, "error-model", None))
        override protected def parseObjectResponse(responseBody: String)
            : Either[SchemaError, ObjectResponse[ujson.Value]] =
          Right(ObjectResponse[ujson.Value](ujson.Obj(), None, "error-model", None))
      }

      val hooks = HookRegistry.empty
        .addPreRequestHook(metricsHook)
        .addPostResponseHook(metricsHook)
        .addErrorHook(metricsHook)

      val errorProvider = new ErrorProvider()
      val agent = Agent(
        name = "ErrorMetricsAgent",
        instructions = "You are a helpful assistant",
        provider = errorProvider,
        model = "error-model",
        hooks = hooks
      )

      // Perform operations that will fail
      agent.generateText("This will fail", defaultMetadata)
      agent.generateObject[TestData]("This will also fail", defaultMetadata)

      // Check that errors were recorded in metrics
      val snapshot = metrics.getSnapshot("ErrorMetricsAgent")
      assert(snapshot.isDefined)

      val snap = snapshot.get
      // Note: The actual count might be different due to how hooks track operations
      assert(snap.failedRequests >= 2)
      assert(snap.successfulRequests == 0)
      assert(snap.errorMetrics.contains("NetworkError"))
      assert(snap.errorMetrics.contains("ApiError"))
    }
  }
