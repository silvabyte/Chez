package boogieloops.ai.examples

import upickle.default.ReadWriter
import boogieloops.ai.{Agent, RequestMetadata}
import boogieloops.ai.providers.OpenAIProvider
import scribe.Logging
import upickle.default.*
import boogieloops.schema.derivation.Schema

object OpenAIExample extends App with Logging:

  Config.initialize(sys.env.getOrElse("ENV_DIR", os.pwd.toString))

  logger.info(
    s"Initialized config from directory: ${sys.env.getOrElse("ENV_DIR", os.pwd.toString)}"
  )

  // Example 1: Basic text generation with OpenAI
  def exampleBasicOpenAI(): Unit = {
    logger.info("Example 1: Basic OpenAI text generation")

    val apiKey = Config.get("OPENAI_API_KEY", "")
    if (apiKey.isEmpty) {
      logger.error("OPENAI_API_KEY not set in environment")
      return
    }

    val provider = new OpenAIProvider(apiKey)

    val agent = Agent(
      name = "OpenAIAssistant",
      instructions = "You are a helpful AI assistant. Be concise and clear.",
      provider = provider,
      model = "gpt-4o-mini"
    )

    val metadata = RequestMetadata(
      tenantId = Some("demo"),
      userId = Some("user1"),
      conversationId = Some("conv1")
    )

    logger.info("Making request to OpenAI...")
    agent.generateText("What is the capital of France?", metadata) match {
      case Right(response) =>
        logger.info(s"Response: ${response.content}")
        logger.info(s"Usage: ${response.usage}")
      case Left(error) =>
        logger.error(s"Error: $error")
    }
  }

  // Example 2: Using different OpenAI models
  def exampleDifferentModels(): Unit = {
    logger.info("Example 2: Different OpenAI models")

    val apiKey = Config.OPENAI_API_KEY
    val provider = new OpenAIProvider(apiKey)

    val models = List("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo")

    models.foreach { model =>
      logger.info(s"Testing model: $model")

      val agent = Agent(
        name = s"Agent-$model",
        instructions = "Explain quantum computing in one sentence.",
        provider = provider,
        model = model,
        temperature = Some(0.5)
      )

      val metadata = RequestMetadata()

      agent.generateText("Explain quantum computing", metadata) match {
        case Right(response) =>
          logger.info(s"$model response: ${response.content}")
        case Left(error) =>
          logger.error(s"$model error: $error")
      }
    }
  }

  // Example 3: Structured output with OpenAI
  def exampleStructuredOutput(): Unit = {
    logger.info("Example 3: Structured output with OpenAI")

    case class Recipe(
        @Schema.description("Recipe name") name: String,
        @Schema.description("List of ingredients") ingredients: List[String],
        @Schema.description("Cooking time in minutes") @Schema.minimum(1) cookingTime: Int,
        @Schema.description("Difficulty level") difficulty: String,
        @Schema.description("Step-by-step instructions") steps: List[String]
    ) derives Schema, ReadWriter

    val provider = new OpenAIProvider(Config.OPENAI_API_KEY)

    val agent = Agent(
      name = "ChefAssistant",
      instructions = "You are a professional chef who creates simple, delicious recipes.",
      provider = provider,
      model = "gpt-4o-mini"
    )

    val metadata = RequestMetadata()

    agent.generateObject[Recipe](
      "Create a simple pasta recipe that can be made in under 30 minutes",
      metadata
    ) match {
      case Right(response) =>
        val recipe = response.data
        logger.info(s"Recipe: ${recipe.name}")
        logger.info(s"Cooking time: ${recipe.cookingTime} minutes")
        logger.info(s"Difficulty: ${recipe.difficulty}")
        logger.info(s"Ingredients: ${recipe.ingredients.mkString(", ")}")
        logger.info("Steps:")
        recipe.steps.zipWithIndex.foreach { case (step, i) =>
          logger.info(s"  ${i + 1}. $step")
        }
      case Left(error) =>
        logger.error(s"Error generating recipe: $error")
    }
  }

  // Example 4: Using OpenAI with conversation history
  def exampleConversationHistory(): Unit = {
    logger.info("Example 4: Conversation with history")

    val provider = new OpenAIProvider(Config.OPENAI_API_KEY)

    val agent = Agent(
      name = "ConversationalAssistant",
      instructions = "You are a helpful assistant that remembers the context of our conversation.",
      provider = provider,
      model = "gpt-4o-mini"
    )

    val metadata = RequestMetadata(
      tenantId = Some("demo"),
      userId = Some("alice"),
      conversationId = Some("story-session")
    )

    val prompts = List(
      "Let's write a story about a robot named Max",
      "What special ability does Max have?",
      "How does Max use this ability to help others?"
    )

    prompts.foreach { prompt =>
      logger.info(s"User: $prompt")
      agent.generateText(prompt, metadata) match {
        case Right(response) =>
          logger.info(s"Assistant: ${response.content}")
        case Left(error) =>
          logger.error(s"Error: $error")
      }
    }
  }

  // Example 5: Using OpenAI with temperature and max tokens
  def exampleTemperatureAndTokens(): Unit = {
    logger.info("Example 5: Temperature and token control")

    val provider = new OpenAIProvider(Config.OPENAI_API_KEY)

    // Low temperature for factual responses
    val factualAgent = Agent(
      name = "FactualAssistant",
      instructions = "Provide accurate, factual information.",
      provider = provider,
      model = "gpt-4o-mini",
      temperature = Some(0.1),
      maxTokens = Some(100)
    )

    // High temperature for creative responses
    val creativeAgent = Agent(
      name = "CreativeAssistant",
      instructions = "Be creative and imaginative.",
      provider = provider,
      model = "gpt-4o-mini",
      temperature = Some(0.9),
      maxTokens = Some(200)
    )

    val metadata = RequestMetadata()
    val prompt = "Write about a futuristic city"

    logger.info("Low temperature (factual):")
    factualAgent.generateText(prompt, metadata) match {
      case Right(response) =>
        logger.info(s"Response: ${response.content}")
      case Left(error) =>
        logger.error(s"Error: $error")
    }

    logger.info("\nHigh temperature (creative):")
    creativeAgent.generateText(prompt, metadata) match {
      case Right(response) =>
        logger.info(s"Response: ${response.content}")
      case Left(error) =>
        logger.error(s"Error: $error")
    }
  }

  // Example 6: Creating OpenAI Agent directly
  def exampleAgentDirect(): Unit = {
    logger.info("Example 6: Creating OpenAI Agent directly")

    val apiKey = Config.OPENAI_API_KEY
    val provider = new OpenAIProvider(apiKey)

    val agent = Agent(
      name = "DirectAgent",
      instructions = "You are created directly. Be helpful and concise.",
      provider = provider,
      model = "gpt-4o-mini",
      temperature = Some(0.7)
    )

    val metadata = RequestMetadata()
    agent.generateText("What are your capabilities?", metadata) match {
      case Right(response) =>
        logger.info(s"Response: ${response.content}")
      case Left(error) =>
        logger.error(s"Error: $error")
    }
  }

  // Run examples
  println("OpenAI Integration Examples")
  println("=" * 50)

  try {
    exampleBasicOpenAI()
    println()
    exampleDifferentModels()
    println()
    exampleStructuredOutput()
    println()
    exampleConversationHistory()
    println()
    exampleTemperatureAndTokens()
    println()
    exampleAgentDirect()
  } catch {
    case e: Exception =>
      logger.error(s"Error running examples: ${e.getMessage}")
  }

  println("\n=== OpenAI Examples Complete ===")
