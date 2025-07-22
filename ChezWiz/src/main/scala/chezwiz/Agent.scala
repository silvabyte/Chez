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
  ChezError
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

  def generateText(userChatMessage: String): Either[ChezError, ChatResponse] = {
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

    config.provider.chat(request) match {
      case Right(response) =>
        // Add assistant response to conversation history
        val assistantMsg = ChatMessage(Role.Assistant, response.content)
        history = history :+ assistantMsg
        logger.info(s"Agent '${config.name}' generated response: ${response.content.take(100)}...")
        Right(response)
      case Left(error) =>
        logger.error(s"Agent '${config.name}' failed to generate text: $error")
        Left(error)
    }
  }

  def generateTextWithoutHistory(userChatMessage: String): Either[ChezError, ChatResponse] = {
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

    config.provider.chat(request) match {
      case Right(response) =>
        logger.info(s"Agent '${config.name}' generated text without history successfully")
        Right(response)
      case Left(error) =>
        logger.error(s"Agent '${config.name}' failed to generate text without history: $error")
        Left(error)
    }
  }

  def generateObject[T: Schema: Reader: scala.reflect.ClassTag](userChatMessage: String)
      : Either[ChezError, ObjectResponse[T]] = {
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

    config.provider.generateObject(request) match {
      case Right(jsonResponse) =>
        try {
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
          Right(typedResponse)
        } catch {
          case ex: ujson.ParsingFailedException =>
            val error = ChezError.SchemaConversionError(
              s"Failed to parse response as expected type: ${ex.getMessage}",
              scala.reflect.classTag[T].runtimeClass.getSimpleName
            )
            logger.error(s"Agent '${config.name}' failed to parse structured object", ex)
            Left(error)
          case ex: Exception =>
            val error = ChezError.SchemaConversionError(
              s"Unexpected error during type conversion: ${ex.getMessage}",
              scala.reflect.classTag[T].runtimeClass.getSimpleName
            )
            logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
            Left(error)
        }
      case Left(error) =>
        logger.error(s"Agent '${config.name}' failed to generate structured object: $error")
        Left(error)
    }
  }

  def generateObjectWithoutHistory[T: Schema: Reader: scala.reflect.ClassTag](
      userChatMessage: String
  ): Either[ChezError, ObjectResponse[T]] = {
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

    config.provider.generateObject(request) match {
      case Right(jsonResponse) =>
        try {
          val typedResponse = ObjectResponse[T](
            data = read[T](jsonResponse.data),
            usage = jsonResponse.usage,
            model = jsonResponse.model,
            finishReason = jsonResponse.finishReason
          )
          logger.info(s"Agent '${config.name}' generated structured object")
          Right(typedResponse)
        } catch {
          case ex: ujson.ParsingFailedException =>
            val error = ChezError.SchemaConversionError(
              s"Failed to parse response as expected type: ${ex.getMessage}",
              scala.reflect.classTag[T].runtimeClass.getSimpleName
            )
            logger.error(s"Agent '${config.name}' failed to parse structured object", ex)
            Left(error)
          case ex: Exception =>
            val error = ChezError.SchemaConversionError(
              s"Unexpected error during type conversion: ${ex.getMessage}",
              scala.reflect.classTag[T].runtimeClass.getSimpleName
            )
            logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
            Left(error)
        }
      case Left(error) =>
        logger.error(
          s"Agent '${config.name}' failed to generate structured object without history: $error"
        )
        Left(error)
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
