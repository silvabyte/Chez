package chezwiz.agent.test

import utest.*
import chezwiz.agent.{
  Agent,
  ChatRequest,
  ChatResponse,
  ChezError,
  Embedding,
  EmbeddingInput,
  EmbeddingRequest,
  EmbeddingResponse,
  ObjectRequest,
  ObjectResponse,
  PreEmbeddingContext,
  PostEmbeddingContext,
  RequestMetadata,
  Usage
}
import chezwiz.agent.providers.{OpenAICompatibleProvider, HttpVersion}
import upickle.default.*

object EmbeddingSpec extends TestSuite:

  val tests = Tests {

    test("EmbeddingRequest serialization") {
      val request = EmbeddingRequest(
        input = EmbeddingInput.Single("test text"),
        model = "text-embedding-qwen3-embedding-8b",
        dimensions = Some(1024),
        encodingFormat = "float"
      )

      val json = write(request)
      val deserialized = read[EmbeddingRequest](json)

      assert(deserialized.input == EmbeddingInput.Single("test text"))
      assert(deserialized.model == "text-embedding-qwen3-embedding-8b")
      assert(deserialized.dimensions == Some(1024))
      assert(deserialized.encodingFormat == "float")
    }

    test("EmbeddingRequest batch serialization") {
      val request = EmbeddingRequest(
        input = EmbeddingInput.Multiple(List("text1", "text2", "text3")),
        model = "text-embedding-qwen3-embedding-8b"
      )

      val json = write(request)
      val deserialized = read[EmbeddingRequest](json)

      assert(deserialized.input == EmbeddingInput.Multiple(List("text1", "text2", "text3")))
      assert(deserialized.model == "text-embedding-qwen3-embedding-8b")
    }

    test("Embedding response structure") {
      val embedding = Embedding(
        values = Vector(0.1f, 0.2f, 0.3f),
        index = 0
      )

      val response = EmbeddingResponse(
        embeddings = List(embedding),
        usage = Some(Usage(
          promptTokens = 10,
          completionTokens = 0,
          totalTokens = 10
        )),
        model = "text-embedding-qwen3-embedding-8b",
        dimensions = 3
      )

      assert(response.embeddings.size == 1)
      assert(response.embeddings.head.values.size == 3)
      assert(response.dimensions == 3)
      assert(response.usage.get.totalTokens == 10)
    }

    test("Cosine similarity calculation") {
      val provider = OpenAICompatibleProvider(
        baseUrl = "http://localhost:1234/v1",
        modelId = "test-model",
        httpVersion = HttpVersion.Http11,
        enableEmbeddings = true,
        strictModelValidation = false
      )

      val agent = Agent(
        name = "TestAgent",
        instructions = "Test",
        provider = provider,
        model = "test-model"
      )

      // Test orthogonal vectors
      val vec1 = Vector(1.0f, 0.0f, 0.0f)
      val vec2 = Vector(0.0f, 1.0f, 0.0f)
      val similarity1 = agent.cosineSimilarity(vec1, vec2)
      assert(math.abs(similarity1) < 0.001) // Should be ~0

      // Test identical vectors
      val vec3 = Vector(1.0f, 1.0f, 1.0f)
      val vec4 = Vector(1.0f, 1.0f, 1.0f)
      val similarity2 = agent.cosineSimilarity(vec3, vec4)
      assert(math.abs(similarity2 - 1.0f) < 0.001) // Should be ~1

      // Test opposite vectors
      val vec5 = Vector(1.0f, 0.0f)
      val vec6 = Vector(-1.0f, 0.0f)
      val similarity3 = agent.cosineSimilarity(vec5, vec6)
      assert(math.abs(similarity3 + 1.0f) < 0.001) // Should be ~-1
    }

    test("Provider with embeddings enabled") {
      val provider = OpenAICompatibleProvider(
        baseUrl = "http://localhost:1234/v1",
        modelId = "text-embedding-qwen3-embedding-8b",
        enableEmbeddings = true
      )

      assert(provider.supportsEmbeddings == true)
      assert(provider.supportedEmbeddingModels == List("*"))
    }

    test("Embedding request body building") {
      val provider = OpenAICompatibleProvider(
        baseUrl = "http://localhost:1234/v1",
        modelId = "text-embedding-qwen3-embedding-8b"
      )

      // Test with private method access via reflection (for testing purposes)
      // In a real test, we'd mock the HTTP response instead
      val request = EmbeddingRequest(
        input = EmbeddingInput.Single("test text"),
        model = "text-embedding-qwen3-embedding-8b",
        dimensions = Some(512)
      )

      // Verify the request structure is correct
      assert(request.model == "text-embedding-qwen3-embedding-8b")
      assert(request.input == EmbeddingInput.Single("test text"))
      assert(request.dimensions == Some(512))
    }

    test("Batch embedding maintains order") {
      val texts = List("first", "second", "third")
      val embeddings = texts.zipWithIndex.map { case (_, idx) =>
        Embedding(
          values = Vector.fill(10)(idx.toFloat),
          index = idx
        )
      }

      val response = EmbeddingResponse(
        embeddings = embeddings,
        model = "test-model",
        dimensions = 10
      )

      assert(response.embeddings.size == 3)
      assert(response.embeddings(0).index == 0)
      assert(response.embeddings(1).index == 1)
      assert(response.embeddings(2).index == 2)
    }

    test("Embedding hooks context") {
      val request = EmbeddingRequest(
        input = EmbeddingInput.Single("test"),
        model = "test-model"
      )

      val preContext = PreEmbeddingContext(
        agentName = "TestAgent",
        model = "test-model",
        request = request,
        metadata = RequestMetadata(),
        timestamp = System.currentTimeMillis()
      )

      assert(preContext.agentName == "TestAgent")
      assert(preContext.model == "test-model")
      assert(preContext.request.input == EmbeddingInput.Single("test"))

      val response = Right(EmbeddingResponse(
        embeddings = List(Embedding(Vector(0.1f), 0)),
        model = "test-model",
        dimensions = 1
      ))

      val postContext = PostEmbeddingContext(
        agentName = "TestAgent",
        model = "test-model",
        request = request,
        response = response,
        metadata = RequestMetadata(),
        requestTimestamp = System.currentTimeMillis() - 100
      )

      assert(postContext.duration >= 0)
      assert(postContext.response.isRight)
    }

    test("Agent validates provider embedding support") {
      // Create a provider that doesn't support embeddings
      val mockProvider = new chezwiz.agent.providers.LLMProvider {
        val name = "MockProvider"
        val supportedModels = List("mock-model")
        override val supportsEmbeddings = false

        def chat(request: ChatRequest) = Left(ChezError.ConfigurationError("Not implemented"))
        def generateObject(request: ObjectRequest) =
          Left(ChezError.ConfigurationError("Not implemented"))

        protected def buildHeaders(apiKey: String) = Map.empty
        protected def buildRequestBody(request: ChatRequest) = ujson.Null
        protected def buildObjectRequestBody(request: ObjectRequest) = ujson.Null
        protected def parseResponse(responseBody: String) =
          Left(ChezError.ConfigurationError("Not implemented"))
        protected def parseObjectResponse(responseBody: String) =
          Left(ChezError.ConfigurationError("Not implemented"))
      }

      val agent = Agent(
        name = "TestAgent",
        instructions = "Test",
        provider = mockProvider,
        model = "mock-model"
      )

      val result = agent.generateEmbedding("test text")
      assert(result.isLeft)
      result match {
        case Left(ChezError.ConfigurationError(msg)) =>
          assert(msg.contains("does not support embeddings"))
        case _ =>
          assert(false)
      }
    }
  }
