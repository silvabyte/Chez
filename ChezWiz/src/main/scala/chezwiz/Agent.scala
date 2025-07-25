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
  ChezError,
  RequestMetadata
}
import ujson.Value
import upickle.default.*
import chez.derivation.Schema
import chez.Chez
import java.util.UUID

case class AgentConfig(
    id: String,
    name: String,
    instructions: String,
    provider: LLMProvider,
    model: String,
    temperature: Option[Double] = None,
    maxTokens: Option[Int] = None,
    hooks: HookRegistry = HookRegistry.empty,
    idGenerator: () => String = () => UUID.randomUUID().toString
)

class Agent(config: AgentConfig, initialHistory: Vector[ChatMessage] = Vector.empty)
    extends Logging:

  // Scoped history storage: Map[ScopeKey, Vector[ChatMessage]]
  private var scopedHistories: Map[String, Vector[ChatMessage]] = Map.empty

  private def createScopeKey(metadata: RequestMetadata): String = {
    val parts = List(
      metadata.tenantId.getOrElse("_"),
      metadata.userId.getOrElse("_"),
      metadata.conversationId.getOrElse("_")
    )
    parts.mkString(":")
  }

  private def getHistory(metadata: RequestMetadata): Vector[ChatMessage] = {
    val scopeKey = createScopeKey(metadata)
    scopedHistories.getOrElse(scopeKey, Vector(ChatMessage(Role.System, config.instructions)))
  }

  private def updateHistory(
      metadata: RequestMetadata,
      newHistory: Vector[ChatMessage]
  ): Unit = {
    val scopeKey = createScopeKey(metadata)
    scopedHistories = scopedHistories.updated(scopeKey, newHistory)
  }

  def id: String = config.id
  def name: String = config.name
  def provider: LLMProvider = config.provider
  def model: String = config.model

  def generateText(
      userChatMessage: String,
      metadata: RequestMetadata
  ): Either[ChezError, ChatResponse] = {
    logger.info(s"Agent '${config.name}' generating text for: $userChatMessage")

    // Get scoped history and add user message
    var currentHistory = getHistory(metadata)
    val userMsg = ChatMessage(Role.User, userChatMessage)
    currentHistory = currentHistory :+ userMsg

    val request = ChatRequest(
      messages = currentHistory.toList,
      model = config.model,
      temperature = config.temperature,
      maxTokens = config.maxTokens,
      stream = false,
      metadata = Some(metadata)
    )

    // Execute pre-request hooks
    val requestTimestamp = System.currentTimeMillis()
    config.hooks.executePreRequestHooks(PreRequestContext(
      agentName = config.name,
      model = config.model,
      request = request,
      metadata = metadata,
      timestamp = requestTimestamp
    ))

    val result = config.provider.chat(request)

    // Execute post-response hooks
    config.hooks.executePostResponseHooks(PostResponseContext(
      agentName = config.name,
      model = config.model,
      request = request,
      response = result,
      metadata = metadata,
      requestTimestamp = requestTimestamp
    ))

    result match {
      case Right(response) =>
        // Add assistant response to scoped conversation history
        val assistantMsg = ChatMessage(Role.Assistant, response.content)
        currentHistory = currentHistory :+ assistantMsg
        updateHistory(metadata, currentHistory)
        logger.info(s"Agent '${config.name}' generated response: ${response.content.take(100)}...")
        Right(response)
      case Left(error) =>
        // Execute error hooks
        config.hooks.executeErrorHooks(ErrorContext(
          agentName = config.name,
          model = config.model,
          error = error,
          metadata = metadata,
          operation = "generateText",
          request = Some(Left(request))
        ))
        logger.error(s"Agent '${config.name}' failed to generate text: $error")
        Left(error)
    }
  }

  def generateTextWithoutHistory(
      userChatMessage: String,
      metadata: RequestMetadata
  ): Either[ChezError, ChatResponse] = {
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
      stream = false,
      metadata = Some(metadata)
    )

    // Execute pre-request hooks
    val requestTimestamp = System.currentTimeMillis()
    config.hooks.executePreRequestHooks(PreRequestContext(
      agentName = config.name,
      model = config.model,
      request = request,
      metadata = metadata,
      timestamp = requestTimestamp
    ))

    val result = config.provider.chat(request)

    // Execute post-response hooks
    config.hooks.executePostResponseHooks(PostResponseContext(
      agentName = config.name,
      model = config.model,
      request = request,
      response = result,
      metadata = metadata,
      requestTimestamp = requestTimestamp
    ))

    result match {
      case Right(response) =>
        logger.info(s"Agent '${config.name}' generated text without history successfully")
        Right(response)
      case Left(error) =>
        // Execute error hooks
        config.hooks.executeErrorHooks(ErrorContext(
          agentName = config.name,
          model = config.model,
          error = error,
          metadata = metadata,
          operation = "generateTextWithoutHistory",
          request = Some(Left(request))
        ))
        logger.error(s"Agent '${config.name}' failed to generate text without history: $error")
        Left(error)
    }
  }

  def generateObject[T: Schema: Reader: scala.reflect.ClassTag](
      userChatMessage: String,
      metadata: RequestMetadata
  )
      : Either[ChezError, ObjectResponse[T]] = {
    logger.info(s"Agent '${config.name}' generating structured object for: $userChatMessage")

    // Get scoped history and add user message
    var currentHistory = getHistory(metadata)
    val userMsg = ChatMessage(Role.User, userChatMessage)
    currentHistory = currentHistory :+ userMsg

    // Get the schema directly from the type parameter
    val schema = summon[Schema[T]].schema
    val targetType = scala.reflect.classTag[T].runtimeClass.getSimpleName

    val request = ObjectRequest(
      messages = currentHistory.toList,
      model = config.model,
      schema = schema,
      temperature = config.temperature,
      maxTokens = config.maxTokens,
      stream = false,
      metadata = Some(metadata)
    )

    // Execute pre-object-request hooks
    val requestTimestamp = System.currentTimeMillis()
    config.hooks.executePreObjectRequestHooks(PreObjectRequestContext(
      agentName = config.name,
      model = config.model,
      request = request,
      metadata = metadata,
      targetType = targetType,
      timestamp = requestTimestamp
    ))

    val result = config.provider.generateObject(request)

    // Execute post-object-response hooks
    config.hooks.executePostObjectResponseHooks(PostObjectResponseContext(
      agentName = config.name,
      model = config.model,
      request = request,
      response = result,
      metadata = metadata,
      targetType = targetType,
      requestTimestamp = requestTimestamp
    ))

    result match {
      case Right(jsonResponse) =>
        try {
          val typedResponse = ObjectResponse[T](
            data = read[T](jsonResponse.data),
            usage = jsonResponse.usage,
            model = jsonResponse.model,
            finishReason = jsonResponse.finishReason
          )

          // Add assistant response to scoped conversation history (convert object to string representation)
          val assistantMsg = ChatMessage(Role.Assistant, jsonResponse.data.toString())
          currentHistory = currentHistory :+ assistantMsg
          updateHistory(metadata, currentHistory)

          logger.info(s"Agent '${config.name}' generated structured object")
          Right(typedResponse)
        } catch {
          case ex: ujson.ParsingFailedException =>
            val error = ChezError.SchemaConversionError(
              s"Failed to parse response as expected type: ${ex.getMessage}",
              targetType
            )
            // Execute error hooks for parsing failure
            config.hooks.executeErrorHooks(ErrorContext(
              agentName = config.name,
              model = config.model,
              error = error,
              metadata = metadata,
              operation = "generateObject",
              request = Some(Right(request))
            ))
            logger.error(s"Agent '${config.name}' failed to parse structured object", ex)
            Left(error)
          case ex: Exception =>
            val error = ChezError.SchemaConversionError(
              s"Unexpected error during type conversion: ${ex.getMessage}",
              targetType
            )
            // Execute error hooks for unexpected error
            config.hooks.executeErrorHooks(ErrorContext(
              agentName = config.name,
              model = config.model,
              error = error,
              metadata = metadata,
              operation = "generateObject",
              request = Some(Right(request))
            ))
            logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
            Left(error)
        }
      case Left(error) =>
        // Execute error hooks for provider error
        config.hooks.executeErrorHooks(ErrorContext(
          agentName = config.name,
          model = config.model,
          error = error,
          metadata = metadata,
          operation = "generateObject",
          request = Some(Right(request))
        ))
        logger.error(s"Agent '${config.name}' failed to generate structured object: $error")
        Left(error)
    }
  }

  def generateObjectWithoutHistory[T: Schema: Reader: scala.reflect.ClassTag](
      userChatMessage: String,
      metadata: RequestMetadata
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
    val targetType = scala.reflect.classTag[T].runtimeClass.getSimpleName

    val request = ObjectRequest(
      messages = messages,
      model = config.model,
      schema = schema,
      temperature = config.temperature,
      maxTokens = config.maxTokens,
      stream = false,
      metadata = Some(metadata)
    )

    // Execute pre-object-request hooks
    val requestTimestamp = System.currentTimeMillis()
    config.hooks.executePreObjectRequestHooks(PreObjectRequestContext(
      agentName = config.name,
      model = config.model,
      request = request,
      metadata = metadata,
      targetType = targetType,
      timestamp = requestTimestamp
    ))

    val result = config.provider.generateObject(request)

    // Execute post-object-response hooks
    config.hooks.executePostObjectResponseHooks(PostObjectResponseContext(
      agentName = config.name,
      model = config.model,
      request = request,
      response = result,
      metadata = metadata,
      targetType = targetType,
      requestTimestamp = requestTimestamp
    ))

    result match {
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
              targetType
            )
            // Execute error hooks for parsing failure
            config.hooks.executeErrorHooks(ErrorContext(
              agentName = config.name,
              model = config.model,
              error = error,
              metadata = metadata,
              operation = "generateObjectWithoutHistory",
              request = Some(Right(request))
            ))
            logger.error(s"Agent '${config.name}' failed to parse structured object", ex)
            Left(error)
          case ex: Exception =>
            val error = ChezError.SchemaConversionError(
              s"Unexpected error during type conversion: ${ex.getMessage}",
              targetType
            )
            // Execute error hooks for unexpected error
            config.hooks.executeErrorHooks(ErrorContext(
              agentName = config.name,
              model = config.model,
              error = error,
              metadata = metadata,
              operation = "generateObjectWithoutHistory",
              request = Some(Right(request))
            ))
            logger.error(s"Agent '${config.name}' encountered unexpected error", ex)
            Left(error)
        }
      case Left(error) =>
        // Execute error hooks for provider error
        config.hooks.executeErrorHooks(ErrorContext(
          agentName = config.name,
          model = config.model,
          error = error,
          metadata = metadata,
          operation = "generateObjectWithoutHistory",
          request = Some(Right(request))
        ))
        logger.error(
          s"Agent '${config.name}' failed to generate structured object without history: $error"
        )
        Left(error)
    }
  }

  def getConversationHistory(metadata: RequestMetadata): List[ChatMessage] =
    getHistory(metadata).toList

  def clearHistory(metadata: RequestMetadata): Unit = {
    logger.info(s"Agent '${config.name}' clearing conversation history")
    val scopeKey = createScopeKey(metadata)
    scopedHistories =
      scopedHistories.updated(scopeKey, Vector(ChatMessage(Role.System, config.instructions)))

    // Execute history hooks
    config.hooks.executeHistoryHooks(HistoryContext(
      agentName = config.name,
      operation = HistoryOperation.Clear,
      metadata = metadata,
      message = None,
      historySize = 1 // Only system message remains
    ))
  }

  def clearAllHistories(): Unit = {
    logger.info(s"Agent '${config.name}' clearing all conversation histories")
    scopedHistories = Map.empty

    // Execute history hooks (use empty metadata for clearAll operation)
    config.hooks.executeHistoryHooks(HistoryContext(
      agentName = config.name,
      operation = HistoryOperation.ClearAll,
      metadata = RequestMetadata(), // Empty metadata for clearAll
      message = None,
      historySize = 0
    ))
  }

  def addChatMessage(message: ChatMessage, metadata: RequestMetadata): Unit = {
    val currentHistory = getHistory(metadata)
    val newHistory = currentHistory :+ message
    updateHistory(metadata, newHistory)

    // Execute history hooks
    config.hooks.executeHistoryHooks(HistoryContext(
      agentName = config.name,
      operation = HistoryOperation.Add,
      metadata = metadata,
      message = Some(message),
      historySize = newHistory.size
    ))
  }

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
      maxTokens: Option[Int] = None,
      hooks: HookRegistry = HookRegistry.empty,
      idGenerator: () => String = () => UUID.randomUUID().toString
  ): Agent = {
    val generatedId = idGenerator()
    val config = AgentConfig(generatedId, name, instructions, provider, model, temperature, maxTokens, hooks, idGenerator)
    new Agent(config)
  }
