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

class AnthropicProvider(protected val apiKey: String) extends BaseLLMProvider:

  override val name: String = "Anthropic"
  override protected val baseUrl: String = "https://api.anthropic.com/v1"

  override val supportedModels: List[String] = List(
    "claude-3-5-sonnet-20241022",
    "claude-3-5-haiku-20241022",
    "claude-3-opus-20240229",
    "claude-3-sonnet-20240229",
    "claude-3-haiku-20240307"
  )

  override protected def buildHeaders(apiKey: String): Map[String, String] = {
    Map(
      "x-api-key" -> apiKey,
      "Content-Type" -> "application/json",
      "anthropic-version" -> "2023-06-01"
    )
  }

  override protected def buildRequestBody(request: ChatRequest): ujson.Value = {
    // Anthropic separates system message from user/assistant messages
    val (systemChatMessage, conversationChatMessages) =
      request.messages.partition(_.role == Role.System)

    val messages = ujson.Arr(
      conversationChatMessages.map(msg => {
        ujson.Obj(
          "role" -> (msg.role match
            case Role.User => "user"
            case Role.Assistant => "assistant"
            case Role.System =>
              throw new IllegalStateException("System messages should be filtered out")),
          "content" -> msg.content
        )
      })*
    )

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "max_tokens" -> request.maxTokens.getOrElse(1024) // Anthropic requires max_tokens
    )

    // Add system message if present
    systemChatMessage.headOption.foreach(sys => baseObj("system") = sys.content)

    request.temperature.foreach(temp => baseObj("temperature") = temp)

    baseObj
  }

  override protected def parseResponse(responseBody: String): Either[ChezError, ChatResponse] = {
    try {
      // First try to parse as successful response (Anthropic returns OpenAI-compatible format)
      try {
        val openAIResponse = read[OpenAIResponse](responseBody)
        Right(openAIResponse.toChatResponse)
      } catch {
        case _: upickle.core.AbortException | _: ujson.ParsingFailedException =>
          // If that fails, try to parse as error response
          try {
            val errorResponse = read[ErrorResponse](responseBody)
            Left(ChezError.ApiError(
              message = errorResponse.error.message,
              code = errorResponse.error.code.orElse(errorResponse.error.`type`),
              statusCode = None
            ))
          } catch {
            case _: Exception =>
              Left(ChezError.ParseError(s"Failed to parse Anthropic response: $responseBody"))
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(ChezError.ParseError(s"Failed to parse Anthropic response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Left(
          ChezError.ParseError(s"Missing required field in Anthropic response: ${ex.getMessage}")
        )
      case ex: Exception =>
        Left(ChezError.ParseError(s"Unexpected error parsing Anthropic response: ${ex.getMessage}"))
    }
  }

  override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value = {
    // Anthropic separates system message from user/assistant messages
    val (systemChatMessage, conversationChatMessages) =
      request.messages.partition(_.role == Role.System)

    val messages = ujson.Arr(
      conversationChatMessages.map(msg => {
        ujson.Obj(
          "role" -> (msg.role match
            case Role.User => "user"
            case Role.Assistant => "assistant"
            case Role.System =>
              throw new IllegalStateException("System messages should be filtered out")),
          "content" -> msg.content
        )
      })*
    )

    // Add schema-based tool for structured output
    val tool = ujson.Obj(
      "name" -> "structured_response",
      "description" -> request.schema.description.getOrElse(
        "Generate a structured response according to the provided schema"
      ),
      "input_schema" -> request.schema.toJsonSchema
    )

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "max_tokens" -> request.maxTokens.getOrElse(1024),
      "tools" -> ujson.Arr(tool),
      "tool_choice" -> ujson.Obj("type" -> "tool", "name" -> "structured_response")
    )

    // Add system message if present
    systemChatMessage.headOption.foreach(sys => baseObj("system") = sys.content)

    request.temperature.foreach(temp => baseObj("temperature") = temp)

    baseObj
  }

  override protected def parseObjectResponse(responseBody: String)
      : Either[ChezError, ObjectResponse[ujson.Value]] = {
    try {
      // First try to parse as successful response (Anthropic returns OpenAI-compatible format)
      try {
        val openAIResponse = read[OpenAIResponse](responseBody)
        Right(openAIResponse.toObjectResponse)
      } catch {
        case _: upickle.core.AbortException | _: ujson.ParsingFailedException =>
          // If that fails, try to parse as error response
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
                ChezError.ParseError(s"Failed to parse Anthropic object response: $responseBody")
              )
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(ChezError.ParseError(s"Failed to parse Anthropic object response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(
          s"Missing required field in Anthropic object response: ${ex.getMessage}"
        ))
      case ex: Exception =>
        Left(ChezError.ParseError(
          s"Unexpected error parsing Anthropic object response: ${ex.getMessage}"
        ))
    }
  }
