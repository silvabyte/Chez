package chezwiz.agent

import scala.concurrent.{Future, ExecutionContext}
import scribe.Logging
import chezwiz.agent.providers.LLMProvider
import chezwiz.agent.{
  ChatRequest,
  ChatResponse,
  ObjectRequest,
  ObjectResponse,
  ChatMessage,
  Role,
  LLMError,
  JsonSchema
}
import ujson.Value

case class AgentConfig(
    name: String,
    instructions: String,
    provider: LLMProvider,
    model: String,
    temperature: Option[Double] = None,
    maxTokens: Option[Int] = None
)

class Agent(config: AgentConfig, initialHistory: Vector[ChatMessage] = Vector.empty)(implicit
    val ec: ExecutionContext
) extends Logging:

  private var history: Vector[ChatMessage] =
    if initialHistory.isEmpty then Vector(ChatMessage(Role.System, config.instructions))
    else initialHistory

  def name: String = config.name
  def provider: LLMProvider = config.provider
  def model: String = config.model

  def generateText(userChatMessage: String): Future[ChatResponse] =
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

    config.provider
      .chat(request)
      .map { response =>
        // Add assistant response to conversation history
        val assistantMsg = ChatMessage(Role.Assistant, response.content)
        history = history :+ assistantMsg

        logger.info(s"Agent '${config.name}' generated response: ${response.content.take(100)}...")
        response
      }(ec)
      .recover {
        case ex: LLMError =>
          logger.error(s"Agent '${config.name}' failed to generate text", ex)
          throw ex
        case ex =>
          logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
          throw LLMError(s"Unexpected error: ${ex.getMessage}")
      }(ec)

  def generateTextWithoutHistory(userChatMessage: String): Future[ChatResponse] =
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

    config.provider
      .chat(request)
      .recover {
        case ex: LLMError =>
          logger.error(s"Agent '${config.name}' failed to generate text without history", ex)
          throw ex
        case ex =>
          logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
          throw LLMError(s"Unexpected error: ${ex.getMessage}")
      }(ec)

  def generateObject(userChatMessage: String, schema: JsonSchema): Future[ObjectResponse] =
    logger.info(s"Agent '${config.name}' generating structured object for: $userChatMessage")

    // Add user message to conversation history
    val userMsg = ChatMessage(Role.User, userChatMessage)
    history = history :+ userMsg

    val request = ObjectRequest(
      messages = history.toList,
      model = config.model,
      schema = schema,
      temperature = config.temperature,
      maxTokens = config.maxTokens,
      stream = false
    )

    config.provider
      .generateObject(request)
      .map { response =>
        // Add assistant response to conversation history (convert object to string representation)
        val assistantMsg = ChatMessage(Role.Assistant, response.`object`.toString())
        history = history :+ assistantMsg

        logger.info(s"Agent '${config.name}' generated structured object")
        response
      }(ec)
      .recover {
        case ex: LLMError =>
          logger.error(s"Agent '${config.name}' failed to generate structured object", ex)
          throw ex
        case ex =>
          logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
          throw LLMError(s"Unexpected error: ${ex.getMessage}")
      }(ec)

  def generateObjectWithoutHistory(
      userChatMessage: String,
      schema: JsonSchema
  ): Future[ObjectResponse] =
    logger.info(
      s"Agent '${config.name}' generating structured object without history for: $userChatMessage"
    )

    val messages = List(
      ChatMessage(Role.System, config.instructions),
      ChatMessage(Role.User, userChatMessage)
    )

    val request = ObjectRequest(
      messages = messages,
      model = config.model,
      schema = schema,
      temperature = config.temperature,
      maxTokens = config.maxTokens,
      stream = false
    )

    config.provider
      .generateObject(request)
      .recover {
        case ex: LLMError =>
          logger.error(
            s"Agent '${config.name}' failed to generate structured object without history",
            ex
          )
          throw ex
        case ex =>
          logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
          throw LLMError(s"Unexpected error: ${ex.getMessage}")
      }(ec)

  def getConversationHistory: List[ChatMessage] = history.toList

  def clearHistory(): Unit =
    logger.info(s"Agent '${config.name}' clearing conversation history")
    history = Vector(ChatMessage(Role.System, config.instructions))

  def addChatMessage(message: ChatMessage): Unit =
    history = history :+ message

  def withSystemChatMessage(systemChatMessage: String): Agent =
    val newConfig = config.copy(instructions = systemChatMessage)
    new Agent(newConfig)

object Agent:
  def apply(
      name: String,
      instructions: String,
      provider: LLMProvider,
      model: String,
      temperature: Option[Double] = None,
      maxTokens: Option[Int] = None
  )(implicit ec: ExecutionContext): Agent =
    val config = AgentConfig(name, instructions, provider, model, temperature, maxTokens)
    new Agent(config)
