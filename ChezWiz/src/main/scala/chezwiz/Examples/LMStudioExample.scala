package chezwiz.agent.examples

import chezwiz.agent.{Agent, AgentFactory, RequestMetadata}
import chezwiz.agent.providers.{CustomEndpointProvider, HttpVersion}
import scribe.Logging

object LMStudioExample extends App with Logging:

  Config.initialize(sys.env.getOrElse("ENV_DIR", os.pwd.toString))

  logger.info(
    s"Initialized config from directory: ${sys.env.getOrElse("ENV_DIR", os.pwd.toString)}"
  )

  // Example 1: Using LM Studio with default settings
  def exampleBasicLMStudio(): Unit = {
    logger.info("Example 1: Basic LM Studio usage")

    // Get URL from environment variable or use default
    val lmStudioUrl = Config.get("LM_STUDIO_URL", "http://localhost:1234/v1")
    val modelId = Config.get("LM_STUDIO_MODEL", "local-model")

    logger.info(s"LM Studio URL: $lmStudioUrl")
    logger.info(s"LM Studio Model: $modelId")

    // LM Studio uses HTTP/1.1 for compatibility with local servers
    val provider = CustomEndpointProvider.forLMStudio(
      baseUrl = lmStudioUrl,
      modelId = modelId
      // httpVersion defaults to Http11 for LM Studio
    )

    val agent = Agent(
      name = "LMStudioAssistant",
      instructions = "You are a helpful AI assistant running on a local LM Studio server.",
      provider = provider,
      model = modelId
    )

    val metadata = RequestMetadata(
      tenantId = Some("local"),
      userId = Some("user1"),
      conversationId = Some("conv1")
    )

    logger.info("Making request to LM Studio...")
    agent.generateText("Hello! Can you tell me about yourself?", metadata) match {
      case Right(response) =>
        logger.info(s"Response: ${response.content}")
      case Left(error) =>
        logger.error(s"Error: $error")
    }
  }

  // Example 2: Using LM Studio with custom endpoint URL
  def exampleCustomEndpoint(): Unit = {
    logger.info("Example 2: Custom LM Studio endpoint")

    // Get URL from environment variable or use default
    val lmStudioUrl = Config.get("LM_STUDIO_URL", "http://localhost:1234/v1")
    val modelId = Config.get("LM_STUDIO_MODEL", "local-model")

    val provider = CustomEndpointProvider.forLMStudio(
      baseUrl = lmStudioUrl,
      modelId = modelId
    )

    val agent = Agent(
      name = "CustomLMStudioAgent",
      instructions = "You are a coding assistant specialized in Scala.",
      provider = provider,
      model = modelId,
      temperature = Some(0.7),
      maxTokens = Some(500)
    )

    val metadata = RequestMetadata(
      tenantId = Some("local"),
      userId = Some("developer1")
    )

    agent.generateText("Write a simple Scala function to calculate factorial", metadata) match {
      case Right(response) =>
        logger.info(s"Generated code:\n${response.content}")
      case Left(error) =>
        logger.error(s"Error: $error")
    }
  }

  // Example 4: Using custom provider directly
  def exampleCustomProvider(): Unit = {
    logger.info("Example 4: Using custom provider directly")

    // Get URL from environment variable or use default
    val lmStudioUrl = Config.get("LM_STUDIO_URL", "http://localhost:1234/v1")
    val modelId = Config.get("LM_STUDIO_MODEL", "local-model")

    val lmStudioProvider = CustomEndpointProvider.forLMStudio(
      baseUrl = lmStudioUrl,
      modelId = modelId
    )

    val agent = Agent(
      name = "CustomProviderAgent",
      instructions = "You are a helpful assistant running on LM Studio.",
      provider = lmStudioProvider,
      model = modelId
    )

    val metadata = RequestMetadata()

    agent.generateText("Explain quantum computing in simple terms", metadata) match {
      case Right(response) =>
        logger.info(s"Response: ${response.content}")
      case Left(error) =>
        logger.error(s"Error: $error")
    }
  }

  // Example 5: Structured output with LM Studio
  def exampleStructuredOutput(): Unit = {
    logger.info("Example 5: Structured output with LM Studio")

    import upickle.default.*
    import chez.derivation.*

    case class MovieReview(
        title: String,
        rating: Double,
        summary: String,
        pros: List[String],
        cons: List[String]
    ) derives Schema, ReadWriter

    val lmStudioUrl = Config.get("LM_STUDIO_URL", "http://localhost:1234/v1")
    val modelId = Config.get("LM_STUDIO_MODEL", "local-model")

    val provider = CustomEndpointProvider.forLMStudio(
      baseUrl = lmStudioUrl,
      modelId = modelId
    )

    val agent = Agent(
      name = "MovieReviewer",
      instructions = "You are a movie critic who provides structured reviews.",
      provider = provider,
      model = modelId
    )

    val metadata = RequestMetadata()

    agent.generateObject[MovieReview](
      "Review the movie 'The Matrix' (1999)",
      metadata
    ) match {
      case Right(response) =>
        val review = response.data
        logger.info(s"Movie: ${review.title}")
        logger.info(s"Rating: ${review.rating}/10")
        logger.info(s"Summary: ${review.summary}")
        logger.info(s"Pros: ${review.pros.mkString(", ")}")
        logger.info(s"Cons: ${review.cons.mkString(", ")}")
      case Left(error) =>
        logger.error(s"Error generating structured output: $error")
    }
  }

  // Example 6: Explicit HTTP version configuration
  def exampleHttpVersionConfig(): Unit = {
    logger.info("Example 6: HTTP version configuration")

    // Get URL from environment variable or use default
    val lmStudioUrl = Config.get("LM_STUDIO_URL", "http://localhost:1234/v1")
    val modelId = Config.get("LM_STUDIO_MODEL", "local-model")

    // LM Studio with explicit HTTP/1.1 (default for forLMStudio)
    val lmStudioProvider = CustomEndpointProvider.forLMStudio(
      baseUrl = lmStudioUrl,
      modelId = modelId,
      httpVersion = HttpVersion.Http11 // Explicit, but this is the default
    )

    // LM Studio forced to use HTTP/2 (not recommended for local servers)
    val lmStudioHttp2 = CustomEndpointProvider.forLMStudio(
      baseUrl = lmStudioUrl,
      modelId = modelId,
      httpVersion = HttpVersion.Http2 // Override default
    )

    // OpenAI-compatible endpoint with explicit HTTP/2 (default)
    val openAICompatible = CustomEndpointProvider.forOpenAICompatible(
      baseUrl = "https://api.example.com/v1",
      apiKey = "your-api-key",
      httpVersion = HttpVersion.Http2 // Explicit, but this is the default
    )

    // Custom endpoint with HTTP/1.1 for compatibility
    val customProvider = new CustomEndpointProvider(
      baseUrl = "http://192.168.1.100:8080/v1",
      requiresAuthentication = false,
      httpVersion = HttpVersion.Http11 // Use HTTP/1.1 for local network
    )

    logger.info(s"LM Studio provider HTTP version: ${lmStudioProvider.httpVersion}")
    logger.info(s"Custom provider HTTP version: ${customProvider.httpVersion}")
  }

  // Run examples
  println("LM Studio Integration Examples")
  println("=" * 50)

  // Uncomment the examples you want to run:
  exampleBasicLMStudio()
  exampleCustomEndpoint()
  exampleStructuredOutput()
