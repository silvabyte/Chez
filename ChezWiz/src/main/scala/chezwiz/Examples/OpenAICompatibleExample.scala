package chezwiz.agent.examples

import chezwiz.agent.{Agent, RequestMetadata, ChatMessage, Role}
import chezwiz.agent.providers.{OpenAICompatibleProvider, HttpVersion}
import scribe.Logging
import upickle.default.*
import chez.derivation.Schema

object OpenAICompatibleExample extends App with Logging {

  Config.initialize(sys.env.getOrElse("ENV_DIR", os.pwd.toString))

  // Force HTTP/1.1 for Java 24 compatibility
  val useHttp11 = HttpVersion.Http11

  val llamaCppUrl = Config.get("LLAMA_CPP_URL", "http://localhost:39080/v1")
  val llamaCppModel = Config.get("LLAMA_CPP_MODEL", "qwen3-coder-30b-q5")

  val lmStudioUrl = Config.get("LM_STUDIO_URL", "http://localhost:1234/v1")
  val lmStudioModel = Config.get("LM_STUDIO_MODEL", "local-model")

  logger.info(
    s"Initialized config from directory: ${sys.env.getOrElse("ENV_DIR", os.pwd.toString)}"
  )

  println("OpenAI Compatible Provider Examples")
  println("=" * 50)

  // Example 1: Using the unified provider for LLamaCPP
  def testLLamaCPP(): Unit = {
    logger.info("Example 1: Testing LLamaCPP with unified provider")
    logger.info(s"LLamaCPP URL: $llamaCppUrl")
    logger.info(s"LLamaCPP Model: $llamaCppModel")

    val provider = OpenAICompatibleProvider(
      baseUrl = llamaCppUrl,
      modelId = llamaCppModel,
      strictModelValidation = false,
      httpVersion = HttpVersion.Http11
    )

    val agent = Agent(
      name = "LLamaCPP-Agent",
      instructions = "You are a helpful AI assistant running on a LLamaCPP server.",
      provider = provider,
      model = llamaCppModel
    )

    val metadata = RequestMetadata(
      tenantId = Some("test"),
      userId = Some("user1"),
      conversationId = Some("llamacpp-test")
    )

    logger.info("Making request to LLamaCPP server...")
    agent.generateText("Hello! Please introduce yourself briefly.", metadata) match {
      case Right(response) =>
        logger.info(s"✅ LLamaCPP Response: ${response.content}")
      case Left(error) =>
        logger.error(s"❌ LLamaCPP Error: $error")
    }
  }

  // Example 2: Using the unified provider as LM Studio replacement
  def testAsLMStudio(): Unit = {
    logger.info("Example 2: Testing LM Studio mode with unified provider")
    logger.info(s"LM Studio URL: $lmStudioUrl")
    logger.info(s"LM Studio Model: $lmStudioModel")

    val provider = OpenAICompatibleProvider(
      baseUrl = lmStudioUrl,
      modelId = lmStudioModel,
      httpVersion = HttpVersion.Http11, // LM Studio often works better with HTTP/1.1
      enableEmbeddings = true,
      strictModelValidation = false
    )

    val agent = Agent(
      name = "LMStudio-Agent",
      instructions = "You are a helpful AI assistant running on LM Studio.",
      provider = provider,
      model = lmStudioModel
    )

    val metadata = RequestMetadata(
      tenantId = Some("test"),
      userId = Some("user1"),
      conversationId = Some("lmstudio-test")
    )

    logger.info("Making request to LM Studio server...")
    agent.generateText("What are your capabilities?", metadata) match {
      case Right(response) =>
        logger.info(s"✅ LM Studio Response: ${response.content}")
      case Left(error) =>
        logger.error(s"❌ LM Studio Error: $error")
    }
  }

  // Example 3: Using the unified provider as CustomEndpoint replacement
  def testAsCustomEndpoint(): Unit = {
    logger.info("Example 3: Testing Custom Endpoint mode with unified provider")

    val provider = OpenAICompatibleProvider(
      baseUrl = llamaCppUrl,
      apiKey = "", // No auth for this local server
      modelId = llamaCppModel,
      supportedModels = List(llamaCppModel),
      customHeaders = Map("X-Custom-Header" -> "test-value"),
      enableEmbeddings = false,
      strictModelValidation = true,
      httpVersion = HttpVersion.Http11
    )

    val agent = Agent(
      name = "Custom-Agent",
      instructions = "You are a helpful AI assistant on a custom endpoint.",
      provider = provider,
      model = llamaCppModel
    )

    val metadata = RequestMetadata(
      tenantId = Some("test"),
      userId = Some("user1"),
      conversationId = Some("custom-test")
    )

    logger.info("Making request to custom endpoint...")
    agent.generateText("Explain the benefits of using unified providers.", metadata) match {
      case Right(response) =>
        logger.info(s"✅ Custom Endpoint Response: ${response.content}")
      case Left(error) =>
        logger.error(s"❌ Custom Endpoint Error: $error")
    }
  }

  // Example 4: Structured output test
  def testStructuredOutput(): Unit = {
    logger.info("Example 4: Testing structured output with unified provider")

    case class CodeReview(
        language: String,
        rating: Int,
        summary: String,
        improvements: List[String],
        positives: List[String]
    ) derives Schema, ReadWriter

    val provider = OpenAICompatibleProvider(
      baseUrl = llamaCppUrl,
      modelId = llamaCppModel,
      strictModelValidation = false,
      httpVersion = HttpVersion.Http11
    )

    val agent = Agent(
      name = "CodeReviewer",
      instructions = "You are a code reviewer that provides structured feedback.",
      provider = provider,
      model = llamaCppModel
    )

    val metadata = RequestMetadata()

    val codeToReview = """
def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n-1) + fibonacci(n-2)
"""

    agent.generateObject[CodeReview](
      s"Review this Python code and provide structured feedback: $codeToReview",
      metadata
    ) match {
      case Right(response) =>
        val review = response.data
        logger.info(s"✅ Structured Code Review:")
        logger.info(s"  Language: ${review.language}")
        logger.info(s"  Rating: ${review.rating}/10")
        logger.info(s"  Summary: ${review.summary}")
        logger.info(s"  Improvements: ${review.improvements.mkString(", ")}")
        logger.info(s"  Positives: ${review.positives.mkString(", ")}")
      case Left(error) =>
        logger.error(s"❌ Structured Output Error: $error")
    }
  }

  // Example 5: Embedding test (if supported)
  def testEmbeddings(): Unit = {
    logger.info("Example 5: Testing embeddings with unified provider")

    val provider = OpenAICompatibleProvider(
      baseUrl = lmStudioUrl,
      modelId = "text-embedding-model", // Assuming an embedding model
      enableEmbeddings = true,
      strictModelValidation = false,
      httpVersion = HttpVersion.Http11
    )

    val agent = Agent(
      name = "Embedding-Agent",
      instructions = "Generate embeddings for text.",
      provider = provider,
      model = "text-embedding-model"
    )

    val metadata = RequestMetadata()

    if (provider.supportsEmbeddings) {
      agent.generateEmbedding("Hello world, this is a test sentence.", metadata = metadata) match {
        case Right(response) =>
          logger.info(s"✅ Embedding generated:")
          logger.info(s"  Dimensions: ${response.dimensions}")
          logger.info(s"  Model: ${response.model}")
          val preview = response.embeddings.head.values.take(5)
          logger.info(s"  Values preview: [${preview.mkString(", ")}...]")
        case Left(error) =>
          logger.error(s"❌ Embedding Error: $error")
      }
    } else {
      logger.info("⚠️  Embeddings not enabled for this provider configuration")
    }
  }

  // Run examples
  try {
    testLLamaCPP()
    println()

    // testAsLMStudio()  // Uncomment if you have LM Studio running
    // println()

    testAsCustomEndpoint()
    println()

    testStructuredOutput()
    println()

    // testEmbeddings()  // Uncomment if you have embedding models

    logger.info("🎉 All tests completed!")

  } catch {
    case ex: Exception =>
      logger.error(s"💥 Test failed with exception: ${ex.getMessage}")
      ex.printStackTrace()
  }
}
