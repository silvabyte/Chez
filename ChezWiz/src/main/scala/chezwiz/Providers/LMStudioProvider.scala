package chezwiz.agent.providers

import upickle.default.*
import chezwiz.agent.{
  ChatRequest,
  ChatResponse,
  ObjectRequest,
  ObjectResponse,
  Role,
  Usage,
  ChezError
}

/**
 * Provider for LM Studio local LLM server.
 * LM Studio runs OpenAI-compatible API locally without authentication.
 * 
 * @param baseUrl The base URL of the LM Studio server (e.g., "http://localhost:1234/v1")
 * @param modelId The model ID to use (defaults to "local-model")
 */
class LMStudioProvider(
    override protected val baseUrl: String,
    val modelId: String = "local-model"
) extends BaseLLMProvider:

  override val name: String = s"LMStudio($baseUrl)"
  
  // LM Studio doesn't require API key
  protected val apiKey: String = ""
  
  // LM Studio runs locally, so HTTP/1.1 is more reliable
  override val httpVersion: HttpVersion = HttpVersion.Http11
  
  // Allow any model by default, or restrict to the specified model
  override val supportedModels: List[String] = List(modelId)

  override protected def buildHeaders(apiKey: String): Map[String, String] = {
    // LM Studio only needs Content-Type header
    Map("Content-Type" -> "application/json")
  }

  override protected def buildRequestBody(request: ChatRequest): ujson.Value = {
    val messages = ujson.Arr(
      request.messages.map(msg => {
        ujson.Obj(
          "role" -> (msg.role match
            case Role.System => "system"
            case Role.User => "user"
            case Role.Assistant => "assistant"),
          "content" -> msg.content
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

  override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value = {
    val messages = ujson.Arr(
      request.messages.map(msg => {
        ujson.Obj(
          "role" -> (msg.role match
            case Role.System => "system"
            case Role.User => "user"
            case Role.Assistant => "assistant"),
          "content" -> msg.content
        )
      })*
    )

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "stream" -> request.stream
    )

    // LM Studio uses json_schema format for structured output
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
              Left(ChezError.ParseError(s"Failed to parse LM Studio response: $responseBody"))
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(ChezError.ParseError(s"Failed to parse LM Studio response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(
          s"Missing required field in LM Studio response: ${ex.getMessage}"
        ))
      case ex: Exception =>
        Left(ChezError.ParseError(
          s"Unexpected error parsing LM Studio response: ${ex.getMessage}"
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
                s"Failed to parse LM Studio object response: $responseBody"
              ))
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(
          ChezError.ParseError(s"Failed to parse LM Studio response body: ${ex.getMessage}")
        )
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(
          s"Missing required field in LM Studio response: ${ex.getMessage}"
        ))
      case ex: Exception =>
        Left(ChezError.ParseError(s"Unexpected error in parseObjectResponse: ${ex.getMessage}"))
    }
  }

  override def validateModel(model: String): Either[ChezError.ModelNotSupported, Unit] = {
    // Allow any model for LM Studio since users can load different models
    Right(())
  }

object LMStudioProvider:
  /**
   * Create a new LM Studio provider
   * 
   * @param baseUrl The base URL of the LM Studio server (e.g., "http://localhost:1234/v1")
   * @param modelId The model ID to use (defaults to "local-model")
   */
  def apply(
      baseUrl: String = "http://localhost:1234/v1",
      modelId: String = "local-model"
  ): LMStudioProvider = 
    new LMStudioProvider(baseUrl, modelId)