package chezwiz.agent.examples

import chezwiz.agent.{Agent, AgentFactory, RequestMetadata}
import chezwiz.agent.providers.AnthropicProvider
import scribe.Logging
import upickle.default.*
import chez.derivation.Schema

object AnthropicExample extends App with Logging:

  Config.initialize(sys.env.getOrElse("ENV_DIR", os.pwd.toString))

  logger.info(
    s"Initialized config from directory: ${sys.env.getOrElse("ENV_DIR", os.pwd.toString)}"
  )

  // Example 1: Basic text generation with Anthropic
  def exampleBasicAnthropic(): Unit = {
    logger.info("Example 1: Basic Anthropic text generation")

    val apiKey = Config.get("ANTHROPIC_API_KEY", "")
    if (apiKey.isEmpty) {
      logger.error("ANTHROPIC_API_KEY not set in environment")
      return
    }

    val provider = new AnthropicProvider(apiKey)

    val agent = Agent(
      name = "ClaudeAssistant",
      instructions =
        "You are Claude, a helpful AI assistant created by Anthropic. Be honest, harmless, and helpful.",
      provider = provider,
      model = "claude-3-5-haiku-20241022"
    )

    val metadata = RequestMetadata(
      tenantId = Some("demo"),
      userId = Some("user1"),
      conversationId = Some("conv1")
    )

    logger.info("Making request to Anthropic...")
    agent.generateText("What makes you different from other AI assistants?", metadata) match {
      case Right(response) =>
        logger.info(s"Response: ${response.content}")
        logger.info(s"Usage: ${response.usage}")
      case Left(error) =>
        logger.error(s"Error: $error")
    }
  }

  // Example 2: Using different Claude models
  def exampleDifferentModels(): Unit = {
    logger.info("Example 2: Different Claude models")

    val apiKey = Config.ANTHROPIC_API_KEY
    val provider = new AnthropicProvider(apiKey)

    val models = List(
      "claude-3-5-sonnet-20241022",
      "claude-3-5-haiku-20241022",
      "claude-3-opus-20240229"
    )

    models.foreach { model =>
      logger.info(s"Testing model: $model")

      val agent = Agent(
        name = s"Claude-$model",
        instructions = "Explain machine learning in one concise sentence.",
        provider = provider,
        model = model,
        temperature = Some(0.5)
      )

      val metadata = RequestMetadata()

      agent.generateText("Explain machine learning", metadata) match {
        case Right(response) =>
          logger.info(s"$model response: ${response.content}")
        case Left(error) =>
          logger.error(s"$model error: $error")
      }
    }
  }

  // Example 3: Structured output with Claude
  def exampleStructuredOutput(): Unit = {
    logger.info("Example 3: Structured output with Claude")

    case class CodeReview(
        @Schema.description("Overall code quality score (1-10)") score: Int,
        @Schema.description("Summary of the code") summary: String,
        @Schema.description("List of strengths") strengths: List[String],
        @Schema.description("List of areas for improvement") improvements: List[String],
        @Schema.description("Security concerns if any") securityConcerns: List[String],
        @Schema.description("Specific actionable recommendations") recommendations: List[String]
    ) derives Schema, ReadWriter

    val provider = new AnthropicProvider(Config.ANTHROPIC_API_KEY)

    val agent = Agent(
      name = "CodeReviewer",
      instructions = "You are an expert code reviewer. Provide constructive and detailed feedback.",
      provider = provider,
      model = "claude-3-5-sonnet-20241022"
    )

    val metadata = RequestMetadata()

    val codeSnippet = """
    def processData(data: List[String]): Map[String, Int] = {
      var result = Map[String, Int]()
      for (i <- 0 until data.length) {
        val item = data(i)
        if (result.contains(item)) {
          result = result + (item -> (result(item) + 1))
        } else {
          result = result + (item -> 1)
        }
      }
      result
    }
    """

    agent.generateObject[CodeReview](
      s"Review this Scala code and provide structured feedback:\n$codeSnippet",
      metadata
    ) match {
      case Right(response) =>
        val review = response.data
        logger.info(s"Code Quality Score: ${review.score}/10")
        logger.info(s"Summary: ${review.summary}")
        logger.info("Strengths:")
        review.strengths.foreach(s => logger.info(s"  - $s"))
        logger.info("Areas for Improvement:")
        review.improvements.foreach(i => logger.info(s"  - $i"))
        if (review.securityConcerns.nonEmpty) {
          logger.info("Security Concerns:")
          review.securityConcerns.foreach(c => logger.info(s"  - $c"))
        }
        logger.info("Recommendations:")
        review.recommendations.foreach(r => logger.info(s"  - $r"))
      case Left(error) =>
        logger.error(s"Error generating code review: $error")
    }
  }

  // Example 4: Claude's conversational abilities
  def exampleConversation(): Unit = {
    logger.info("Example 4: Multi-turn conversation with Claude")

    val provider = new AnthropicProvider(Config.ANTHROPIC_API_KEY)

    val agent = Agent(
      name = "ClaudeConversationalist",
      instructions = """You are Claude, having a thoughtful conversation. 
        Remember what we discuss and build on previous topics.
        Be engaging and ask clarifying questions when appropriate.""",
      provider = provider,
      model = "claude-3-5-haiku-20241022"
    )

    val metadata = RequestMetadata(
      tenantId = Some("demo"),
      userId = Some("researcher"),
      conversationId = Some("research-discussion")
    )

    val conversation = List(
      "I'm researching the impact of AI on software development. What are your thoughts?",
      "That's interesting. How do you think AI will change the role of junior developers?",
      "What skills should developers focus on to remain valuable in an AI-enhanced future?"
    )

    conversation.foreach { prompt =>
      logger.info(s"User: $prompt")
      agent.generateText(prompt, metadata) match {
        case Right(response) =>
          logger.info(s"Claude: ${response.content}")
        case Left(error) =>
          logger.error(s"Error: $error")
      }
    }
  }

  // Example 5: Claude with different temperatures
  def exampleTemperatureVariations(): Unit = {
    logger.info("Example 5: Temperature effects on Claude's responses")

    val provider = new AnthropicProvider(Config.ANTHROPIC_API_KEY)
    val prompt = "Write a creative description of a sunset"
    val metadata = RequestMetadata()

    val temperatures = List(0.0, 0.5, 1.0)

    temperatures.foreach { temp =>
      val agent = Agent(
        name = s"Claude-temp-$temp",
        instructions = "You are a creative writer.",
        provider = provider,
        model = "claude-3-5-haiku-20241022",
        temperature = Some(temp),
        maxTokens = Some(150)
      )

      logger.info(s"\nTemperature $temp:")
      agent.generateText(prompt, metadata) match {
        case Right(response) =>
          logger.info(s"Response: ${response.content}")
        case Left(error) =>
          logger.error(s"Error: $error")
      }
    }
  }

  // Example 6: Using AgentFactory for Anthropic
  def exampleAgentFactory(): Unit = {
    logger.info("Example 6: Using AgentFactory for Anthropic")

    val apiKey = Config.ANTHROPIC_API_KEY

    AgentFactory.createAnthropicAgent(
      name = "FactoryClaude",
      instructions = "You are Claude created via AgentFactory. Be helpful and thoughtful.",
      apiKey = apiKey,
      model = "claude-3-5-sonnet-20241022",
      temperature = Some(0.7),
      maxTokens = Some(500)
    ) match {
      case Right(agent) =>
        val metadata = RequestMetadata()
        agent.generateText(
          "What are the key principles of good API design?",
          metadata
        ) match {
          case Right(response) =>
            logger.info(s"Response: ${response.content}")
          case Left(error) =>
            logger.error(s"Error: $error")
        }
      case Left(error) =>
        logger.error(s"Failed to create agent: $error")
    }
  }

  // Example 7: Complex structured output
  def exampleComplexStructuredOutput(): Unit = {
    logger.info("Example 7: Complex structured output with Claude")

    case class TechnicalAnalysis(
        @Schema.description("Technology name") technology: String,
        @Schema.description("Brief description") description: String,
        @Schema.description("Key advantages") advantages: List[String],
        @Schema.description("Key disadvantages") disadvantages: List[String],
        @Schema.description("Use cases") useCases: List[UseCase],
        @Schema.description("Comparison with alternatives") alternatives: List[Alternative],
        @Schema.description("Future outlook") futureOutlook: String,
        @Schema.description("Recommended for") recommendedFor: List[String]
    ) derives Schema, ReadWriter

    case class UseCase(
        @Schema.description("Use case name") name: String,
        @Schema.description("Description") description: String,
        @Schema.description("Why it's suitable") rationale: String
    ) derives Schema, ReadWriter

    case class Alternative(
        @Schema.description("Alternative technology") name: String,
        @Schema.description("When to prefer this alternative") whenToUse: String
    ) derives Schema, ReadWriter

    val provider = new AnthropicProvider(Config.ANTHROPIC_API_KEY)

    val agent = Agent(
      name = "TechAnalyst",
      instructions =
        "You are a senior technology analyst providing comprehensive technical analysis.",
      provider = provider,
      model = "claude-3-5-sonnet-20241022"
    )

    val metadata = RequestMetadata()

    agent.generateObject[TechnicalAnalysis](
      "Provide a comprehensive analysis of GraphQL as an API technology",
      metadata
    ) match {
      case Right(response) =>
        val analysis = response.data
        logger.info(s"Technology: ${analysis.technology}")
        logger.info(s"Description: ${analysis.description}")
        logger.info("Advantages:")
        analysis.advantages.foreach(a => logger.info(s"  + $a"))
        logger.info("Disadvantages:")
        analysis.disadvantages.foreach(d => logger.info(s"  - $d"))
        logger.info("Use Cases:")
        analysis.useCases.foreach { uc =>
          logger.info(s"  * ${uc.name}: ${uc.description}")
          logger.info(s"    Rationale: ${uc.rationale}")
        }
        logger.info("Alternatives:")
        analysis.alternatives.foreach { alt =>
          logger.info(s"  * ${alt.name}: ${alt.whenToUse}")
        }
        logger.info(s"Future Outlook: ${analysis.futureOutlook}")
        logger.info(s"Recommended for: ${analysis.recommendedFor.mkString(", ")}")
      case Left(error) =>
        logger.error(s"Error generating analysis: $error")
    }
  }

  // Run examples
  println("Anthropic (Claude) Integration Examples")
  println("=" * 50)

  try {
    exampleBasicAnthropic()
    println()
    exampleDifferentModels()
    println()
    exampleStructuredOutput()
    println()
    exampleConversation()
    println()
    exampleTemperatureVariations()
    println()
    exampleAgentFactory()
    println()
    exampleComplexStructuredOutput()
  } catch {
    case e: Exception =>
      logger.error(s"Error running examples: ${e.getMessage}")
  }

  println("\n=== Anthropic Examples Complete ===")
