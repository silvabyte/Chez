package chezwiz.agent.providers

import upickle.default.*
import chezwiz.agent.{
  ChatRequest,
  ChatResponse,
  ObjectRequest,
  ObjectResponse,
  Role,
  Usage,
  ChezError,
  MessageContent,
  MessageContentPart,
  EmbeddingRequest,
  EmbeddingResponse,
  Embedding,
  EmbeddingInput
}

/**
 * Unified provider for OpenAI-compatible endpoints.
 * Consolidates the functionality of LMStudioProvider and CustomEndpointProvider.
 *
 * @param baseUrl The base URL of the API endpoint
 * @param apiKey API key for authentication (empty string for no auth)
 * @param modelId The default model ID to use
 * @param supportedModels List of supported models (empty = allow any)
 * @param customHeaders Additional headers to include in requests
 * @param enableEmbeddings Whether this endpoint supports embeddings
 * @param strictModelValidation Whether to enforce supported model validation
 * @param httpVersion HTTP protocol version to use
 * @param timeouts Connection and request timeout settings
 */
class OpenAICompatibleProvider(
    override protected val baseUrl: String,
    protected val apiKey: String = "",
    val modelId: String = "local-model",
    override val supportedModels: List[String] = List.empty,
    val customHeaders: Map[String, String] = Map.empty,
    val enableEmbeddings: Boolean = false,
    val strictModelValidation: Boolean = true,
    override val httpVersion: HttpVersion = HttpVersion.Http2,
    override val timeouts: ProviderTimeouts = ProviderTimeouts()
) extends BaseLLMProvider:

  override val name: String = s"OpenAICompatible($baseUrl)"

  override protected def buildHeaders(apiKey: String): Map[String, String] = {
    val baseHeaders = Map("Content-Type" -> "application/json")

    val authHeaders = if (apiKey.nonEmpty) {
      Map("Authorization" -> s"Bearer $apiKey")
    } else {
      Map.empty
    }

    baseHeaders ++ authHeaders ++ customHeaders
  }

  override protected def buildRequestBody(request: ChatRequest): ujson.Value = {
    val messages = ujson.Arr(
      request.messages.map(msg => {
        val roleStr = msg.role match
          case Role.System => "system"
          case Role.User => "user"
          case Role.Assistant => "assistant"

        val contentValue = msg.content match {
          case MessageContent.Text(text) => ujson.Str(text)
          case MessageContent.ImageUrl(url, detail) =>
            ujson.Arr(
              ujson.Obj("type" -> "text", "text" -> ""),
              ujson.Obj(
                "type" -> "image_url",
                "image_url" -> ujson.Obj(
                  "url" -> url,
                  "detail" -> detail
                )
              )
            )
          case MessageContent.MultiModal(parts) =>
            ujson.Arr(
              parts.map {
                case MessageContentPart.TextPart(_, text) =>
                  ujson.Obj("type" -> "text", "text" -> text)
                case MessageContentPart.ImageUrlPart(_, imageUrl) =>
                  ujson.Obj(
                    "type" -> "image_url",
                    "image_url" -> ujson.Obj(
                      "url" -> imageUrl.url,
                      "detail" -> imageUrl.detail
                    )
                  )
              }*
            )
        }

        ujson.Obj(
          "role" -> roleStr,
          "content" -> contentValue
        )
      })*
    )

    val model = if request.model.isEmpty then modelId else request.model

    val baseObj = ujson.Obj(
      "model" -> model,
      "messages" -> messages,
      "stream" -> request.stream
    )

    request.temperature.foreach(temp => baseObj("temperature") = temp)
    request.maxTokens.foreach(tokens => baseObj("max_tokens") = tokens)

    baseObj
  }

  override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value = {
    val messages = ujson.Arr(
      request.messages.map(msg => {
        val roleStr = msg.role match
          case Role.System => "system"
          case Role.User => "user"
          case Role.Assistant => "assistant"

        val contentValue = msg.content match {
          case MessageContent.Text(text) => ujson.Str(text)
          case MessageContent.ImageUrl(url, detail) =>
            ujson.Arr(
              ujson.Obj("type" -> "text", "text" -> ""),
              ujson.Obj(
                "type" -> "image_url",
                "image_url" -> ujson.Obj(
                  "url" -> url,
                  "detail" -> detail
                )
              )
            )
          case MessageContent.MultiModal(parts) =>
            ujson.Arr(
              parts.map {
                case MessageContentPart.TextPart(_, text) =>
                  ujson.Obj("type" -> "text", "text" -> text)
                case MessageContentPart.ImageUrlPart(_, imageUrl) =>
                  ujson.Obj(
                    "type" -> "image_url",
                    "image_url" -> ujson.Obj(
                      "url" -> imageUrl.url,
                      "detail" -> imageUrl.detail
                    )
                  )
              }*
            )
        }

        ujson.Obj(
          "role" -> roleStr,
          "content" -> contentValue
        )
      })*
    )

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "stream" -> request.stream
    )

    // Standard json_schema format for structured output
    baseObj("response_format") = ujson.Obj(
      "type" -> "json_schema",
      "json_schema" -> ujson.Obj(
        "name" -> "structured_output",
        "strict" -> "true",
        "schema" -> request.schema.toJsonSchema
      )
    )

    request.temperature.foreach(temp => baseObj("temperature") = temp)
    request.maxTokens.foreach(tokens => baseObj("max_tokens") = tokens)

    baseObj
  }

  override protected def parseResponse(responseBody: String): Either[ChezError, ChatResponse] = {
    try {
      try {
        val openAIResponse = read[OpenAIResponse](responseBody)
        Right(openAIResponse.toChatResponse)
      } catch {
        case _: upickle.core.AbortException | _: ujson.ParsingFailedException =>
          try {
            val errorResponse = read[ErrorResponse](responseBody)
            Left(ChezError.ApiError(
              message = errorResponse.error.message,
              code = errorResponse.error.code.orElse(errorResponse.error.`type`),
              statusCode = None
            ))
          } catch {
            case _: Exception =>
              Left(
                ChezError.ParseError(s"Failed to parse OpenAI-compatible response: $responseBody")
              )
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(ChezError.ParseError(s"Failed to parse response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(
          s"Missing required field in response: ${ex.getMessage}"
        ))
      case ex: Exception =>
        Left(ChezError.ParseError(
          s"Unexpected error parsing response: ${ex.getMessage}"
        ))
    }
  }

  override protected def parseObjectResponse(responseBody: String)
      : Either[ChezError, ObjectResponse[ujson.Value]] = {
    try {
      try {
        val openAIResponse = read[OpenAIResponse](responseBody)
        Right(openAIResponse.toObjectResponse)
      } catch {
        case _: upickle.core.AbortException | _: ujson.ParsingFailedException =>
          try {
            val errorResponse = read[ErrorResponse](responseBody)
            Left(ChezError.ApiError(
              message = errorResponse.error.message,
              code = errorResponse.error.code.orElse(errorResponse.error.`type`),
              statusCode = None
            ))
          } catch {
            case _: Exception =>
              Left(ChezError.ParseError(
                s"Failed to parse OpenAI-compatible object response: $responseBody"
              ))
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(
          ChezError.ParseError(s"Failed to parse response body: ${ex.getMessage}")
        )
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(
          s"Missing required field in response: ${ex.getMessage}"
        ))
      case ex: Exception =>
        Left(ChezError.ParseError(s"Unexpected error in parseObjectResponse: ${ex.getMessage}"))
    }
  }

  override def validateModel(model: String): Either[ChezError.ModelNotSupported, Unit] = {
    if (!strictModelValidation) {
      Right(())
    } else if (supportedModels.isEmpty || supportedModels.contains(model)) {
      Right(())
    } else {
      Left(ChezError.ModelNotSupported(model, name, supportedModels))
    }
  }

  // Conditional embedding support
  override val supportsEmbeddings: Boolean = enableEmbeddings
  override val supportedEmbeddingModels: List[String] =
    if (enableEmbeddings) List("*") else List.empty

  override def embed(request: EmbeddingRequest): Either[ChezError, EmbeddingResponse] = {
    if (!enableEmbeddings) {
      return Left(ChezError.ConfigurationError(
        s"Provider $name does not support embeddings"
      ))
    }

    val url = s"$baseUrl/embeddings"
    val body = buildEmbeddingRequestBody(request)

    for {
      responseText <- makeRequest(url, buildHeaders(apiKey), body)
      response <- parseEmbeddingResponse(responseText)
    } yield response
  }

  private def buildEmbeddingRequestBody(request: EmbeddingRequest): ujson.Value = {
    val baseObj = ujson.Obj(
      "model" -> request.model,
      "input" -> (request.input match {
        case EmbeddingInput.Single(text) => ujson.Str(text)
        case EmbeddingInput.Multiple(texts) => ujson.Arr(texts.map(ujson.Str(_))*)
      })
    )

    request.dimensions.foreach(d => baseObj("dimensions") = d)
    if (request.encodingFormat != "float") {
      baseObj("encoding_format") = request.encodingFormat
    }

    baseObj
  }

  private def parseEmbeddingResponse(responseBody: String): Either[ChezError, EmbeddingResponse] = {
    try {
      val json = ujson.read(responseBody)

      // Parse the OpenAI-compatible embedding response format
      val data = json("data").arr
      val embeddings = data.zipWithIndex.map { case (item, idx) =>
        val embeddingArray = item("embedding").arr.map(_.num.toFloat).toVector
        Embedding(
          values = embeddingArray,
          index = item.obj.get("index").map(_.num.toInt).getOrElse(idx)
        )
      }.toList

      val usage = json.obj.get("usage").map { u =>
        Usage(
          promptTokens = u("prompt_tokens").num.toInt,
          completionTokens = u.obj.get("completion_tokens").map(_.num.toInt).getOrElse(0),
          totalTokens = u("total_tokens").num.toInt
        )
      }

      val model = json("model").str
      val dimensions = embeddings.headOption.map(_.values.size).getOrElse(0)

      Right(EmbeddingResponse(
        embeddings = embeddings,
        usage = usage,
        model = model,
        dimensions = dimensions
      ))
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(
          ChezError.ParseError(s"Failed to parse embedding response: ${ex.getMessage}")
        )
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(
          s"Missing required field in embedding response: ${ex.getMessage}"
        ))
      case ex: Exception =>
        Left(ChezError.ParseError(
          s"Unexpected error parsing embedding response: ${ex.getMessage}"
        ))
    }
  }

object OpenAICompatibleProvider:
  /**
   * Create a provider for any OpenAI-compatible endpoint
   */
  def apply(
      baseUrl: String,
      apiKey: String = "",
      modelId: String = "local-model",
      supportedModels: List[String] = List.empty,
      customHeaders: Map[String, String] = Map.empty,
      enableEmbeddings: Boolean = false,
      strictModelValidation: Boolean = true,
      httpVersion: HttpVersion = HttpVersion.Http2,
      timeouts: ProviderTimeouts = ProviderTimeouts()
  ): OpenAICompatibleProvider = {
    new OpenAICompatibleProvider(
      baseUrl = baseUrl,
      apiKey = apiKey,
      modelId = modelId,
      supportedModels = supportedModels,
      customHeaders = customHeaders,
      enableEmbeddings = enableEmbeddings,
      strictModelValidation = strictModelValidation,
      httpVersion = httpVersion,
      timeouts = timeouts
    )
  }
