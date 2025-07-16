import utest.*
import chezwiz.agent.*
import chezwiz.agent.providers.*
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration.*
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object AgentSpec extends TestSuite:

  // Mock LLM Provider for testing
  class MockLLMProvider extends LLMProvider:
    override val name: String = "Mock"
    override val supportedModels: List[String] = List("mock-model-1", "mock-model-2")

    override def chat(request: ChatRequest): Future[ChatResponse] =
      Future.successful(
        ChatResponse(
          content = s"Mock response to: ${request.messages.last.content}",
          usage = Some(Usage(10, 20, 30)),
          model = request.model,
          finishReason = Some("stop")
        )
      )

    override def generateObject(request: ObjectRequest): Future[ObjectResponse] =
      Future.successful(
        ObjectResponse(
          `object` = ujson.Obj("mock" -> "response"),
          usage = Some(Usage(10, 20, 30)),
          model = request.model,
          finishReason = Some("stop")
        )
      )

    override protected def buildHeaders(apiKey: String): Map[String, String] = Map.empty
    override protected def buildRequestBody(request: ChatRequest): ujson.Value = ujson.Obj()
    override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value = ujson.Obj()
    override protected def parseResponse(responseBody: String): scala.util.Try[ChatResponse] = ???
    override protected def parseObjectResponse(responseBody: String)
        : scala.util.Try[ObjectResponse] = ???

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

      val response = Await.result(agent.generateText("Hello"), 5.seconds)
      assert(response.content == "Mock response to: Hello")
      assert(response.model == "mock-model-1")
      assert(response.usage.isDefined)
    }

    test("Agent tracks conversation history") {
      val mockProvider = new MockLLMProvider()
      val agent = Agent(
        name = "Test Agent",
        instructions = "You are a helpful assistant",
        provider = mockProvider,
        model = "mock-model-1"
      )

      Await.result(agent.generateText("First message"), 5.seconds)
      Await.result(agent.generateText("Second message"), 5.seconds)

      val history = agent.getConversationHistory
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

      Await.result(agent.generateText("Test message"), 5.seconds)
      assert(agent.getConversationHistory.size > 1)

      agent.clearHistory()
      val history = agent.getConversationHistory
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
        case Success(agent) =>
          assert(agent.name == "OpenAI Test")
          assert(agent.model == "gpt-4o-mini")
          assert(agent.provider.name == "OpenAI")
        case Failure(ex) => throw ex
    }

    test("AgentFactory creates Anthropic agent successfully") {
      val result = AgentFactory.createAnthropicAgent(
        name = "Anthropic Test",
        instructions = "Test instructions",
        apiKey = "test-key",
        model = "claude-3-5-haiku-20241022"
      )

      result match
        case Success(agent) =>
          assert(agent.name == "Anthropic Test")
          assert(agent.model == "claude-3-5-haiku-20241022")
          assert(agent.provider.name == "Anthropic")
        case Failure(ex) => throw ex
    }

    test("AgentFactory fails with unsupported model") {
      val result = AgentFactory.createOpenAIAgent(
        name = "Test",
        instructions = "Test",
        apiKey = "test-key",
        model = "unsupported-model"
      )

      result match
        case Success(_) => throw new Exception("Should have failed")
        case Failure(ex) => assert(ex.getMessage.contains("not supported by OpenAI provider"))
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
  }
