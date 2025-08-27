package chezwiz.agent

import scribe.Logging
import upickle.default.Reader
import upickle.default.{read, write}
import chezwiz.agent.providers.LLMProvider
import chezwiz.agent.{
  ChatRequest,
  ChatResponse,
  ObjectRequest,
  ObjectResponse,
  ChatMessage,
  Role,
  ChezError,
  RequestMetadata,
  EmbeddingRequest,
  EmbeddingResponse,
  EmbeddingInput
}
import ujson.Value

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
  // scalafix:off DisableSyntax.var
  // Disabling because mutable state is required to maintain conversation history across
  // multiple chat interactions - each scope needs its own conversation history that evolves over time
  @volatile private var _scopedHistories: Map[String, Vector[ChatMessage]] = Map.empty
  // scalafix:on DisableSyntax.var

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
    _scopedHistories.getOrElse(scopeKey, Vector(ChatMessage.text(Role.System, config.instructions)))
  }

  private def updateHistory(
      metadata: RequestMetadata,
      newHistory: Vector[ChatMessage]
  ): Unit = {
    val scopeKey = createScopeKey(metadata)
    _scopedHistories = _scopedHistories.updated(scopeKey, newHistory)
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
    val initialHistory = getHistory(metadata)
    val userMsg = ChatMessage.text(Role.User, userChatMessage)
    val currentHistory = initialHistory :+ userMsg

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
        val assistantMsg = ChatMessage.text(Role.Assistant, response.content)
        val updatedHistory = currentHistory :+ assistantMsg
        updateHistory(metadata, updatedHistory)
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
      ChatMessage.text(Role.System, config.instructions),
      ChatMessage.text(Role.User, userChatMessage)
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
    val initialHistory = getHistory(metadata)
    val userMsg = ChatMessage.text(Role.User, userChatMessage)
    val currentHistory = initialHistory :+ userMsg

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
          val assistantMsg = ChatMessage.text(Role.Assistant, jsonResponse.data.toString())
          val updatedHistory = currentHistory :+ assistantMsg
          updateHistory(metadata, updatedHistory)

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
      ChatMessage.text(Role.System, config.instructions),
      ChatMessage.text(Role.User, userChatMessage)
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
    _scopedHistories =
      _scopedHistories.updated(scopeKey, Vector(ChatMessage.text(Role.System, config.instructions)))

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
    _scopedHistories = Map.empty

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

  def generateWithMessages(
      messages: List[ChatMessage],
      metadata: RequestMetadata
  ): Either[ChezError, ChatResponse] = {
    logger.info(s"Agent '${config.name}' generating with custom messages")

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
        logger.info(s"Agent '${config.name}' generated response with custom messages")
        Right(response)
      case Left(error) =>
        // Execute error hooks
        config.hooks.executeErrorHooks(ErrorContext(
          agentName = config.name,
          model = config.model,
          error = error,
          metadata = metadata,
          operation = "generateWithMessages",
          request = Some(Left(request))
        ))
        logger.error(s"Agent '${config.name}' failed to generate with custom messages: $error")
        Left(error)
    }
  }

  // Embedding methods
  def generateEmbedding(
      text: String,
      model: Option[String] = None,
      metadata: RequestMetadata = RequestMetadata()
  ): Either[ChezError, EmbeddingResponse] = {
    if (!config.provider.supportsEmbeddings) {
      return Left(ChezError.ConfigurationError(
        s"Provider ${config.provider.name} does not support embeddings"
      ))
    }

    val embeddingModel = model.getOrElse(config.model)

    logger.info(s"Agent '${config.name}' generating embedding for text with model: $embeddingModel")

    val request = EmbeddingRequest(
      input = EmbeddingInput.Single(text),
      model = embeddingModel,
      metadata = Some(metadata)
    )

    // Execute pre-embedding hooks
    val requestTimestamp = System.currentTimeMillis()
    config.hooks.executePreEmbeddingHooks(PreEmbeddingContext(
      agentName = config.name,
      model = embeddingModel,
      request = request,
      metadata = metadata,
      timestamp = requestTimestamp
    ))

    val result = config.provider.embed(request)

    // Execute post-embedding hooks
    config.hooks.executePostEmbeddingHooks(PostEmbeddingContext(
      agentName = config.name,
      model = embeddingModel,
      request = request,
      response = result,
      metadata = metadata,
      requestTimestamp = requestTimestamp
    ))

    result match {
      case Right(response) =>
        logger.info(
          s"Agent '${config.name}' successfully generated embedding (dimensions: ${response.dimensions})"
        )
        Right(response)
      case Left(error) =>
        // Execute error hooks
        config.hooks.executeErrorHooks(ErrorContext(
          agentName = config.name,
          model = embeddingModel,
          error = error,
          metadata = metadata,
          operation = "generateEmbedding",
          request = None
        ))
        logger.error(s"Agent '${config.name}' failed to generate embedding: $error")
        Left(error)
    }
  }

  def generateEmbeddings(
      texts: List[String],
      model: Option[String] = None,
      metadata: RequestMetadata = RequestMetadata()
  ): Either[ChezError, EmbeddingResponse] = {
    if (!config.provider.supportsEmbeddings) {
      return Left(ChezError.ConfigurationError(
        s"Provider ${config.provider.name} does not support embeddings"
      ))
    }

    val embeddingModel = model.getOrElse(config.model)

    logger.info(
      s"Agent '${config.name}' generating embeddings for ${texts.size} texts with model: $embeddingModel"
    )

    val request = EmbeddingRequest(
      input = EmbeddingInput.Multiple(texts),
      model = embeddingModel,
      metadata = Some(metadata)
    )

    // Execute pre-embedding hooks
    val requestTimestamp = System.currentTimeMillis()
    config.hooks.executePreEmbeddingHooks(PreEmbeddingContext(
      agentName = config.name,
      model = embeddingModel,
      request = request,
      metadata = metadata,
      timestamp = requestTimestamp
    ))

    val result = config.provider.embed(request)

    // Execute post-embedding hooks
    config.hooks.executePostEmbeddingHooks(PostEmbeddingContext(
      agentName = config.name,
      model = embeddingModel,
      request = request,
      response = result,
      metadata = metadata,
      requestTimestamp = requestTimestamp
    ))

    result match {
      case Right(response) =>
        logger.info(
          s"Agent '${config.name}' successfully generated ${response.embeddings.size} embeddings (dimensions: ${response.dimensions})"
        )
        Right(response)
      case Left(error) =>
        // Execute error hooks
        config.hooks.executeErrorHooks(ErrorContext(
          agentName = config.name,
          model = embeddingModel,
          error = error,
          metadata = metadata,
          operation = "generateEmbeddings",
          request = None
        ))
        logger.error(s"Agent '${config.name}' failed to generate embeddings: $error")
        Left(error)
    }
  }

  // Utility method for cosine similarity
  def cosineSimilarity(embedding1: Vector[Float], embedding2: Vector[Float]): Float = {
    require(embedding1.size == embedding2.size, "Embeddings must have the same dimensions")

    val dotProduct = embedding1.zip(embedding2).map { case (a, b) => a * b }.sum
    val norm1 = math.sqrt(embedding1.map(x => x * x).sum).toFloat
    val norm2 = math.sqrt(embedding2.map(x => x * x).sum).toFloat

    if (norm1 == 0 || norm2 == 0) 0.0f
    else dotProduct / (norm1 * norm2)
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
    val config = AgentConfig(
      generatedId,
      name,
      instructions,
      provider,
      model,
      temperature,
      maxTokens,
      hooks,
      idGenerator
    )
    new Agent(config)
  }
