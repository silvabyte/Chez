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

class CustomEndpointProvider(
    protected val apiKey: String = "",
    override protected val baseUrl: String,
    override val supportedModels: List[String] = List.empty,
    val useOpenAIFormat: Boolean = true,
    val requiresAuthentication: Boolean = true,
    val customHeaders: Map[String, String] = Map.empty,
    override val httpVersion: HttpVersion = HttpVersion.Http2,
    val useJsonSchemaFormat: Boolean = false // Use json_schema format for structured output
) extends BaseLLMProvider:

  override val name: String = s"CustomEndpoint($baseUrl)"

  override protected def buildHeaders(apiKey: String): Map[String, String] = {
    val baseHeaders = Map("Content-Type" -> "application/json")

    val authHeaders = if (requiresAuthentication && apiKey.nonEmpty) {
      Map("Authorization" -> s"Bearer $apiKey")
    } else {
      Map.empty
    }

    baseHeaders ++ authHeaders ++ customHeaders
  }

  override protected def buildRequestBody(request: ChatRequest): ujson.Value = {
    // TODO: use case class and pattern match vs this nyucky amatuer hour stuff
    if (useOpenAIFormat) {
      buildOpenAICompatibleRequestBody(request)
    } else {
      throw new NotImplementedError(
        "Custom request format not yet implemented. Use useOpenAIFormat=true"
      )
    }
  }

  private def buildOpenAICompatibleRequestBody(request: ChatRequest): ujson.Value = {
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

  override protected def parseResponse(responseBody: String): Either[ChezError, ChatResponse] = {
    if (useOpenAIFormat) {
      parseOpenAICompatibleResponse(responseBody)
    } else {
      throw new NotImplementedError(
        "Custom response format not yet implemented. Use useOpenAIFormat=true"
      )
    }
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
    if (useOpenAIFormat) {
      buildOpenAICompatibleObjectRequestBody(request)
    } else {
      throw new NotImplementedError(
        "Custom object request format not yet implemented. Use useOpenAIFormat=true"
      )
    }
  }

  private def buildOpenAICompatibleObjectRequestBody(request: ObjectRequest): ujson.Value = {
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

    // Add response_format for structured output
    if (useJsonSchemaFormat) {
      // LM Studio and other providers that support json_schema format
      baseObj("response_format") = ujson.Obj(
        "type" -> "json_schema",
        "json_schema" -> ujson.Obj(
          "name" -> "structured_output",
          "strict" -> "true",
          "schema" -> request.schema.toJsonSchema
        )
      )
    } else {
      // Standard OpenAI json_object format
      baseObj("response_format") = ujson.Obj(
        "type" -> "json_object"
      )
    }

    request.temperature.foreach(temp => baseObj("temperature") = temp)
    request.maxTokens.foreach(tokens => baseObj("max_tokens") = tokens)

    baseObj
  }

  override protected def parseObjectResponse(responseBody: String)
      : Either[ChezError, ObjectResponse[ujson.Value]] = {
    if (useOpenAIFormat) {
      parseOpenAICompatibleObjectResponse(responseBody)
    } else {
      throw new NotImplementedError(
        "Custom object response format not yet implemented. Use useOpenAIFormat=true"
      )
    }
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
  def forLMStudio(
      baseUrl: String,
      modelId: String = "local-model",
      httpVersion: HttpVersion = HttpVersion.Http11
  ): CustomEndpointProvider = {
    new CustomEndpointProvider(
      apiKey = "",
      baseUrl = baseUrl,
      supportedModels = List(modelId),
      useOpenAIFormat = true,
      requiresAuthentication = false,
      customHeaders = Map.empty,
      httpVersion = httpVersion,
      useJsonSchemaFormat = true // LM Studio supports json_schema format
    )
  }

  def forOpenAICompatible(
      baseUrl: String,
      apiKey: String,
      supportedModels: List[String] = List.empty,
      customHeaders: Map[String, String] = Map.empty,
      httpVersion: HttpVersion = HttpVersion.Http2,
      useJsonSchemaFormat: Boolean = false
  ): CustomEndpointProvider = {
    new CustomEndpointProvider(
      apiKey = apiKey,
      baseUrl = baseUrl,
      supportedModels = supportedModels,
      useOpenAIFormat = true,
      requiresAuthentication = true,
      customHeaders = customHeaders,
      httpVersion = httpVersion,
      useJsonSchemaFormat = useJsonSchemaFormat
    )
  }
