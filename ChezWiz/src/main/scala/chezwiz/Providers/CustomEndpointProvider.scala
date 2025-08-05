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
  MessageContentPart
}

/**
 * Provider for custom OpenAI-compatible endpoints.
 *
 * @param apiKey API key for authentication
 * @param baseUrl Base URL of the API endpoint
 * @param supportedModels List of supported models (empty = allow any)
 * @param customHeaders Additional headers to include in requests
 * @param httpVersion HTTP protocol version to use
 */
class CustomEndpointProvider(
    protected val apiKey: String,
    override protected val baseUrl: String,
    override val supportedModels: List[String] = List.empty,
    val customHeaders: Map[String, String] = Map.empty,
    override val httpVersion: HttpVersion = HttpVersion.Http2
) extends BaseLLMProvider:

  override val name: String = s"CustomEndpoint($baseUrl)"

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
    buildOpenAICompatibleRequestBody(request)
  }

  private def buildOpenAICompatibleRequestBody(request: ChatRequest): ujson.Value = {
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

    request.temperature.foreach(temp => baseObj("temperature") = temp)
    request.maxTokens.foreach(tokens => baseObj("max_tokens") = tokens)

    baseObj
  }

  override protected def parseResponse(responseBody: String): Either[ChezError, ChatResponse] = {
    parseOpenAICompatibleResponse(responseBody)
  }

  private def parseOpenAICompatibleResponse(responseBody: String)
      : Either[ChezError, ChatResponse] = {
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
              Left(ChezError.ParseError(s"Failed to parse custom endpoint response: $responseBody"))
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(ChezError.ParseError(s"Failed to parse custom endpoint response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(
          s"Missing required field in custom endpoint response: ${ex.getMessage}"
        ))
      case ex: Exception =>
        Left(ChezError.ParseError(
          s"Unexpected error parsing custom endpoint response: ${ex.getMessage}"
        ))
    }
  }

  override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value = {
    buildOpenAICompatibleObjectRequestBody(request)
  }

  private def buildOpenAICompatibleObjectRequestBody(request: ObjectRequest): ujson.Value = {
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

    // Standard OpenAI json_object format
    baseObj("response_format") = ujson.Obj(
      "type" -> "json_object"
    )

    request.temperature.foreach(temp => baseObj("temperature") = temp)
    request.maxTokens.foreach(tokens => baseObj("max_tokens") = tokens)

    baseObj
  }

  override protected def parseObjectResponse(responseBody: String)
      : Either[ChezError, ObjectResponse[ujson.Value]] = {
    parseOpenAICompatibleObjectResponse(responseBody)
  }

  private def parseOpenAICompatibleObjectResponse(responseBody: String)
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
                s"Failed to parse custom endpoint object response: $responseBody"
              ))
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(
          ChezError.ParseError(s"Failed to parse custom endpoint response body: ${ex.getMessage}")
        )
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(
          s"Missing required field in custom endpoint response: ${ex.getMessage}"
        ))
      case ex: Exception =>
        Left(ChezError.ParseError(s"Unexpected error in parseObjectResponse: ${ex.getMessage}"))
    }
  }

  override def validateModel(model: String): Either[ChezError.ModelNotSupported, Unit] = {
    if (supportedModels.isEmpty || supportedModels.contains(model)) {
      Right(())
    } else {
      Left(ChezError.ModelNotSupported(model, name, supportedModels))
    }
  }

object CustomEndpointProvider:
  /**
   * Create a provider for any OpenAI-compatible API endpoint
   *
   * @param baseUrl Base URL of the API endpoint
   * @param apiKey API key for authentication
   * @param supportedModels List of supported models (empty = allow any)
   * @param customHeaders Additional headers to include in requests
   * @param httpVersion HTTP protocol version to use
   */
  def apply(
      baseUrl: String,
      apiKey: String,
      supportedModels: List[String] = List.empty,
      customHeaders: Map[String, String] = Map.empty,
      httpVersion: HttpVersion = HttpVersion.Http2
  ): CustomEndpointProvider = {
    new CustomEndpointProvider(
      apiKey = apiKey,
      baseUrl = baseUrl,
      supportedModels = supportedModels,
      customHeaders = customHeaders,
      httpVersion = httpVersion
    )
  }
