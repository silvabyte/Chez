package chezwiz.agent.providers

import upickle.default.{read, write}
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

class OpenAIProvider(protected val apiKey: String) extends BaseLLMProvider:

  override val name: String = "OpenAI"
  override protected val baseUrl: String = "https://api.openai.com/v1"

  override val supportedModels: List[String] = List(
    "gpt-4o",
    "gpt-4o-mini",
    "gpt-4-turbo",
    "gpt-4",
    "gpt-3.5-turbo"
  )

  override protected def buildHeaders(apiKey: String): Map[String, String] = {
    Map(
      "Authorization" -> s"Bearer $apiKey",
      "Content-Type" -> "application/json"
    )
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
    try {
      // First try to parse as successful response
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
              Left(ChezError.ParseError(s"Failed to parse OpenAI response: $responseBody"))
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(ChezError.ParseError(s"Failed to parse OpenAI response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(s"Missing required field in OpenAI response: ${ex.getMessage}"))
      case ex: Exception =>
        Left(ChezError.ParseError(s"Unexpected error parsing OpenAI response: ${ex.getMessage}"))
    }
  }

  // Helper method to ensure schema is OpenAI compliant
  private def ensureOpenAICompliantSchema(schema: ujson.Value): ujson.Value = {
    schema match
      case obj: ujson.Obj =>
        val newObj = obj.copy()

        // For object types, ensure OpenAI strict mode compliance
        if (obj.obj.get("type").exists(_.str == "object")) {
          // Add additionalProperties: false if not present
          if (!obj.obj.contains("additionalProperties")) {
            newObj("additionalProperties") = false
          }

          // Ensure all properties are required for strict mode
          obj.obj.get("properties") match {
            case Some(propsObj: ujson.Obj) =>
              val allPropertyKeys = propsObj.obj.keys.toList.sorted
              // If there's no required array or it doesn't contain all properties, update it
              val currentRequired = obj.obj.get("required") match {
                case Some(arr: ujson.Arr) => arr.arr.map(_.str).toSet
                case _ => Set.empty[String]
              }

              if (currentRequired != allPropertyKeys.toSet) {
                newObj("required") = ujson.Arr(allPropertyKeys.map(ujson.Str(_))*)
              }
            case _ => // No properties, no required array needed
          }
        }

        // Recursively process nested objects
        obj.obj.foreach { case (key, value) =>
          key match
            case "properties" =>
              value match
                case propsObj: ujson.Obj =>
                  val newProps = ujson.Obj()
                  propsObj.obj.foreach { case (propKey, propValue) =>
                    newProps(propKey) = ensureOpenAICompliantSchema(propValue)
                  }
                  newObj("properties") = newProps
                case _ => // Keep as is
            case "items" =>
              newObj("items") = ensureOpenAICompliantSchema(value)
            case _ => // Keep as is
        }
        newObj
      case _ => schema
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

    // Ensure the schema is OpenAI compliant
    val compliantSchema = ensureOpenAICompliantSchema(request.schema.toJsonSchema)

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "stream" -> request.stream,
      "response_format" -> ujson.Obj(
        "type" -> "json_schema",
        "json_schema" -> ujson.Obj(
          "name" -> "structured_response",
          "schema" -> compliantSchema,
          "strict" -> true
        )
      )
    )

    request.temperature.foreach(temp => baseObj("temperature") = temp)
    request.maxTokens.foreach(tokens => baseObj("max_tokens") = tokens)

    baseObj
  }

  override protected def parseObjectResponse(responseBody: String)
      : Either[ChezError, ObjectResponse[ujson.Value]] = {
    try {
      // First try to parse as successful response
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
              Left(ChezError.ParseError(s"Failed to parse OpenAI object response: $responseBody"))
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(ChezError.ParseError(s"Failed to parse OpenAI response body: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(s"Missing required field in OpenAI response: ${ex.getMessage}"))
      case ex: Exception =>
        Left(ChezError.ParseError(s"Unexpected error in parseObjectResponse: ${ex.getMessage}"))
    }
  }
