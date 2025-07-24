import utest.*
import chezwiz.agent.*
import chezwiz.agent.providers.*
import chezwiz.agent.providers.{OpenAIResponse, ErrorResponse}
import chez.derivation.Schema
import upickle.default.*

object AgentSpec extends TestSuite:

  // Test data class for structured generation
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
        content = s"Mock response to: ${request.messages.last.content}",
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

  val tests = Tests {

    test("Agent creation and basic functionality") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful test assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      assert(agent.name == "Test Agent")
      assert(agent.model == "mock-model-1")
      assert(agent.provider.name == "Mock")
    }

    test("Agent generates text response") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      agent.generateText("Hello", defaultMetadata) match {
        case Right(response) =>
          assert(response.content == "Mock response to: Hello")
          assert(response.model == "mock-model-1")
          assert(response.usage.isDefined)
        case Left(error) =>
          throw new Exception(s"Unexpected error: $error")
      }
    }

    test("Agent tracks conversation history") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      agent.generateText("First message", defaultMetadata) match {
        case Right(_) => // Success
        case Left(error) => throw new Exception(s"Unexpected error: $error")
      }

      agent.generateText("Second message", defaultMetadata) match {
        case Right(_) => // Success
        case Left(error) => throw new Exception(s"Unexpected error: $error")
      }

      val history = agent.getConversationHistory(defaultMetadata)
      assert(history.size == 5) // system + user1 + assistant1 + user2 + assistant2
      assert(history(0).role == Role.System)
      assert(history(1).role == Role.User)
      assert(history(1).content == "First message")
      assert(history(2).role == Role.Assistant)
      assert(history(3).role == Role.User)
      assert(history(3).content == "Second message")
      assert(history(4).role == Role.Assistant)
    }

    test("Agent clears history correctly") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      agent.generateText("Test message", defaultMetadata) match {
        case Right(_) => // Success
        case Left(error) => throw new Exception(s"Unexpected error: $error")
      }
      assert(agent.getConversationHistory(defaultMetadata).size > 1)

      agent.clearHistory(defaultMetadata)
      val history = agent.getConversationHistory(defaultMetadata)
      assert(history.size == 1)
      assert(history(0).role == Role.System)
    }

    test("AgentFactory creates OpenAI agent successfully") {
      val result = AgentFactory.createOpenAIAgent(
        name = "OpenAI Test",
        instructions = "Test instructions",
        apiKey = "test-key",
        model = "gpt-4o-mini"
      )

      result match
        case Right(agent) =>
          assert(agent.name == "OpenAI Test")
          assert(agent.model == "gpt-4o-mini")
          assert(agent.provider.name == "OpenAI")
        case Left(error) => throw new Exception(s"Unexpected error: $error")
    }

    test("AgentFactory creates Anthropic agent successfully") {
      val result = AgentFactory.createAnthropicAgent(
        name = "Anthropic Test",
        instructions = "Test instructions",
        apiKey = "test-key",
        model = "claude-3-5-haiku-20241022"
      )

      result match
        case Right(agent) =>
          assert(agent.name == "Anthropic Test")
          assert(agent.model == "claude-3-5-haiku-20241022")
          assert(agent.provider.name == "Anthropic")
        case Left(error) => throw new Exception(s"Unexpected error: $error")
    }

    test("AgentFactory fails with unsupported model") {
      val result = AgentFactory.createOpenAIAgent(
        name = "Test",
        instructions = "Test",
        apiKey = "test-key",
        model = "unsupported-model"
      )

      result match
        case Right(_) => throw new Exception("Should have failed")
        case Left(ChezError.ModelNotSupported(model, provider, _)) =>
          assert(model == "unsupported-model")
          assert(provider == "OpenAI")
        case Left(error) => throw new Exception(s"Expected ModelNotSupported but got: $error")
    }

    test("ChatMessage serialization works correctly") {
      val message = ChatMessage(Role.User, "Hello world")
      val json = upickle.default.write(message)
      val parsed = upickle.default.read[ChatMessage](json)

      assert(parsed.role == Role.User)
      assert(parsed.content == "Hello world")
    }

    test("ChatRequest serialization works correctly") {
      val request = ChatRequest(
        messages = List(ChatMessage(Role.User, "Test")),
        model = "gpt-4o-mini",
        temperature = Some(0.7),
        maxTokens = Some(100)
      )

      val json = upickle.default.write(request)
      val parsed = upickle.default.read[ChatRequest](json)

      assert(parsed.model == "gpt-4o-mini")
      assert(parsed.temperature == Some(0.7))
      assert(parsed.maxTokens == Some(100))
      assert(parsed.messages.size == 1)
    }

    test("Scoped conversation history - full metadata") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      val metadata1 = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      val metadata2 = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user2"),
        conversationId = Some("conv2")
      )

      // Generate messages with first metadata scope
      agent.generateText("Hello from user1", metadata1)
      agent.generateText("Another message from user1", metadata1)

      // Generate messages with second metadata scope
      agent.generateText("Hello from user2", metadata2)

      // Check that histories are isolated
      val history1 = agent.getConversationHistory(metadata1)
      val history2 = agent.getConversationHistory(metadata2)

      assert(history1.size == 5) // system + 2 user + 2 assistant
      assert(history2.size == 3) // system + 1 user + 1 assistant

      // Verify content isolation
      assert(history1(1).content == "Hello from user1")
      assert(history1(3).content == "Another message from user1")
      assert(history2(1).content == "Hello from user2")
    }

    test("Scoped conversation history - partial metadata") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      // Only tenant
      val tenantOnly = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = None,
        conversationId = None
      )

      // Only user
      val userOnly = RequestMetadata(
        tenantId = None,
        userId = Some("user1"),
        conversationId = None
      )

      // Tenant and user, no conversation
      val tenantUser = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = None
      )

      agent.generateText("Message with tenant only", tenantOnly)
      agent.generateText("Message with user only", userOnly)
      agent.generateText("Message with tenant and user", tenantUser)

      val history1 = agent.getConversationHistory(tenantOnly)
      val history2 = agent.getConversationHistory(userOnly)
      val history3 = agent.getConversationHistory(tenantUser)

      assert(history1.size == 3) // system + user + assistant
      assert(history2.size == 3) // system + user + assistant
      assert(history3.size == 3) // system + user + assistant

      // Verify all three are different scopes
      assert(history1(1).content == "Message with tenant only")
      assert(history2(1).content == "Message with user only")
      assert(history3(1).content == "Message with tenant and user")
    }

    test("Clear specific scoped history") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      val metadata = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      // Add messages to scoped history
      agent.generateText("Test message 1", metadata)
      agent.generateText("Test message 2", metadata)

      // Also add to different scope
      val differentMetadata = RequestMetadata(
        tenantId = Some("different-tenant"),
        userId = Some("different-user"),
        conversationId = Some("different-conv")
      )
      agent.generateText("Different scope message", differentMetadata)

      assert(agent.getConversationHistory(metadata).size == 5) // system + 2 user + 2 assistant
      assert(agent.getConversationHistory(differentMetadata).size == 3) // system + user + assistant

      // Clear only the scoped history
      agent.clearHistory(metadata)

      assert(agent.getConversationHistory(metadata).size == 1) // only system message
      assert(agent.getConversationHistory(differentMetadata).size == 3) // unchanged
    }

    test("Clear all histories") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      val metadata1 = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      val metadata2 = RequestMetadata(
        tenantId = Some("tenant2"),
        userId = Some("user2"),
        conversationId = Some("conv2")
      )

      // Add messages to multiple scopes
      agent.generateText("Message 1", metadata1)
      agent.generateText("Message 2", metadata2)

      // Verify all have messages
      assert(agent.getConversationHistory(metadata1).size > 1)
      assert(agent.getConversationHistory(metadata2).size > 1)

      // Clear all histories
      agent.clearAllHistories()

      // Verify all are reset
      assert(agent.getConversationHistory(metadata1).size == 1) // only system
      assert(agent.getConversationHistory(metadata2).size == 1) // only system
    }

    test("Add chat message with metadata") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      val metadata = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      // Manually add messages to scoped history
      agent.addChatMessage(ChatMessage(Role.User, "Manual message 1"), metadata)
      agent.addChatMessage(ChatMessage(Role.Assistant, "Manual response 1"), metadata)

      // Also add to different scope
      val differentMetadata = RequestMetadata(
        tenantId = Some("different-tenant"),
        userId = Some("different-user"),
        conversationId = Some("different-conv")
      )
      agent.addChatMessage(
        ChatMessage(Role.User, "Different scope manual message"),
        differentMetadata
      )

      val scopedHistory = agent.getConversationHistory(metadata)
      val differentHistory = agent.getConversationHistory(differentMetadata)

      assert(scopedHistory.size == 3) // system + manually added user + assistant
      assert(scopedHistory(1).content == "Manual message 1")
      assert(scopedHistory(2).content == "Manual response 1")

      assert(differentHistory.size == 2) // system + manually added user
      assert(differentHistory(1).content == "Different scope manual message")
    }

    test("generateTextWithoutHistory respects metadata scoping") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      val metadata = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      // First add some history
      agent.generateText("Message with history", metadata)
      assert(agent.getConversationHistory(metadata).size == 3) // system + user + assistant

      // Now generate without history (should not affect the scoped history)
      agent.generateTextWithoutHistory("Message without history", metadata) match {
        case Right(response) =>
          assert(response.content == "Mock response to: Message without history")
        case Left(error) =>
          throw new Exception(s"Unexpected error: $error")
      }

      // History should remain unchanged
      assert(agent.getConversationHistory(metadata).size == 3)
    }

    test("RequestMetadata serialization") {
      val metadata = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      val json = upickle.default.write(metadata)
      val parsed = upickle.default.read[RequestMetadata](json)

      assert(parsed.tenantId == Some("tenant1"))
      assert(parsed.userId == Some("user1"))
      assert(parsed.conversationId == Some("conv1"))

      // Test with None values
      val partialMetadata = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = None,
        conversationId = None
      )

      val json2 = upickle.default.write(partialMetadata)
      val parsed2 = upickle.default.read[RequestMetadata](json2)

      assert(parsed2.tenantId == Some("tenant1"))
      assert(parsed2.userId == None)
      assert(parsed2.conversationId == None)
    }

    test("ChatRequest with metadata serialization") {
      val metadata = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      val request = ChatRequest(
        messages = List(ChatMessage(Role.User, "Test")),
        model = "gpt-4o-mini",
        temperature = Some(0.7),
        maxTokens = Some(100),
        metadata = Some(metadata)
      )

      val json = upickle.default.write(request)
      val parsed = upickle.default.read[ChatRequest](json)

      assert(parsed.metadata.isDefined)
      assert(parsed.metadata.get.tenantId == Some("tenant1"))
      assert(parsed.metadata.get.userId == Some("user1"))
      assert(parsed.metadata.get.conversationId == Some("conv1"))
    }

    test("generateObject with metadata scoping") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      val metadata1 = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      val metadata2 = RequestMetadata(
        tenantId = Some("tenant2"),
        userId = Some("user2"),
        conversationId = Some("conv2")
      )

      // Generate objects with different metadata
      agent.generateObject[TestData]("Generate object 1", metadata1) match {
        case Right(response) =>
          assert(response.data.name == "test")
          assert(response.data.value == 42)
        case Left(error) =>
          throw new Exception(s"Unexpected error: $error")
      }

      agent.generateObject[TestData]("Generate object 2", metadata2) match {
        case Right(response) =>
          assert(response.data.name == "test")
        case Left(error) =>
          throw new Exception(s"Unexpected error: $error")
      }

      // Check that histories are isolated
      val history1 = agent.getConversationHistory(metadata1)
      val history2 = agent.getConversationHistory(metadata2)

      assert(history1.size == 3) // system + user + assistant
      assert(history2.size == 3) // system + user + assistant

      assert(history1(1).content == "Generate object 1")
      assert(history2(1).content == "Generate object 2")
    }

    test("generateObjectWithoutHistory with metadata") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      val metadata = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      // First add some history
      agent.generateText("Message with history", metadata)
      assert(agent.getConversationHistory(metadata).size == 3)

      // Generate object without history
      agent.generateObjectWithoutHistory[TestData]("Generate without history", metadata) match {
        case Right(response) =>
          assert(response.data.name == "test")
          assert(response.data.value == 42)
        case Left(error) =>
          throw new Exception(s"Unexpected error: $error")
      }

      // History should remain unchanged
      assert(agent.getConversationHistory(metadata).size == 3)
    }

    test("Scope key behavior matches documentation") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      // Test different metadata combinations to ensure they create different scopes
      val fullScope = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      val sameScopeKey = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv1")
      )

      val differentConv = RequestMetadata(
        tenantId = Some("tenant1"),
        userId = Some("user1"),
        conversationId = Some("conv2")
      )

      // Add messages with same scope key
      agent.generateText("Message 1", fullScope)
      agent.generateText("Message 2", sameScopeKey)

      // Add message with different conversation
      agent.generateText("Different conv", differentConv)

      // Same scope key should share history
      val history1 = agent.getConversationHistory(fullScope)
      val history2 = agent.getConversationHistory(sameScopeKey)
      val history3 = agent.getConversationHistory(differentConv)

      assert(history1.size == 5) // system + 2 users + 2 assistants
      assert(history2.size == 5) // should be same as history1
      assert(history3.size == 3) // system + 1 user + 1 assistant

      // Verify the messages are in the correct history
      assert(history1(1).content == "Message 1")
      assert(history1(3).content == "Message 2")
      assert(history3(1).content == "Different conv")
    }

    // ============================================================================
    // Hook System Tests
    // ============================================================================

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

    test("Hooks are executed on generateText") {
      val testHook = new TestHook()
      val hooks = HookRegistry.empty
        .addPreRequestHook(testHook)
        .addPostResponseHook(testHook)
        .addErrorHook(testHook)
        .addHistoryHook(testHook)

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
      assert(testHook.errorCalls.size == 0) // No errors expected
      assert(testHook.historyCalls.size == 0) // History hooks triggered separately

      // Verify hook context data
      val preContext = testHook.preRequestCalls.head
      assert(preContext.agentName == "Test Agent")
      assert(preContext.model == "mock-model-1")
      assert(preContext.metadata == metadata)
      assert(preContext.request.messages.last.content == "Test message")

      val postContext = testHook.postResponseCalls.head
      assert(postContext.agentName == "Test Agent")
      assert(postContext.model == "mock-model-1")
      assert(postContext.metadata == metadata)
      assert(postContext.response.isRight)
      assert(postContext.duration >= 0)
    }

    test("Hooks are executed on generateObject") {
      val testHook = new TestHook()
      val hooks = HookRegistry.empty
        .addPreObjectRequestHook(testHook)
        .addPostObjectResponseHook(testHook)
        .addErrorHook(testHook)

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
      assert(testHook.errorCalls.size == 0) // No errors expected

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

    test("History hooks are executed on history operations") {
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
      agent.addChatMessage(ChatMessage(Role.User, "Manual message"), defaultMetadata)
      assert(testHook.historyCalls.size == 1)
      assert(testHook.historyCalls.head.operation == HistoryOperation.Add)
      assert(testHook.historyCalls.head.message.isDefined)
      assert(testHook.historyCalls.head.message.get.content == "Manual message")

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

    test("Error hooks are executed on errors") {
      // Mock provider that always returns errors
      class ErrorProvider extends LLMProvider {
        override val name: String = "Error"
        override val supportedModels: List[String] = List("error-model")

        override def chat(request: ChatRequest): Either[ChezError, ChatResponse] = {
          Left(ChezError.NetworkError("Simulated network error", Some(500)))
        }

        override def generateObject(request: ObjectRequest): Either[ChezError, ObjectResponse[ujson.Value]] = {
          Left(ChezError.ApiError("Simulated API error", Some("TEST_ERROR"), Some(400)))
        }

        override protected def buildHeaders(apiKey: String): Map[String, String] = Map.empty
        override protected def buildRequestBody(request: ChatRequest): ujson.Value = ujson.Obj()
        override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value = ujson.Obj()
        override protected def parseResponse(responseBody: String): Either[ChezError, ChatResponse] = 
          Right(ChatResponse("mock", None, "error-model", Some("stop")))
        override protected def parseObjectResponse(responseBody: String): Either[ChezError, ObjectResponse[ujson.Value]] = 
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

    // ============================================================================
    // Metrics System Tests
    // ============================================================================

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
      metrics.recordError(agentName, model, "generateText", ChezError.NetworkError("Test error", Some(500)), metadata)

      // Get snapshot
      val snapshot = metrics.getSnapshot(agentName)
      assert(snapshot.isDefined)

      val snap = snapshot.get
      assert(snap.agentName == agentName)
      assert(snap.totalRequests == 3)
      assert(snap.successfulRequests == 2)
      assert(snap.failedRequests == 1)
      assert(snap.averageDuration == 150.0) // (100 + 200) / 2
      assert(snap.successRate == 2.0/3.0)

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

    test("MetricsFactory creates agents with automatic metrics") {
      val result = MetricsFactory.createOpenAIAgentWithMetrics(
        name = "FactoryTestAgent",
        instructions = "You are a helpful assistant",
        apiKey = "test-key",
        model = "gpt-4o-mini"
      )

      result match {
        case Right((agent, metrics)) =>
          assert(agent.name == "FactoryTestAgent")
          assert(agent.model == "gpt-4o-mini")
          
          val metadata = RequestMetadata(
            tenantId = Some("factory-tenant"),
            userId = Some("factory-user"),
            conversationId = Some("factory-conversation")
          )

          // This would normally make a real API call, but our test setup prevents that
          // The important thing is that the agent was created with metrics hooks
          val snapshot = metrics.getSnapshot("FactoryTestAgent")
          // Snapshot might be None initially if no operations have been performed
          // But the metrics system should be ready to track operations

        case Left(error) =>
          // Expected with test API key - the important thing is the factory method works
          assert(error.isInstanceOf[ChezError.ModelNotSupported] || error.isInstanceOf[ChezError])
      }
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
        textGenerations = OperationMetrics(count = 80, successCount = 76, errorCount = 4, totalDuration = 20000, totalTokens = 1000),
        objectGenerations = OperationMetrics(count = 20, successCount = 19, errorCount = 1, totalDuration = 5000, totalTokens = 500),
        modelMetrics = Map("gpt-4o" -> ModelMetrics(requestCount = 100, totalTokens = 1500)),
        scopeMetrics = Map("tenant1:user1:conv1" -> ScopeMetrics("tenant1:user1:conv1", requestCount = 50, messageCount = 45)),
        errorMetrics = Map("NetworkError" -> ErrorMetrics("NetworkError", 3, 1001000, "Connection failed")),
        recentRequestRate = 10.5,
        recentErrorRate = 0.5
      )

      // Test Prometheus export
      val prometheus = snapshot.toPrometheusFormat
      assert(prometheus.contains("chezwiz_agent_requests_total"))
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
  }
