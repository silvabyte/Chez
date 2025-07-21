package chezwiz.agent

import scribe.Logging
import chezwiz.agent.providers.LLMProvider
import chezwiz.agent.{
  ChatRequest,
  ChatResponse,
  ObjectRequest,
  ObjectResponse,
  ChatMessage,
  Role,
  LLMError
}
import ujson.Value
import upickle.default.*
import chez.derivation.Schema
import chez.Chez

case class AgentConfig(
    name: String,
    instructions: String,
    provider: LLMProvider,
    model: String,
    temperature: Option[Double] = None,
    maxTokens: Option[Int] = None
)

class Agent(config: AgentConfig, initialHistory: Vector[ChatMessage] = Vector.empty)
    extends Logging:

  private var history: Vector[ChatMessage] = {
    if initialHistory.isEmpty then Vector(ChatMessage(Role.System, config.instructions))
    else initialHistory
  }

  def name: String = config.name
  def provider: LLMProvider = config.provider
  def model: String = config.model

  def generateText(userChatMessage: String): ChatResponse = {
    logger.info(s"Agent '${config.name}' generating text for: $userChatMessage")

    // Add user message to conversation history
    val userMsg = ChatMessage(Role.User, userChatMessage)
    history = history :+ userMsg

    val request = ChatRequest(
      messages = history.toList,
      model = config.model,
      temperature = config.temperature,
      maxTokens = config.maxTokens,
      stream = false
    )

    try {
      val response = config.provider.chat(request)

      // Add assistant response to conversation history
      val assistantMsg = ChatMessage(Role.Assistant, response.content)
      history = history :+ assistantMsg

      logger.info(s"Agent '${config.name}' generated response: ${response.content.take(100)}...")
      response
    } catch {
      case ex: LLMError =>
        logger.error(s"Agent '${config.name}' failed to generate text", ex)
        throw ex
      case ex =>
        logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
        throw LLMError(s"Unexpected error: ${ex.getMessage}")
    }
  }

  def generateTextWithoutHistory(userChatMessage: String): ChatResponse = {
    logger.info(s"Agent '${config.name}' generating text without history for: $userChatMessage")

    val messages = List(
      ChatMessage(Role.System, config.instructions),
      ChatMessage(Role.User, userChatMessage)
    )

    val request = ChatRequest(
      messages = messages,
      model = config.model,
      temperature = config.temperature,
      maxTokens = config.maxTokens,
      stream = false
    )

    try {
      config.provider.chat(request)
    } catch {
      case ex: LLMError =>
        logger.error(s"Agent '${config.name}' failed to generate text without history", ex)
        throw ex
      case ex =>
        logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
        throw LLMError(s"Unexpected error: ${ex.getMessage}")
    }
  }

  def generateObject[T: Schema: Reader](userChatMessage: String): ObjectResponse[T] = {
    logger.info(s"Agent '${config.name}' generating structured object for: $userChatMessage")

    // Add user message to conversation history
    val userMsg = ChatMessage(Role.User, userChatMessage)
    history = history :+ userMsg

    // Get the schema directly from the type parameter
    val schema = summon[Schema[T]].schema

    val request = ObjectRequest(
      messages = history.toList,
      model = config.model,
      schema = schema,
      temperature = config.temperature,
      maxTokens = config.maxTokens,
      stream = false
    )

    try {
      val jsonResponse = config.provider.generateObject(request)
      val typedResponse = ObjectResponse[T](
        data = read[T](jsonResponse.data),
        usage = jsonResponse.usage,
        model = jsonResponse.model,
        finishReason = jsonResponse.finishReason
      )

      // Add assistant response to conversation history (convert object to string representation)
      val assistantMsg = ChatMessage(Role.Assistant, jsonResponse.data.toString())
      history = history :+ assistantMsg

      logger.info(s"Agent '${config.name}' generated structured object")

      typedResponse
    } catch {
      case ex: LLMError =>
        logger.error(s"Agent '${config.name}' failed to generate structured object", ex)
        throw ex
      case ex: ujson.ParsingFailedException =>
        logger.error(s"Agent '${config.name}' failed to parse structured object", ex)
        throw LLMError(s"Failed to parse response as expected type: ${ex.getMessage}")
      case ex =>
        logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
        throw LLMError(s"Unexpected error: ${ex.getMessage}")
    }
  }

  def generateObjectWithoutHistory[T: Schema: Reader](
      userChatMessage: String
  ): ObjectResponse[T] = {
    logger.info(
      s"Agent '${config.name}' generating structured object without history for: $userChatMessage"
    )

    val messages = List(
      ChatMessage(Role.System, config.instructions),
      ChatMessage(Role.User, userChatMessage)
    )

    // Get the schema directly from the type parameter
    val schema = summon[Schema[T]].schema

    val request = ObjectRequest(
      messages = messages,
      model = config.model,
      schema = schema,
      temperature = config.temperature,
      maxTokens = config.maxTokens,
      stream = false
    )

    try {
      val jsonResponse = config.provider.generateObject(request)
      val typedResponse = ObjectResponse[T](
        data = read[T](jsonResponse.data),
        usage = jsonResponse.usage,
        model = jsonResponse.model,
        finishReason = jsonResponse.finishReason
      )
      logger.info(s"Agent '${config.name}' generated structured object")

      typedResponse
    } catch {
      case ex: LLMError =>
        logger.error(
          s"Agent '${config.name}' failed to generate structured object without history",
          ex
        )
        throw ex
      case ex: ujson.ParsingFailedException =>
        logger.error(s"Agent '${config.name}' failed to parse structured object", ex)
        throw LLMError(s"Failed to parse response as expected type: ${ex.getMessage}")
      case ex =>
        logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
        throw LLMError(s"Unexpected error: ${ex.getMessage}")
    }
  }

  def getConversationHistory: List[ChatMessage] = history.toList

  def clearHistory(): Unit = {
    logger.info(s"Agent '${config.name}' clearing conversation history")
    history = Vector(ChatMessage(Role.System, config.instructions))
  }

  def addChatMessage(message: ChatMessage): Unit =
    history = history :+ message

  def withSystemChatMessage(systemChatMessage: String): Agent = {
    val newConfig = config.copy(instructions = systemChatMessage)
    new Agent(newConfig)
  }

object Agent:
  def apply(
      name: String,
      instructions: String,
      provider: LLMProvider,
      model: String,
      temperature: Option[Double] = None,
      maxTokens: Option[Int] = None
  ): Agent = {
    val config = AgentConfig(name, instructions, provider, model, temperature, maxTokens)
    new Agent(config)
  }
