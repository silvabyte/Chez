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
      agent.addChatMessage(ChatMessage(Role.User, "Different scope manual message"), differentMetadata)

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
  }
