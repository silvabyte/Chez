package chezwiz.agent.examples

import chezwiz.agent.{
  Agent,
  AgentFactory,
  RequestMetadata,
  ChatMessage,
  MessageContentPart,
  Role,
  ImageUrlContent
}
import chezwiz.agent.providers.{LMStudioProvider, CustomEndpointProvider, HttpVersion}
import scribe.Logging
import upickle.default.*
import chez.derivation.Schema

object LMStudioExample extends App with Logging {

  Config.initialize(sys.env.getOrElse("ENV_DIR", os.pwd.toString))

  logger.info(
    s"Initialized config from directory: ${sys.env.getOrElse("ENV_DIR", os.pwd.toString)}"
  )

  // Example 1: Using LM Studio with default settings
  def example1(): Unit = {
    logger.info("Example 1: Basic LM Studio usage")

    // Get URL from environment variable or use default
    val lmStudioUrl = Config.get("LM_STUDIO_URL", "http://localhost:1234/v1")
    val modelId = Config.get("LM_STUDIO_MODEL", "local-model")

    logger.info(s"LM Studio URL: $lmStudioUrl")
    logger.info(s"LM Studio Model: $modelId")

    // Create LM Studio provider
    val provider = LMStudioProvider(
      httpVersion = HttpVersion.Http11,
      baseUrl = lmStudioUrl,
      modelId = modelId
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

  // Example 2: Structured output with LM Studio
  def example2(): Unit = {
    logger.info("Example 2: Structured output with LM Studio")

    case class MovieReview(
        title: String,
        rating: Double,
        summary: String,
        pros: List[String],
        cons: List[String]
    ) derives Schema, ReadWriter

    val lmStudioUrl = Config.get("LM_STUDIO_URL", "http://localhost:1234/v1")
    val modelId = Config.get("LM_STUDIO_MODEL", "local-model")

    val provider = LMStudioProvider(
      httpVersion = HttpVersion.Http11,
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

  def example3(): Unit = {
    logger.info("Example 3: OCR Image text extraction into structured objects")

    // Load image from resources as base64 data URL
    val resourcePath = os.pwd / "ChezWiz" / "src" / "resources" / "tweets.png"
    val imageBytes = os.read.bytes(resourcePath)
    val base64Image = java.util.Base64.getEncoder.encodeToString(imageBytes)
    val imageUrl = s"data:image/png;base64,$base64Image"

    // Use the same URL for vision model if LM_STUDIO_URL2 is not set
    val visionLmStudioUrl =
      Config.get("LM_STUDIO_URL2", "bad_url")
    val visionModelId =
      Config.get("LM_STUDIO_VISION_MODEL", "badvisonmodel")
    val lmStudioUrl = Config.get("LM_STUDIO_URL", "http://localhost:1234/v1")
    val codeModelId =
      Config.get("LM_STUDIO_CODE_MODEL", Config.get("LM_STUDIO_MODEL", "local-model"))

    logger.info(s"Vision LM Studio URL: $visionLmStudioUrl")
    logger.info(s"Vision Model: $visionModelId")
    logger.info(s"Code LM Studio URL: $lmStudioUrl")
    logger.info(s"Code Model: $codeModelId")
    logger.info(s"Image size: ${imageBytes.length} bytes")

    // Vision OCR Agent
    val visionOCRAgent = Agent(
      name = "vision-ocr-agent",
      instructions =
        "You are a helpful assistant that extracts text from images accurately. Analyze the image carefully and extract all readable text.",
      provider = LMStudioProvider(
        visionLmStudioUrl,
        visionModelId,
        httpVersion = HttpVersion.Http11
      ),
      model = visionModelId,
      temperature = Some(0.0),
      maxTokens = Some(1600),
      idGenerator = () => "vision-ocr-agent"
    )

    // Tweet Structuring Agent
    val tweetAgentInstructions =
      """You are an expert in converting raw text into structured tweet models.

Always output a valid JSON object with each object containing these exact keys:
{
  "data": [
    {
      "displayName": "the extracted display name for the user",
      "userName": "the extracted username",
      "tweet": "the tweet content"
    }
  ]
}
"""
    val tweetAgent = Agent(
      name = "tweet-agent",
      instructions = tweetAgentInstructions,
      provider = LMStudioProvider(
        lmStudioUrl,
        codeModelId,
        httpVersion = HttpVersion.Http11
      ),
      model = codeModelId,
      temperature = Some(0.3),
      maxTokens = Some(2000),
      idGenerator = () => "tweet-agent"
    )

    // Input message to OCR agent - system message should be for the OCR agent
    val ocrMessages = List(
      ChatMessage.multiModal(
        Role.User,
        List(
          MessageContentPart.TextPart(
            "text",
            "Extract all text from this image of tweets. Return only the text content you can see, maintaining the structure."
          ),
          MessageContentPart.ImageUrlPart(
            "image_url",
            ImageUrlContent(url = imageUrl, detail = "high")
          )
        )
      )
    )

    // Result type
    case class Tweet(
        displayName: String,
        userName: String,
        tweet: String
    ) derives Schema, ReadWriter

    case class TweetResults(
        data: List[Tweet]
    ) derives Schema, ReadWriter

    val metadata = RequestMetadata()

    // First, extract text from the image using the vision model
    // Note: This requires a vision-capable model (e.g., llava, qwen-vl, cogvlm, etc.)
    visionOCRAgent.generateWithMessages(ocrMessages, metadata) match {
      case Right(extractedTextResponse) =>
        val extractedText = extractedTextResponse.content
        logger.info(s"Successfully extracted text from image")
        logger.info(s"Extracted text:\n$extractedText")

        // Then, structure the extracted text into tweets
        val structuringPrompt = {
          s"""Given the following extracted text from a Twitter/X screenshot, 
             |structure it into individual tweets as JSON. The text is:
             |
             |$extractedText
             |
             |Extract and structure each tweet with the display name, username, and tweet content.""".stripMargin
        }

        tweetAgent.generateObject[TweetResults](
          structuringPrompt,
          metadata
        ) match {
          case Right(response) =>
            logger.info(s"Successfully structured ${response.data.data.size} tweets:")
            response.data.data.foreach { tweet =>
              logger.info(s"  - ${tweet.displayName} (@${tweet.userName}): ${tweet.tweet}")
            }
          case Left(err) =>
            logger.error(s"Error structuring tweets: $err")
        }

      case Left(error) =>
        logger.error(s"Failed to extract text from image: $error")
        logger.error("This example requires a vision-capable model (e.g., llava, qwen-vl, cogvlm)")
        logger.error("Please ensure:")
        logger.error("  1. You have a vision model loaded in LM Studio")
        logger.error("  2. Set LM_STUDIO_VISION_MODEL environment variable to the model name")
        logger.error("  3. The model supports multimodal/vision input")
    }
  }

// Run examples
  println("LM Studio Integration Examples")
  println("=" * 50)

// Uncomment the examples you want to run:
  example1()
  example2()
  example3()
}
