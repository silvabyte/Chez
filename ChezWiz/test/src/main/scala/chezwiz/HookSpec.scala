import utest.*
import chezwiz.agent.{Agent, AgentFactory, ChatMessage, ChatRequest, ChatResponse, ChezError, ErrorContext, ErrorHook, HistoryContext, HistoryHook, HistoryOperation, HookRegistry, MessageContent, ObjectRequest, ObjectResponse, PostObjectResponseContext, PostObjectResponseHook, PostResponseContext, PostResponseHook, PreObjectRequestContext, PreObjectRequestHook, PreRequestContext, PreRequestHook, RequestMetadata, Role, ScopeChangeContext, ScopeChangeHook, Usage}
import chezwiz.agent.providers.*
import chez.derivation.Schema
import upickle.default.*

object HookSpec extends TestSuite:

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

    override def chat(request: ChatRequest): Either[ChezError, ChatResponse] = {
      Right(ChatResponse(
        content = s"Mock response to: ${request.messages.last.content match {
            case MessageContent.Text(text) => text
            case _ => "non-text content"
          }}",
        usage = Some(Usage(10, 20, 30)),
        model = request.model,
        finishReason = Some("stop")
      ))
    }

    override def generateObject(request: ObjectRequest)
        : Either[ChezError, ObjectResponse[ujson.Value]] = {
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
    override protected def parseResponse(responseBody: String): Either[ChezError, ChatResponse] =
      Right(ChatResponse("mock", None, "mock-model-1", Some("stop")))
    override protected def parseObjectResponse(responseBody: String)
        : Either[ChezError, ObjectResponse[ujson.Value]] = {
      Right(ObjectResponse[ujson.Value](
        data = ujson.Obj("name" -> "test", "value" -> 42),
        usage = None,
        model = "mock-model-1",
        finishReason = Some("stop")
      ))
    }

  // Test hook that tracks method calls
  class TestHook extends PreRequestHook with PostResponseHook with PreObjectRequestHook
      with PostObjectResponseHook with ErrorHook with HistoryHook with ScopeChangeHook {

    var preRequestCalls: List[PreRequestContext] = List.empty
    var postResponseCalls: List[PostResponseContext] = List.empty
    var preObjectRequestCalls: List[PreObjectRequestContext] = List.empty
    var postObjectResponseCalls: List[PostObjectResponseContext] = List.empty
    var errorCalls: List[ErrorContext] = List.empty
    var historyCalls: List[HistoryContext] = List.empty
    var scopeChangeCalls: List[ScopeChangeContext] = List.empty

    override def onPreRequest(context: PreRequestContext): Unit = {
      preRequestCalls = preRequestCalls :+ context
    }

    override def onPostResponse(context: PostResponseContext): Unit = {
      postResponseCalls = postResponseCalls :+ context
    }

    override def onPreObjectRequest(context: PreObjectRequestContext): Unit = {
      preObjectRequestCalls = preObjectRequestCalls :+ context
    }

    override def onPostObjectResponse(context: PostObjectResponseContext): Unit = {
      postObjectResponseCalls = postObjectResponseCalls :+ context
    }

    override def onError(context: ErrorContext): Unit = {
      errorCalls = errorCalls :+ context
    }

    override def onHistoryChange(context: HistoryContext): Unit = {
      historyCalls = historyCalls :+ context
    }

    override def onScopeChange(context: ScopeChangeContext): Unit = {
      scopeChangeCalls = scopeChangeCalls :+ context
    }

    def reset(): Unit = {
      preRequestCalls = List.empty
      postResponseCalls = List.empty
      preObjectRequestCalls = List.empty
      postObjectResponseCalls = List.empty
      errorCalls = List.empty
      historyCalls = List.empty
      scopeChangeCalls = List.empty
    }
  }

  val tests = Tests {

    test("PreRequestHook and PostResponseHook are executed on generateText") {
      val testHook = new TestHook()
      val hooks = HookRegistry.empty
        .addPreRequestHook(testHook)
        .addPostResponseHook(testHook)

      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
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

      // Generate text which should trigger hooks
      agent.generateText("Test message", metadata)

      // Verify hooks were called
      assert(testHook.preRequestCalls.size == 1)
      assert(testHook.postResponseCalls.size == 1)

      // Verify hook context data
      val preContext = testHook.preRequestCalls.head
      assert(preContext.agentName == "Test Agent")
      assert(preContext.model == "mock-model-1")
      assert(preContext.metadata == metadata)
      assert(preContext.request.messages.last.content == MessageContent.Text("Test message"))

      val postContext = testHook.postResponseCalls.head
      assert(postContext.agentName == "Test Agent")
      assert(postContext.model == "mock-model-1")
      assert(postContext.metadata == metadata)
      assert(postContext.response.isRight)
      assert(postContext.duration >= 0)
    }

    test("PreObjectRequestHook and PostObjectResponseHook are executed on generateObject") {
      val testHook = new TestHook()
      val hooks = HookRegistry.empty
        .addPreObjectRequestHook(testHook)
        .addPostObjectResponseHook(testHook)

      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1",
        hooks = hooks
      )

      // Generate object which should trigger hooks
      agent.generateObject[TestData]("Generate test data", defaultMetadata)

      // Verify hooks were called
      assert(testHook.preObjectRequestCalls.size == 1)
      assert(testHook.postObjectResponseCalls.size == 1)

      // Verify hook context data
      val preContext = testHook.preObjectRequestCalls.head
      assert(preContext.agentName == "Test Agent")
      assert(preContext.model == "mock-model-1")
      assert(preContext.targetType == "TestData")

      val postContext = testHook.postObjectResponseCalls.head
      assert(postContext.agentName == "Test Agent")
      assert(postContext.response.isRight)
      assert(postContext.duration >= 0)
      assert(postContext.targetType == "TestData")
    }

    test("HistoryHook is executed on history operations") {
      val testHook = new TestHook()
      val hooks = HookRegistry.empty.addHistoryHook(testHook)

      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1",
        hooks = hooks
      )

      // Test addChatMessage
      agent.addChatMessage(ChatMessage.text(Role.User, "Manual message"), defaultMetadata)
      assert(testHook.historyCalls.size == 1)
      assert(testHook.historyCalls.head.operation == HistoryOperation.Add)
      assert(testHook.historyCalls.head.message.isDefined)
      assert(
        testHook.historyCalls.head.message.get.content == MessageContent.Text("Manual message")
      )

      // Test clearHistory
      testHook.reset()
      agent.clearHistory(defaultMetadata)
      assert(testHook.historyCalls.size == 1)
      assert(testHook.historyCalls.head.operation == HistoryOperation.Clear)
      assert(testHook.historyCalls.head.historySize == 1) // Only system message remains

      // Test clearAllHistories
      testHook.reset()
      agent.clearAllHistories()
      assert(testHook.historyCalls.size == 1)
      assert(testHook.historyCalls.head.operation == HistoryOperation.ClearAll)
      assert(testHook.historyCalls.head.historySize == 0)
    }

    test("ErrorHook is executed on errors") {
      // Mock provider that always returns errors
      class ErrorProvider extends LLMProvider {
        override val name: String = "Error"
        override val supportedModels: List[String] = List("error-model")

        override def chat(request: ChatRequest): Either[ChezError, ChatResponse] = {
          Left(ChezError.NetworkError("Simulated network error", Some(500)))
        }

        override def generateObject(request: ObjectRequest)
            : Either[ChezError, ObjectResponse[ujson.Value]] = {
          Left(ChezError.ApiError("Simulated API error", Some("TEST_ERROR"), Some(400)))
        }

        override protected def buildHeaders(apiKey: String): Map[String, String] = Map.empty
        override protected def buildRequestBody(request: ChatRequest): ujson.Value = ujson.Obj()
        override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value =
          ujson.Obj()
        override protected def parseResponse(responseBody: String)
            : Either[ChezError, ChatResponse] =
          Right(ChatResponse("mock", None, "error-model", Some("stop")))
        override protected def parseObjectResponse(responseBody: String)
            : Either[ChezError, ObjectResponse[ujson.Value]] =
          Right(ObjectResponse[ujson.Value](ujson.Obj(), None, "error-model", Some("stop")))
      }

      val testHook = new TestHook()
      val hooks = HookRegistry.empty
        .addPreRequestHook(testHook)
        .addPostResponseHook(testHook)
        .addErrorHook(testHook)

      val errorProvider = new ErrorProvider()
      val agent = Agent(
        name = "Error Agent",
        instructions = "You are a helpful assistant",
        provider = errorProvider,
        model = "error-model",
        hooks = hooks
      )

      // Test error in generateText
      agent.generateText("This will fail", defaultMetadata)

      assert(testHook.errorCalls.size == 1)
      assert(testHook.errorCalls.head.agentName == "Error Agent")
      assert(testHook.errorCalls.head.operation == "generateText")
      assert(testHook.errorCalls.head.error.isInstanceOf[ChezError.NetworkError])

      // Test error in generateObject
      testHook.reset()
      agent.generateObject[TestData]("This will also fail", defaultMetadata)

      assert(testHook.errorCalls.size == 1)
      assert(testHook.errorCalls.head.operation == "generateObject")
      assert(testHook.errorCalls.head.error.isInstanceOf[ChezError.ApiError])
    }

    test("Multiple hooks are executed in order") {
      val hook1 = new TestHook()
      val hook2 = new TestHook()

      val hooks = HookRegistry.empty
        .addPreRequestHook(hook1)
        .addPreRequestHook(hook2)
        .addPostResponseHook(hook1)
        .addPostResponseHook(hook2)

      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Multi Hook Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1",
        hooks = hooks
      )

      agent.generateText("Test with multiple hooks", defaultMetadata)

      // Both hooks should have been called
      assert(hook1.preRequestCalls.size == 1)
      assert(hook2.preRequestCalls.size == 1)
      assert(hook1.postResponseCalls.size == 1)
      assert(hook2.postResponseCalls.size == 1)

      // Both should have the same context data
      assert(hook1.preRequestCalls.head.agentName == hook2.preRequestCalls.head.agentName)
      assert(hook1.postResponseCalls.head.agentName == hook2.postResponseCalls.head.agentName)
    }

    test("Hook failures do not break agent functionality") {
      // Hook that always throws exceptions
      class FailingHook extends PreRequestHook with PostResponseHook {
        override def onPreRequest(context: PreRequestContext): Unit = {
          throw new RuntimeException("Hook failure")
        }
        override def onPostResponse(context: PostResponseContext): Unit = {
          throw new RuntimeException("Hook failure")
        }
      }

      val failingHook = new FailingHook()
      val hooks = HookRegistry.empty
        .addPreRequestHook(failingHook)
        .addPostResponseHook(failingHook)

      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Resilient Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1",
        hooks = hooks
      )

      // Agent should still work despite hook failures
      val result = agent.generateText("Test with failing hooks", defaultMetadata)
      assert(result.isRight)
      result match {
        case Right(response) => assert(response.content.contains("Mock response"))
        case Left(_) => throw new Exception("Agent should have succeeded despite hook failures")
      }
    }

    test("HookRegistry utility methods work correctly") {
      val hook1 = new TestHook()
      val hook2 = new TestHook()

      // Empty registry
      val emptyRegistry = HookRegistry.empty
      assert(!emptyRegistry.hasAnyHooks)

      // Registry with hooks
      val populatedRegistry = emptyRegistry
        .addPreRequestHook(hook1)
        .addPostResponseHook(hook2)
        .addErrorHook(hook1)

      assert(populatedRegistry.hasAnyHooks)

      // Clear registry
      populatedRegistry.clear()
      assert(!populatedRegistry.hasAnyHooks)
    }

    test("HookRegistry can add and execute hooks of all types") {
      val testHook = new TestHook()

      val registry = HookRegistry.empty
        .addPreRequestHook(testHook)
        .addPostResponseHook(testHook)
        .addPreObjectRequestHook(testHook)
        .addPostObjectResponseHook(testHook)
        .addErrorHook(testHook)
        .addHistoryHook(testHook)
        .addScopeChangeHook(testHook)

      // Test pre-request hook execution
      val preRequestContext = PreRequestContext(
        agentName = "TestAgent",
        model = "test-model",
        request = ChatRequest(List(ChatMessage.text(Role.User, "test")), "test-model"),
        metadata = defaultMetadata
      )
      registry.executePreRequestHooks(preRequestContext)
      assert(testHook.preRequestCalls.size == 1)

      // Test post-response hook execution
      val postResponseContext = PostResponseContext(
        agentName = "TestAgent",
        model = "test-model",
        request = ChatRequest(List(ChatMessage.text(Role.User, "test")), "test-model"),
        response = Right(ChatResponse("response", None, "test-model", None)),
        metadata = defaultMetadata,
        requestTimestamp = 1000
      )
      registry.executePostResponseHooks(postResponseContext)
      assert(testHook.postResponseCalls.size == 1)

      // Test error hook execution
      val errorContext = ErrorContext(
        agentName = "TestAgent",
        model = "test-model",
        error = ChezError.NetworkError("test error", None),
        metadata = defaultMetadata,
        operation = "test-operation"
      )
      registry.executeErrorHooks(errorContext)
      assert(testHook.errorCalls.size == 1)

      // Test history hook execution
      val historyContext = HistoryContext(
        agentName = "TestAgent",
        metadata = defaultMetadata,
        operation = HistoryOperation.Add,
        message = Some(ChatMessage.text(Role.User, "test message")),
        historySize = 1,
        timestamp = System.currentTimeMillis()
      )
      registry.executeHistoryHooks(historyContext)
      assert(testHook.historyCalls.size == 1)

      // Test scope change hook execution
      val scopeChangeContext = ScopeChangeContext(
        agentName = "TestAgent",
        previousMetadata = None,
        newMetadata = defaultMetadata
      )
      registry.executeScopeChangeHooks(scopeChangeContext)
      assert(testHook.scopeChangeCalls.size == 1)
    }

    test("Hook context durations are calculated correctly") {
      val testHook = new TestHook()
      val hooks = HookRegistry.empty.addPostResponseHook(testHook)

      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Timing Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1",
        hooks = hooks
      )

      // Generate text and verify duration is positive
      agent.generateText("Test timing", defaultMetadata)

      assert(testHook.postResponseCalls.size == 1)
      val context = testHook.postResponseCalls.head
      assert(context.duration >= 0)
      assert(context.responseTimestamp >= context.requestTimestamp)
    }

    test("Hooks work correctly with generateTextWithoutHistory") {
      val testHook = new TestHook()
      val hooks = HookRegistry.empty
        .addPreRequestHook(testHook)
        .addPostResponseHook(testHook)

      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1",
        hooks = hooks
      )

      // Generate text without history
      agent.generateTextWithoutHistory("Test without history", defaultMetadata)

      // Verify hooks were called
      assert(testHook.preRequestCalls.size == 1)
      assert(testHook.postResponseCalls.size == 1)

      // Verify context
      val preContext = testHook.preRequestCalls.head
      assert(
        preContext.request.messages.exists(_.content == MessageContent.Text("Test without history"))
      )
    }

    test("Hooks work correctly with generateObjectWithoutHistory") {
      val testHook = new TestHook()
      val hooks = HookRegistry.empty
        .addPreObjectRequestHook(testHook)
        .addPostObjectResponseHook(testHook)

      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1",
        hooks = hooks
      )

      // Generate object without history
      agent.generateObjectWithoutHistory[TestData]("Generate without history", defaultMetadata)

      // Verify hooks were called
      assert(testHook.preObjectRequestCalls.size == 1)
      assert(testHook.postObjectResponseCalls.size == 1)

      // Verify context
      val preContext = testHook.preObjectRequestCalls.head
      assert(preContext.targetType == "TestData")
    }

    test("Error context includes request information") {
      class ErrorProvider extends LLMProvider {
        override val name: String = "Error"
        override val supportedModels: List[String] = List("error-model")

        override def chat(request: ChatRequest): Either[ChezError, ChatResponse] = {
          Left(ChezError.NetworkError("Network failed", Some(503)))
        }

        override def generateObject(request: ObjectRequest)
            : Either[ChezError, ObjectResponse[ujson.Value]] = {
          Left(ChezError.ApiError("Too many requests", Some("RATE_LIMIT"), Some(429)))
        }

        override protected def buildHeaders(apiKey: String): Map[String, String] = Map.empty
        override protected def buildRequestBody(request: ChatRequest): ujson.Value = ujson.Obj()
        override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value =
          ujson.Obj()
        override protected def parseResponse(responseBody: String)
            : Either[ChezError, ChatResponse] =
          Right(ChatResponse("", None, "error-model", None))
        override protected def parseObjectResponse(responseBody: String)
            : Either[ChezError, ObjectResponse[ujson.Value]] =
          Right(ObjectResponse[ujson.Value](ujson.Obj(), None, "error-model", None))
      }

      val testHook = new TestHook()
      val hooks = HookRegistry.empty.addErrorHook(testHook)

      val errorProvider = new ErrorProvider()
      val agent = Agent(
        name = "Error Test Agent",
        instructions = "You are a helpful assistant",
        provider = errorProvider,
        model = "error-model",
        hooks = hooks
      )

      // Test chat error
      agent.generateText("This will fail", defaultMetadata)
      assert(testHook.errorCalls.size == 1)
      val chatError = testHook.errorCalls.head
      assert(chatError.request.isDefined)
      assert(chatError.request.get.isLeft) // ChatRequest

      // Test object error
      testHook.reset()
      agent.generateObject[TestData]("This will also fail", defaultMetadata)
      assert(testHook.errorCalls.size == 1)
      val objectError = testHook.errorCalls.head
      assert(objectError.request.isDefined)
      assert(objectError.request.get.isRight) // ObjectRequest
    }
  }
